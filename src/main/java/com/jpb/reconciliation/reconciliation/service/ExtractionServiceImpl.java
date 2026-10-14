package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconFieldDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.REProcessManager;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconKeyIdentifyMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldFormatMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldTypeMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconKeyIdentifyMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.util.CommonReport;

import net.sf.jasperreports.engine.JRException;

@Service
public class ExtractionServiceImpl implements ExtractionService {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	ReconFileDetailsMasterRepository reconFileDetailsMasterRepository;

	@Autowired
	ReconFieldDetailsMasterRepository reconFieldDetailsMasterRepository;

	@Autowired
	ReconFieldTypeMasterRepository reconFieldTypeMasterRepository;

	@Autowired
	ReconFieldFormatMasterRepository fieldFormatMasterRepository;

	@Autowired
	ReconKeyIdentifyMasterRepository reconKeyIdentifyMasterRepository;

	@Autowired
	SqlLoaderService sqlLoaderService;

	@Autowired
	CommonReport commonReportService;

	@Autowired
	FileOpearationService fileOpearationService;

	@Autowired
	AuditLogManagerService auditLogManagerService;

	@Autowired
	ReconUserRepository reconUserRepository;

	@Autowired
	NTSLSettlementService ntslSettlementService;

	@Autowired
	ReconProcessManagerRepository processManagerRepository;

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
//	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private final Executor extractionExecutor;

	@Value("${app.uDrive}")
	private String uDrivePath;

	private Logger logger = LoggerFactory.getLogger(ExtractionServiceImpl.class);

	public ExtractionServiceImpl(@Qualifier("extractionExecutor") Executor extractionExecutor) {
		this.extractionExecutor = extractionExecutor;
	}

	@Override
	public CompletableFuture<String> startExtraction(ReconFileDetailsMaster reconFileDetails,
			List<ReconBatchProcessEntity> extractionProcessList, List<File> fileList, ReconUser userDetails)
			throws IOException, InterruptedException, JRException {
		ReconTemplateDetails templateDetails = reconFileDetails.getReconTemplateDetails();
		truncateStageTable(templateDetails.getStageTabName());
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		if (!extractionProcessList.isEmpty()) {
			for (int i = 0; i < extractionProcessList.size(); i++) {
				int index = i;
				CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
					try {
						String generateControlFile = generateControlFile(fileList.get(index),
								reconFileDetails.getReconFileDelimiter(), templateDetails.getReconTemplateId(),
								templateDetails.getStageTabName(), reconFileDetails.getReconFileName());
						String generateLogFile = generateLogFile();
						String generateBadFile = generateBadFile();
						logger.info("CONTROL FILE PATH :::::::" + generateControlFile);
						logger.info("LOG FILE PATH :::::::::::" + generateLogFile);
						logger.info("BAD FILE PATH :::::::::::" + generateBadFile);
						if (!generateControlFile.isEmpty() && !generateLogFile.isEmpty()) {
							String sqlLoaderStatus = sqlLoaderService.startLoading(generateControlFile, generateLogFile,
									generateBadFile, reconFileDetails, extractionProcessList.get(index), userDetails,
									fileList.get(index));
							logger.info("LOADER OUTPUT ::::::::::::::::::::" + sqlLoaderStatus);
						}
					} catch (Exception e) {
						logger.error("Error during extraction for file: {}", fileList.get(index).getName(), e);
					}
				}, extractionExecutor);
				futures.add(future);
			}

		}
		// Combine all futures into a single CompletableFuture.
		CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		// Return a new CompletableFuture that completes with the message
		// ONLY after all underlying tasks are done.
		return allOfFuture.thenApply(v -> {
//			fileOpearationService.moveExtractedFiles(reconFileDetails);
			if (reconFileDetails.getReconTemplateDetails().getSettlementFlag().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
				Boolean ntslSettleFlag = ntslSettlementService.ntslSettlementProcess(reconFileDetails);
				logger.info("ntslSettleFlag" + ntslSettleFlag);
			}
			Boolean globalReportFlag;
			try {
				globalReportFlag = commonReportService.generateGlobalReconciliationReport(reconFileDetails);
			} catch (Exception e) {
				logger.error("Global report generation failed for process: " + reconFileDetails.getReconFileId(), e);
				globalReportFlag = false;
			}
			for (ReconBatchProcessEntity process : extractionProcessList) {
				// Re-fetch — the async sqlLoaderService task updates extractionStatus
				// on its own DB row, so this in-memory reference may still say "Running".
				ReconBatchProcessEntity current = reconBatchProcessEntityRepository
						.findById(process.getSequenceNo()).orElse(process);
				current.setReportStatus("Error".equalsIgnoreCase(current.getExtractionStatus())
						? "Error" : "Completed");
				reconBatchProcessEntityRepository.save(current);
			}
			logger.info("Global report :::::::" + globalReportFlag);
			logger.info("All extraction processes have successfully completed.");
			return "Extraction process completed.";
		});

	}

	public String generateControlFile(File fileLocation, String fileSeprator, Long templateId, String targetTableName,
			String fileName) throws IOException {

		List<ReconFieldDetailsDto> getFiledData = getFiledDataByTemplateId(templateId);
		StringBuilder controlFileContent = new StringBuilder();
		logger.info("FILE DATA ::::::::" + getFiledData);

		if (fileName.equalsIgnoreCase("AEPS ACQUIRER ISSUER FILE") || fileName.equalsIgnoreCase("AEPS ISSUER FILE")
				|| fileName.equalsIgnoreCase("DEBITCARD_ATM_RAW")) {
			controlFileContent.append("OPTIONS (multithreading=TRUE, PARALLEL=TRUE) \n");
		} else if (fileName.equalsIgnoreCase("DEBITCARD_SWITCH")) {
			controlFileContent.append("OPTIONS (multithreading=TRUE, skip=2, PARALLEL=TRUE) \n");
		} else {
			controlFileContent.append("OPTIONS (multithreading=TRUE, skip=1, PARALLEL=TRUE) \n");
		}

		controlFileContent.append("UNRECOVERABLE \n");
		controlFileContent.append("LOAD DATA \n");
		controlFileContent.append("INFILE '").append(fileLocation).append("'\n");
		controlFileContent.append("INTO TABLE ").append(targetTableName).append("\n");
		controlFileContent.append("append \n");
//		if (fileName.equalsIgnoreCase("CBS_AEPS")) {
//			controlFileContent.append("WHEN (NARRATION != 'TOTAL') \n");
//		}
		if (fileName.equalsIgnoreCase("EPIK_AEP_AEPS") || fileName.equalsIgnoreCase("CBS_AEPS")
				|| fileName.equalsIgnoreCase("CBS OB") || fileName.equalsIgnoreCase("ELMS_CBS")
				|| fileName.equalsIgnoreCase("CBS_GL") || fileName.equalsIgnoreCase("UPI_MERCHANT")
				|| fileName.equalsIgnoreCase("DEBITCARD_CBS") || fileName.equalsIgnoreCase("POS_PRESENTMENT_FILE")) {
			controlFileContent.append("FIELDS TERMINATED BY '").append(fileSeprator).append("'")
					.append(" OPTIONALLY ENCLOSED BY '\"' \n");
		} else {
			controlFileContent.append("FIELDS TERMINATED BY '").append(fileSeprator).append("'\n");
		}
		controlFileContent.append("TRAILING NULLCOLS \n");
		controlFileContent.append("(\n");
		for (ReconFieldDetailsDto filed : getFiledData) {
			if (!filed.getRftFieldTypeDesc().equalsIgnoreCase("VARCHAR2")) {
				if (filed.getRftFieldTypeDesc().equalsIgnoreCase("NUMBER")) {
					if (fileName.equalsIgnoreCase("FEBA SWITCH DB") || fileName.equalsIgnoreCase("EPIK_AEP_AEPS")
							|| fileName.equalsIgnoreCase("CBS_AEPS")
							|| fileName.equalsIgnoreCase("AEPS CREDIT ADJUSTMENT")
							|| fileName.equalsIgnoreCase("ELMS_CBS") || fileName.equalsIgnoreCase("DEBITCARD_CBS")
							|| fileName.equalsIgnoreCase("NEFT SFMS")) {
						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ").append("\"TO_")
								.append(filed.getRftFieldTypeDesc()).append("(:").append(filed.getRfmShortName())
								.append(")\" ,\n");
					} else if (fileName.equalsIgnoreCase("AEPS ACQUIRER ISSUER FILE")
							|| fileName.equalsIgnoreCase("AEPS ISSUER FILE")
							|| fileName.equalsIgnoreCase("DEBITCARD_ATM_RAW")
							|| fileName.equalsIgnoreCase("DEBITCARD_POS_RAW")) {
						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" POSITION(")
								.append(filed.getReconFromPosn()).append(":").append(filed.getReconToPosn()).append(")")
								.append(" ").append("\"TO_").append(filed.getRftFieldTypeDesc()).append("(:")
								.append(filed.getRfmShortName()).append(") /100\" ,\n");
					} else {
						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ").append("\"TO_")
								.append(filed.getRftFieldTypeDesc()).append("(:").append(filed.getRfmShortName())
								.append(") /100\" ,\n");
					}
				} else if (filed.getRftFieldTypeDesc().equalsIgnoreCase("TODATE")) {
					controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ").append("DATE")
							.append(" \"").append(filed.getRffFieldFormatDesc()).append("\" \n");
				} else if (filed.getRftFieldTypeDesc().equalsIgnoreCase("DATE")
						|| filed.getRftFieldTypeDesc().equalsIgnoreCase("TIMESTAMP")) {
					if (fileName.equalsIgnoreCase("CBS_AEPS") || fileName.equalsIgnoreCase("ELMS_CBS")
							|| fileName.equalsIgnoreCase("DEBITCARD_CBS")) {
						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ").append("\"TO_")
								.append(filed.getRftFieldTypeDesc()).append("(TRIM(BOTH '''' FROM :")
								.append(filed.getRfmShortName()).append("), '").append(filed.getRffFieldFormatDesc())
								.append("')\"").append(",\n");
					} else if (fileName.equalsIgnoreCase("AEPS ACQUIRER ISSUER FILE")
							|| fileName.equalsIgnoreCase("AEPS ISSUER FILE")
							|| fileName.equalsIgnoreCase("DEBITCARD_ATM_RAW")
							|| fileName.equalsIgnoreCase("DEBITCARD_POS_RAW")) {
						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" POSITION(")
								.append(filed.getReconFromPosn()).append(":").append(filed.getReconToPosn())
								.append(") ").append(filed.getRftFieldTypeDesc()).append(" \"")
								.append(filed.getRffFieldFormatDesc()).append("\" ,\n");
					} else {
						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ")
								.append(filed.getRftFieldTypeDesc()).append(" ").append("\"")
								.append(filed.getRffFieldFormatDesc()).append("\"").append(",\n");
					}
				} else {
					controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ")
							.append(filed.getRftFieldTypeDesc()).append(" ").append("\"")
							.append(filed.getRffFieldFormatDesc()).append("\"").append(",\n");
					// .append(filed.getKeyName()).append(" ").append(filed.getRfmColOffset())
				}
			} else {
				if (fileName.equalsIgnoreCase("CBS_AEPS") || fileName.equalsIgnoreCase("ELMS_CBS")
						|| fileName.equalsIgnoreCase("DEBITCARD_CBS") || (fileName.equalsIgnoreCase("CBS_GL"))) {
					controlFileContent.append(" ").append(filed.getRfmShortName()).append(" \"TRIM(BOTH '''' FROM :")
							.append(filed.getRfmShortName()).append(")\"").append(",\n");
				} else if (fileName.equalsIgnoreCase("AEPS ACQUIRER ISSUER FILE")
						|| fileName.equalsIgnoreCase("AEPS ISSUER FILE")
						|| fileName.equalsIgnoreCase("DEBITCARD_ATM_RAW")
						|| fileName.equalsIgnoreCase("DEBITCARD_POS_RAW")) {
					controlFileContent.append(" ").append(filed.getRfmShortName()).append(" POSITION(")
							.append(filed.getReconFromPosn()).append(":").append(filed.getReconToPosn()).append(")")
							.append(",\n");
				} else if (fileName.equalsIgnoreCase("NEFT SFMS")
						&& filed.getRfmShortName().equalsIgnoreCase("TRAN_SEQ_NUM")) {
					controlFileContent.append(" ").append(filed.getRfmShortName())
							.append(" \"REGEXP_REPLACE(REPLACE(:TRAN_SEQ_NUM, ' ', ''), '^/[^/]+/', '')\"")
							.append(",\n");
				}

				else {
					controlFileContent.append(" ").append(filed.getRfmShortName()).append(",\n");
				}
			}
		}
		controlFileContent.append(" REC_FLG ").append("CONSTANT ").append("0").append(",\n");
		controlFileContent.append(" REV_FLAG ").append("CONSTANT ").append(CommonConstants.INACTIVE_N).append(",\n");
		controlFileContent.append(" PREMANRECREL_FLG ").append("CONSTANT ").append("0").append(",\n");
		controlFileContent.append(" MANRECREL_FLG ").append("CONSTANT ").append("0").append(",\n");
		controlFileContent.append(" FILE_NAME ").append("CONSTANT ").append("'").append(fileLocation.getName())
				.append("'").append(",\n");

		if (fileName.equalsIgnoreCase("AEPS CREDIT ADJUSTMENT")) {
			controlFileContent.append("TRAN_DATE").append(" \"to_date(substr(:TRAN_DATE1,1,9),'DD-MON-YY')\"")
					.append(",\n");
		}

		if (fileName.equalsIgnoreCase("DEBITCARD_POS_RAW")) {
			controlFileContent.append("TRAN_DATE").append(" \"to_date(:TRANS_DATE,'YYMMDD')\"").append(",\n");
		}

		if (fileName.equalsIgnoreCase("EPIK_AEP_AEPS")) {
			controlFileContent.append("DR_CR_FLAG").append(" \"DECODE(:TRANSACTION_TYPE,")
					.append("'DEBIT','D','CREDIT','C','COMMISSION','C')\" ").append(",\n");
		}

		if (fileName.equalsIgnoreCase("CBS_AEPS") || fileName.equalsIgnoreCase("ELMS_CBS")
				|| fileName.equalsIgnoreCase("DEBITCARD_CBS") || fileName.equalsIgnoreCase("CBS_TRANSACTION_PRODUCT_GL")
				|| fileName.equalsIgnoreCase("NEFT CBS")) {
			controlFileContent.append("DR_CR_FLAG").append(" \"CASE ")
					.append("WHEN TO_NUMBER(RTRIM(:DEBIT_AMT, ',')) > 0 THEN 'D' ")
					.append("WHEN TO_NUMBER(RTRIM(:CREDIT_AMT, ',')) > 0 THEN 'C' ").append("ELSE NULL END\" ")
					.append(",\n");
		}

		if (fileName.equalsIgnoreCase("CBS_AEPS") || fileName.equalsIgnoreCase("ELMS_CBS")
				|| fileName.equalsIgnoreCase("DEBITCARD_CBS") || fileName.equalsIgnoreCase("CBS_TRANSACTION_PRODUCT_GL")
				|| fileName.equalsIgnoreCase("NEFT CBS")) {
			controlFileContent.append("TRAN_AMOUNT").append(" \"CASE ")
					.append("WHEN TO_NUMBER(RTRIM(:DEBIT_AMT, ',')) > 0 THEN TO_NUMBER(:DEBIT_AMT) ")
					.append("WHEN TO_NUMBER(RTRIM(:CREDIT_AMT, ',')) > 0 THEN TO_NUMBER(:CREDIT_AMT) ")
					.append("ELSE NULL END\" ").append(",\n");
		}

		if (controlFileContent.charAt(controlFileContent.length() - 2) == ',') {
			controlFileContent.deleteCharAt(controlFileContent.length() - 2);
		}
		controlFileContent.append(")\n");
		logger.info("CONTROL FILE CONTENT :::::::::::" + controlFileContent);
		String filePath = generateFilePath(targetTableName, fileLocation);

		File fileDirectory = new File(filePath).getParentFile();
		logger.info("FILE DIRECTORY ::::::::" + fileDirectory);
		if (!fileDirectory.exists()) {
			boolean dirCreated = fileDirectory.mkdir();
			if (!dirCreated) {
				throw new IOException("Failed to create directories for the control file");
			}
		}
		try (FileWriter fileWriter = new FileWriter(filePath)) {
			fileWriter.write(controlFileContent.toString());
		}

		logger.info("File Generated Successfully :::::::" + filePath);
		return filePath;
	}

	private List<ReconFieldDetailsDto> getFiledDataByTemplateId(Long templateId) {
		List<ReconFieldDetailsDto> filedData = new ArrayList<>();

		// Fetch all field details with type & format using JOIN FETCH
		List<ReconFieldDetailsMaster> reconFieldDetailsMaster = reconFieldDetailsMasterRepository
				.findFullFieldDetailsByTemplateId(templateId);
		logger.info("FILED DETAILS MASTER  =========> {}", reconFieldDetailsMaster);
		if (reconFieldDetailsMaster != null && !reconFieldDetailsMaster.isEmpty()) {
			for (ReconFieldDetailsMaster fieldDetailsMaster : reconFieldDetailsMaster) {
				ReconFieldDetailsDto fieldDetails = new ReconFieldDetailsDto();

				// Field details from master
				fieldDetails.setRfmColPosn(fieldDetailsMaster.getReconColumnPosn());
				fieldDetails.setRfmShortName(fieldDetailsMaster.getReconShortName());
				fieldDetails.setRfmColOffset(fieldDetailsMaster.getReconColumnOffset());
				fieldDetails.setReconFromPosn(fieldDetailsMaster.getReconFromPosn());
				fieldDetails.setReconToPosn(fieldDetailsMaster.getReconToPosn());

				// Field Type (already fetched)
				if (fieldDetailsMaster.getReconFieldTypeMaster() != null) {
					fieldDetails.setRftFieldTypeDesc(fieldDetailsMaster.getReconFieldTypeMaster().getFieldTypeDes());
				}

				// Field Format (already fetched)
				if (fieldDetailsMaster.getReconFieldFormatMaster() != null) {
					fieldDetails.setRffFieldFormatDesc(
							fieldDetailsMaster.getReconFieldFormatMaster().getReconFieldFormatDesc());
				}

				// Key Identify (still need to fetch from repository)
				List<ReconKeyIdentifyMaster> keyIdentify = reconKeyIdentifyMasterRepository
						.findByKeyIdentityId(fieldDetailsMaster.getReconKeyIdentifier());

				if (keyIdentify != null && !keyIdentify.isEmpty()) {
					// Assuming only 1 key per field
					fieldDetails.setKeyName(keyIdentify.get(0).getKeyName());
				}

				filedData.add(fieldDetails);
			}
		}

		logger.info("ALL COLUMNS NAME BY TEMPLATE => {}", filedData);
		return filedData;
	}

	private String generateFilePath(String targetTableName, File fileLocation) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
		String fileName = fileLocation.getName();
		if (fileName.lastIndexOf(".") > 0) {
			fileName = fileName.substring(0, fileName.lastIndexOf("."));
		}
		return uDrivePath + "/controlfile/" + fileName + timestamp + ".ctl";
	}

	private String generateBadFile() throws IOException {
		String timestamp = new SimpleDateFormat("yyyy-mm-dd-hh.mm.ss").format(new Date());
		String badFilePath = uDrivePath + "/badfile/" + timestamp + ".bad";

		File badFile = new File(badFilePath).getParentFile();
		if (!badFile.exists()) {
			Boolean dirCreated = badFile.mkdir();
			if (!dirCreated) {
				throw new IOException("Failed to create directories for the bad file");
			}
		}
		return badFilePath;
	}

	private String generateLogFile() throws IOException {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
		String logFilePath = uDrivePath + "/logfile/" + timestamp + ".log";

		File logFile = new File(logFilePath).getParentFile();
		if (!logFile.exists()) {
			boolean dirCreated = logFile.mkdir();
			if (!dirCreated) {
				throw new IOException("Failed to create directories for the log file");
			}
		}
		return logFilePath;
	}

	@Override
	public ResponseEntity<RestWithStatusList> refreshProcessData(Long processId) {
		RestWithStatusList restWithStatusList = null;
		List<Object> processList = new ArrayList<>();
		List<REProcessManager> extractionProcessList = processManagerRepository.findByProcessId(processId);
		if (!extractionProcessList.isEmpty()) {
			for (REProcessManager process : extractionProcessList) {
				if (process.getExtractionStatus().equalsIgnoreCase("Running")) {
					processList.add(process);
					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "The process is currently running....",
							processList);
				} else {
					processList.add(process);
					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Process data found successfully",
							processList);
				}
			}
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Process data not found", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public List<ReconBatchProcessEntity> extractionRunningStatus(List<File> fileList,
			ReconFileDetailsMaster reconFileDetails, ReconUser userData) {
		logger.info("FILE LIST ::::::::::::" + fileList);
		List<ReconBatchProcessEntity> processList = new ArrayList<>();
		if (!fileList.isEmpty()) {
			for (int i = 0; i < fileList.size(); i++) {
				ReconBatchProcessEntity process = new ReconBatchProcessEntity();
				process.setProcessId(reconFileDetails.getReconFileId());
				process.setProcessType("EXTRACTION");
				process.setStartTime(LocalDateTime.now().format(dateTimeFormatter));
				process.setEndTime(null);
				process.setStatus("Running");
				process.setFileName(fileList.get(i).getName());
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
				processList.add(process);
			}
		}

		return processList;
	}

	@Override
	public ResponseEntity<RestWithStatusList> refreshExtraction(List<RefreshRequestDto.ProcessManager> requestProcess) {
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
				if (process.getExtractionStatus().equalsIgnoreCase("Running")) {
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

	@Retryable(value = { SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 5000, multiplier = 2))
	private void truncateStageTable(String tableName) {
		String sql = "TRUNCATE TABLE " + tableName;
		jdbcTemplate.execute(sql);
		logger.info("TRUNCATE TABLE SUCCESSFULLY" + sql);
	}

	@Recover
	private void recoverTruncateStageTable(SQLException e, String tableName) {
		logger.error("RECOVERY: Failed to TRUNCATE TABLE {} after multiple retries (Error Code: {}). Error: {}",
				tableName, e.getErrorCode(), e.getMessage(), e);
		throw new RuntimeException("Failed to truncate table " + tableName + " after retries.", e);
	}

}
