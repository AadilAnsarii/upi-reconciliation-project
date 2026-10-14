package com.jpb.reconciliation.reconciliation.service.v2;

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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldFormatMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldTypeMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconKeyIdentifyMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.AuditLogManagerService;
import com.jpb.reconciliation.reconciliation.service.ExtractionServiceImpl;
import com.jpb.reconciliation.reconciliation.service.NTSLSettlementService;
import com.jpb.reconciliation.reconciliation.util.v2.CommonAiReport;

@Service
public class ExtractionAiServiceImpl implements ExtractionAiService {

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
	SqlLoaderAiService sqlLoaderAiService;

	@Autowired
	CommonAiReport commonAiReport;

	@Autowired
	FileOpearationAiService fileOpearationAiService;

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

	public ExtractionAiServiceImpl(@Qualifier("extractionExecutor") Executor extractionExecutor) {
		this.extractionExecutor = extractionExecutor;
	}

	@Override
	public List<ReconBatchProcessEntity> extractionRunningStatus(List<File> processedFiles,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconUser userData) {
		logger.info("File List For Extraction Processing ::::::::::::" + processedFiles);
		List<ReconBatchProcessEntity> processList = new ArrayList<>();
		for (int i = 0; i < processedFiles.size(); i++) {
			ReconBatchProcessEntity process = new ReconBatchProcessEntity();

			process.setTemplateId(reconTemplateFileDetails.get().getTemplate().getTemplateId());
			process.setProcessType("EXTRACTION");
			process.setStartTime(LocalDateTime.now().format(dateTimeFormatter));
			process.setEndTime(null);
			process.setStatus("Running");
			process.setFileName(processedFiles.get(i).getName());
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
		return processList;
	}

	@Override
	public CompletableFuture<String> startExtraction(Optional<ReconTmpltFieldDtls> reconTemplateFileDetails,
			List<ReconBatchProcessEntity> runningExtraction, List<File> processedFiles, ReconUser userData) {

//		ReconTemplateDetails templateDetails = reconFileDetails.getReconTemplateDetails();
		truncateStageTable(reconTemplateFileDetails.get().getTemplate().getStageTabName());
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		if (!runningExtraction.isEmpty()) {
			for (int i = 0; i < runningExtraction.size(); i++) {
				int index = i;
				CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
					try {
						String generateControlFile = generateControlFile(processedFiles.get(index),
								reconTemplateFileDetails.get().getTemplate().getDelimiter(),
								reconTemplateFileDetails.get().getTemplate().getReconTemplateId(),
								reconTemplateFileDetails.get().getTemplate().getStageTabName(),
								reconTemplateFileDetails.get().getTemplate().getTemplateName(),
								reconTemplateFileDetails);

						String generateLogFile = generateLogFile();
						String generateBadFile = generateBadFile();
						logger.info("CONTROL FILE PATH :::::::" + generateControlFile);
						logger.info("LOG FILE PATH :::::::::::" + generateLogFile);
						logger.info("BAD FILE PATH :::::::::::" + generateBadFile);
						if (!generateControlFile.isEmpty() && !generateLogFile.isEmpty()) {
							String sqlLoaderStatus = sqlLoaderAiService.startLoading(generateControlFile,
									generateLogFile, generateBadFile, reconTemplateFileDetails,
									runningExtraction.get(index), userData, processedFiles.get(index));
							logger.info("LOADER OUTPUT ::::::::::::::::::::" + sqlLoaderStatus);
						}
					} catch (Exception e) {
						logger.error("Error during extraction for file: {}", processedFiles.get(index).getName(), e);
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
			fileOpearationAiService.moveExtractedFiles(reconTemplateFileDetails);
//			if (reconTemplateFileDetails.getReconTemplateDetails().getSettlementFlag().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
//				Boolean ntslSettleFlag = ntslSettlementService.ntslSettlementProcess(reconTemplateFileDetails);
//				logger.info("ntslSettleFlag" + ntslSettleFlag);
//			}
			Boolean globalReportFlag;
			try {
				globalReportFlag = commonAiReport.generateGlobalReconciliationReport(reconTemplateFileDetails);
			} catch (Exception e) {
				logger.error("Global report generation failed for template: "
						+ reconTemplateFileDetails.get().getTemplate().getTemplateId(), e);
				globalReportFlag = false;
			}
			for (ReconBatchProcessEntity process : runningExtraction) {
				// Re-fetch — the async sqlLoaderAiService task updates extractionStatus
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

	@Retryable(value = { SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 5000, multiplier = 2))
	private void truncateStageTable(String tableName) {
		String sql = "TRUNCATE TABLE " + tableName;
		jdbcTemplate.execute(sql);
		logger.info("TRUNCATE TABLE SUCCESSFULLY" + sql);
	}

	public String generateControlFile(File fileLocation, String fileSeprator, Long templateId, String targetTableName,
			String fileName, Optional<ReconTmpltFieldDtls> reconTemplateFileDetails) throws IOException {
		logger.info("Template Details ::::::::::" + reconTemplateFileDetails);
//		List<ReconFieldDetailsDto> getFiledData = getFiledDataByTemplateId(templateId);
		StringBuilder controlFileContent = new StringBuilder();
//		logger.info("FILE DATA ::::::::" + getFiledData);

		// Fixed-width records have no delimiter — fields are read via POSITION(),
		// so FIELDS TERMINATED BY must be omitted (there is no separator to declare).
		String templateType = reconTemplateFileDetails.get().getTemplate().getTemplateType();
		boolean isFixedWidth = templateType != null && (templateType.equalsIgnoreCase("FIXED_WIDTH")
				|| templateType.equalsIgnoreCase("FIXED WIDTH") || templateType.equalsIgnoreCase("FIXED"));
		boolean isExcel = templateType != null && templateType.equalsIgnoreCase("EXCEL");
		boolean isXml = templateType != null && templateType.equalsIgnoreCase("XML");

		// EXCEL/XML: both are converted to CSV before reaching this point
		// (ExcelToCsvConvertorService / XmlToCsvConvertorService), and both converters
		// always write exactly ONE header line into that CSV regardless of the
		// source file's own structure — so skip=1 unconditionally here, not the
		// original headerLineCount (EXCEL) or the template's hasHeader flag (XML,
		// which is normally CommonConstants.INACTIVE_N since raw XML has no header row of its own).
		if (isExcel || isXml) {
			controlFileContent.append("OPTIONS (multithreading=TRUE, skip=1, PARALLEL=TRUE) \n");
		} else if (reconTemplateFileDetails.get().getTemplate().getHasHeader().equals(CommonConstants.ACTIVE_Y)) {
			controlFileContent.append("OPTIONS (multithreading=TRUE, skip=")
					.append(reconTemplateFileDetails.get().getTemplate().getHeaderLineCount())
					.append(", PARALLEL=TRUE) \n");
		} else {
			controlFileContent.append("OPTIONS (multithreading=TRUE, PARALLEL=TRUE) \n");
		}

		controlFileContent.append("UNRECOVERABLE \n");
		controlFileContent.append("LOAD DATA \n");
//		controlFileContent.append("INFILE '").append(reconTemplateFileDetails.get().getTemplate().getFilePath())
//				.append("'\n");
		controlFileContent.append("INFILE '").append(fileLocation.getAbsolutePath()).append("'\n");
		controlFileContent.append("INTO TABLE ").append(reconTemplateFileDetails.get().getTemplate().getStageTabName())
				.append("\n");
		controlFileContent.append("append \n");

		// EXCEL/XML templates always store delimiter=null (delimiter is a CSV-only
		// config field, nulled out at save time), but both converters always write
		// comma-separated CSV — so the control file must use ',' regardless of the
		// stored (null) delimiter value, otherwise "FIELDS TERMINATED BY 'null'" breaks the load.
		if ((fileSeprator == null || fileSeprator.isEmpty()) && (isExcel || isXml)) {
			fileSeprator = ",";
		}

		if (!isFixedWidth) {
			controlFileContent.append("FIELDS TERMINATED BY '").append(fileSeprator).append("'")
					.append(" OPTIONALLY ENCLOSED BY '\"' \n");
		}

//		if (fileName.equalsIgnoreCase("EPIK_AEP_AEPS") || fileName.equalsIgnoreCase("CBS_AEPS")
//				|| fileName.equalsIgnoreCase("CBS OB") || fileName.equalsIgnoreCase("ELMS_CBS")
//				|| fileName.equalsIgnoreCase("CBS_GL") || fileName.equalsIgnoreCase("UPI_MERCHANT")
//				|| fileName.equalsIgnoreCase("DEBITCARD_CBS") || fileName.equalsIgnoreCase("POS_PRESENTMENT_FILE")) {
//			controlFileContent.append("FIELDS TERMINATED BY '").append(fileSeprator).append("'")
//					.append(" OPTIONALLY ENCLOSED BY '\"' \n");
//		} else {
//			controlFileContent.append("FIELDS TERMINATED BY '").append(fileSeprator).append("'\n");
//		}

		controlFileContent.append("TRAILING NULLCOLS \n");
		controlFileContent.append("(\n");

		for (ReconTmpltFieldDtls filed : reconTemplateFileDetails.get().getTemplate().getFieldDetails()) {
			if (!filed.getReconFieldTypeMaster().getFieldTypeDes().equalsIgnoreCase("STRING")) {
				if (filed.getReconFieldTypeMaster().getFieldTypeDes().equalsIgnoreCase("NUMBER")
						|| filed.getReconFieldTypeMaster().getFieldTypeDes().equalsIgnoreCase("DECIMAL")) {

					String numSourceExpr = CommonConstants.ACTIVE_Y.equalsIgnoreCase(filed.getTrimFlag())
							? "TRIM(BOTH '''' FROM :" + filed.getShortName() + ")"
							: ":" + filed.getShortName();

					if (isFixedWidth) {
						controlFileContent.append(" ").append(filed.getShortName()).append(" POSITION(")
								.append(filed.getFromPosition()).append(":").append(filed.getToPosition())
								.append(")").append(" ").append("\"TO_NUMBER(").append(numSourceExpr)
								.append(")\", \n");
					}

					else {
						controlFileContent.append(" ").append(filed.getShortName()).append(" ").append("\"TO_NUMBER(")
								.append(numSourceExpr).append(")\", \n");
					}

//					if (fileName.equalsIgnoreCase("FEBA SWITCH DB") || fileName.equalsIgnoreCase("EPIK_AEP_AEPS")
//							|| fileName.equalsIgnoreCase("CBS_AEPS")
//							|| fileName.equalsIgnoreCase("AEPS CREDIT ADJUSTMENT")
//							|| fileName.equalsIgnoreCase("ELMS_CBS") || fileName.equalsIgnoreCase("DEBITCARD_CBS")) {
//						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ").append("\"TO_")
//								.append(filed.getRftFieldTypeDesc()).append("(:").append(filed.getRfmShortName())
//								.append(")\" ,\n");
//					} else if (fileName.equalsIgnoreCase("AEPS ACQUIRER ISSUER FILE")
//							|| fileName.equalsIgnoreCase("AEPS ISSUER FILE")
//							|| fileName.equalsIgnoreCase("DEBITCARD_ATM_RAW")
//							|| fileName.equalsIgnoreCase("DEBITCARD_POS_RAW")) {
//						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" POSITION(")
//								.append(filed.getReconFromPosn()).append(":").append(filed.getReconToPosn()).append(")")
//								.append(" ").append("\"TO_").append(filed.getRftFieldTypeDesc()).append("(:")
//								.append(filed.getRfmShortName()).append(") /100\" ,\n");
//					} else {
//						controlFileContent.append(" ").append(filed.getShortName()).append(" ")
//								.append(filed.getReconFieldTypeMaster().getFieldTypeDes()).append(" ").append("\"")
//								.append(filed.getReconFieldTypeMaster().getFieldTypeDes()).append("\"").append(",\n");
//					}
				} else if (filed.getReconFieldTypeMaster().getFieldTypeDes().equalsIgnoreCase("TODATE")) {
					controlFileContent.append(" ").append(filed.getShortName()).append(" ").append("DATE").append(" \"")
							.append(filed.getReconFieldFormatMaster().getReconFieldFormatDesc()).append("\" \n");
				} else if (filed.getReconFieldTypeMaster().getFieldTypeDes().equalsIgnoreCase("DATE")
						|| filed.getReconFieldTypeMaster().getFieldTypeDes().equalsIgnoreCase("TIMESTAMP")) {

					if (isFixedWidth) {
						controlFileContent.append(" ").append(filed.getShortName()).append(" POSITION(")
								.append(filed.getFromPosition()).append(":").append(filed.getToPosition()).append(") ")
								.append(filed.getReconFieldTypeMaster().getFieldTypeDes()).append(" \"")
								.append(filed.getReconFieldFormatMaster().getReconFieldFormatDesc()).append("\" ,\n");
					} else if (filed.getTrimFlag().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
						controlFileContent.append(" ").append(filed.getShortName()).append(" ").append("\"TO_")
								.append(filed.getReconFieldTypeMaster().getFieldTypeDes())
								.append("(TRIM(BOTH '''' FROM :").append(filed.getShortName()).append("), '")
								.append(filed.getReconFieldFormatMaster().getReconFieldFormatDesc()).append("')\"").append(",\n");
					} else {
						controlFileContent.append(" ").append(filed.getShortName()).append(" ")
								.append(filed.getReconFieldTypeMaster().getFieldTypeDes()).append(" ").append("\"")
								.append(filed.getReconFieldFormatMaster().getReconFieldFormatDesc()).append("\"").append(",\n");
					}

//					if (fileName.equalsIgnoreCase("CBS_AEPS") || fileName.equalsIgnoreCase("ELMS_CBS")
//							|| fileName.equalsIgnoreCase("DEBITCARD_CBS")) {
//						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ").append("\"TO_")
//								.append(filed.getRftFieldTypeDesc()).append("(TRIM(BOTH '''' FROM :")
//								.append(filed.getRfmShortName()).append("), '").append(filed.getRffFieldFormatDesc())
//								.append("')\"").append(",\n");
//					} else if (fileName.equalsIgnoreCase("AEPS ACQUIRER ISSUER FILE")
//							|| fileName.equalsIgnoreCase("AEPS ISSUER FILE")
//							|| fileName.equalsIgnoreCase("DEBITCARD_ATM_RAW")
//							|| fileName.equalsIgnoreCase("DEBITCARD_POS_RAW")) {
//						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" POSITION(")
//								.append(filed.getReconFromPosn()).append(":").append(filed.getReconToPosn())
//								.append(") ").append(filed.getRftFieldTypeDesc()).append(" \"")
//								.append(filed.getRffFieldFormatDesc()).append("\" ,\n");
//					} else {
//						controlFileContent.append(" ").append(filed.getRfmShortName()).append(" ")
//								.append(filed.getRftFieldTypeDesc()).append(" ").append("\"")
//								.append(filed.getRffFieldFormatDesc()).append("\"").append(",\n");
//					}
				} else if (filed.getReconFieldTypeMaster().getFieldTypeDes().equalsIgnoreCase("BOOLEAN")) {
					// Stage column is VARCHAR2(1) (Oracle tables have no BOOLEAN type,
					// see SP_STAGE_TAB_CREATION), so load it as a plain string value.
					if (isFixedWidth) {
						controlFileContent.append(" ").append(filed.getShortName()).append(" POSITION(")
								.append(filed.getFromPosition()).append(":").append(filed.getToPosition()).append(")")
								.append(",\n");
					} else if (filed.getTrimFlag().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
						controlFileContent.append(" ").append(filed.getShortName()).append(" \"TRIM(BOTH '''' FROM :")
								.append(filed.getShortName()).append(")\"").append(",\n");
					} else {
						controlFileContent.append(" ").append(filed.getShortName()).append(",\n");
					}
				} else {
					controlFileContent.append(" ").append(filed.getShortName()).append(" ")
							.append(filed.getReconFieldTypeMaster().getFieldTypeDes()).append(" ").append("\"")
							.append(filed.getReconFieldTypeMaster().getFieldTypeDes()).append("\"").append(",\n");
					// .append(filed.getKeyName()).append(" ").append(filed.getRfmColOffset())
				}
			} else {
				if (isFixedWidth) {
					controlFileContent.append(" ").append(filed.getShortName()).append(" POSITION(")
							.append(filed.getFromPosition()).append(":").append(filed.getToPosition()).append(")")
							.append(",\n");
				} else if (filed.getTrimFlag().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
					controlFileContent.append(" ").append(filed.getShortName()).append(charSpec(filed))
							.append(" \"TRIM(BOTH '''' FROM :")
							.append(filed.getShortName()).append(")\"").append(",\n");
				} else {
					controlFileContent.append(" ").append(filed.getShortName()).append(charSpec(filed))
							.append(",\n");
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
				|| fileName.equalsIgnoreCase("DEBITCARD_CBS")
				|| fileName.equalsIgnoreCase("CBS_TRANSACTION_PRODUCT_GL")) {
			controlFileContent.append("DR_CR_FLAG").append(" \"CASE ")
					.append("WHEN TO_NUMBER(RTRIM(:DEBIT_AMT, ',')) > 0 THEN 'D' ")
					.append("WHEN TO_NUMBER(RTRIM(:CREDIT_AMT, ',')) > 0 THEN 'C' ").append("ELSE NULL END\" ")
					.append(",\n");
		}

		if (fileName.equalsIgnoreCase("CBS_AEPS") || fileName.equalsIgnoreCase("ELMS_CBS")
				|| fileName.equalsIgnoreCase("DEBITCARD_CBS")
				|| fileName.equalsIgnoreCase("CBS_TRANSACTION_PRODUCT_GL")) {
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

	/**
	 * Explicit {@code CHAR(n)} spec for a delimited string field.
	 *
	 * SQL*Loader caps a delimited character field at 255 bytes unless the control file
	 * states a length. The generator did not, so every template with a field longer than
	 * 255 characters lost rows to "Field in data file exceeds maximum length" — and since
	 * the loader aborts at 50 errors, the whole extraction failed. Seen on a Diebold sheet
	 * whose RAW_TRANSACTION_BLOCK column is 575 chars.
	 *
	 * Emitted only for delimited loads; fixed-width fields carry POSITION(from:to), which
	 * already bounds them. Returns "" when the length is unknown, keeping the old
	 * behaviour rather than guessing.
	 */
	private String charSpec(ReconTmpltFieldDtls field) {
		Long length = field.getFieldLength();
		if (null == length || length <= 0) {
			return "";
		}
		return " CHAR(" + length + ")";
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

}
