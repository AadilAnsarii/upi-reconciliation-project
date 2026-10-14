package com.jpb.reconciliation.reconciliation.controller.v2;

import java.io.File; 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import com.jpb.reconciliation.reconciliation.controller.ExtractionController;
import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.LoadMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.repository.LoadMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFileTmpltMastRepository;
import com.jpb.reconciliation.reconciliation.service.LoadMasterService;
import com.jpb.reconciliation.reconciliation.service.ReportGenerationService;
import com.jpb.reconciliation.reconciliation.service.excelreader.ExcelToCsvConvertorService;
import com.jpb.reconciliation.reconciliation.service.xmlreader.XmlToCsvConvertorService;
import com.jpb.reconciliation.reconciliation.service.v2.ExtractionAiService;

import net.sf.jasperreports.engine.JRException;

@RestController
@RequestMapping(path = "/api/v2/extraction")
public class ExtractionAiController {

	@Autowired
	ExtractionAiService extractionAiService;

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

	@Autowired
	XmlToCsvConvertorService xmlToCsvConvertorService;

	Logger logger = LoggerFactory.getLogger(ExtractionController.class);

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@Autowired
	ReconFileTmpltMastRepository reconFileTmpltMastRepository;

	@GetMapping(value = "/start-extraction", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> startExtraction(@RequestParam Long templateId,
			@AuthenticationPrincipal UserDetails userDetails) throws IOException, InterruptedException, JRException {
		RestWithStatusList restWithStatusList;
		List<Object> runningProcessData = new ArrayList<>();
		List<File> processedFiles = new ArrayList<>();

		// Find Template Data — look up the real template by its own templateId
		// (recon_file_tmplt_mast), not by a field's field_id (recon_tmplt_field_dtls)
		List<ReconFileTmpltMast> matchedTemplates = reconFileTmpltMastRepository.findByIdWithDetails(templateId);
		Optional<ReconTmpltFieldDtls> reconTemplateFileDetails = matchedTemplates.isEmpty() ? Optional.empty()
				: matchedTemplates.get(0).getFieldDetails().stream().findFirst();
		logger.info("TEMPLATE DETAILS WITH FILE :::::::::::::::::" + reconTemplateFileDetails);
		ReconUser userData = reconUserRepository.findByUsername(userDetails.getUsername()).get();
		// Find running process by file id
		List<ReconBatchProcessEntity> checkProcessIsRunning = reconBatchProcessEntityRepository
				.findByTemplateIdAndStatus(templateId, "Running");
		// Find completed process by its file id
		List<ReconBatchProcessEntity> checkProcessCompleted = reconBatchProcessEntityRepository
				.findByTemplateIdAndStatus(templateId, "Completed");

		if (reconTemplateFileDetails.isPresent()) {
			// Check GL Flag For Extraction Process If N Then check GL Master Table
			String glFlag = reconTemplateFileDetails.get().getTemplate().getGlFlag();
			if (glFlag == null || glFlag.equalsIgnoreCase(CommonConstants.INACTIVE_N)) {
				// Get file list from file path store at file details
				List<File> originalFileList = getFileListFromDirectory(reconTemplateFileDetails);
				// Check which type of file, if found excel then convert into csv
				// Check any space into file name

				for (File file : originalFileList) {
					String fileName = file.getName();
					// Checking spaces contain into file name
					if (fileName.contains(" ")) {
						restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
								"Please remove space from uploaded input file name.", runningProcessData);
						return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
					}

					File convertedFile = null;
					try {
						if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
							logger.info("Converting Excel file to CSV: " + fileName);
							excelToCsvConvertorService.convertExcelToCsv(file,
									reconTemplateFileDetails.get().getTemplate().getHeaderLineCount());
							// Construct the path for the converted CSV file
							convertedFile = new File(file.getParent(), fileName.replaceFirst("\\..*", ".csv"));
						} else if (fileName.endsWith(".xml")) {
							logger.info("Converting XML file to CSV: " + fileName);
							xmlToCsvConvertorService.convertXmlToCsv(file,
									reconTemplateFileDetails.get().getTemplate().getXmlRootTag(),
									reconTemplateFileDetails.get().getTemplate().getXmlRowTag(),
									new ArrayList<>(reconTemplateFileDetails.get().getTemplate().getFieldDetails()));
							convertedFile = new File(file.getParent(), fileName.replaceFirst("\\..*", ".csv"));
						} else {
							processedFiles.add(file);
						}
					} catch (Exception e) {
						logger.error("Failed to convert file for: " + fileName, e);
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
					return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
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
					return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
				}
				// Start actual process start , first start process is running mode and then
				// start actual loading
				if (!originalFileList.isEmpty()) {
					List<ReconBatchProcessEntity> runningExtraction = extractionAiService
							.extractionRunningStatus(processedFiles, reconTemplateFileDetails, userData);
					logger.info("RUNNING EXTRACTION ::::::::::::::" + runningExtraction);

					for (ReconBatchProcessEntity runningProcess : runningExtraction) {
						runningProcessData.add(runningProcess);
						logger.info("RUNNING EXTRACTION EACH PROCESS ::::::::::::::" + runningProcess);
					}
					if (!runningExtraction.isEmpty()) {
						CompletableFuture<String> extractionStatusFuture = extractionAiService
								.startExtraction(reconTemplateFileDetails, runningExtraction, processedFiles, userData);
						// String finalStatus = extractionStatusFuture.join();
						// logger.info("EXTRACTION STATUS ::::::::::::::" + finalStatus);
					}
				} else {
					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
							"File not found for given file path location!!!", null);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
				}
			} else {
				LoadMasterEntity loadMasterEntity = loadMasterRepository.findByRlmFileId(templateId);
				if (loadMasterEntity != null) {
					ReconBatchProcessEntity extractionStatus = loadMasterService
							.extractionRunningStatusforGlFlagYNewV2(templateId, reconTemplateFileDetails, userData);
					runningProcessData.add(extractionStatus);
					CompletableFuture<String> extractionDataStatus = loadMasterService.startDataLoadingNewV2(templateId,
							reconTemplateFileDetails, userData, extractionStatus, loadMasterEntity);
					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Extraction process has started",
							runningProcessData);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);

				} else {
					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
							"File Configuration Not Found FOR Load DATA!!!", null);
					return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
				}
			}

		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
					"Template Data not found for extraction execution, process failed.", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
		}
		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Extraction process has started", runningProcessData);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}
	
	// V2 status refresh. Rows written by ExtractionAiServiceImpl only populate
	// templateId (not processId, which is V1-only), so this looks up by
	// templateId+sequenceNo instead of reusing V1's processId-keyed refresh endpoint.
	// ProcessManager.getProcessId() is repurposed here to carry templateId.
	@PostMapping(value = "/refresh-extraction-status", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> refreshExtractionStatus(
			@RequestBody List<RefreshRequestDto.ProcessManager> requestProcess) {
		List<Object> refreshList = new ArrayList<>();
		for (RefreshRequestDto.ProcessManager data : requestProcess) {
			ReconBatchProcessEntity process = reconBatchProcessEntityRepository
					.findByTemplateIdAndSequenceNo(data.getProcessId(), data.getSequenceId());
			if (process != null) {
				refreshList.add(process);
			}
		}
		if (refreshList.isEmpty()) {
			// Not an error — pollUntilSettled (frontend) calls this every 2s right after
			// start-extraction fires off the batch insert asynchronously, so the very first
			// tick or two can legitimately land before that row exists yet. A 404 here reads
			// as a broken/missing endpoint in the Network tab; it's really just "not written
			// yet", which the next poll resolves — same FAILURE body either way.
			return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "Process data not found", null),
					HttpStatus.OK);
		}
		return new ResponseEntity<>(
				new RestWithStatusList(CommonConstants.SUCCESS, "Process data found successfully", refreshList), HttpStatus.OK);
	}

	public List<File> getFileListFromDirectory(Optional<ReconTmpltFieldDtls> reconTemplateFileDetails) {
		List<File> fileList = new ArrayList<>();
//			File manualFile = reportGenerationService.generateManualFileForProcess(fileDetails);
		File filesDir = new File(reconTemplateFileDetails.get().getTemplate().getFilePath());

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
