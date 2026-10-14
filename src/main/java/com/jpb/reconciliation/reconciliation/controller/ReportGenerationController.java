package com.jpb.reconciliation.reconciliation.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.ReportRequest;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.ReportGenerationService;
import com.jpb.reconciliation.reconciliation.service.SegretionService;
import com.jpb.reconciliation.reconciliation.service.jasper.JasperReportService;

import net.sf.jasperreports.engine.JRException;

@RestController
@RequestMapping("/api/v1/")
public class ReportGenerationController {

    private final SegretionService segretionService;
    
    @Autowired
    JasperReportService jasperReportService;
    
	@Autowired
	ReportGenerationService reportGenerationService;

	Logger logger = LoggerFactory.getLogger(ReportGenerationController.class);

    ReportGenerationController(SegretionService segretionService) {
        this.segretionService = segretionService;
    }

	@PostMapping(value = "generate-report", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<ResponseDto> generateReport(@RequestParam Long processId) throws JRException, IOException {
		return reportGenerationService.generateJasperReport(processId);
	}
    
	@PostMapping(value = "retrive-report", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> retriveReport(@RequestBody ReportDto reportDto) {
		return reportGenerationService.retriveReport(reportDto);
	}
	
	@PostMapping(value = "view-extraction-details")
	ResponseEntity<RestWithStatusList> viewExtractionDetails(@RequestBody ReportDto extractionRequest){
		return reportGenerationService.viewExtrationDetails(extractionRequest);
	}

	@GetMapping(value = "download-report")
	public ResponseEntity<?> downloadReport(@RequestParam String fileName) {
		String reportpath = "U:\\Recon_Project\\recon_springboot\\Report\\" + fileName;
		logger.info("FILE PATH FOR DOENLOAD REPORT ::::::::" + reportpath);
		File reportFile = new File(reportpath);

		if (!reportFile.exists()) {
			return new ResponseEntity<>(
					new ResponseDto(CommonConstants.STATUS_404, "FILE " + CommonConstants.MESSAGE_404),
					HttpStatus.NOT_FOUND);

		
		}
		FileSystemResource fileSystemResource = new FileSystemResource(reportFile);
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + fileName)
				.header("Content-Type", "application/csv").body(fileSystemResource);
	}
    
	@PostMapping(value = "download-report-zip")
	public ResponseEntity<?> downloadZipReport(@RequestBody List<ReportRequest.Report> reports) {
		if (reports == null || reports.isEmpty()) {
			return new ResponseEntity<>(new ResponseDto(CommonConstants.FAILURE, "No reports specified for download"),
					HttpStatus.BAD_REQUEST);
		}
		List<FileSystemResource> filesToDownload = new ArrayList<>();
		for (ReportRequest.Report report : reports) {
			File reportFile = new File(report.getReportLocation());
			if (!reportFile.exists()) {
				logger.info("FILE NOT FOUND AT LOCATION ::::::::::::::" + report.getReportLocation());
				return new ResponseEntity<>(new ResponseDto(CommonConstants.FAILURE, "FILE " + CommonConstants.MESSAGE_404),
						HttpStatus.NOT_FOUND);
			}
			FileSystemResource fileSystemResource = new FileSystemResource(reportFile);
			filesToDownload.add(fileSystemResource);
		}
		try {
			byte[] zipFileContent = createZipFileFromReport(filesToDownload);
			return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=reports.zip")
					.header("Content-Type", "application/zip").body(zipFileContent);
		} catch (IOException e) {
			logger.error("Error while creating zip file: " + e.getMessage(), e);
			return new ResponseEntity<>(new ResponseDto(CommonConstants.FAILURE, "Internal Server Error"),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private byte[] createZipFileFromReport(List<FileSystemResource> filesToDownload) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
			for (FileSystemResource file : filesToDownload) {
				zipOutputStream.putNextEntry(new ZipEntry(file.getFilename()));
				byte[] buffer = new byte[1024];
				int length;
				try (InputStream inputStream = file.getInputStream()) {
					while ((length = inputStream.read(buffer)) > 0) {
						zipOutputStream.write(buffer, 0, length);
					}
				}
				zipOutputStream.closeEntry();
			}
		}
		return byteArrayOutputStream.toByteArray();
	}
	
	
}
