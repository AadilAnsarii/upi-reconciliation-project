package com.jpb.reconciliation.reconciliation.service;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.REProcessManager;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessManagerRepository;

@Service
public class SegretionServiceImpl implements SegretionService {

	@Autowired
	private SimpleJdbcCall simpleJdbcCall;

	private final JdbcTemplate jdbcTemplate;
	
	public SegretionServiceImpl(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Autowired
	ReconProcessManagerRepository processManagerRepository;

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	Logger logger = LoggerFactory.getLogger(SegretionServiceImpl.class);

	@Override
	@Async
	public CompletableFuture<String> startSegretion(Long processId) {
		List<REProcessManager> extractionList = processManagerRepository.findByProcessId(processId);
		String segretionMsg = null;
		if (extractionList != null) {
			simpleJdbcCall.withProcedureName("SP_PROCESS_DATA");
			simpleJdbcCall.declareParameters(new SqlParameter("p_id", Types.INTEGER));
			new SqlParameter("result", Types.VARCHAR);
			Map<String, Object> inputParameter = new HashMap<>();
			inputParameter.put("p_id", processId);
			Map<String, Object> result = simpleJdbcCall.execute(inputParameter);
			logger.info("PROCEDURE LOG INFO :::::::::::::::" + result);
			segretionMsg = (String) result.get("P_ERROR_MESSAGE");

			if (segretionMsg.equalsIgnoreCase("OK")) {
				for (REProcessManager process : extractionList) {
					process.setExtractionStatus("Completed");
					processManagerRepository.save(process);
					processManagerRepository.flush();
				}
			} else {
				for (REProcessManager process : extractionList) {
					process.setExtractionStatus("Error");
					processManagerRepository.save(process);
					processManagerRepository.flush();
				}
			}
		} else {
			logger.error("PROCESS DATA NOT FOUND FOR SEGRETION STATUS ::::::::::::" + processId);
		}
		return CompletableFuture.completedFuture(segretionMsg);
	}

	@Override
	@Transactional
	@Retryable(value = { DataAccessResourceFailureException.class, SQLRecoverableException.class,
			SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
	public Boolean startSegretion(ReconBatchProcessEntity reconProcessManager,
			ReconFileDetailsMaster reconFileDetails) {
		String segretionMsg = null;
		Long templateId = reconFileDetails.getReconTemplateDetails().getReconTemplateId();
		logger.info("Template Id :::::::::::::" + reconFileDetails.getReconTemplateDetails().getReconTemplateId());
        
		simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_PROCESS_DATA").declareParameters(
				new SqlParameter("p_templateId", Types.NUMERIC), new SqlParameter("p_fileName", Types.VARCHAR),
				new SqlOutParameter("p_error_message", Types.VARCHAR));

		Map<String, Object> inputParameter = new HashMap<>();
		inputParameter.put("p_templateId", templateId);
		inputParameter.put("p_fileName", reconProcessManager.getFileName());
		logger.info("INPUT PARAMETER LOG INFO :::::::::::::::" + inputParameter);

		Map<String, Object> result = null;
		try {
			result = simpleJdbcCall.execute(inputParameter);
			logger.info("Result from SP_PROCESS_DATA::::::::::::::: {}", result);
			segretionMsg = (String) result.get("p_error_message");
			logger.info("Result from segretionMsg::::::::::::::: {}", segretionMsg);
		} catch (Exception e) {
			logger.warn("Transient error during SP_PROCESS_DATA execution for Template ID {}. Retrying. Error: {}",
					templateId, e.getMessage());
			throw e;
		}
		
		if (segretionMsg != null && segretionMsg.equalsIgnoreCase("OK")) {
			reconProcessManager.setSegretionStatus("Completed");
			reconBatchProcessEntityRepository.save(reconProcessManager);
			logger.info("Segregation completed successfully by SP_PROCESS_DATA for Template ID: {}", templateId);
			return true;
		} else {
			logger.error("SP_PROCESS_DATA returned an error status for Template ID: {}. Message from SP: {}",
					templateId, (segretionMsg != null ? segretionMsg : "NULL or Empty"));
			reconProcessManager.setSegretionStatus("Error");
			reconBatchProcessEntityRepository.save(reconProcessManager);
			return false;
		}
	}

	@Recover
	public Boolean recoverDataAccessResourceFailure(DataAccessResourceFailureException e,
			ReconBatchProcessEntity reconProcessManager, ReconFileDetailsMaster reconFileDetails) {
		logger.error("RECOVERY: DataAccessResourceFailureException after all retries for file: {}. Error: {}",
				reconFileDetails.getReconFileName(), e.getMessage(), e);
		reconProcessManager.setSegretionStatus("Failed (DB Resource Exceeded Retries)");
		reconBatchProcessEntityRepository.save(reconProcessManager);
		return false;
	}

	@Recover
	public Boolean recoverSQLRecoverableException(SQLRecoverableException e,
			ReconBatchProcessEntity reconProcessManager, ReconFileDetailsMaster reconFileDetails) {
		logger.error("RECOVERY: SQLRecoverableException (Socket Timeout) after all retries for file: {}. Error: {}",
				reconFileDetails.getReconFileName(), e.getMessage(), e);
		reconProcessManager.setSegretionStatus("Failed (Socket Timeout Exceeded Retries)");
		reconBatchProcessEntityRepository.save(reconProcessManager);
		return false;
	}

	@Recover
	public Boolean recoverSQLException(SQLException e, ReconBatchProcessEntity reconProcessManager,
			ReconFileDetailsMaster reconFileDetails) {
		logger.error("RECOVERY: SQLException (Code: {}) after all retries for file: {}. Error: {}", e.getErrorCode(),
				reconFileDetails.getReconFileName(), e.getMessage(), e);

		if (e.getErrorCode() == 54) {
			logger.error("RECOVERY: ORA-00054: Resource busy error for file: {} after multiple retries.",
					reconFileDetails.getReconFileName());
			reconProcessManager.setSegretionStatus("Failed (ORA-00054 Exceeded Retries)");
		} else {
			reconProcessManager.setSegretionStatus("Failed (SQL Error Exceeded Retries)");
		}
		reconBatchProcessEntityRepository.save(reconProcessManager);
		return false;
	}

	@Recover
	public Boolean recoverGeneralException(RuntimeException e, ReconBatchProcessEntity reconProcessManager,
			ReconFileDetailsMaster reconFileDetails) {
		logger.error("RECOVERY: General Exception after all retries for file: {}. Error: {}",
				reconFileDetails.getReconFileName(), e.getMessage(), e);
		reconProcessManager.setSegretionStatus("Failed (General Error Exceeded Retries)");
		reconBatchProcessEntityRepository.save(reconProcessManager);
		return false;
	}

	@Recover
	private void recoverTruncateStageTable(SQLException e, String tableName) {
		logger.error("RECOVERY: Failed to TRUNCATE TABLE {} after multiple retries (Error Code: {}). Error: {}",
				tableName, e.getErrorCode(), e.getMessage(), e);
		throw new RuntimeException("Failed to truncate table " + tableName + " after retries.", e);
	}

}
