//package com.jpb.reconciliation.reconciliation.service.impl;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import org.jfree.util.Log;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.RowMapper;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.jpb.reconciliation.reconciliation.constants.FileProcessStatus;
//import com.jpb.reconciliation.reconciliation.dto.ReportDto;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportData;
//import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportHeader;
//import com.jpb.reconciliation.reconciliation.entity.ExceptionReconReportEntity;
//import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
//import com.jpb.reconciliation.reconciliation.repository.ExceptionReconReportRepository;
//import com.jpb.reconciliation.reconciliation.repository.ReportRepository;
//import com.jpb.reconciliation.reconciliation.secondary.entity.FileProcessStatusEntity;
//import com.jpb.reconciliation.reconciliation.secondary.repository.FileProcessStatusRepository;
//import com.jpb.reconciliation.reconciliation.secondary.repository.SecondaryProcedureRepository;
//import com.jpb.reconciliation.reconciliation.service.FileProcessStatusService;
//import com.jpb.reconciliation.reconciliation.service.fileoperation.FileProcessorService;
//
//
//@Service
//public class FileProcessStatusServiceImpl implements FileProcessStatusService {
//
//	@Value("${app.uploadFileDir}")
//	private String uploadFilePathDir;
//
//	Logger logger = LoggerFactory.getLogger(FileProcessStatusServiceImpl.class);
//
//	@Autowired
//	FileProcessStatusRepository fileProcessStatusRepository;
//
//	@Autowired
//	SecondaryProcedureRepository secondaryProcedureRepository;
//
//	@Autowired
//	ExceptionReconReportRepository exceptionReconReportRepository;
//
//	@Autowired
//	FileProcessorService fileProcessor;
//
//	@Autowired
//	ReportRepository reportRepository;
//
//	@Autowired
//	JdbcTemplate jdbcTemplate;
//
//	@Value("${app.reconReport}")
//	private String reconFileLoc;
//
//	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy");
//
//	@Override
//	public ResponseEntity<RestWithStatusList> uploadFile(MultipartFile file, UserDetails userDetails, String fileType) {
//		RestWithStatusList restWithStatusList = null;
//		try {
//			Path copyLocation = Paths.get(uploadFilePathDir + file.getOriginalFilename());
//
//			try (InputStream inputStream = file.getInputStream()) {
//				Files.copy(inputStream, copyLocation, StandardCopyOption.REPLACE_EXISTING);
//			}
//
//			String filePath = copyLocation.toAbsolutePath().toString();
//
//			// Save Data For Uploaded File
//			FileProcessStatusEntity newFileEntry = new FileProcessStatusEntity();
//			newFileEntry.setFileName(file.getOriginalFilename());
//			newFileEntry.setFilePath(filePath);
//			newFileEntry.setUploadedBy(userDetails.getUsername());
//			newFileEntry.setUploadedDate(LocalDateTime.now());
//			newFileEntry.setFileType(fileType);
//			newFileEntry.setStatus(FileProcessStatus.PENDING);
//			fileProcessStatusRepository.save(newFileEntry);
//
//			// Process File and Load Data
//			Boolean processLoadStatus = fileProcessor.processAndLoadData(newFileEntry);
//			Log.info("Process and Load Data Status ::::::::::::" + processLoadStatus);
//			if (processLoadStatus) {
//				Boolean processingSPStatus = secondaryProcedureRepository.fileProcessingData(newFileEntry);
//				Log.info("procedure processingSPStatus  ::::::::::::" + processLoadStatus);
//				if (processingSPStatus) {
//					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS,
//							"File uploaded successfully! Awaiting Checker Approval.", null);
//					return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//				} else {
//					newFileEntry.setRejectionReason("File Not Correct");
//					newFileEntry.setStatus(FileProcessStatus.REJECTED);
//					fileProcessStatusRepository.save(newFileEntry);
//					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "File Rejected", null);
//					return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//				}
//			} else {
//				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "file not upload.", null);
//				return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//			}
//
//		} catch (IOException e) {
//			logger.info("File UPLOAD ERRROR" + e);
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "File upload failed due to server error", null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_GATEWAY);
//		}
//
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> getFileList() {
//		RestWithStatusList restWithStatusList = null;
//		List<Object> approvalPendingFile = new ArrayList<>();
//		List<FileProcessStatusEntity> approvalPendingFileList = fileProcessStatusRepository
//				.findByStatusAndUploadDataStatus(FileProcessStatus.PENDING, FileProcessStatus.PROCESSED_SUCCESS);
//		if (!approvalPendingFileList.isEmpty()) {
//			approvalPendingFile.addAll(approvalPendingFileList);
//			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "File Found For Approvals", approvalPendingFile);
//		} else {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "File Not Found For Approval.", approvalPendingFile);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
//		}
//		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> fileActionApproval(FileProcessStatusEntity fileInputByUser) {
//		FileProcessStatusEntity fileEntryForProcess = fileProcessStatusRepository
//				.findByFileIdAndUploadedBy(fileInputByUser.getFileId(), fileInputByUser.getUploadedBy());
//		if (fileEntryForProcess != null) {
//
//			if (fileInputByUser.getStatus() == FileProcessStatus.REJECTED) {
//				Boolean processRejectedStatus = secondaryProcedureRepository.approvalProcess(fileEntryForProcess,
//						fileInputByUser);
//				logger.info("File Rejected Status :::::::" + processRejectedStatus);
//				if (processRejectedStatus) {
//					fileEntryForProcess.setStatus(FileProcessStatus.REJECTED);
//					fileEntryForProcess.setRejectionReason(fileInputByUser.getRejectionReason());
//					fileEntryForProcess.setCheckerId(fileInputByUser.getCheckerId());
//					fileEntryForProcess.setApprovalDate(LocalDateTime.now());
//					fileProcessStatusRepository.save(fileEntryForProcess);
//
//					return new ResponseEntity<>(
//							new RestWithStatusList(CommonConstants.SUCCESS, "File successfully marked as REJECTED.", null),
//							HttpStatus.OK);
//				} else {
//					return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "Process failed", null),
//							HttpStatus.OK);
//				}
//
//			} else if (fileInputByUser.getStatus() == FileProcessStatus.APPROVED) {
//				Boolean processApprovalStatus = secondaryProcedureRepository.approvalProcess(fileEntryForProcess,
//						fileInputByUser);
//				logger.info("File Approval Status :::::::" + processApprovalStatus);
//				if (processApprovalStatus) {
//					fileEntryForProcess.setStatus(FileProcessStatus.APPROVED);
//					fileEntryForProcess.setCheckerId(fileInputByUser.getCheckerId());
//					fileEntryForProcess.setApprovalDate(LocalDateTime.now());
//					fileProcessStatusRepository.save(fileEntryForProcess);
//				} else {
//					return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "Process failed", null),
//							HttpStatus.OK);
//				}
//
//			}
//
//		} else {
//			return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "Data Not Found", null), HttpStatus.OK);
//		}
//		return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "File approved successfully.", null),
//				HttpStatus.OK);
//
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> generateFileUploadReport(ReportDto ntslReportRequest) {
//		RestWithStatusList restWithStatusList;
//		List<ExceptionReconReportEntity> reportList = exceptionReconReportRepository
//				.findByProcessId(ntslReportRequest.getProcessId());
//		LocalDate requestReportDate = ntslReportRequest.getReportDate();
//		String formattedDate = requestReportDate.format(formatter);
//		if (!reportList.isEmpty()) {
//			for (ExceptionReconReportEntity report : reportList) {
//				JasperReportHeader reportHeader = getReportHeader(report.getReportHeader());
//				List<JasperReportData> reportData = getReportData(report.getReportQuery(), formattedDate);
//
//				if (reportHeader == null || reportData.isEmpty()) {
//					logger.info(
//							"Report header or Report data is needed for Jasper report. Skipping report for process: "
//									+ report.getProcessId());
//					continue;
//				}
//
//				String filePath = generateFileName(report.getFileName());
//
//				try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
//					writer.write(reportHeader.getReportHeader());
//					writer.newLine();
//					for (JasperReportData jsReport : reportData) {
//						writer.write(jsReport.getRowData());
//						writer.newLine();
//					}
//					writer.newLine();
//
//					// Saving Report
//					File getFileName = new File(filePath);
//					ReportEntity newReconciliationReport = new ReportEntity();
//					newReconciliationReport.setProcessId(ntslReportRequest.getProcessId());
//					newReconciliationReport.setReportDate(LocalDate.now());
//					newReconciliationReport.setReportFileName(ntslReportRequest.getReportFileName());
//					newReconciliationReport.setReportLocation(filePath);
//					newReconciliationReport.setReportName(getFileName.getName());
//					reportRepository.save(newReconciliationReport);
//
//					logger.info("Report exported successfully to: " + filePath);
//				} catch (Exception e) {
//					logger.info("Error writing to CSV file: " + e.getMessage());
//				}
//			}
//		} else {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
//					ntslReportRequest.getReportFileName() + "Data Not Found.", null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//		}
//		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, ntslReportRequest.getReportFileName()
//				+ " Report Generated Successfully, Please click retrieve to download report", null);
//		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//	}
//
//	private String generateFileName(String fileName) {
//		SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss");
//		String formatDate = date.format(new Date());
//		String reportFilePath = reconFileLoc + fileName + formatDate + ".csv";
//		return reportFilePath;
//	}
//
//	private JasperReportHeader getReportHeader(String reportHeaderSql) {
//		try {
//			return jdbcTemplate.queryForObject(reportHeaderSql, (rs, rowNum) -> {
//				JasperReportHeader header = new JasperReportHeader();
//				header.setReportHeader(rs.getString("HEADER"));
//				return header;
//			});
//		} catch (Exception e) {
//			logger.error("Error retrieving report header: " + reportHeaderSql, e);
//			return null;
//		}
//	}
//
//	private List<JasperReportData> getReportData(String reportQuerySql, String dateInput) {
//
//		Object[] params = new Object[] { dateInput };
//
//		return jdbcTemplate.query(reportQuerySql, params, new RowMapper<JasperReportData>() {
//			@Override
//			public JasperReportData mapRow(ResultSet rs, int rowNum) throws SQLException {
//				JasperReportData reportdata = new JasperReportData();
//				reportdata.setRowData(rs.getString("DATA"));
//				return reportdata;
//			}
//		});
//	}
//
//}
