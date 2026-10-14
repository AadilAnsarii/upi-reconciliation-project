package com.jpb.reconciliation.reconciliation.service.forcematch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
import com.jpb.reconciliation.reconciliation.entity.TTUMRefundQueryMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReportRepository;
import com.jpb.reconciliation.reconciliation.repository.TTUMRefundQueryMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ForceMatchExecutionService {

	private Logger logger = LoggerFactory.getLogger(ForceMatchExecutionService.class);

	private final JdbcTemplate jdbcTemplate;

	@Value("${app.ttumFile}")
	private String ttumFileLocation;

	@Autowired
	ReportRepository reportRepository;

	@Autowired
	TTUMRefundQueryMasterRepository refundQueryMasterRepository;

	@Autowired
	ReconUserRepository reconUserRepository;
	@Autowired
	ReconProcessDefMasterRepository reconProcessDefMasterRepository;

	public ForceMatchExecutionService(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Executes SP_FORCE_MATCH stored procedure.
	 *
	 * Procedure signature: SP_FORCE_MATCH ( Prm_Process_Id IN VARCHAR2, Prm_User_Id
	 * IN NUMBER, Prm_Error OUT VARCHAR2, Prm_Knockoff_Flag OUT CHAR, Prm_Ttum_Id
	 * OUT NUMBER, Prm_Ttum_Error OUT NUMBER )
	 */

	public ResponseEntity<RestWithStatusList> executeForceMatch(Long prmProcessId, UserDetails userDetails) {
		RestWithStatusList restWithStatusList = null;
		List<Object> forceMatchReportList = new ArrayList<>();
		String prmError = null;
		Optional<ReconUser> user = reconUserRepository.findByUsername(userDetails.getUsername());
		Long userId = null;
		if (user.isPresent()) {
			userId = user.get().getUserId();
			logger.info("Current user id is ::::::::::" + userId);
		} else {
			logger.info("Current user id is not found::::::::::" + userId);
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
					"Logged-in user session is invalid or user not found in database.", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.UNAUTHORIZED);
		}

		if (prmProcessId == null) {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Recon Process Id Not Found", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}
		Map<String, Object> result = callForceMatchProcedure(prmProcessId, userId);
		logger.info("Force Match Procedure Output ::::::::::" + result);
		prmError = (String) result.get("prm_error");
		if (prmError != null && prmError.equalsIgnoreCase("OK")) {
			List<ReportEntity> forceMatchReport = generateForceMatchReport(prmProcessId);
			List<ReportDto> mapReportDto = forceMatchReport.stream().map(entity -> {
				ReportDto report = new ReportDto();
				report.setReportId(entity.getReportId());
				report.setProcessId(entity.getProcessId());
				report.setReportName(entity.getReportName());
				report.setReportFileName(entity.getReportFileName());
				report.setReportDate(entity.getReportDate());
				return report;
			}).collect(Collectors.toList());
			forceMatchReportList.addAll(mapReportDto);
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Force Match Execute Successfully",
					forceMatchReportList);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
		} else {
			logger.error("Procedure failed for ID {}. DB Error: {}", prmProcessId, prmError);
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Procedure returned failure", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.EXPECTATION_FAILED);
		}
	}

	private Map<String, Object> callForceMatchProcedure(Long prmProcessId, Long userId) {
		SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_force_match")
				.declareParameters(new SqlParameter("prm_process_id", Types.VARCHAR),
						new SqlParameter("prm_user_id", Types.NUMERIC), new SqlOutParameter("prm_error", Types.VARCHAR),
						new SqlOutParameter("prm_knockoff_flag", Types.CHAR),
						new SqlOutParameter("prm_ttum_id", Types.NUMERIC),
						new SqlOutParameter("prm_ttum_error", Types.NUMERIC));

		Map<String, Object> inParams = new HashMap<>();
		inParams.put("prm_process_id", prmProcessId);
		inParams.put("prm_user_id", userId);
		logger.info("Calling Force Match Procedure Input Parameter :::::::" + inParams);
		try {
			return jdbcCall.execute(inParams);
		} catch (Exception e) {
			logger.error("Critical error calling sp_force_match", e);
			Map<String, Object> errorMap = new HashMap<>();
			errorMap.put("prm_error", "EXCEPTION: " + e.getMessage());
			return errorMap;
		}
	}

	private List<ReportEntity> generateForceMatchReport(Long prmProcessId) {
		List<ReportEntity> responseList = new ArrayList<>();

		List<TTUMRefundQueryMasterEntity> queryConfigs = refundQueryMasterRepository.findByProcessId(prmProcessId);

		for (TTUMRefundQueryMasterEntity config : queryConfigs) {
			try {
				if (config.getRefundQuery() == null)
					continue;

				String fileName = config.getFileName();
				String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
				fileName += timestamp;
				String extension = CommonConstants.ACTIVE_Y.equalsIgnoreCase(config.getIsCsv()) ? ".csv" : ".txt";
				fileName += extension;

				List<Map<String, Object>> rows = jdbcTemplate.queryForList(config.getRefundQuery());
				String filePath = ttumFileLocation + fileName;
				generatePhysicalFile(rows, filePath, config.getIsCsv());

				if (config.getUpdateQuery() != null) {
					jdbcTemplate.update(config.getUpdateQuery());
				}

				Path path = Paths.get(filePath);
				ReportEntity reportRecord = new ReportEntity();
				reportRecord.setProcessId(prmProcessId);
				reportRecord.setReportDate(LocalDate.now());
				reportRecord.setReportFileName(config.getFileName());
				reportRecord.setReportLocation(filePath);
				reportRecord.setReportName("FORCE_MATCH");
				reportRecord.setFileName(path.getFileName().toString());
				reportRepository.save(reportRecord);

				responseList.add(reportRecord);

			} catch (Exception e) {
				logger.error("Error generating report for Action ID: " + config.getActionId(), e);
			}
		}
		return responseList;
	}

	private void generatePhysicalFile(List<Map<String, Object>> rows, String path, String isCsv) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
			for (Map<String, Object> row : rows) {
				String debitLine = String.valueOf(row.getOrDefault("DEBIT", ""));
				String creditLine = String.valueOf(row.getOrDefault("CREDIT", ""));

				if (!debitLine.isEmpty()) {
					writer.write(debitLine);
					writer.newLine();
				}

				if (!creditLine.isEmpty()) {
					writer.write(creditLine);
					writer.newLine();
				}
			}
			writer.flush();
		}
	}

}