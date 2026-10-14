package com.jpb.reconciliation.reconciliation.service;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import org.springframework.transaction.annotation.Transactional; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;

@Service
public class DataUpdateService {

	@Autowired
	@Qualifier("simpleJdbcCall")
	private SimpleJdbcCall simpleJdbcCall;

	private final JdbcTemplate jdbcTemplate;
	
	public DataUpdateService(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	Logger logger = LoggerFactory.getLogger(DataUpdateService.class);

	@Transactional
	@Retryable(value = { DataAccessResourceFailureException.class, SQLRecoverableException.class,
			SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 1.5))
	public Boolean dataUpdateProcess(ReconFileDetailsMaster reconFileDetails,
			ReconBatchProcessEntity reconProcessManager) {
		String dataUpdateMsg = null;
		simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_DATA_UPDATE").declareParameters(
				new SqlParameter("prm_stage_tab_name", Types.VARCHAR), new SqlOutParameter("p_err_msg", Types.VARCHAR));

		Map<String, Object> inputParam = new HashMap<>();
		inputParam.put("prm_stage_tab_name", reconFileDetails.getReconTemplateDetails().getStageTabName());

		Map<String, Object> result = null;
		try {
			result = simpleJdbcCall.execute(inputParam);
			logger.info("Result from SP_DATA_UPDATE :::::::::::: {}", result);

			dataUpdateMsg = (String) result.get("p_err_msg");
			logger.info("Data updation status :::::::::::: {}", dataUpdateMsg);
		} catch (Exception e) {
			throw e;
		}

		if (dataUpdateMsg != null && dataUpdateMsg.equalsIgnoreCase("OK")) {
			reconProcessManager.setSegretionStatus("Completed");
			reconBatchProcessEntityRepository.save(reconProcessManager);
			logger.info("SP_DATA_UPDATE completed successfully by stage table : {}",
					reconFileDetails.getReconTemplateDetails().getStageTabName());
			return true;
		} else {
			logger.error("SP_DATA_UPDATE returned an error status by stage table: {}",
					reconFileDetails.getReconTemplateDetails().getStageTabName());
			reconProcessManager.setSegretionStatus("Error");
			reconProcessManager.setStatus("Error");
			reconProcessManager.setReportStatus("Error");
			reconBatchProcessEntityRepository.save(reconProcessManager);
			return false;
		}
	}

}
