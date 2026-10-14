package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException; // Spring's generic data access exception
import org.springframework.jdbc.BadSqlGrammarException; // Specific for ORA-00942
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessDataService {

	private static final Logger logger = LoggerFactory.getLogger(ProcessDataService.class);

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public ProcessDataService(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) { 
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public boolean processDataForTemplate(Long templateId) {
		String stageTableName = null;

		try {
			String sqlSelectTableName = "SELECT RTD_STAGE_TAB_NAME FROM rcn_template_dtl WHERE RTD_TEMPLATE_ID = ?";
			stageTableName = jdbcTemplate.queryForObject(sqlSelectTableName, String.class, templateId);
			logger.info("Stage table name for template {}: {}", templateId, stageTableName);
		} catch (BadSqlGrammarException e) {
			logger.error("ORA-00942: Table or view 'rcn_template_dtl' does not exist or invalid SQL. Error: {}",
					e.getMessage(), e);
			throw new RuntimeException("Configuration error: rcn_template_dtl table issue.", e);
		} catch (DataAccessException e) {
			logger.error("Error while fetching stage table name for template {}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Database error fetching stage table name.", e);
		} catch (Exception e) {
			logger.error("Unexpected error fetching stage table name for template {}: {}", templateId, e.getMessage(),
					e);
			throw new RuntimeException("Unexpected error fetching stage table name.", e);
		}

		if (stageTableName == null) {
			logger.error("No stage table found for template ID: {}", templateId);
			return false;
		}

		List<Map<String, Object>> rules = null;
		try {
			String sqlSelectRules = "SELECT RRM_DATA_TBL_NAME, RRM_WHERE_CLAUSE FROM rcn_rule_mast WHERE RRM_TMPLT_ID = ?";
			rules = jdbcTemplate.queryForList(sqlSelectRules, templateId);
			logger.info("Found {} rules for template {}.", rules.size(), templateId);
		} catch (BadSqlGrammarException e) {
			logger.error("ORA-00942: Table or view 'rcn_rule_mast' does not exist or invalid SQL. Error: {}",
					e.getMessage(), e);
			throw new RuntimeException("Configuration error: rcn_rule_mast table issue.", e);
		} catch (DataAccessException e) {
			logger.error("Error while fetching rules for template {}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Database error fetching rules.", e);
		}

		if (rules.isEmpty()) {
			logger.warn("No rules found for template ID: {}. No data will be processed.", templateId);
			return true;
		}

		String dataCols = null;
		try {
			String sqlSelectCols = "SELECT LISTAGG(COLUMN_NAME,',') WITHIN GROUP (ORDER BY COLUMN_ID) FROM USER_TAB_COLUMNS WHERE TABLE_NAME=UPPER(?)";
			dataCols = jdbcTemplate.queryForObject(sqlSelectCols, String.class, stageTableName);
			logger.info("Columns for stage table {}: {}", stageTableName, dataCols);
		} catch (BadSqlGrammarException e) {
			logger.error("ORA-00942: Table or view '{}' does not exist or invalid SQL for column fetching. Error: {}",
					stageTableName, e.getMessage(), e);
			throw new RuntimeException("Configuration error: Stage table columns issue.", e);
		} catch (DataAccessException e) {
			logger.error("Error while fetching columns for stage table {}: {}", stageTableName, e.getMessage(), e);
			throw new RuntimeException("Database error fetching stage table columns.", e);
		}

		if (dataCols == null || dataCols.isEmpty()) {
			logger.error("No columns found for stage table: {}", stageTableName);
			return false;
		}

		for (Map<String, Object> rule : rules) {
			String dataTableName = (String) rule.get("RRM_DATA_TBL_NAME");
			String whereClause = (String) rule.get("RRM_WHERE_CLAUSE");
			if (whereClause == null || whereClause.trim().isEmpty()) {
				whereClause = "";
			} else {
				whereClause = " WHERE " + whereClause;
			}

			String sqlInsert = "INSERT INTO " + dataTableName + " (" + dataCols + ") " + "SELECT " + dataCols + " FROM "
					+ stageTableName + whereClause;

			logger.info("Executing dynamic INSERT SQL: {}", sqlInsert);

			try {
				int rowsAffected = jdbcTemplate.update(sqlInsert);
				logger.info("Inserted {} rows into {} for rule.", rowsAffected, dataTableName);
			} catch (BadSqlGrammarException e) {
				logger.error(
						"ORA-00942: Table or view '{}' does not exist or invalid SQL in dynamic INSERT. SQL: {}. Error: {}",
						dataTableName, sqlInsert, e.getMessage(), e);
				throw new RuntimeException("Configuration error: Target table or dynamic SQL issue.", e);
			} catch (DataAccessException e) {
				logger.error("Database error during dynamic INSERT for table {}. SQL: {}. Error: {}", dataTableName,
						sqlInsert, e.getMessage(), e);
				throw e;
			} catch (Exception e) {
				logger.error("Unexpected error during dynamic INSERT for table {}. SQL: {}. Error: {}", dataTableName,
						sqlInsert, e.getMessage(), e);
				throw new RuntimeException("Unexpected error during dynamic INSERT.", e);
			}
		}

		return true;
	}
}