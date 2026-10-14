package com.jpb.reconciliation.reconciliation.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.config.SchedulerConfig;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RecUpiMisTCCReportDTO;
import com.jpb.reconciliation.reconciliation.dto.RefundTransactionDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TTUMReportDto;
import com.jpb.reconciliation.reconciliation.dto.TTUMReportResponseDto;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.TTUMConfigMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.TTUMRefundQueryMasterEntity;
import com.jpb.reconciliation.reconciliation.mapper.TTUMConfigMasterMapper;
import com.jpb.reconciliation.reconciliation.repository.AuditLogManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.TTUMConfigMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.TTUMEntriesRepository;
import com.jpb.reconciliation.reconciliation.repository.TTUMRefundQueryMasterRepository;

@Service
public class TTUMReportServiceImpl implements TTUMReportService {

	private final AuditLogManagerRepository auditLogManagerRepository;

	private final SchedulerConfig schedulerConfig;

	@Autowired
	TTUMConfigMasterRepository ttumConfigMasterRepository;

	@Autowired
	SimpleJdbcCall simpleJdbcCall;

	@Autowired
	ReconProcessDefMasterRepository processDefMasterRepository;

	@Autowired
	ProcessMasterRepository processMasterRepository;

	@Autowired
	TTUMRefundQueryMasterRepository refundQueryMasterRepository;

	@Autowired
	TTUMEntriesRepository ttumEntriesRepository;

	@Value("${app.ttumFile}")
	private String ttumFileLocation;

	private final JdbcTemplate jdbcTemplate;
	

	Logger logger = LoggerFactory.getLogger(TTUMReportServiceImpl.class);

	TTUMReportServiceImpl(SchedulerConfig schedulerConfig, AuditLogManagerRepository auditLogManagerRepository,DataSource dataSource) {
		this.schedulerConfig = schedulerConfig;
		this.auditLogManagerRepository = auditLogManagerRepository;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public ResponseEntity<RestWithStatusList> getAllTTUMList() {
		RestWithStatusList restWithStatusList;
		List<Object> ttumList = new ArrayList<>();

		List<TTUMConfigMasterEntity> ttumConfigData = ttumConfigMasterRepository.findAll();

		logger.info("TTUM CONFIG DATA ::::::::::::::::::::::::::::" + ttumConfigData);
		if (!ttumConfigData.isEmpty()) {
			for (TTUMConfigMasterEntity configMast : ttumConfigData) {
				ReconProcessDefMaster processFile = processDefMasterRepository
						.findByReconProcessId(configMast.getTtumProcessId());
				TTUMReportDto ttumReportData = TTUMConfigMasterMapper.mapToTTUMReportDto(processFile, configMast,
						new TTUMReportDto());
				ttumList.add(ttumReportData);
			}
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Not Found TTUM Report Data.", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Found TTUM Report Data.", ttumList);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> generateTTUMReport(TTUMReportDto ttumGenerateReportRequest) {
		RestWithStatusList restWithStatusList = null;
		List<Object> reportObjectList = new ArrayList<>();

		if (ttumGenerateReportRequest != null) {
			logger.info("PROCEDURE Started :::::::::::::::");
//			MapSqlParameterSource inParams = new MapSqlParameterSource();
			simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_TTUM_PROCESS").declareParameters(
					new SqlParameter("p_user_id", Types.NUMERIC),
					new SqlOutParameter("p_err_msg", Types.VARCHAR));
			
//			simpleJdbcCall.withProcedureName("SP_TTUM_PROCESS")
//					.declareParameters(
//							new SqlParameter("p_user_id", Types.NUMERIC),
//							new SqlOutParameter("p_err_msg", Types.VARCHAR));
			
			Map<String, Object> inputParameter = new HashMap<>();
			inputParameter.put("p_user_id", ttumGenerateReportRequest.getUserId());
			
			Map<String, Object> result = simpleJdbcCall.execute(inputParameter);
			logger.info("PROCEDURE OUTPUT :::::::::::::::" + result);
			String ttumReportSPResult = (String) result.get("p_err_msg");

			if (ttumReportSPResult != null && ttumReportSPResult.equalsIgnoreCase("OK")) {
				List<TTUMReportResponseDto> ttumReport = generateTTUMReportFile(ttumGenerateReportRequest);
				logger.info("TTUM REPORT DATA :::::::" + ttumReport);
				reportObjectList.addAll(ttumReport.stream().collect(Collectors.toList()));
				restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Report generated succussfully",
						reportObjectList);
			} else {
				String errorMessage = (ttumReportSPResult != null && !ttumReportSPResult.isEmpty()) ? ttumReportSPResult
						: "Report generation process failed with unknown error.";
				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, errorMessage, reportObjectList);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Send valid request", reportObjectList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	private List<TTUMReportResponseDto> generateTTUMReportFile(TTUMReportDto ttumGenerateReportRequest) {
		logger.info("TTUM GENERATION REQUEST :::::::::::" + ttumGenerateReportRequest);

		List<TTUMRefundQueryMasterEntity> ttumRefundQueryList = refundQueryMasterRepository
				.findByProcessId(ttumGenerateReportRequest.getTtumProcessId());
		logger.info("TTUM DATA BY PROCESS ID ::::::::::::::" + ttumRefundQueryList);

		List<TTUMReportResponseDto> ttumReportResponseList = new ArrayList<>();
		if (!ttumRefundQueryList.isEmpty()) {
			for (TTUMRefundQueryMasterEntity ttumRefundData : ttumRefundQueryList) {
				if (ttumRefundData.getTtumDescription().equalsIgnoreCase("UPIINW_TCC_CBS_TTUM")) {
					String trqRefundQuery = ttumRefundData.getRefundQuery();
					List<RefundTransactionDTO> trqRefundList = getRefundTransactions(trqRefundQuery);
					if (!trqRefundList.isEmpty()) {
						TTUMReportResponseDto cbsTTUMReport = writeTTUMReportFile(trqRefundList,
								ttumGenerateReportRequest);
						Boolean updateStatus = updateTTUMData(ttumRefundData.getUpdateQuery());
						logger.info("cbsTTUMReport updateStatus ::::" + updateStatus);
						ttumReportResponseList.add(cbsTTUMReport);
					}
				} else {
					String trqRefundQuery = ttumRefundData.getRefundQuery();
					List<RecUpiMisTCCReportDTO> recUpiMisTCCReportList = getRecUpiMisTCCReportData(trqRefundQuery);
					if (!recUpiMisTCCReportList.isEmpty()) {
						TTUMReportResponseDto npciDccReport = writeNPCIDCCReport(recUpiMisTCCReportList,
								ttumGenerateReportRequest);
						Boolean updateStatus = updateTTUMData(ttumRefundData.getUpdateQuery());
						logger.info("npciDccReport updateStatus::::" + updateStatus);
						ttumReportResponseList.add(npciDccReport);
					}
				}
			}
			return ttumReportResponseList;
		}
		return ttumReportResponseList;
	}

	private List<RefundTransactionDTO> getRefundTransactions(String refundQuery) {
		logger.info("REFUND QUERY IS ::::::::::: {}" + refundQuery);
		if (refundQuery != null) {
			return jdbcTemplate.query(refundQuery, new RowMapper<RefundTransactionDTO>() {
				@Override
				public RefundTransactionDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
					RefundTransactionDTO refundTransaction = new RefundTransactionDTO();
					refundTransaction.setCredit(rs.getString("CREDIT"));
					refundTransaction.setDebit(rs.getString("DEBIT"));
					return refundTransaction;
				}

			});
		}
		return null;
	}

	private TTUMReportResponseDto writeTTUMReportFile(List<RefundTransactionDTO> refundList,
			TTUMReportDto ttumGenerateReportRequest) {
		TTUMReportResponseDto ttumReportResponse = new TTUMReportResponseDto();
		if (refundList != null) {
			String filePath = generateTTUMFileName("UPIINW_TCC_CBS_TTUM");
//			String filePath = "/app/jpbrecon/JPB_RECON/TTUMReport/UPIINW_TCC_CBS_TTUM.csv";  // SIT
//			String filePath = "/home/jioappadm/jpbrecon/TTUMReport/UPIINW_TCC_CBS_TTUM.csv";  // PROD
//			String filePath = "C:\\Akshay_Ramani\\Jio_Bank_Projects\\TTUMReport\\UPIINW_TCC_CBS_TTUM.csv";
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				for (RefundTransactionDTO refundFile : refundList) {
					writer.write(refundFile.getCredit());
					writer.newLine();
					writer.write(refundFile.getDebit());
					writer.newLine();
				}
				ttumReportResponse.setInsertCode(ttumGenerateReportRequest.getInsertCode());
				ttumReportResponse.setIsCBSTTUM(ttumGenerateReportRequest.getIsCBSTTUM());
				ttumReportResponse.setReportFileLocation(filePath);
				ttumReportResponse.setTtumDescription(ttumGenerateReportRequest.getTtumDescription());
				ttumReportResponse.setSettleFileId(ttumGenerateReportRequest.getSettleFileId());
				ttumReportResponse.setTtumProcessId(ttumGenerateReportRequest.getTtumProcessId());
				return ttumReportResponse;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			ttumReportResponse = null;
		}
		return ttumReportResponse;
	}

	private String generateTTUMFileName(String fileName) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
		if (fileName.equalsIgnoreCase("NPCIDCC_REPORT")) {
			return ttumFileLocation + fileName + timestamp + ".csv";
		} else {
			return ttumFileLocation + fileName + timestamp + ".txt";
		}

	}

	private List<RecUpiMisTCCReportDTO> getRecUpiMisTCCReportData(String trqRefundQuery) {
		logger.info("REFUND QUERY IS ::::::::::: {}" + trqRefundQuery);
		if (trqRefundQuery != null) {
			return jdbcTemplate.query(trqRefundQuery, new RowMapper<RecUpiMisTCCReportDTO>() {
				@Override
				public RecUpiMisTCCReportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
					RecUpiMisTCCReportDTO recUpiMisTCCReport = new RecUpiMisTCCReportDTO();
					recUpiMisTCCReport.setBankAdjRef(rs.getString("BANKADJREF"));
					recUpiMisTCCReport.setFlag(rs.getString("FLAG"));
					recUpiMisTCCReport.setShtDat(rs.getString("SHTDAT"));
					recUpiMisTCCReport.setAdjAmt(rs.getBigDecimal("ADJAMT"));
					recUpiMisTCCReport.setShSer(rs.getString("SHSER"));
					recUpiMisTCCReport.setUtxId(rs.getString("UTXID"));
					recUpiMisTCCReport.setReason(rs.getString("REASON"));
					recUpiMisTCCReport.setSpecifyOther(rs.getString("SPECIFYOTHER"));
					return recUpiMisTCCReport;
				}
			});
		}
		return null;
	}

	private TTUMReportResponseDto writeNPCIDCCReport(List<RecUpiMisTCCReportDTO> recUpiMisTCCReportList,
			TTUMReportDto ttumGenerateReportRequest) {
		String filePath = generateTTUMFileName("NPCIDCC_REPORT");
//		String filePath = "/app/jpbrecon/JPB_RECON/TTUMReport/NPCIDCC_REPORT.csv"; //SIT
//		String filePath = "/home/jioappadm/jpbrecon/TTUMReport/NPCIDCC_REPORT.csv";  // PROD
//		String filePath = "C:\\Akshay_Ramani\\Jio_Bank_Projects\\TTUMReport\\NPCIDCC_REPORT.csv";
		File file = new File(filePath);
		String fileName = file.getName();
		TTUMReportResponseDto ttumReportResponse = new TTUMReportResponseDto();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write("Bankadjref,Flag,shtdat,adjamt,Shser,UTXID,filename,reason,specifyother");
			writer.newLine();
			for (RecUpiMisTCCReportDTO recUpiMisTCCReport : recUpiMisTCCReportList) {
				String line = String.join(",", recUpiMisTCCReport.getBankAdjRef(), recUpiMisTCCReport.getFlag(),
						recUpiMisTCCReport.getShtDat(), String.valueOf(recUpiMisTCCReport.getAdjAmt()),
						recUpiMisTCCReport.getShSer(), recUpiMisTCCReport.getUtxId(), fileName,
						recUpiMisTCCReport.getReason(), recUpiMisTCCReport.getSpecifyOther());

				writer.write(line);
				writer.newLine();
			}
			ttumReportResponse.setInsertCode(ttumGenerateReportRequest.getInsertCode());
			ttumReportResponse.setIsCBSTTUM(ttumGenerateReportRequest.getIsCBSTTUM());
			ttumReportResponse.setReportFileLocation(filePath);
//			ttumReportResponse.setTtumDescription(ttumGenerateReportRequest.getTtumDescription());
			ttumReportResponse.setTtumDescription("NPCIDCC_TTUM");
			ttumReportResponse.setSettleFileId(ttumGenerateReportRequest.getSettleFileId());
			ttumReportResponse.setTtumProcessId(ttumGenerateReportRequest.getTtumProcessId());
			return ttumReportResponse;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Boolean updateTTUMData(String updateQuery) {
		if (updateQuery == null || updateQuery.trim().isEmpty()) {
			logger.warn("TTUM refund query not found");
			return false;
		}
		try {
			int rowsAffected = jdbcTemplate.update(updateQuery);
			if (rowsAffected > 0) {
				logger.info("Successfully updated. Rows affected: {}", rowsAffected);
				return true;
			} else {
				logger.warn("Update query executed but no rows were affected. Query: {}", updateQuery);
				return false;
			}
		} catch (DataAccessException e) {
			logger.error("Failed to TTUM update. Error: {}", e.getMessage(), e);
			throw new RuntimeException("Error updating TTUM data due to database issue.", e);
		}
	}

}
