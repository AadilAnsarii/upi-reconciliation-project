package com.jpb.reconciliation.reconciliation.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.LoadMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.LoadMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.ExtractionService;
import com.jpb.reconciliation.reconciliation.service.LoadMasterService;
import com.jpb.reconciliation.reconciliation.service.LoadMasterServiceImpl;
import com.jpb.reconciliation.reconciliation.service.ReportGenerationService;
import com.jpb.reconciliation.reconciliation.service.SegretionService;
import com.jpb.reconciliation.reconciliation.service.excelreader.ExcelToCsvConvertorService;

import net.sf.jasperreports.engine.JRException;

@RestController
@RequestMapping(path = "/api/v1/extraction")
public class ExtractionController {

	@Autowired
	ExtractionService extractionService;

	@Autowired
	SegretionService segretionService;

	@Autowired
	private LoadMasterService loadMasterService;

	@Autowired
	ReportGenerationService reportGenerationService;

	@Autowired
	ReconUserRepository reconUserRepository;

	@Autowired
	private LoadMasterRepository loadMasterRepository;

	@Autowired
	ExcelToCsvConvertorService excelToCsvConvertorService;

	Logger logger = LoggerFactory.getLogger(ExtractionController.class);

	@Autowired
	ReconFileDetailsMasterRepository reconFileDetailsMasterRepository;

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@GetMapping(value = "/start-extraction", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> startExtraction(@RequestParam Long processId,
			@AuthenticationPrincipal UserDetails userDetails) throws IOException, InterruptedException, JRException {
		RestWithStatusList restWithStatusList;
		List<Object> runningProcessData = new ArrayList<>();
		List<File> processedFiles = new ArrayList<>();
		// Find file details by file id
		ReconFileDetailsMaster reconFileDetails = reconFileDetailsMasterRepository.findByReconFileId(processId);
		logger.info("FILE DETAILS WITH TEMPLATE DETAILS :::::::::::::::::" + reconFileDetails);
		ReconUser userData = reconUserRepository.findByUsername(userDetails.getUsername()).get();
		// Find running process by file id
		List<ReconBatchProcessEntity> checkProcessIsRunning = reconBatchProcessEntityRepository
				.findByProcessIdAndStatus(processId, "Running");
		// Find completed process by its file id
		List<ReconBatchProcessEntity> checkProcessCompleted = reconBatchProcessEntityRepository
				.findByProcessIdAndStatus(processId, "Completed");

		if (reconFileDetails != null) {
			// Check GL Flag For Extraction Process If N Then check GL Master Table
			if (reconFileDetails.getRfdGlFlag().equalsIgnoreCase(CommonConstants.INACTIVE_N)) {
				// Get file list from file path store at file details
				List<File> originalFileList = getFileListFromDirectory(reconFileDetails);
				// Check which type of file, if found excel then convert into csv
				// Check any space into file name

				for (File file : originalFileList) {
					String fileName = file.getName();
					// Checking spaces contain into file name
					if (fileName.contains(" ")) {
						restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
								"Please remove space from uploaded input file name.", runningProcessData);
						return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
					}

					File convertedFile = null;
					try {
						if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
							logger.info("Converting Excel file to CSV: " + fileName);
							excelToCsvConvertorService.convertExcelToCsv(file);
							// Construct the path for the converted CSV file
							convertedFile = new File(file.getParent(), fileName.replaceFirst("\\..*", ".csv"));
						} else {
							processedFiles.add(file);
						}
					} catch (Exception e) {
						logger.error("Failed to convert Excel to CSV for file: " + fileName, e);
						restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
								"File conversion failed for: " + fileName, null);
						return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
					}

					if (convertedFile != null && convertedFile.exists()) {
						processedFiles.add(convertedFile);
						file.delete();
					}
				}

				// Check file is completed with file name, if yes then process failed
				Set<String> completedFileNames = checkProcessCompleted.stream()
						.map(ReconBatchProcessEntity::getFileName).collect(Collectors.toSet());
				logger.info("COMPLETED FILE NAMES :::::::::::" + completedFileNames);
				boolean checkFileIsCompleted = originalFileList.stream()
						.anyMatch(file -> completedFileNames.contains(file.getName()));
				if (checkFileIsCompleted) {
					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
							"Extraction process file from the source directory has already been processed.",
							runningProcessData);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
				}

				// Check file is running with file name, if yes then process failed
				Set<String> runningFileNames = checkProcessIsRunning.stream().map(ReconBatchProcessEntity::getFileName)
						.collect(Collectors.toSet());
				logger.info("RUNNING FILE NAMES :::::::::::" + runningFileNames);
				boolean checkFileIsRunning = originalFileList.stream()
						.anyMatch(f -> runningFileNames.contains(f.getName()));
				if (checkFileIsRunning) {
					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Extraction process file is running",
							runningProcessData);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
				}
				// Start actual process start , first start process is running mode and then
				// start actual loading
				if (!originalFileList.isEmpty()) {
					List<ReconBatchProcessEntity> runningExtraction = extractionService
							.extractionRunningStatus(processedFiles, reconFileDetails, userData);
					logger.info("RUNNING EXTRACTION ::::::::::::::" + runningExtraction);

					for (ReconBatchProcessEntity runningProcess : runningExtraction) {
						runningProcessData.add(runningProcess);
						logger.info("RUNNING EXTRACTION EACH PROCESS ::::::::::::::" + runningProcess);
					}
					if (!runningExtraction.isEmpty()) {
						CompletableFuture<String> extractionStatusFuture = extractionService
								.startExtraction(reconFileDetails, runningExtraction, processedFiles, userData);
						// String finalStatus = extractionStatusFuture.join();
						// logger.info("EXTRACTION STATUS ::::::::::::::" + finalStatus);
					}
				} else {
					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
							"File not found for given file path location!!!", null);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
				}
			} else {
				LoadMasterEntity loadMasterEntity = loadMasterRepository.findByRlmFileId(processId);
				if (loadMasterEntity != null) {
					ReconBatchProcessEntity extractionStatus = loadMasterService
							.extractionRunningStatusforGlFlagY(processId, reconFileDetails, userData);
					runningProcessData.add(extractionStatus);
					CompletableFuture<String> extractionDataStatus = loadMasterService.startDataLoading(processId,
							reconFileDetails, userData, extractionStatus, loadMasterEntity);
					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Extraction process has started",
							runningProcessData);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);

				} else {
					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "File Configuration Not Found FOR Load DATA!!!", null);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
				}
			}

		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
					"Data not found for extraction execution, process failed.", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Extraction process has started", runningProcessData);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@GetMapping(value = "/refresh-extraction", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> refreshProcessData(@RequestParam Long processId) {
		return extractionService.refreshProcessData(processId);
	}

	@PostMapping(value = "/refresh-extraction-status", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> refreshExtraction(
			@RequestBody List<RefreshRequestDto.ProcessManager> requestProcess) {
		return extractionService.refreshExtraction(requestProcess);
	}

	public List<File> getFileListFromDirectory(ReconFileDetailsMaster fileDetails) {
		List<File> fileList = new ArrayList<>();
//			File manualFile = reportGenerationService.generateManualFileForProcess(fileDetails);
		File filesDir = new File(fileDetails.getReconFileDestinationPath());

		if (filesDir.exists() && filesDir.isDirectory()) {
			File[] files = filesDir.listFiles();
			if (files != null) {
				for (File file : files) {
					fileList.add(file);
				}
			}
		}

		return fileList;
	}
}
