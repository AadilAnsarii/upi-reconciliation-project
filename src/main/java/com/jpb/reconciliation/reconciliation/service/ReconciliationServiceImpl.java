package com.jpb.reconciliation.reconciliation.service;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto.ProcessManager;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessManagerRepository;
import com.jpb.reconciliation.reconciliation.service.jasper.JasperReportService;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {

	@Autowired
	ReconProcessManagerRepository processManagerRepository;

	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	JasperReportService jasperReportService;

	@Autowired
	public ReconciliationServiceImpl(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Autowired
	ReportGenerationService reportGenerationService;

	@Autowired
	AuditLogManagerService auditLogManagerService;

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	Logger logger = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
//	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Value("${oracle.jdbc.query-timeout-seconds:900}")
	private int queryTimeoutSeconds;

	@Override
	public List<ReconBatchProcessEntity> runReconciliation(Long processId, ReconProcessDefMaster reconProcessDefMaster,
			ReconUser userData) {
		List<ReconBatchProcessEntity> processList = new ArrayList<>();
		ReconBatchProcessEntity process = new ReconBatchProcessEntity();
		process.setProcessId(reconProcessDefMaster.getReconProcessId());
		process.setProcessType("RECONCILIATION");
		process.setStartTime(LocalDateTime.now().format(dateTimeFormatter));
		process.setEndTime(null);
		process.setStatus("Running");
		process.setFileName(reconProcessDefMaster.getReconProcessName());
		process.setHeaderDetails(null);
		process.setControlFileHeaderDetails(null);
		process.setSeqHeaderDetails(null);
		process.setInstCode(null);
		process.setInsertUser(userData.getUserId());
		process.setInsertDate(LocalDate.now());
		process.setExtractionStatus(null);
		process.setFileDate(null);
		process.setErrorDescription(null);
		process.setExtractionProcedureStatus(null);
		process.setSettleProcedureStatus(null);
		process.setDataCount(null);
		process.setReconStatus("Running");
		process.setReportStatus("Running");
		process.setSegretionStatus(null);
		reconBatchProcessEntityRepository.save(process);

		auditLogManagerService.extractionAudit(process, userData);
		processList.add(process);
		return processList;
	}

	@Override
	public ResponseEntity<RestWithStatusList> refreshReconciliation(List<ProcessManager> requestProcess) {
		RestWithStatusList restWithStatusList = null;
		List<Object> refreshExtractionList = new ArrayList<>();
		List<ReconBatchProcessEntity> refreshList = new ArrayList<>();
		for (RefreshRequestDto.ProcessManager data : requestProcess) {
			ReconBatchProcessEntity extractionProcessList = reconBatchProcessEntityRepository
					.findByProcessIdAndSequenceNo(data.getProcessId(), data.getSequenceId());
			refreshList.add(extractionProcessList);
		}
		if (!refreshList.isEmpty()) {
			for (ReconBatchProcessEntity process : refreshList) {
				if (process.getReconStatus().equalsIgnoreCase("Running")) {
					refreshExtractionList.add(process);
					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "The process is currently running....",
							refreshExtractionList);
				} else {
					refreshExtractionList.add(process);
					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Process data found successfully",
							refreshExtractionList);
				}
			}
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Process data not found", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		logger.info("DATA LIST ::::::::::::::::::::::" + refreshList);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	@Async
	public CompletableFuture<String> startReconciliation(Long processId,
			List<ReconBatchProcessEntity> reconciliationStatus, ReconProcessDefMaster reconProcessDefMaster,
			ReconUser userData) {
		try {
			boolean purgeReconStatus = this.purgeReconProcess(processId);
			logger.info("preReconProcessStatus ::::::::::::::::::" + purgeReconStatus);
			for (ReconBatchProcessEntity process : reconciliationStatus) {

				if (purgeReconStatus) {
					boolean preReconProcessStatus = this.startPreReconProcess(processId);
					boolean reconProcessStatus = this.reconProcess(processId);
					boolean reconInsertStatus = this.reconInsert(processId);

					logger.info("preReconProcessStatus ::::::::::::::::::" + preReconProcessStatus);
					logger.info("reconProcessStatus :::::::::::::::::::::" + reconProcessStatus);
					logger.info("reconInsertStatus ::::::::::::::::::::::" + reconInsertStatus);

					if (preReconProcessStatus && reconProcessStatus && reconInsertStatus) {
						process.setReconStatus("Completed");
						process.setStatus("Completed");
						process.setProcessId(processId);
						process.setReconDataCount("8");
						process.setEndTime(LocalDateTime.now().format(dateTimeFormatter));
						process.setDataCount(null);
						reconBatchProcessEntityRepository.save(process);
						
						reportGenerationService.generateReconciliationReport(reconProcessDefMaster, process);
						reportGenerationService.generateExceptionReport(reconProcessDefMaster, process);
						jasperReportService.generateGlobalReconciliationReport(reconProcessDefMaster, process);
						auditLogManagerService.extractionAudit(process, userData);
					} else {
						logger.warn("One or more reconciliation steps failed for process ID: " + processId
								+ ". Initiating rollback.");
						process.setReconStatus("Error");
						process.setStatus("Error");
						process.setProcessId(processId);
						process.setEndTime(LocalDateTime.now().format(dateTimeFormatter));
						process.setDataCount(null);
						process.setReportStatus("Error");
						reconBatchProcessEntityRepository.save(process);
						auditLogManagerService.extractionAudit(process, userData);
						boolean reconRollback = this.startReconRollback(processId);
						return CompletableFuture.completedFuture("Reconciliation failed for process ID: " + processId
								+ ". Rollback initiated: " + reconRollback);
					}
				} else {
					logger.info("Purge Recon Process Failed");
					process.setReconStatus("Error");
					process.setStatus("Error");
					process.setReportStatus("Error");
					process.setProcessId(processId);
					process.setDataCount(null);
					reconBatchProcessEntityRepository.save(process);
					return CompletableFuture.completedFuture("Reconciliation failed for process ID: " + processId);
				}
			}
		} catch (Exception e) {
			logger.error("CRITICAL ERROR during reconciliation for process ID: " + processId
					+ ". This error should trigger a transaction rollback.", e);
			CompletableFuture<String> failedFuture = new CompletableFuture<>();
			failedFuture.completeExceptionally(
					new RuntimeException("Reconciliation process failed for ID: " + processId, e));
			return failedFuture;
		}
		return CompletableFuture.completedFuture("Reconciliation completed successfully for process ID: " + processId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private boolean purgeReconProcess(Long processId) {
		boolean purgeReconProcessStatus = false;
		String purgeReconResult = null;
		logger.info("sp_purge_recon Procedure has started:::::::::::::::::" + processId);

		SimpleJdbcCall preReconCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_purge_recon")
				.declareParameters(new SqlParameter("prm_process_id", Types.NUMERIC),
						new SqlOutParameter("p_err_msg", Types.VARCHAR));

		preReconCall.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("prm_process_id", processId);
		Map<String, Object> result = null;
		try {
			logger.debug("Executing sp_purge_recon with timeout: {} seconds", queryTimeoutSeconds);
			result = preReconCall.execute(parameters);
			logger.debug("sp_purge_recon execution completed successfully.");
		} catch (org.springframework.dao.DataAccessException e) {
			logger.error("Database access error during sp_purge_recon for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to DataAccessException.");
			}
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during sp_purge_recon for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to unexpected exception.");
			}
			throw new RuntimeException("Failed to execute sp_purge_recon", e);
		} finally {
			logger.debug("Finally block reached for sp_purge_recon for process ID: {}", processId);
		}

		logger.info("sp_purge_recon Output ::::::::::::::::::::::::" + result);
		purgeReconResult = (String) result.get("p_err_msg");
		if (purgeReconResult != null && purgeReconResult.equalsIgnoreCase("OK")) {
			purgeReconProcessStatus = true;
		}
		return purgeReconProcessStatus;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean startPreReconProcess(Long processId) {
		boolean preReconProcessStatus = false;
		String preReconResult = null;
		logger.info("sp_prerecon_process Procedure has started:::::::::::::::::" + processId);

		SimpleJdbcCall preReconCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_prerecon_process")
				.declareParameters(new SqlParameter("prm_process_id", Types.NUMERIC),
						new SqlOutParameter("PRM_ERROR", Types.VARCHAR));

		preReconCall.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("prm_process_id", processId);
		Map<String, Object> result = null;
		try {
			logger.debug("Executing sp_prerecon_process with timeout: {} seconds", queryTimeoutSeconds);
			result = preReconCall.execute(parameters);
			logger.debug("sp_prerecon_process execution completed successfully.");
		} catch (org.springframework.dao.DataAccessException e) {
			logger.error("Database access error during sp_prerecon_process for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to DataAccessException.");
			}
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during sp_prerecon_process for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to unexpected exception.");
			}
			throw new RuntimeException("Failed to execute sp_prerecon_process", e);
		} finally {
			logger.debug("Finally block reached for sp_prerecon_process for process ID: {}", processId);
		}

		logger.info("sp_prerecon_process Output ::::::::::::::::::::::::" + result);
		preReconResult = (String) result.get("PRM_ERROR");
		if (preReconResult != null && preReconResult.equalsIgnoreCase("OK")) {
			preReconProcessStatus = true;
		}
		return preReconProcessStatus;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean reconProcess(Long processId) {
		boolean reconProcessStatus = false;
		String reconProcessResult = null;
		logger.info("sp_recon_process Procedure has started:::::::::::::::::" + processId);
		SimpleJdbcCall reconCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_recon_process")
				.declareParameters(new SqlParameter("PRM_PROCESS_ID", Types.NUMERIC),
						new SqlOutParameter("PRM_ERROR", Types.VARCHAR));

		reconCall.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);

		Map<String, Object> param = new HashMap<>();
		param.put("PRM_PROCESS_ID", processId);
		Map<String, Object> result = null;
		try {
			logger.debug("Executing sp_recon_process with timeout: {} seconds", queryTimeoutSeconds);
			result = reconCall.execute(param);
			logger.debug("sp_recon_process execution completed successfully.");
		} catch (org.springframework.dao.DataAccessException e) {
			logger.error("Database access error during sp_recon_process for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to DataAccessException.");
			}
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during sp_recon_process for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to unexpected exception.");
			}
			throw new RuntimeException("Failed to execute sp_recon_process", e);
		} finally {
			logger.debug("Finally block reached for sp_recon_process for process ID: {}", processId);
		}
		logger.info("sp_recon_process Output ::::::::::::::::::::::::" + result);
		reconProcessResult = (String) result.get("PRM_ERROR");
		if (reconProcessResult != null && reconProcessResult.equalsIgnoreCase("OK")) {
			reconProcessStatus = true;
		}
		return reconProcessStatus;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean reconInsert(Long processId) {
		boolean reconInsrt = false;
		String reconInsertResult = null;
		logger.info("sp_recon_insert Procedure has started:::::::::::::::::" + processId);
		SimpleJdbcCall reconInsertCall = new SimpleJdbcCall(jdbcTemplate) // Use the injected jdbcTemplate
				.withProcedureName("sp_recon_insert")
				.declareParameters(new SqlParameter("prm_process_id", Types.NUMERIC),
						new SqlOutParameter("P_ERR_MSG", Types.VARCHAR));

		reconInsertCall.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);

		Map<String, Object> prm = new HashMap<>();
		prm.put("prm_process_id", processId);
		Map<String, Object> result = null;
		try {
			logger.debug("Executing sp_recon_insert with timeout: {} seconds", queryTimeoutSeconds);
			result = reconInsertCall.execute(prm);
			logger.debug("sp_recon_insert execution completed successfully.");
		} catch (org.springframework.dao.DataAccessException e) {
			logger.error("Database access error during sp_recon_insert for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to DataAccessException.");
			}
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during sp_recon_insert for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to unexpected exception.");
			}
			throw new RuntimeException("Failed to execute sp_recon_insert", e);
		} finally {
			logger.debug("Finally block reached for sp_recon_insert for process ID: {}", processId);
		}
		logger.info("sp_recon_insert Output ::::::::::::::::::::::::" + result);
		reconInsertResult = (String) result.get("P_ERR_MSG");
		if (reconInsertResult != null && reconInsertResult.equalsIgnoreCase("OK")) {
			reconInsrt = true;
		}
		return reconInsrt;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean startReconRollback(Long processId) {
		boolean reconRollbackStatus = false;
		String reconRollbackResult = null;

		logger.info("sp_recon_rollback Procedure has started:::::::::::::::::" + processId);

		SimpleJdbcCall rollbackCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_recon_rollback")
				.declareParameters(new SqlParameter("prm_process_id", Types.NUMERIC),
						new SqlOutParameter("P_ERR_MSG", Types.VARCHAR));

		rollbackCall.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("prm_process_id", processId);
		Map<String, Object> result = null;
		try {
			logger.debug("Executing sp_recon_rollback with timeout: {} seconds", queryTimeoutSeconds);
			result = rollbackCall.execute(parameters);
			logger.debug("sp_recon_rollback execution completed successfully.");
		} catch (org.springframework.dao.DataAccessException e) {
			logger.error("Database access error during sp_recon_rollback for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to DataAccessException.");
			}
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during sp_recon_rollback for process ID: " + processId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to unexpected exception.");
			}
			throw new RuntimeException("Failed to execute sp_recon_rollback", e);
		} finally {
			logger.debug("Finally block reached for sp_recon_rollback for process ID: {}", processId);
		}
		logger.info("Recon Rollback Output ::::::::::::::::::::::::" + result);
		reconRollbackResult = (String) result.get("P_ERR_MSG");
		if (reconRollbackResult != null && reconRollbackResult.equalsIgnoreCase("OK")) {
			reconRollbackStatus = true;
		}
		return reconRollbackStatus;
	}
}