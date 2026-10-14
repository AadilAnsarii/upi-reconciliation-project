package com.jpb.reconciliation.reconciliation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.repository.ReportRepository;
import com.jpb.reconciliation.reconciliation.service.jasper.JasperReportService;

import net.sf.jasperreports.engine.JRException;

@Service
public class SqlLoaderServiceImpl implements SqlLoaderService {

	private Logger logger = LoggerFactory.getLogger(SqlLoaderServiceImpl.class);

	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");

	@Autowired
	ReconProcessManagerRepository processManagerRepository;

	@Autowired
	ReportRepository reportRepository;

	@Autowired
	ReportGenerationService reportGenerationService;

	@Autowired
	AuditLogManagerService auditLogManagerService;

	@Autowired
	SegretionService segretionService;

	@Autowired
	ReconUserRepository reconUserRepository;

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@Autowired
	DataUpdateService dataUpdateService;
	
	@Value("${spring.datasource.username}")
	private String userName;

	@Value("${spring.datasource.password}")
	private String password;

	@Value("${sqlldr.url}")
	private String url;

	@Autowired
	JasperReportService jasperReportService;

	@Override
	public String startLoading(String controlFile, String logFile, String badFile,
			ReconFileDetailsMaster reconFileDetails, ReconBatchProcessEntity reconProcessManager, ReconUser userDetails,
			File file) throws JRException, IOException {

		String cmd = "sqlldr " + userName + "/" + password + url + " control=" + controlFile + " log=" + logFile
				+ " bad=" + badFile + " direct=true";
		logger.info("SQL LOADER COMMAND :::::::::: " + cmd);

		StringBuilder output = new StringBuilder();
		StringBuilder errorOutput = new StringBuilder();
		String dataCount = null;
		int exitCode = -1;

		try {
			Process process = Runtime.getRuntime().exec(cmd);
			logger.info("Output of sqlloader ::::::::::");

			// Thread to read standard output
			Thread outputReader = new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					Pattern pattern = Pattern.compile("(\\d+) Rows successfully loaded.");
					Matcher matcher;
					while ((line = reader.readLine()) != null) {
						output.append(line).append("\n");
						matcher = pattern.matcher(line);
						if (matcher.find()) {
							reconProcessManager.setDataCount(matcher.group(1));
						}
					}
				} catch (IOException e) {
					logger.error("Error reading process output: {}", e.getMessage());
				}
			});

			// Thread to read error output
			Thread errorReader = new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						errorOutput.append(line).append("\n");
					}
				} catch (IOException e) {
					logger.error("Error reading process error output: {}", e.getMessage());
				}
			});

			outputReader.start();
			errorReader.start();

			exitCode = process.waitFor();

			// Wait for threads to finish reading streams
			outputReader.join();
			errorReader.join();

			// Log both outputs
			logger.info("SQL Loader Standard Output:\n" + output.toString());
			logger.info("SQL Loader Error Output:\n" + errorOutput.toString());

		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Error executing SQL Loader command: {}", e.getMessage(), e);
			errorOutput.append("Process execution failed: ").append(e.getMessage());
		}

		dataCount = reconProcessManager.getDataCount();
		updateBatchProcessStatus(reconProcessManager, reconFileDetails, userDetails, exitCode, output.toString(),
				errorOutput.toString(), dataCount);

		if ("Completed".equalsIgnoreCase(reconProcessManager.getExtractionStatus())) {
			// EXTRACTION COMPLETED
			// IF FILE UPDATE FLAG IS Y THEN START PROCESS
			if (reconFileDetails.getFileUpdateFlag().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
				Boolean fileDataUpdateStatus = dataUpdateService.dataUpdateProcess(reconFileDetails,
						reconProcessManager);
				if (Boolean.FALSE.equals(fileDataUpdateStatus)) {
					logger.info("SP Data Update FAILED To EXECUTE :::::::::::::::" + fileDataUpdateStatus);
					return null;
				} else if (reconFileDetails.getReconFileName().equalsIgnoreCase("CBS_AEPS")
						|| reconFileDetails.getReconFileName().equalsIgnoreCase("EPIK_AEP_AEPS")) {
					reportGenerationService.writeReversalReport(reconFileDetails, reconProcessManager);
				}
			}


			// SEGRETION PROCESS STARTED
			Boolean segregationStatus = segretionService.startSegretion(reconProcessManager, reconFileDetails);
			logger.info("Segregation Status :::::::::::::::" + segregationStatus);
			if (Boolean.TRUE.equals(segregationStatus)) {
				ResponseEntity<ResponseDto> result = reportGenerationService.generateReport(reconFileDetails, output,
						reconProcessManager, dataCount, file);
				logger.info("REPORT STATUS ::::::::::::::::::" + result);
				
				if (result != null && result.getBody() != null) {
					ResponseDto responseDto = result.getBody();
					String statusCode = responseDto.getStatusCode();
					if (CommonConstants.STATUS_200.equalsIgnoreCase(statusCode)) {
						reconProcessManager.setReportStatus("Completed");
						reconProcessManager.setStatus("Completed");
					} else {
						reconProcessManager.setStatus("Error");
						reconProcessManager.setReportStatus("Error");
					}
					reconBatchProcessEntityRepository.save(reconProcessManager);
					reconBatchProcessEntityRepository.flush();
				}
			} else {
				reconProcessManager.setStatus("Error");
				reconProcessManager.setReportStatus("Error");
				reconBatchProcessEntityRepository.save(reconProcessManager);
				reconBatchProcessEntityRepository.flush();
			}
		} else {
			reconProcessManager.setExtractionStatus("Error");
			reconProcessManager.setStatus("Error");
			reconProcessManager.setSegretionStatus("Error");
			reconProcessManager.setReportStatus("Error");
		}

		auditLogManagerService.extractionAudit(reconProcessManager, userDetails);
		return output.toString();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void updateBatchProcessStatus(ReconBatchProcessEntity reconProcessManager,
			ReconFileDetailsMaster reconFileDetails, ReconUser userDetails, int exitCode, String processOutput,
			String errorOutput, String dataCount) {
		if (exitCode != 0) {
			reconProcessManager.setExtractionStatus("Error");
			reconProcessManager.setStatus("Error");
			reconProcessManager.setSegretionStatus("Error");
			reconProcessManager.setReportStatus("Error");
			String combinedError = "SQL*Loader Exit Code: " + exitCode + "\n" + "Standard Output:\n" + processOutput
					+ "\n" + "Error Output:\n" + errorOutput;
			reconProcessManager.setErrorDescription(combinedError);
		} else {
			reconProcessManager.setExtractionStatus("Completed");
			reconProcessManager.setEndTime(LocalDateTime.now().format(dateTimeFormatter));
			reconProcessManager.setDataCount(dataCount);
			reconProcessManager.setStatus("Completed");
		}
		logger.info("SQL LOADER STATUS ::::::::::::::::::::::" + reconProcessManager);
		reconBatchProcessEntityRepository.save(reconProcessManager);
		reconBatchProcessEntityRepository.flush();
		logger.info("Updated ReconBatchProcessEntity status for process ID: {}", reconProcessManager.getProcessId());
	}
}