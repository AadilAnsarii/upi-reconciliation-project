//package com.jpb.reconciliation.reconciliation.service.fileoperation;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import com.jpb.reconciliation.reconciliation.constants.FileProcessStatus;
//import com.jpb.reconciliation.reconciliation.secondary.entity.FileProcessStatusEntity;
//import com.jpb.reconciliation.reconciliation.secondary.repository.FileProcessStatusRepository;
//import com.jpb.reconciliation.reconciliation.secondary.repository.ReconOfflineRefundElmsRepository;
//
//@Service
//public class FileProcessorService {
//
//	Logger logger = LoggerFactory.getLogger(FileProcessorService.class);
//
//	@Value("${app.uDrive}")
//	private String uDrivePath;
//
//	@Value("${spring.datasource.secondary.username}")
//	private String secondUserName;
//
//	@Value("${spring.datasource.secondary.password}")
//	private String secondPassword;
//
//	@Value("${sqlldr.secondUrl}")
//	private String url;
//
//	@Autowired
//	ReconOfflineRefundElmsRepository tccCbsTtumUpiInwRepository;
//
//	@Autowired
//	FileProcessStatusRepository fileProcessStatusRepository;
//
////	@Async
//	public Boolean processAndLoadData(FileProcessStatusEntity fileEntryForProcess) {
//		Boolean processLoadStatus = false;
//		FileProcessStatus finalStatus = FileProcessStatus.PROCESSED_FAILURE;
//
//		try {
//			String filePath = fileEntryForProcess.getFilePath();
//			String controlFile = generateControlFile(filePath, fileEntryForProcess);
//			String logFile = generateLogFile();
//			String badFile = generateBadFile();
//
//			logger.info("Control File Is :: {}", controlFile);
//			logger.info("Log File Is :: {}, Bad File Is {}", logFile, badFile);
//            
//			if (!controlFile.isEmpty() && !logFile.isEmpty()) {
//				Boolean sqlLoaderSuccess = startLoadingData(controlFile, logFile, badFile, fileEntryForProcess);
//				logger.info("LOADER OUTPUT :::::::::::::::::::: {}", sqlLoaderSuccess);
//
//				if (sqlLoaderSuccess) {
//					finalStatus = FileProcessStatus.PROCESSED_SUCCESS;
//					processLoadStatus = true;
//				} else {
//					finalStatus = FileProcessStatus.PROCESSED_FAILURE;
//					processLoadStatus = false;
//				}
//			}
//
//		} catch (IOException e) {
//			logger.error("IO Error during file preparation or directory creation for file ID {}",
//					fileEntryForProcess.getFileId(), e);
//			finalStatus = FileProcessStatus.PROCESSED_FAILURE;
//			processLoadStatus = false;
//		} catch (Exception e) {
//			logger.error("Unexpected error during file processing for file ID {}", fileEntryForProcess.getFileId(), e);
//			finalStatus = FileProcessStatus.PROCESSED_FAILURE;
//		} finally {
//			fileEntryForProcess.setUploadDataStatus(finalStatus);
////			fileEntryForProcess.setApprovalDate(LocalDateTime.now());
//			fileProcessStatusRepository.save(fileEntryForProcess);
//			logger.info("File ID {} processing finished with status: {}", fileEntryForProcess.getFileId(), finalStatus);
//		}
//		return processLoadStatus;
//	}
//
//	private String generateControlFile(String filePath, FileProcessStatusEntity fileEntryForProcess) {
//		StringBuilder controlFileContent = new StringBuilder();
//		if (fileEntryForProcess.getFileType().equalsIgnoreCase("ELMS")) {
//			controlFileContent.append("OPTIONS (multithreading=TRUE, PARALLEL=TRUE) \n");
//		} else if (fileEntryForProcess.getFileType().equalsIgnoreCase("MERCHANT")) {
//			controlFileContent.append("OPTIONS (multithreading=TRUE, skip=1, PARALLEL=TRUE) \n");
//		}
//
//		controlFileContent.append("UNRECOVERABLE \n");
//		controlFileContent.append("LOAD DATA \n");
//		controlFileContent.append("INFILE '").append(filePath).append("'\n");
//		if (fileEntryForProcess.getFileType().equalsIgnoreCase("ELMS")) {
//			controlFileContent.append("INTO TABLE ").append("RCN_OFFLINE_REFUND_ELMS").append("\n");
//		} else if (fileEntryForProcess.getFileType().equalsIgnoreCase("MERCHANT")) {
//			controlFileContent.append("INTO TABLE ").append("RCN_OFFLINE_REFUND_MERCHANT").append("\n");
//		}
//
//		controlFileContent.append("append \n");
//		if (fileEntryForProcess.getFileType().equalsIgnoreCase("ELMS")) {
//			controlFileContent.append("FIELDS TERMINATED BY '").append("|").append("'\n");
//		} else if (fileEntryForProcess.getFileType().equalsIgnoreCase("MERCHANT")) {
//			controlFileContent.append("FIELDS TERMINATED BY '").append(",").append("'\n");
//		}
//
//		controlFileContent.append("TRAILING NULLCOLS \n");
//		controlFileContent.append("(\n");
//
//		if (fileEntryForProcess.getFileType().equalsIgnoreCase("ELMS")) {
//			controlFileContent.append(" ACCOUNT_NUMBER ").append(",\n");
//			controlFileContent.append(" DEBIT_CREDIT_FLAG ").append(",\n");
//			controlFileContent.append(" NARRATION ").append(",\n");
//			controlFileContent.append(" INSERT_CODE ").append(",\n");
//			controlFileContent.append(" AMOUNT ").append(",\n");
//			controlFileContent.append(" FILE_NAME ").append("CONSTANT ").append("'")
//					.append(fileEntryForProcess.getFileName()).append("'").append(",\n");
//			controlFileContent.append(" USER_ID ").append("CONSTANT ").append("'")
//					.append(fileEntryForProcess.getUploadedBy()).append("'").append(",\n");
//		} else if (fileEntryForProcess.getFileType().equalsIgnoreCase("MERCHANT")) {
//			controlFileContent.append(" BANK_ADJ_REF ").append(",\n");
//			controlFileContent.append(" FLAG ").append(",\n");
//			controlFileContent.append(" SHTDAT ").append(",\n");
//			controlFileContent.append(" ADJAMT ").append(",\n");
//			controlFileContent.append(" SHSER ").append(",\n");
//			controlFileContent.append(" UTXID ").append(",\n");
//			controlFileContent.append(" FILE_NAME ").append(",\n");
//			controlFileContent.append(" REASON_CODE ").append(",\n");
//			controlFileContent.append(" SPECIFY_OTHER ").append(",\n");
//			controlFileContent.append(" USER_ID ").append("CONSTANT ").append("'")
//					.append(fileEntryForProcess.getUploadedBy()).append("'").append(",\n");
//		}
//
//		if (controlFileContent.charAt(controlFileContent.length() - 2) == ',') {
//			controlFileContent.deleteCharAt(controlFileContent.length() - 2);
//		}
//		controlFileContent.append(")\n");
//		logger.info("CONTROL FILE CONTENT :::::::::::" + controlFileContent);
//
//		String fileControlFilePath = generateFilePath(fileEntryForProcess);
//
//		try (FileWriter fileWriter = new FileWriter(fileControlFilePath)) {
//			fileWriter.write(controlFileContent.toString());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return fileControlFilePath;
//	}
//
//	private String generateFilePath(FileProcessStatusEntity fileEntryForProcess) {
//		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
//		String fileName = fileEntryForProcess.getFileName();
//		if (fileName.lastIndexOf(".") > 0) {
//			fileName = fileName.substring(0, fileName.lastIndexOf("."));
//		}
//		return uDrivePath + "/controlfile/" + fileName + timestamp + ".ctl";
//	}
//
//	private String generateBadFile() throws IOException {
//		String timestamp = new SimpleDateFormat("yyyy-mm-dd-hh.mm.ss").format(new Date());
//		String badFilePath = uDrivePath + "/badfile/" + timestamp + ".bad";
//
//		File badFile = new File(badFilePath).getParentFile();
//		if (!badFile.exists()) {
//			Boolean dirCreated = badFile.mkdir();
//			if (!dirCreated) {
//				throw new IOException("Failed to create directories for the bad file");
//			}
//		}
//		return badFilePath;
//	}
//
//	private String generateLogFile() throws IOException {
//		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
//		String logFilePath = uDrivePath + "/logfile/" + timestamp + ".log";
//
//		File logFile = new File(logFilePath).getParentFile();
//		if (!logFile.exists()) {
//			boolean dirCreated = logFile.mkdir();
//			if (!dirCreated) {
//				throw new IOException("Failed to create directories for the log file");
//			}
//		}
//		return logFilePath;
//	}
//
//	private Boolean startLoadingData(String controlFile, String logFile, String badFile,
//			FileProcessStatusEntity fileEntryForProcess) {
//		final AtomicBoolean processStatus = new AtomicBoolean(false);
//		String cmd = "sqlldr " + secondUserName + "/" + secondPassword + url + " control=" + controlFile + " log="
//				+ logFile + " bad=" + badFile + " direct=true";
//		logger.info("SQL LOADER COMMAND :::::::::: " + cmd);
//
//		StringBuilder output = new StringBuilder();
//		StringBuilder errorOutput = new StringBuilder();
//		String dataCount = null;
//		int exitCode = -1;
//
//		try {
//			Process process = Runtime.getRuntime().exec(cmd);
//			logger.info("Output of sqlloader ::::::::::");
//
//			// Thread to read standard output
//			Thread outputReader = new Thread(() -> {
//				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//					String line;
//					Pattern pattern = Pattern.compile("(\\d+) Rows successfully loaded.");
//					Matcher matcher;
//					while ((line = reader.readLine()) != null) {
//						output.append(line).append("\n");
//						matcher = pattern.matcher(line);
//						if (matcher.find()) {
////							reconProcessManager.setDataCount(matcher.group(1));
//							processStatus.set(true);
//						}
//					}
//				} catch (IOException e) {
//					logger.error("Error reading process output: {}", e.getMessage());
//				}
//			});
//
//			// Thread to read error output
//			Thread errorReader = new Thread(() -> {
//				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
//					String line;
//					while ((line = reader.readLine()) != null) {
//						errorOutput.append(line).append("\n");
//					}
//				} catch (IOException e) {
//					logger.error("Error reading process error output: {}", e.getMessage());
//				}
//			});
//
//			outputReader.start();
//			errorReader.start();
//
//			exitCode = process.waitFor();
//
//			// Wait for threads to finish reading streams
//			outputReader.join();
//			errorReader.join();
//			// Log both outputs
//			logger.info("SQL Loader Standard Output:\n" + output.toString());
//			logger.info("SQL Loader Error Output:\n" + errorOutput.toString());
//
//		} catch (IOException | InterruptedException e) {
//			Thread.currentThread().interrupt();
//			logger.error("Error executing SQL Loader command: {}", e.getMessage(), e);
//			errorOutput.append("Process execution failed: ").append(e.getMessage());
//			processStatus.set(false);
//		}
//		return processStatus.get();
//	}
//
//
//}