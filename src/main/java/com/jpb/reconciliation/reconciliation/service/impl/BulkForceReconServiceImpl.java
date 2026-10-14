package com.jpb.reconciliation.reconciliation.service.impl;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ManRecActionDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ManRecProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.repository.ManRecActionDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ManRecProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.service.BulkForceReconService;

@Service
public class BulkForceReconServiceImpl implements BulkForceReconService {

	@Autowired
	ReconProcessDefMasterRepository reconProcessDefMasterRepository;

	@Autowired
	ManRecActionDefMasterRepository manRecActionDefMasterRepository;

	@Autowired
	ManRecProcessDefMasterRepository manRecProcessDefMasterRepository;

	Logger logger = LoggerFactory.getLogger(BulkForceReconServiceImpl.class);

	private final JdbcTemplate jdbcTemplate;

	 public BulkForceReconServiceImpl(DataSource dataSource) {
	        this.jdbcTemplate = new JdbcTemplate(dataSource);
	    }

	@Value("${oracle.jdbc.query-timeout-seconds:900}")
	private int queryTimeoutSeconds;

	@Override
	public ResponseEntity<RestWithStatusList> getBulkForceReconProcessList() {
		RestWithStatusList restWithStatusList = null;
		List<Object> reconProcessList = new ArrayList<>();

		List<ReconProcessDefMaster> getAllreconProcessList = reconProcessDefMasterRepository.findAll();
		if (getAllreconProcessList.isEmpty()) {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Recon Bulk Force Process Not Available",
					reconProcessList);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}

		reconProcessList.addAll(getAllreconProcessList);
		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Recon Bulk Force Process Available", reconProcessList);
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusList> processReconBulkForceMatch(Long reconProcessId) {
		RestWithStatusList restWithStatusList = null;
		List<Object> bulkForceMatch = new ArrayList<>();

		if (reconProcessId == null) {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Recon Process Id Not Found", bulkForceMatch);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}

		Boolean reconProcessBulkForce = startBulkForceRecon(reconProcessId);
		logger.info("reconProcessBulkForce:::::::::::" + reconProcessBulkForce);

		if (reconProcessBulkForce) {
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Recon Bulk Force Process Is Completed",
					bulkForceMatch);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private Boolean startBulkForceRecon(Long reconProcessId) {
		boolean bulkForceReconStatus = false;
		String bulkForceReconResult = null;
		logger.info("sp_bulkforcerecon_process Procedure has started:::::::::::::::::" + reconProcessId);

		SimpleJdbcCall bulkForceRecon = new SimpleJdbcCall(jdbcTemplate).withProcedureName("sp_bulkforcerecon_process")
				.declareParameters(new SqlParameter("prm_process_id", Types.NUMERIC),
						new SqlOutParameter("PRM_ERROR", Types.VARCHAR));

		bulkForceRecon.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("prm_process_id", reconProcessId);
		Map<String, Object> result = null;
		try {
			logger.debug("Executing sp_bulkforcerecon_process with timeout: {} seconds", queryTimeoutSeconds);
			result = bulkForceRecon.execute(parameters);
			logger.debug("sp_bulkforcerecon_process execution completed successfully.");
		} catch (org.springframework.dao.DataAccessException e) {
			logger.error("Database access error during sp_bulkforcerecon_process for process ID: " + reconProcessId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to DataAccessException.");
			}
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during sp_bulkforcerecon_process for process ID: " + reconProcessId
					+ ". Attempting transaction rollback.", e);
			if (TransactionAspectSupport.currentTransactionStatus() != null) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				logger.debug("Transaction marked for rollback due to unexpected exception.");
			}
			throw new RuntimeException("Failed to execute sp_bulkforcerecon_process", e);
		} finally {
			logger.debug("Finally block reached for sp_bulkforcerecon_process for process ID: {}", reconProcessId);
		}

		logger.info("sp_bulkforcerecon_process Output ::::::::::::::::::::::::" + result);
		bulkForceReconResult = (String) result.get("PRM_ERROR");
		if (bulkForceReconResult != null && bulkForceReconResult.equalsIgnoreCase("OK")) {
			bulkForceReconStatus = true;
		}
		return bulkForceReconStatus;
	}

	@Override
	public ResponseEntity<RestWithStatusList> actionForReconBulkProcess(Long processId) {
		ReconProcessDefMaster reconProcessDefMaster = reconProcessDefMasterRepository.findByReconProcessId(processId);
		ManRecProcessDefMaster manRecProcessData = manRecProcessDefMasterRepository.findByManRecProcessId(processId);
		if (reconProcessDefMaster != null && manRecProcessData != null) {
			Optional<ManRecActionDefMaster> manRecActionData = manRecActionDefMasterRepository
					.findByManRecActionId(manRecProcessData.getManRecActionId());




		} else {
			logger.info("Recon Process Def Master OR Man Rec Process Data Configure Not Found:::::::::::"
					+ reconProcessDefMaster + manRecProcessData);
		}

		return null;
	}

}
