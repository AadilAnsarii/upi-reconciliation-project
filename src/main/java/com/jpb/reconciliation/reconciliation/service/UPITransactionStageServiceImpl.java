package com.jpb.reconciliation.reconciliation.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UPITransactionRequestDto;
import com.jpb.reconciliation.reconciliation.dto.UPITransactionStageDto;

import oracle.jdbc.OracleTypes;

@Service
public class UPITransactionStageServiceImpl implements UPITransactionStageService {

	private final SimpleJdbcCall simpleJdbcCall;

	Logger logger = LoggerFactory.getLogger(UPITransactionStageServiceImpl.class);
	
	@Value("${app.transactionFile}")
	private String transactionFileLocatin;

	public UPITransactionStageServiceImpl(SimpleJdbcCall simpleJdbcCall) {
		this.simpleJdbcCall = simpleJdbcCall;

		this.simpleJdbcCall.withProcedureName("SP_FETCH_TRANSACTION_DETAILS").declareParameters(
				new SqlParameter("p_posting_batch_id", Types.VARCHAR),
				new SqlParameter("p_reference_number", Types.VARCHAR),
				new SqlParameter("p_payer_account_number", Types.VARCHAR),
				new SqlParameter("p_payee_account_number", Types.VARCHAR), new SqlParameter("p_amount", Types.NUMERIC),
				new SqlParameter("p_from_date", Types.DATE), new SqlParameter("p_to_date", Types.DATE),
				new SqlParameter("p_file_name", Types.VARCHAR), new SqlOutParameter("p_cursor", OracleTypes.CURSOR),
				new SqlOutParameter("prm_err_msg", Types.VARCHAR));
	}

	@Override
	public ResponseEntity<RestWithStatusList> searchTransaction(UPITransactionRequestDto upiTransactionRequestDto)
			throws IOException {
		RestWithStatusList restWithStatusList;
		List<Object> transactionsList = new ArrayList<>();

		Map<String, Object> inputParameter = new HashMap<>();
		inputParameter.put("p_posting_batch_id", upiTransactionRequestDto.getPostingBatchId() != null && !upiTransactionRequestDto.getPostingBatchId().isEmpty() ? upiTransactionRequestDto.getPostingBatchId() : null);
		inputParameter.put("p_reference_number", upiTransactionRequestDto.getReferenceNumber() != null && !upiTransactionRequestDto.getReferenceNumber().isEmpty() ? upiTransactionRequestDto.getReferenceNumber() : null);
		inputParameter.put("p_payer_account_number", upiTransactionRequestDto.getPayerAccountNumber() != null && !upiTransactionRequestDto.getPayerAccountNumber().isEmpty() ? upiTransactionRequestDto.getPayerAccountNumber() : null);
		inputParameter.put("p_payee_account_number", upiTransactionRequestDto.getPayeeAccountNumber() != null && !upiTransactionRequestDto.getPayeeAccountNumber().isEmpty() ? upiTransactionRequestDto.getPayeeAccountNumber() : null);
		inputParameter.put("p_amount", upiTransactionRequestDto.getTransactionAmount()); 
		inputParameter.put("p_from_date", upiTransactionRequestDto.getFromDate());
		inputParameter.put("p_to_date", upiTransactionRequestDto.getToDate());
		inputParameter.put("p_file_name", upiTransactionRequestDto.getFileName() != null && !upiTransactionRequestDto.getFileName().isEmpty() ? upiTransactionRequestDto.getFileName() : null);

		Map<String, Object> output;
		try {
			output = simpleJdbcCall.execute(inputParameter);
		} catch (Exception e) {
			logger.error("Error executing SP_FETCH_TRANSACTION_DETAILS: {}", e.getMessage(), e);
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Database operation failed unexpectedly: " + e.getMessage(), null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.info("PROCEDURE OUTPUT DATA :::::::::::::: {}", output);
//		String errorMessage = (String) output.get("prm_err_msg");
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> getTransactionData = (List<Map<String, Object>>) output.get("p_cursor");
		logger.info("PROCEDURE p_cursor DATA :::::::::::::: {}", getTransactionData);
		
		if (getTransactionData == null || getTransactionData.isEmpty()) {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "No transaction data found for the specified criteria.", transactionsList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		
		List<UPITransactionStageDto> transactionList = new ArrayList<>();
			for (Map<String, Object> transaction : getTransactionData) {
				UPITransactionStageDto newTransaction = new UPITransactionStageDto();
				newTransaction.setTransactionId((String) transaction.get("TRANSACTION_ID"));
				newTransaction.setTranDate((Timestamp) transaction.get("TRAN_DATE"));
				newTransaction.setTxnExtractionDate((Timestamp) transaction.get("TXN_EXTRACTION_DATE"));
				newTransaction.setPostingDate((Timestamp) transaction.get("POSTING_DATE"));
				newTransaction.setPayerAccountNumber((String) transaction.get("PAYER_ACCOUNT_NUMBER"));
				newTransaction.setPayeeAccountNumber((String) transaction.get("PAYEE_ACCOUNT_NUMBER"));
				newTransaction.setNetAmount(((BigDecimal) transaction.get("NET_AMOUNT")).doubleValue());
				newTransaction.setPayeeMerchantId((String) transaction.get("PAYEE_MERCHANT_ID"));
				newTransaction.setReferenceNumber((String) transaction.get("REFERENCE_NUMBER"));
				newTransaction.setPostingBatchId((String) transaction.get("POSTING_BATCH_ID"));
				newTransaction.setStatus((String) transaction.get("STATUS"));
				newTransaction.setBatchPostingStatus((String) transaction.get("BATCH_POSTING_STATUS"));
				newTransaction.setIdempotentKey((String) transaction.get("IDEMPOTENT_KEY"));
				newTransaction.setTotalAmount(((BigDecimal) transaction.get("TOTAL_AMOUNT")).doubleValue());
				transactionList.add(newTransaction);
			}

			transactionsList.addAll(transactionList);

			if (upiTransactionRequestDto.getReportYN() != null && upiTransactionRequestDto.getReportYN().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
				String filePath = generateTTUMFileName("TRANSACTION_REPORT");
//				String filePath = "/app/jpbrecon/JPB_RECON/TransactionReport/transaction-report.csv"; // SIT
//				String filePath = "/home/jioappadm/jpbrecon/TransactionReport/transaction-report.csv"; //PROD
				Boolean reportFlag = generateTransactionReport(transactionList, filePath);
				if (reportFlag == true) {
					List<Object> reportData = new ArrayList<>();
					ReportDto transactionReport = new ReportDto();
					transactionReport.setReportFileName("Transaction Report");
					transactionReport.setReportLocation(filePath);
					reportData.add(transactionReport);
					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Report generated successfully.",
							reportData);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
				} else {
					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Report generation failed!!", null);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
				}
			}

		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Data found successfully ", transactionsList);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	private Boolean generateTransactionReport(List<UPITransactionStageDto> transactionList, String filePath)
			throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.newLine();
			writer.write(
					"                                                                                   TRANSACTION REPORT                                   ");
			writer.newLine();
			writer.write("Report Date : " + new Date());
			writer.newLine();
			writer.write(
					"------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
			writer.newLine();
			writer.write(
					"TRANSACTION_ID , TRAN_DATE , PAYER_ACCOUNT_NUMBER , PAYEE_ACCOUNT_NUMBER , NET_AMOUNT , TXN_EXTRACTION_DATE , PAYEE_MERCHANT_ID,REFERENCE_NUMBER,POSTING_BATCH_ID , STATUS , POSTING_DATE , TOTAL_AMOUNT ,BATCH_POSTING_STATUS , IDEMPOTENT_KEY ");
			writer.newLine();
			writer.write(
					"------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
			writer.newLine();
			for (UPITransactionStageDto transaction : transactionList) {
				writer.write(transaction.getTransactionId() + ",  " + transaction.getTranDate() + ",  "
						+ transaction.getPayerAccountNumber() + ",  " + transaction.getPayeeAccountNumber() + ",  "
						+ transaction.getNetAmount() + ",  " + transaction.getTxnExtractionDate() + ",  "
						+ transaction.getPayeeMerchantId() + ",  " + transaction.getReferenceNumber() + ",  "
						+ transaction.getPostingBatchId() + ",  " + transaction.getStatus() + ",  "
						+ transaction.getPostingDate() + ",  " + transaction.getTotalAmount() + ",  "
						+ transaction.getBatchPostingStatus() + ",  " + transaction.getIdempotentKey());
				writer.newLine();
			}
			logger.info("CSV file created successfully at " + filePath);
		} catch (IOException e) {
			logger.info("Error writing to CSV file: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	private String generateTTUMFileName(String fileName) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
		return transactionFileLocatin + fileName + timestamp + ".csv";
	}

}