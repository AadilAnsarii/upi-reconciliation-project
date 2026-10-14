package com.jpb.reconciliation.reconciliation.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TTUMReportDto;
import com.jpb.reconciliation.reconciliation.dto.TTUMReportResponseDto;
import com.jpb.reconciliation.reconciliation.service.TTUMReportService;

@RestController
@RequestMapping(path = "/api/v1/ttum")
public class TTUMReportController {

	@Autowired
	TTUMReportService ttumReportService;

	private static final String BASE_REPORT_DIR = "/app/jpbrecon/JPB_RECON/TTUMReport";

	private final String CANONICAL_BASE_DIR;

	public TTUMReportController() throws IOException {
		String canonicalPath = new File(BASE_REPORT_DIR).getCanonicalPath();

		if (canonicalPath.endsWith(File.separator)) {
			CANONICAL_BASE_DIR = canonicalPath;
		} else {
			CANONICAL_BASE_DIR = canonicalPath + File.separator;
		}
	}

	@GetMapping(value = "/get-ttum-report-data", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> getTTUMList() {
		return ttumReportService.getAllTTUMList();
	}

	@PostMapping(value = "/generate-ttum-report", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> generateTTUMReport(@Valid @RequestBody TTUMReportDto ttumGenerateReportRequest) {
		return ttumReportService.generateTTUMReport(ttumGenerateReportRequest);
	}

	@PostMapping(value = "/download-ttum-report")
	ResponseEntity<?> downloadTTUMReport(@RequestBody List<TTUMReportResponseDto> reportReuest) {
		if (reportReuest == null || reportReuest.isEmpty()) {
			return new ResponseEntity<>(new ResponseDto(CommonConstants.FAILURE, "No reports specified for download"),
					HttpStatus.BAD_REQUEST);
		}

		List<FileSystemResource> filesToDownload = new ArrayList<>();
		for (TTUMReportResponseDto report : reportReuest) {
			String userSuppliedPath = report.getReportFileLocation();

			try {

				File requestedFile = new File(userSuppliedPath);
				String canonicalPath = requestedFile.getCanonicalPath();

				if (!canonicalPath.startsWith(CANONICAL_BASE_DIR)) {
					System.err.println("SECURITY ALERT: Path Traversal attempt detected for path: " + userSuppliedPath);
					return new ResponseEntity<>(new ResponseDto(CommonConstants.FAILURE, "Invalid file path access attempt."),
							HttpStatus.FORBIDDEN);
				}

				File safeReportFile = new File(canonicalPath);

				if (!safeReportFile.exists()) {
					return new ResponseEntity<>(new ResponseDto(CommonConstants.FAILURE, "FILE " + CommonConstants.MESSAGE_404),
							HttpStatus.NOT_FOUND);
				}

				FileSystemResource fileSystemResource = new FileSystemResource(safeReportFile);
				filesToDownload.add(fileSystemResource);

			} catch (IOException e) {
				return new ResponseEntity<>(new ResponseDto(CommonConstants.FAILURE, "Error processing file path."),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		try {
			byte[] zipFileContent = createZipFileFromReport(filesToDownload);
			return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=reports.zip")
					.header("Content-Type", "application/zip").body(zipFileContent);
		} catch (IOException e) {
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
