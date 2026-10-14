//package com.jpb.reconciliation.reconciliation.service.impl;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.Date;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import com.jpb.reconciliation.reconciliation.dto.ReportDto;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.entity.NPCISummaryEntity;
//import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
//import com.jpb.reconciliation.reconciliation.repository.NPCISummaryRepository;
//import com.jpb.reconciliation.reconciliation.repository.ReportRepository;
//import com.jpb.reconciliation.reconciliation.service.NTSLReportGeneratorService;
//
//@Service
//public class NTSLReportGeneratorServiceImpl implements NTSLReportGeneratorService {
//
//	Logger logger = LoggerFactory.getLogger(NTSLReportGeneratorServiceImpl.class);
//
//	@Value("${app.ttumFile}")
//	private String filePath;
//
//	@Autowired
//	NPCISummaryRepository npciSummaryRepository;
//
//	@Autowired
//	ReportRepository reportRepository;
//
//	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyy");
//
//	@Override
//	public ResponseEntity<RestWithStatusList> generateNtslReport(ReportDto ntslReportRequest) {
//		RestWithStatusList restWithStatusList = null;
//		LocalDate requestReportDate = ntslReportRequest.getReportDate();
//		String productType = null;
//		if(ntslReportRequest.getReportFileName().equalsIgnoreCase("IMPS NTSL")) {
//			productType = "IMPS";
//		}else if(ntslReportRequest.getReportFileName().equalsIgnoreCase("AEPS NTSL")) {
//			productType = "AEPS";
//		}
//		String formattedDate = requestReportDate.format(formatter);
//		logger.info("Request Report Date:::::" + formattedDate);
//		
//		if (!formattedDate.isEmpty()) {
//			List<NPCISummaryEntity> npciSummaryList = npciSummaryRepository.findByNpciFileDateAndProductType(formattedDate,productType);
//			logger.info("NPCI Summary Data :::::" + npciSummaryList);
//			if (!npciSummaryList.isEmpty()) {
//				Boolean generateReportStatus = generateReport(npciSummaryList, ntslReportRequest);
//				if (generateReportStatus) {
//					restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, ntslReportRequest.getReportFileName()
//							+ " Report Generated Successfully, Please click retrieve to download report", null);
//					return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//				} else {
//					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Report Generation Failed.", null);
//					return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//				}
//
//			} else {
//				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
//						"No data was found for the specified report date.", null);
//				return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//			}
//		} else {
//			logger.info("Request Report Date Is Required:::::" + formattedDate);
//		}
//
//		return null;
//	}
//
//	private Boolean generateReport(List<NPCISummaryEntity> npciSummaryList, ReportDto ntslReportRequest) {
//		Boolean reportStatus = false;
//		String reportName = generatereportName(ntslReportRequest.getReportFileName());
//		logger.info("Generated Report Name :::::" + reportName);
//		if (!reportName.isEmpty()) {
//			try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportName))) {
//				writer.write(
//						"ProductType,FileDate,Cycle,NPCI RAW DATA COUNT,NPCI RAW DATA AMOUNT,NTSL RAW DATA COUNT,NTSL RAW DATA AMOUNT,NPCI_NTSL_DATA_DIFF,NPCI_NTSL_AMOUNT_DIFF");
//				writer.newLine();
//				for (NPCISummaryEntity summary : npciSummaryList) {
//					String dataDiffrenceNpciNtsl = summary.getNpciRawDataCount().subtract(summary.getNtslRawDataCount())
//							.toString();
//					String amountDiffrenceNpciNtsl = summary.getNpciRawDataAmount()
//							.subtract(summary.getNtslRawDataAmount()).toString();
//					writer.write(
//							summary.getProductType() + "," + summary.getNpciFileDate() + "," + summary.getNpciCycle()
//									+ "," + summary.getNpciRawDataCount() + "," + summary.getNpciRawDataAmount() + ","
//									+ summary.getNtslRawDataCount() + "," + summary.getNtslRawDataAmount() + ","
//									+ dataDiffrenceNpciNtsl + "," + amountDiffrenceNpciNtsl);
//					writer.newLine();
//				}
//
//				ReportEntity newReport = new ReportEntity();
//				newReport.setReportLocation(reportName);
//				newReport.setProcessId(ntslReportRequest.getProcessId());
//				Path path = Paths.get(reportName);
//				newReport.setReportName(path.getFileName().toString());
//				newReport.setReportFileName(ntslReportRequest.getReportFileName());
//				newReport.setReportDate(LocalDate.now());
//				reportRepository.save(newReport);
//				return reportStatus = true;
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		} else {
//			return reportStatus;
//		}
//		return reportStatus;
//	}
//
//	private String generatereportName(String reportFileName) {
//		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
//		return filePath + reportFileName + timestamp + ".csv";
//	}
//
//}
