package com.jpb.reconciliation.reconciliation.service;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.entity.LoadMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.repository.LoadMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;

@Service
public class LoadMasterServiceImpl implements LoadMasterService {

	@Autowired
	private SimpleJdbcCall simpleJdbcCall;

	@Autowired
	private SegretionService segretionService;
	@Autowired
	private ReportGenerationService reportGenerationService;

	private final JdbcTemplate jdbcTemplate;

	public LoadMasterServiceImpl(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	Logger logger = LoggerFactory.getLogger(SegretionServiceImpl.class);

	@Autowired
	private LoadMasterRepository loadMasterRepository;
	@Autowired
	private AuditLogManagerService auditLogManagerService;
	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
//	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@Override

	public CompletableFuture<String> startDataLoading(Long processId, ReconFileDetailsMaster reconFileDetails,
			ReconUser userData, ReconBatchProcessEntity extractionStatus, LoadMasterEntity loadMasterEntity) {

		try {
			if (callProcedure(processId, loadMasterEntity)) {
				logger.info("Extraction Completed :::::::::::::::" + processId);
				extractionStatus.setExtractionStatus("Completed");

				Boolean segregationStatus = segretionService.startSegretion(extractionStatus, reconFileDetails);
				logger.info("Segregation Status :::::::::::::::" + segregationStatus);
				if (Boolean.TRUE.equals(segregationStatus)) {
					extractionStatus.setReportStatus("Completed");
					 ResponseEntity<ResponseDto> result =
					 reportGenerationService.generateReport(reconFileDetails, null,
					 extractionStatus, null, null);
					 logger.info("REPORT STATUS ::::::::::::::::::" + result);
					 if (result.getStatusCode().equals(HttpStatus.OK)) {
					 extractionStatus.setReportStatus("Completed");
					 } else {
					 extractionStatus.setReportStatus("Error");
					 }
				} else {
					extractionStatus.setSegretionStatus("Error");
				}

			} else {
				extractionStatus.setExtractionStatus("Error");
			}

			reconBatchProcessEntityRepository.save(extractionStatus);
		} catch (Exception e) {
			logger.error("Exception while data loading for processId: " + processId, e);
		}
		return CompletableFuture.completedFuture("File Processed Successfully" + +processId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private Boolean callProcedure(Long processId, LoadMasterEntity loadMasterEntity) {
		ReconBatchProcessEntity process = new ReconBatchProcessEntity();
		String ExtractionMsg = null;
		simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_LOAD_DATA").declareParameters(
				new SqlParameter("prm_process_id", Types.VARCHAR), new SqlParameter("prm_module", Types.VARCHAR),
				new SqlOutParameter("prm_err_msg", Types.VARCHAR));
		Map<String, Object> inputParameter = new HashMap<>();
		inputParameter.put("prm_process_id", loadMasterEntity.getRlmFileId());
		inputParameter.put("prm_module", loadMasterEntity.getRlmModuleName());
		logger.info("INPUT PARAMETER LOG INFO :::::::::::::::" + inputParameter);
		Map<String, Object> result = null;
		try {
			result = simpleJdbcCall.execute(inputParameter);
			logger.info("Result from SP_LOAD_DATA::::::::::::::: {}", result);
			ExtractionMsg = (String) result.get("prm_err_msg");
			logger.info("Result from Msg::::::::::::::: {}", ExtractionMsg);

		} catch (Exception e) {
			logger.warn("Transient error during SP_LOAD_DATA execution for Process ID {}. Retrying. Error: {}",
					processId, e.getMessage());
			throw e;
		}

		if (ExtractionMsg != null && ExtractionMsg.equalsIgnoreCase("OK")) {
			process.setStatus("Completed");

			reconBatchProcessEntityRepository.save(process);
			logger.info("Extraction completed successfully by SP_LOAD_DATA for Process ID: {}", processId);
			return true;
		} else {
			logger.error("SP_LOAD_DATA returned an error status for Process ID: {}. Message from SP: {}", processId,
					(ExtractionMsg != null ? ExtractionMsg : "NULL or Empty"));
			process.setStatus("Error");

			reconBatchProcessEntityRepository.save(process);
			return false;
		}
	}

	@Override
	public ReconBatchProcessEntity extractionRunningStatusforGlFlagY(Long processId,
			ReconFileDetailsMaster reconFileDetails, ReconUser userData) {

		logger.info("PROCESS ID ::::::::::::" + processId);
		ReconBatchProcessEntity process = new ReconBatchProcessEntity();
		process.setProcessId(processId);
		process.setProcessType("EXTRACTION");
		process.setStartTime(LocalDateTime.now().format(dateTimeFormatter));
		process.setEndTime(null);
		process.setStatus("Running");
		process.setFileName(reconFileDetails.getReconFileName());
		process.setHeaderDetails(null);
		process.setControlFileHeaderDetails(null);
		process.setSeqHeaderDetails(null);
		process.setInstCode(null);
		process.setInsertUser(userData.getUserId());
		process.setInsertDate(LocalDate.now());
		process.setExtractionStatus("Running");
		process.setFileDate(null);
		process.setErrorDescription(null);
		process.setExtractionProcedureStatus(null);
		process.setSettleProcedureStatus(null);
		process.setDataCount(null);
		process.setReconStatus(null);
		process.setReportStatus("Running");
		process.setSegretionStatus("Running");
		reconBatchProcessEntityRepository.save(process);
		auditLogManagerService.extractionAudit(process, userData);
		// processList.add(process);

		return process;
	}

	@Override
	public ReconBatchProcessEntity extractionRunningStatusforGlFlagYNewV2(Long templateId,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconUser userData) {
		logger.info("PROCESS ID ::::::::::::" + templateId);
		ReconBatchProcessEntity process = new ReconBatchProcessEntity();
		process.setTemplateId(templateId);
		process.setProcessType("EXTRACTION");
		process.setStartTime(LocalDateTime.now().format(dateTimeFormatter));
		process.setEndTime(null);
		process.setStatus("Running");
		process.setFileName(reconTemplateFileDetails.get().getTemplate().getTemplateName());
		process.setHeaderDetails(null);
		process.setControlFileHeaderDetails(null);
		process.setSeqHeaderDetails(null);
		process.setInstCode(null);
		process.setInsertUser(userData.getUserId());
		process.setInsertDate(LocalDate.now());
		process.setExtractionStatus("Running");
		process.setFileDate(null);
		process.setErrorDescription(null);
		process.setExtractionProcedureStatus(null);
		process.setSettleProcedureStatus(null);
		process.setDataCount(null);
		process.setReconStatus(null);
		process.setReportStatus("Running");
		process.setSegretionStatus("Running");
		reconBatchProcessEntityRepository.save(process);
		auditLogManagerService.extractionAudit(process, userData);
		// processList.add(process);

		return process;
	}

	@Override
	public CompletableFuture<String> startDataLoadingNewV2(Long templateId,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconUser userData,
			ReconBatchProcessEntity extractionStatus, LoadMasterEntity loadMasterEntity) {
		// TODO Auto-generated method stub
		return null;
	}
}
