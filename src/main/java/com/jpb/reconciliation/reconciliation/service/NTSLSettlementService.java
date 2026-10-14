package com.jpb.reconciliation.reconciliation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;

@Service
public class NTSLSettlementService {

	Logger logger = LoggerFactory.getLogger(NTSLSettlementService.class);

	private final JdbcTemplate jdbcTemplate;

	public NTSLSettlementService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Boolean ntslSettlementProcess(ReconFileDetailsMaster reconFileDetails) {
		Boolean ntslSettlementProcess = false;
		if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_AEPS_AEA_STAGE_T")) {
			if (reconFileDetails.getReconTemplateDetails().getStageTabName().isEmpty()) {
				logger.info("Input table is not found ::::::::::");
				return ntslSettlementProcess;
			} else {
				String sqlQuery1 = "MERGE INTO npci_summary_tbl tgt\r\n" + "USING (\r\n" + "    SELECT\r\n"
						+ "        SUBSTR(file_name, INSTR(file_name, '_') - 6, 6) AS npci_file_date,\r\n"
						+ "        REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1)            AS npci_cyle,\r\n"
						+ "        COUNT(1) AS query_count,\r\n"
						+ "        SUM(DECODE(tran_code, '04', -tran_amount, tran_amount)) AS query_amount\r\n"
						+ "    FROM ";
				String inputTable = reconFileDetails.getReconTemplateDetails().getStageTabName();
				String sqlQuery2 = " WHERE tran_code IN ('01','04','FC')\r\n"
						+ "      AND tran_resp_code IN ('00','71')\r\n"
						+ "    GROUP BY SUBSTR(file_name, INSTR(file_name, '_') - 6, 6),\r\n"
						+ "             REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1)\r\n" + ") src\r\n"
						+ "ON (tgt.npci_file_date = src.npci_file_date AND tgt.npci_cyle = src.npci_cyle AND tgt.product_type='AEPS' )\r\n"
						+ "WHEN MATCHED THEN\r\n"
						+ "    UPDATE SET tgt.npci_raw_data_count  = tgt.npci_raw_data_count + src.query_count,\r\n"
						+ "               tgt.npci_raw_data_amount = tgt.npci_raw_data_amount + src.query_amount\r\n"
						+ "WHEN NOT MATCHED THEN\r\n"
						+ "    INSERT (product_type, npci_file_date, npci_cyle, npci_raw_data_count, npci_raw_data_amount)\r\n"
						+ "    VALUES ('AEPS',\r\n" + "            src.npci_file_date,\r\n"
						+ "            src.npci_cyle,\r\n" + "            src.query_count,\r\n"
						+ "            src.query_amount)";
				String finalQuery = sqlQuery1 + inputTable + sqlQuery2;
				try {
					int rowsAffected = jdbcTemplate.update(finalQuery);
					logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
					ntslSettlementProcess = true;
				} catch (DataAccessException e) {
					logger.error("Error executing MERGE query: {}", e.getMessage(), e);
					ntslSettlementProcess = false;
				}
			}

		} else if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_AEPS_AEP_STAGE_TMP")) {
			if (reconFileDetails.getReconTemplateDetails().getStageTabName().isEmpty()) {
				logger.info("Input table is not found ::::::::::");
				return ntslSettlementProcess;
			} else {
				String sqlISSQuery1 = "MERGE INTO npci_summary_tbl tgt\r\n" + "USING (\r\n" + "SELECT\r\n"
						+ "   SUBSTR(file_name, INSTR(file_name, '_') - 6, 6) AS npci_file_date,\r\n"
						+ "   REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1)   AS NPCI_CYLE,\r\n"
						+ "    COUNT(1) AS NPCI_RAW_DATA_COUNT,\r\n"
						+ "    sum(decode(tran_code,'04',tran_amount,-tran_amount))  AS NPCI_RAW_DATA_AMOUNT\r\n"
						+ "FROM ";
				String inputIssTable = reconFileDetails.getReconTemplateDetails().getStageTabName();
				String sqlIssQuery2 = " where tran_code in ('01','04','FC') and tran_resp_code in ('00','71')\r\n"
						+ "GROUP BY\r\n" + "    SUBSTR(file_name, INSTR(file_name, '_') - 6, 6),\r\n"
						+ "   REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1) \r\n" + ") src\r\n"
						+ "ON (tgt.npci_file_date = src.npci_file_date AND tgt.npci_cyle = src.npci_cyle AND tgt.product_type='AEPS' )\r\n"
						+ "WHEN MATCHED THEN\r\n"
						+ "    UPDATE SET tgt.npci_raw_data_count  = tgt.npci_raw_data_count + src.NPCI_RAW_DATA_COUNT,\r\n"
						+ "               tgt.npci_raw_data_amount = tgt.npci_raw_data_amount + src.NPCI_RAW_DATA_AMOUNT\r\n"
						+ "WHEN NOT MATCHED THEN\r\n"
						+ "    INSERT (product_type, npci_file_date, npci_cyle, npci_raw_data_count, npci_raw_data_amount)\r\n"
						+ "    VALUES ('AEPS',\r\n" + "            src.npci_file_date,\r\n"
						+ "            src.npci_cyle,\r\n" + "            src.NPCI_RAW_DATA_COUNT,\r\n"
						+ "            src.NPCI_RAW_DATA_AMOUNT)";
				String finalIssQuery = sqlISSQuery1 + inputIssTable + sqlIssQuery2;
				try {
					int rowsAffected = jdbcTemplate.update(finalIssQuery);
					logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
					ntslSettlementProcess = true;
				} catch (DataAccessException e) {
					logger.error("Error executing MERGE query: {}", e.getMessage(), e);
					ntslSettlementProcess = false;
				}
			}
		} else if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_AEPS_NTSL_STAGE_T")) {
			String sqlNtslQuery1 = "MERGE INTO npci_summary_tbl tgt\r\n" + "USING (\r\n"
					+ "	select SUBSTR(file_name, INSTR(file_name, '_') - 6, 6) AS NPCI_FILE_DATE,\r\n"
					+ "	REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1)   AS NPCI_CYLE,\r\n"
					+ "    sum(case when DEBIT='0' and CREDIT='0' then '0' else no_of_transaction end) ntsl_raw_data_count,\r\n"
					+ "	sum(debit-credit) ntsl_raw_data_amount from ";
			String inputNtslTable = reconFileDetails.getReconTemplateDetails().getStageTabName();
			String sqlNtslQuery2 = " where  \r\n" + "	debit is not null and debit <> 'Debit'\r\n"
					+ "and  DESCRIPTION like '%Approved Transaction Amount%'\r\n" + "GROUP BY\r\n"
					+ "    SUBSTR(file_name, INSTR(file_name, '_') - 6, 6),\r\n"
					+ "   REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1) \r\n" + ") src\r\n"
					+ "ON (tgt.npci_file_date = src.npci_file_date AND tgt.npci_cyle = src.npci_cyle AND tgt.product_type='AEPS' )\r\n"
					+ "WHEN MATCHED THEN\r\n"
					+ "    UPDATE SET tgt.ntsl_raw_data_count  = src.ntsl_raw_data_count ,\r\n"
					+ "               tgt.ntsl_raw_data_amount = src.ntsl_raw_data_amount\r\n"
					+ "WHEN NOT MATCHED THEN\r\n"
					+ "    INSERT (product_type, npci_file_date, npci_cyle, ntsl_raw_data_count, ntsl_raw_data_amount)\r\n"
					+ "    VALUES ('AEPS',\r\n" + "            src.npci_file_date,\r\n"
					+ "            src.npci_cyle,\r\n" + "            src.ntsl_raw_data_count,\r\n"
					+ "            src.ntsl_raw_data_amount)";
			String finalNtslQuery = sqlNtslQuery1 + inputNtslTable + sqlNtslQuery2;
			try {
				int rowsAffected = jdbcTemplate.update(finalNtslQuery);
				logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
				ntslSettlementProcess = true;
			} catch (DataAccessException e) {
				logger.error("Error executing MERGE query: {}", e.getMessage(), e);
				ntslSettlementProcess = false;
			}
		} else if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_IMPS_ACC_STAGE_T")) {
			String sqlImpsQuery1 = " MERGE INTO npci_summary_tbl tgt\r\n" + "USING (\r\n" + "     SELECT\r\n"
					+ "        SUBSTR(file_name, INSTR(file_name, '_') - 6, 6) AS npci_file_date,\r\n"
					+ "        REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1) AS npci_cyle,\r\n"
					+ "         COUNT(1) AS query_count,\r\n"
					+ "        SUM(CASE WHEN FILE_NAME LIKE 'ISS%' THEN -TRAN_AMOUNT else TRAN_AMOUNT end ) AS query_amount\r\n"
					+ "    FROM ";

			String sqlImpsQuery2 = reconFileDetails.getReconTemplateDetails().getStageTabName();

			String sqlImpsQuery3 = " WHERE \r\n" + "       tran_resp_code IN ('00','08')\r\n"
					+ "       GROUP BY SUBSTR(file_name, INSTR(file_name, '_') - 6, 6),\r\n"
					+ "     REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1) \r\n" + ") src\r\n"
					+ "ON (tgt.npci_file_date = src.npci_file_date AND tgt.npci_cyle = src.npci_cyle AND tgt.product_type='IMPS' )\r\n"
					+ "WHEN MATCHED THEN\r\n"
					+ "    UPDATE SET tgt.npci_raw_data_count  = tgt.npci_raw_data_count + src.query_count,\r\n"
					+ "               tgt.npci_raw_data_amount = tgt.npci_raw_data_amount + src.query_amount\r\n"
					+ "WHEN NOT MATCHED THEN\r\n"
					+ "    INSERT (product_type, npci_file_date, npci_cyle, npci_raw_data_count, npci_raw_data_amount)\r\n"
					+ "    VALUES ('IMPS',\r\n" + "            src.npci_file_date,\r\n"
					+ "            src.npci_cyle,\r\n" + "            src.query_count,\r\n"
					+ "            src.query_amount)";

			String finalIMPSQuery = sqlImpsQuery1 + sqlImpsQuery2 + sqlImpsQuery3;

			try {
				int rowsAffected = jdbcTemplate.update(finalIMPSQuery);
				logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
				ntslSettlementProcess = true;
			} catch (DataAccessException e) {
				logger.error("Error executing MERGE query: {}", e.getMessage(), e);
				ntslSettlementProcess = false;
			}

		} else if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_IMPS_NTSL_STAGE_T")) {
			String sqlImpsNTSLQuery1 = " MERGE INTO npci_summary_tbl tgt\r\n" + "USING (\r\n"
					+ "	select SUBSTR(file_name, INSTR(file_name, '_') - 6, 6) AS NPCI_FILE_DATE,\r\n"
					+ "	REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1)   AS NPCI_CYLE,\r\n"
					+ "    sum(case when DEBIT='0' and CREDIT='0' then '0' else no_of_transaction end) ntsl_raw_data_count,\r\n"
					+ "	sum(debit-credit) ntsl_raw_data_amount from ";

			String sqlImpsNTSLQuery2 = reconFileDetails.getReconTemplateDetails().getStageTabName();

			String sqlImpsNTSLQuery3 = "  where  \r\n" + "	debit is not null and debit <> 'Debit'\r\n"
					+ "and  DESCRIPTION like '%Approved Transaction Amount%'\r\n" + "GROUP BY\r\n"
					+ "    SUBSTR(file_name, INSTR(file_name, '_') - 6, 6),\r\n"
					+ "   REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1)\r\n" + ") src\r\n"
					+ "ON (tgt.npci_file_date = src.npci_file_date AND tgt.npci_cyle = src.npci_cyle AND tgt.product_type='IMPS' )\r\n"
					+ "WHEN MATCHED THEN\r\n"
					+ "    UPDATE SET tgt.ntsl_raw_data_count  = src.ntsl_raw_data_count ,\r\n"
					+ "               tgt.ntsl_raw_data_amount = src.ntsl_raw_data_amount\r\n"
					+ "WHEN NOT MATCHED THEN\r\n"
					+ "    INSERT (product_type, npci_file_date, npci_cyle, ntsl_raw_data_count, ntsl_raw_data_amount)\r\n"
					+ "    VALUES ('IMPS',\r\n" + "            src.npci_file_date,\r\n"
					+ "            src.npci_cyle,\r\n" + "            src.ntsl_raw_data_count,\r\n"
					+ "            src.ntsl_raw_data_amount)";

			String finalIMPSNtslQuery = sqlImpsNTSLQuery1 + sqlImpsNTSLQuery2 + sqlImpsNTSLQuery3;

			try {
				int rowsAffected = jdbcTemplate.update(finalIMPSNtslQuery);
				logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
				ntslSettlementProcess = true;
			} catch (DataAccessException e) {
				logger.error("Error executing MERGE query: {}", e.getMessage(), e);
				ntslSettlementProcess = false;
			}

		}else if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_UPI_NTSL_STAGE_T")) {
			String sqlUpiNtslQuery1 = "MERGE INTO npci_summary_tbl tgt\r\n"
					+ "USING (\r\n"
					+ "SELECT \r\n"
					+ "    SUBSTR(file_name, INSTR(file_name, '_', -1) - 6, 6) AS NPCI_FILE_DATE,\r\n"
					+ "        REGEXP_SUBSTR(file_name, '([^.]+)\\.', 1, 1, NULL, 1) AS npci_cyle,\r\n"
					+ "    SUM(CASE \r\n"
					+ "            WHEN DEBIT = '0' AND CREDIT = '0' THEN 0 \r\n"
					+ "            ELSE TO_NUMBER(no_of_transaction) \r\n"
					+ "        END) AS ntsl_raw_data_count,\r\n"
					+ "    SUM(NVL(TO_NUMBER(debit), 0) + NVL(TO_NUMBER(credit), 0)) AS ntsl_raw_data_amount \r\n"
					+ "FROM " ;
			String sqlUpiNtslQuery2 = reconFileDetails.getReconTemplateDetails().getStageTabName();
			String sqlUpiNtslQuery3 = " WHERE  \r\n"
					+ "    debit IS NOT NULL \r\n"
					+ "    AND debit <> 'Debit'\r\n"
					+ "GROUP BY\r\n"
					+ "    SUBSTR(file_name, INSTR(file_name, '_', -1) - 6, 6),\r\n"
					+ "       REGEXP_SUBSTR(file_name, '([^.]+)\\.', 1, 1, NULL, 1)\r\n"
					+ ") src\r\n"
					+ "ON (\r\n"
					+ "    tgt.npci_file_date = src.npci_file_date \r\n"
					+ "    AND tgt.npci_cyle = src.npci_cyle \r\n"
					+ "    AND tgt.product_type = 'UPI'\r\n"
					+ ")\r\n"
					+ "WHEN MATCHED THEN\r\n"
					+ "    UPDATE SET \r\n"
					+ "        tgt.ntsl_raw_data_count  = src.ntsl_raw_data_count,\r\n"
					+ "        tgt.ntsl_raw_data_amount = src.ntsl_raw_data_amount\r\n"
					+ "WHEN NOT MATCHED THEN\r\n"
					+ "    INSERT (\r\n"
					+ "        product_type, \r\n"
					+ "        npci_file_date, \r\n"
					+ "        npci_cyle, \r\n"
					+ "        ntsl_raw_data_count, \r\n"
					+ "        ntsl_raw_data_amount\r\n"
					+ "    )\r\n"
					+ "    VALUES (\r\n"
					+ "        'UPI',\r\n"
					+ "        src.npci_file_date,\r\n"
					+ "        src.npci_cyle,\r\n"
					+ "        src.ntsl_raw_data_count,\r\n"
					+ "        src.ntsl_raw_data_amount\r\n"
					+ "    )";
			
			String sqlUpiNtslQuery = sqlUpiNtslQuery1 + sqlUpiNtslQuery2 + sqlUpiNtslQuery3;

			try {
				int rowsAffected = jdbcTemplate.update(sqlUpiNtslQuery);
				logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
				ntslSettlementProcess = true;
			} catch (DataAccessException e) {
				logger.error("Error executing MERGE query: {}", e.getMessage(), e);
				ntslSettlementProcess = false;
			}
			
		}
		
		else if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_UPI_ACQ_STAGE_T")) {//
			String sqlUpiOtherFilesQuery1 = "MERGE INTO npci_summary_tbl tgt\r\n"
					+ "USING (\r\n"
					+ "    SELECT\r\n"
					+ "        SUBSTR(file_name, INSTR(file_name, '_', -1) - 6, 6) AS npci_file_date,\r\n"
					+ "     REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1) AS npci_cyle,\r\n"
					+ "        COUNT(1) AS query_count,\r\n"
					+ "        SUM(CASE\r\n"
					+ "                WHEN UPPER(FILE_NAME) LIKE '%ACQ%' OR UPPER(FILE_NAME) LIKE '%ISS%' \r\n"
					+ "                THEN -TO_NUMBER(TRAN_AMOUNT)\r\n"
					+ "                ELSE TO_NUMBER(TRAN_AMOUNT)\r\n"
					+ "            END) AS query_amount\r\n"
					+ "    FROM ";
			String sqlUpiOtherFilesQuery2 = reconFileDetails.getReconTemplateDetails().getStageTabName();
			String sqlUpiOtherFilesQuery3 = " WHERE tran_resp_code IN ('00','RB')\r\n"
					+ "    GROUP BY\r\n"
					+ "        SUBSTR(file_name, INSTR(file_name, '_', -1) - 6, 6),\r\n"
					+ "        REGEXP_SUBSTR(file_name, '_([^_.]+)\\.', 1, 1, NULL, 1)\r\n"
					+ ") src\r\n"
					+ "ON (\r\n"
					+ "    tgt.npci_file_date = src.npci_file_date\r\n"
					+ "    AND tgt.npci_cyle = src.npci_cyle\r\n"
					+ "    AND tgt.product_type = 'UPI'\r\n"
					+ ")\r\n"
					+ "WHEN MATCHED THEN\r\n"
					+ "    UPDATE SET\r\n"
					+ "        tgt.npci_raw_data_count  = NVL(tgt.npci_raw_data_count, 0) + src.query_count,\r\n"
					+ "        tgt.npci_raw_data_amount = NVL(tgt.npci_raw_data_amount, 0) + src.query_amount\r\n"
					+ "WHEN NOT MATCHED THEN\r\n"
					+ "    INSERT (\r\n"
					+ "        product_type,\r\n"
					+ "        npci_file_date,\r\n"
					+ "        npci_cyle,\r\n"
					+ "        npci_raw_data_count,\r\n"
					+ "        npci_raw_data_amount\r\n"
					+ "    )\r\n"
					+ "    VALUES (\r\n"
					+ "        'UPI',\r\n"
					+ "        src.npci_file_date,\r\n"
					+ "        src.npci_cyle,\r\n"
					+ "        src.query_count,\r\n"
					+ "        src.query_amount\r\n"
					+ "    )";
			
			String sqlUpiOtherFilesQuery = sqlUpiOtherFilesQuery1 + sqlUpiOtherFilesQuery2 + sqlUpiOtherFilesQuery3;

			try {
				int rowsAffected = jdbcTemplate.update(sqlUpiOtherFilesQuery);
				logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
				ntslSettlementProcess = true;
			} catch (DataAccessException e) {
				logger.error("Error executing MERGE query: {}", e.getMessage(), e);
				ntslSettlementProcess = false;
			}
			
		}
		
		else if (reconFileDetails.getReconTemplateDetails().getStageTabName()
				.equalsIgnoreCase("REC_CBS_GL_STAGE_T")) {
			String sql7092CbsGlQuery = "MERGE INTO rec_cbs_gl_data t\r\n"
					+ "USING (\r\n"
					+ "    SELECT \r\n"
					+ "        TRIM(tran_seq_num) as seq, \r\n"
					+ "        SUM(debit_amt) as sum_debit, \r\n"
					+ "        SUM(credit_amt) as sum_credit\r\n"
					+ "    FROM rec_cbs_gl_data\r\n"
					+ "    GROUP BY TRIM(tran_seq_num)\r\n"
					+ ") s\r\n"
					+ "ON (TRIM(t.tran_seq_num) = s.seq)\r\n"
					+ "WHEN MATCHED THEN\r\n"
					+ "    UPDATE SET t.rec_flg = 1\r\n"
					+ "    WHERE t.credit_amt = s.sum_debit \r\n"
					+ "       OR t.debit_amt = s.sum_credit; ";
			try {
				int rowsAffected = jdbcTemplate.update(sql7092CbsGlQuery);
				logger.info("MERGE operation completed successfully. Rows affected: {}", rowsAffected);
				ntslSettlementProcess = true;
			} catch (DataAccessException e) {
				logger.error("Error executing MERGE query: {}", e.getMessage(), e);
				ntslSettlementProcess = false;
			}
			
		}
		return ntslSettlementProcess;
	}

}
