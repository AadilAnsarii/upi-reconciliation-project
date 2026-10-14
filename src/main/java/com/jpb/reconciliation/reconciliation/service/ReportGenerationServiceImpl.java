package com.jpb.reconciliation.reconciliation.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.FileDataUpdateReportDto;
import com.jpb.reconciliation.reconciliation.dto.JasperReportDto;
import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TTUMExceptionReportDto;
import com.jpb.reconciliation.reconciliation.dto.aepsreport.AepsCbsReportDto;
import com.jpb.reconciliation.reconciliation.dto.aepsreport.AepsEpikReportDto;
import com.jpb.reconciliation.reconciliation.dto.ckycreport.CkycReconObReportDto;
import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportData;
import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportHeader;
import com.jpb.reconciliation.reconciliation.entity.ExceptionReconReportEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
import com.jpb.reconciliation.reconciliation.repository.ExceptionReconReportRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ReportRepository;
import com.jpb.reconciliation.reconciliation.util.CommonReport;
import com.jpb.reconciliation.reconciliation.util.JdbcReportUtils;
import com.opencsv.CSVWriter;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

@Service
public class ReportGenerationServiceImpl implements ReportGenerationService {

	@Autowired
	ReportRepository reportRepository;

	@Autowired
	ReconProcessManagerRepository reconProcessManagerRepository;

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@Autowired
	ExceptionReconReportRepository exceptionReconReportRepository;

	Logger logger = LoggerFactory.getLogger(ReportGenerationServiceImpl.class);

	@Autowired
	CommonReport commonReportService;

	private final JdbcTemplate jdbcTemplate;

	public ReportGenerationServiceImpl(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Value("${app.uDrive}")
	private String uDrive;

	@Value("${app.aepsDataFile}")
	private String aepsDataFilePath;

	@Override
	public ResponseEntity<String> generateReport(Long processId) {
		String filePath = "U:\\Recon_Project\\recon_springboot\\Report\\report.csv";
		ReportEntity reportData = reportRepository.findByProcessId(processId);
		if (reportData != null) {
			String reportKey = reportData.getReportKey();
			List<ReportEntity> reportDataListByKey = reportRepository.findByReportKey(reportKey);

			if (!reportDataListByKey.isEmpty()) {
				try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
					List<String> columnNames = getColumnsNameList(reportDataListByKey);
					writer.writeNext(columnNames.toArray(new String[0]));

					for (ReportEntity report : reportDataListByKey) {
						String[] row = new String[columnNames.size()];

						for (int i = 0; i < columnNames.size(); i++) {
							try {
								Field field = ReportEntity.class.getDeclaredField(columnNames.get(i));
								field.setAccessible(true);
								row[i] = field.get(report) != null ? field.get(report).toString() : "";
							} catch (Exception e) {
							}
						}
						writer.writeNext(row);
					}
				} catch (Exception e) {
				}
			}
		}

		return null;
	}

	private List<String> getColumnsNameList(List<ReportEntity> reportDataListByKey) {
		List<String> columnName = new ArrayList<>();
		for (ReportEntity columnReportName : reportDataListByKey) {
			columnName.add(columnReportName.getReportHeader());
		}
		return columnName;
	}

	@Override
	public ResponseEntity<ResponseDto> generateJasperReport(Long processId) throws JRException, IOException {
		ReportEntity reportData = reportRepository.findByProcessId(processId);
		logger.info("REPORT TABLE DATA ::::::::" + reportData);
		if (reportData != null) {
			List<Map<String, Object>> rowData = jdbcTemplate.queryForList(reportData.getReportQuery());
			logger.info("REPORT Query Data ::::::::" + rowData);
			String reportFilePath = generateReportFilePath();
			Path path = Paths.get(reportFilePath);
			logger.info("REPORT FILE PATH ::::::::::::::::::::::::::::" + reportFilePath);
			List<JasperReportDto> reportDataList = new ArrayList<>();
			for (Map<String, Object> row : rowData) {
				JasperReportDto jsprData = new JasperReportDto();
				jsprData.setId(((BigDecimal) row.get("ID")).longValue());
				jsprData.setName((String) row.get("NAME"));
				jsprData.setAge(((BigDecimal) row.get("AGE")).longValue());
				jsprData.setProcessId(((BigDecimal) row.get("PROCESS_ID")).longValue());
				reportDataList.add(jsprData);
			}
			logger.info("REPORT DATA LIST ::::::::::::" + reportDataList);
			InputStream jasperTemplateStream = getClass().getClassLoader()
					.getResourceAsStream("jasperreports/report.jrxml");
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperTemplateStream);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("ReportTitle", "My Report");

			JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportDataList);
			logger.info("DATASOURCE LIKE ::::::::::::::::::::::::" + dataSource);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, dataSource);

			try (Writer writer = new FileWriter(new File(reportFilePath))) {
				JRCsvExporter exporter = new JRCsvExporter();
				exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
				exporter.setExporterOutput(new SimpleWriterExporterOutput(writer));
				exporter.exportReport();
				reportData.setReportLocation(reportFilePath);
				reportData.setReportName(path.getFileName().toString());
				reportData.setReportDate(LocalDate.now());
				reportRepository.save(reportData);
			} catch (Exception e) {
				throw new JRException("Error exporting to csv", e);
			}

		} else {
			return new ResponseEntity<>(
					new ResponseDto(CommonConstants.STATUS_404,
							"PROCESS ID " + CommonConstants.MESSAGE_404 + " FOR GENERATING REPORT"),
					HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(new ResponseDto(CommonConstants.STATUS_200, "Report Generated Successfully"), HttpStatus.OK);
	}

	private String generateReportFilePath() throws IOException {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String checkReportDir = uDrive + "\\Report\\" + timeStamp;

		File fileDirectory = new File(checkReportDir).getParentFile();
		if (!fileDirectory.exists()) {
			Boolean reportDirCreated = fileDirectory.mkdir();
			if (!reportDirCreated) {
				throw new IOException("Failed to create directories for the report file");
			}
		}

		String reportTimeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date()) + ".csv";

		String reportPath = checkReportDir + "\\" + reportTimeStamp;
		File reportDateFileDirectory = new File(reportPath).getParentFile();
		logger.info("REPORT DATE FILE CHECK :::::::::::" + reportPath);
		if (!reportDateFileDirectory.exists()) {
			Boolean reportDateDir = reportDateFileDirectory.mkdir();
			if (!reportDateDir) {
				throw new IOException("Failed to create directories for the report date file");
			}
		}
		return reportPath;
	}

	@Override
	public ResponseEntity<RestWithStatusList> retriveReport(ReportDto reportDto) {
		List<ReportEntity> reportData = reportRepository
				.findByReportFileNameAndReportDate(reportDto.getReportFileName(), reportDto.getReportDate());
		logger.info("REPORT DATA BY PROCESS ID  ::::::::::::::" + reportData);
		List<Object> reportList = new ArrayList<>();
		RestWithStatusList restWithStatusList = null;

		if (!reportData.isEmpty()) {
			for (ReportEntity report : reportData) {
				reportList.add(report);
			}

			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Retrive Report Successfully", reportList);
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Report Not Found.", reportList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ResponseDto> generateReport(ReconFileDetailsMaster reconFileDetails, StringBuilder output,
			ReconBatchProcessEntity reconProcessManager, String dataCount, File file) throws JRException, IOException {

		if (reconFileDetails.getFileUpdateFlag().equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
			Boolean reportStatus = writeDataUpdationReport(reconFileDetails, reconProcessManager, file);
			logger.info("DATA UPDATE REPORT GENERATED SUCCESSFULLY :::::::" + reportStatus);
		}

		List<Map<String, Object>> data = new ArrayList<>();
		Map<String, Object> report = new HashMap<>();
		ReportEntity newReport = new ReportEntity();
		SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
		String formattedDate = date.format(new Date());
		report.put("fileName", file.getName().toString());
		report.put("fileDate", formattedDate);
		report.put("extractionDate", formattedDate);
		report.put("totalCount", dataCount);

		BigDecimal getTotalAmount = getTotalAmount(reconFileDetails.getReconTemplateDetails().getStageTabName());
		logger.info("Amount of getTotalAmount :::::::::::::" + getTotalAmount);
		report.put("totalAmount", getTotalAmount);
		data.add(report);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("reportTitle", "JP BANK\nSWITCH UPI SUMMARY REPORT");
		InputStream jasperTemplateStream = getClass().getClassLoader()
				.getResourceAsStream("jasperreports/extraction-report.jrxml");
		JasperReport jasperReport = JasperCompileManager.compileReport(jasperTemplateStream);

		JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
		logger.info("DATASOURCE LIKE ::::::::::::::::::::::::" + dataSource);
		try {
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
			String reportName = genertateReportName(reconFileDetails.getReconFileName(), file.getName().toString(),
					reconProcessManager);
			exportReportToTextFile(jasperPrint, reportName);
			File getFileName = new File(reportName);
			newReport.setProcessId(reconFileDetails.getReconFileId());

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			LocalDate localDate = LocalDate.parse(formattedDate, formatter);
			newReport.setReportDate(LocalDate.now());
			newReport.setFileName(file.getName().toString());
			newReport.setReportFileName(reconFileDetails.getReconFileName());
			newReport.setReportLocation(reportName);
			newReport.setReportName(getFileName.getName());
			reportRepository.save(newReport);
			logger.info("REPORT DATA SAVED SUCCESSFULLY :::::::::::::::" + newReport);
		} catch (JRException e) {
			logger.error("Error generating report", e);
			reconProcessManager.setStatus("Error");
			reconProcessManager.setReportStatus("Error");
			reconBatchProcessEntityRepository.save(reconProcessManager);
			reconBatchProcessEntityRepository.flush();
			return new ResponseEntity<>(
					new ResponseDto(CommonConstants.STATUS_404,
							"PROCESS ID " + CommonConstants.MESSAGE_404 + " FOR GENERATING REPORT"),
					HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(new ResponseDto(CommonConstants.STATUS_200, "Report Generated Successfully"),
				HttpStatus.OK);
	}

	private Boolean writeDataUpdationReport(ReconFileDetailsMaster reconFileDetails,
			ReconBatchProcessEntity reconProcessManager, File file) {
		ReportEntity newReportData = new ReportEntity();
		String dataReportName = generateDataReportName();
		String aepsReportQuery = "select tran_seq_num,'C' FLAG,to_char(traN_DATE,'YYYY-MM-DD') SHTDT,TRAN_AMOUNT ADJAMT,tran_seq_num shser,\r\n"
				+ "REGEXP_SUBSTR(NARRATION, '^([^/]+)', 1, 1, NULL, 1) SHCRD,'' FILE_NAME,1 REASON,REGEXP_SUBSTR(NARRATION, '[^/]+', 1, 2) URN\r\n"
				+ "from REC_AEPS_BC_STAGE_T";
		List<FileDataUpdateReportDto> aepsBcStageDataList = getAepsBCReportData(aepsReportQuery);
		File getFileName = new File(dataReportName);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataReportName))) {
			writer.write("BANK_ADJREFNO,FLAG,SHTDT,ADJAMT,shser,Shcrd,FILE_NAME,REASON,URN");
			writer.newLine();
			for (FileDataUpdateReportDto report : aepsBcStageDataList) {
				writer.write(report.getTranSeqNum() + "," + report.getFlag() + "," + report.getShtdt() + ","
						+ report.getAdjAmt() + "," + report.getShser() + "," + report.getShcrd() + ","
						+ getFileName.getName().toString() + "," + report.getReason() + "," + report.getUrn());

				writer.newLine();
			}
			writer.newLine();
			newReportData.setProcessId(reconFileDetails.getReconFileId());
			newReportData.setReportDate(LocalDate.now());
			newReportData.setFileName(file.getName().toString());
			newReportData.setReportFileName(reconFileDetails.getReconFileName());
			newReportData.setReportLocation(dataReportName);
			newReportData.setReportName(getFileName.getName());
			reportRepository.save(newReportData);
			logger.info("DATA UPDATE REPORT SAVED SUCCESSFULLY :::::::::::::::" + newReportData);
		} catch (Exception e) {
			logger.info("Error writing to CSV file: " + e.getMessage());
		}
		return true;
	}

	private String generateDataReportName() {
		SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss-SS");
		String formatDate = date.format(new Date());
		String dataReportFileNamePath = aepsDataFilePath + "AEPS_RNFI_CR ADJ_" + formatDate + ".csv";
		return dataReportFileNamePath;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void generateReconciliationReport(ReconProcessDefMaster reconProcessDefMaster,
			ReconBatchProcessEntity process) throws IOException {
		String fileName = " ";
		String reconReportName = genertateReportName(reconProcessDefMaster.getReconProcessName(), fileName, process);
		File getFileName = new File(reconReportName);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(reconReportName))) {
			writer.newLine();
			writer.write("                        RECONCILIATION REPORT                 ");
			writer.newLine();
			writer.write("Report Date : " + new Date());
			writer.newLine();

			if (!reconProcessDefMaster.getReconProcessName().equalsIgnoreCase("AEPS_ELMS")
					|| !reconProcessDefMaster.getReconProcessName().equalsIgnoreCase("CKYC_RECON")) {
				process.setReportStatus("Completed");
			}

			ReportEntity newReconciliationReport = new ReportEntity();
			newReconciliationReport.setProcessId(reconProcessDefMaster.getReconProcessId());
			newReconciliationReport.setReportDate(LocalDate.now());
			newReconciliationReport.setReportFileName(reconProcessDefMaster.getReconProcessName());
			newReconciliationReport.setReportLocation(reconReportName);
			newReconciliationReport.setReportName(getFileName.getName());
			reportRepository.save(newReconciliationReport);
			logger.info("Reconciliation report created successfully at ::::::::" + reconReportName);
		} catch (Exception e) {
			logger.info("Error writing to CSV file: " + e.getMessage());
			process.setReportStatus("Error");
			process.setStatus("Error");
		}
		reconBatchProcessEntityRepository.save(process);
	}

	private BigDecimal getTotalAmount(String reconStageTabName) {
		if (!reconStageTabName.isEmpty()) {
			String sql = "SELECT SUM(tran_amount) from " + reconStageTabName;
			logger.info("Executing getTotalAmount query: {}", sql);
			try {
				BigDecimal totalAmount = jdbcTemplate.queryForObject(sql, BigDecimal.class);
				return (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
			} catch (EmptyResultDataAccessException e) {
				logger.warn("No rows found or SUM returned no value for table: {}. Returning 0.0", reconStageTabName,
						e);
				return BigDecimal.ZERO;
			} catch (BadSqlGrammarException e) {
				logger.error("Tran Amount column not present into : {}", reconStageTabName);
				return BigDecimal.ZERO;
			} catch (Exception e) {
				logger.error("Error calculating total amount for table: {}", reconStageTabName, e);
				throw new RuntimeException("Failed to get total amount from table " + reconStageTabName, e);
			}
		} else {
			return BigDecimal.ZERO;
		}
	}

	private void exportReportToTextFile(JasperPrint jasperPrint, String reportName) throws IOException {
		StringBuilder reportContent = new StringBuilder();
		for (int i = 0; i < jasperPrint.getPages().size(); i++) {
			JRPrintPage page = jasperPrint.getPages().get(i);
			for (JRPrintElement element : page.getElements()) {
				if (element instanceof JRPrintText) {
					JRPrintText textElement = (JRPrintText) element;
					reportContent.append(textElement.getFullText()).append(System.lineSeparator());
				}
			}
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportName))) {
			writer.write(reportContent.toString());
		}
	}

	private String genertateReportName(String reconFileName, String fileName,
			ReconBatchProcessEntity reconProcessManager) throws IOException {
		SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
		String formattedDate = date.format(new Date());

		String reportFilePath = uDrive + "/Report/" + formattedDate;

		// Use mkdirs() to create the full path, including parent directories if they
		// don't exist.
		// This is a robust and thread-safe way to create directories.
		File reportDirectory = new File(reportFilePath);
		if (!reportDirectory.exists()) {
			if (!reportDirectory.mkdirs()) {
				reconProcessManager.setStatus("Error");
				reconProcessManager.setReportStatus("Error");
				reconBatchProcessEntityRepository.save(reconProcessManager);
				reconBatchProcessEntityRepository.flush();
				throw new IOException("Failed to create report directories");
			}
		}

		// Remove file .csv Extension
		String fileNameWithoutExtension = "";
		if (fileName.endsWith(".csv")) {
			fileNameWithoutExtension = fileName.substring(0, fileName.length() - ".csv".length());
		} else {
			fileNameWithoutExtension = reconFileName;
		}

		String uniqueId = UUID.randomUUID().toString().substring(0, 2);
		String reportTimeStamp = fileNameWithoutExtension + "_" + "SUMMARY" + "_"
				+ new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss-SSS").format(new Date()) + uniqueId + ".csv";

		// The reportFilePath is already a directory, so we can concatenate the filename
		// The parent directory for the final file is guaranteed to exist from the
		// mkdirs() call above.
		String reportPath = reportFilePath + "/" + reportTimeStamp;

		// Log the final path for debugging
		logger.info("Final report path: " + reportPath);

		return reportPath;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void generateExceptionReport(ReconProcessDefMaster reconProcessDefMaster, ReconBatchProcessEntity process) {

		if (reconProcessDefMaster.getReconProcessName().equalsIgnoreCase("UPIINWARDRECON")) {
			String sqlQuery = "SELECT "
					+ "'UPI INWARD RECON(IN NPCI NOT IN CBS NOT IN SWT)' REMARKS,TRAN_SEQ_NUM,TRUNC(TRAN_DATE) TRAN_DATE,TRAN_TIME ,NVL(TRAN_AMOUNT,0) TRAN_AMOUNT, TRUNC(SYSDATE-TRAN_dATE)||' Days' Adging ,"
					+ "'N/A' DR_ACC_NO, to_char(TRAN_ACCT_NUM) CR_ACC_NO,'N/A' VALUE_DATE," + "'N/A' TRAN_ID,"
					+ "'N/A' SENDER_REF_NUM," + "TRAN_RESP_CODE "
					+ "FROM REC_UPI_MIS_UBEN_DATA WHERE REC_FLG=0 AND DYN_UPHST_UPI_REC_FLAG='0' AND DYN_UPISWT_SWT_REC_FLAG='0' "
					+ "UNION ALL " + "SELECT "
					+ "'UPI INWARD RECON(IN NPCI AND CBS NOT IN SWT)' REMARKS,TRAN_SEQ_NUM,TRUNC(TRAN_DATE) TRAN_DATE,TRAN_TIME ,TRAN_AMOUNT TRAN_AMOUNT, TRUNC(SYSDATE-TRAN_dATE)||' Days' Adging ,"
					+ "'N/A' DR_ACC_NO, to_char(TRAN_ACCT_NUM) CR_ACC_NO,'N/A' VALUE_DATE," + "'N/A' TRAN_ID,"
					+ "'N/A' SENDER_REF_NUM," + "TRAN_RESP_CODE "
					+ "FROM REC_UPI_MIS_UBEN_DATA WHERE REC_FLG=0 AND DYN_UPHST_UPI_REC_FLAG='1' AND DYN_UPISWT_SWT_REC_FLAG='0' "
					+ "UNION ALL " + "SELECT "
					+ "'UPI INWARD RECON(IN CBS NOT IN NPCI NOT IN SWT)' REMARKS,TRAN_SEQ_NUM,TRUNC(TRAN_DATE) TRAN_DATE,to_char(rcre_time,'HH24:MI:SS') TRAN_TIME,NVL(TRAN_AMOUNT,0), TRUNC(SYSDATE-TRAN_dATE)||' Days' Adging,"
					+ "'N/A' DR_ACC_NO,CR_ACC_NO CR_ACC_NO,to_char(TRUNC(VALUE_DATE)) VALUE_DATE,"
					+ "TRIM(TRAN_ID) TRAN_ID," + "SENDER_REF_NUM," + "TRAN_RESP_CODE "
					+ "FROM  REC_UPHST_UPI_UBEN_DATA WHERE REC_FLG=0 AND DYN_UPI_MIS_REC_FLAG='0' AND DYN_UPISWT_SWT_REC_FLAG='0' "
					+ "UNION ALL " + "SELECT "
					+ "'UPI INWARD RECON(IN CBS AND SWT NOT IN NPCI)' REMARKS,TRAN_SEQ_NUM,TRUNC(TRAN_DATE) TRAN_DATE,to_char(rcre_time,'HH24:MI:SS') TRAN_TIME,TRAN_AMOUNT, TRUNC(SYSDATE-TRAN_dATE)||' Days' Adging,"
					+ "'N/A' DR_ACC_NO,CR_ACC_NO CR_ACC_NO,to_char(TRUNC(VALUE_DATE)) VALUE_DATE,"
					+ "TRIM(TRAN_ID) TRAN_ID," + "SENDER_REF_NUM," + "TRAN_RESP_CODE "
					+ "FROM  REC_UPHST_UPI_UBEN_DATA WHERE REC_FLG=0 AND DYN_UPI_MIS_REC_FLAG='0' AND DYN_UPISWT_SWT_REC_FLAG='1' "
					+ "UNION ALL " + "SELECT "
					+ "'UPI INWARD RECON(IN SWT NOT IN NPCI NOT IN CBS)' REMARKS,TRAN_SEQ_NUM,TRUNC(TRAN_DATE) TRAN_DATE,to_char(tran_date,'HH24:MI:SS') TRAN_TIME,NVL(TRAN_AMOUNT,0) TRAN_AMOUNT , TRUNC(SYSDATE-TRAN_dATE)||' Days' Adging,"
					+ "NVL(DR_ACCT_NO,'N/A') DR_ACC_NO,NVL(TRAN_ACCT_NUM,'N/A') CR_ACC_NO,'N/A' VALUE_DATE,"
					+ "'N/A' TRAN_ID," + "'N/A' SENDER_REF_NUM," + "'N/A' TRAN_RESP_CODE "
					+ "FROM  rec_upiswt_swt_uben_DATA WHERE REC_FLG=0 AND DYN_UPI_MIS_REC_FLAG='0' AND DYN_UPHST_UPI_REC_FLAG='0' "
					+ "UNION ALL " + "SELECT "
					+ "'UPI INWARD RECON(IN SWT AND NPCI NOT IN CBS)' REMARKS,TRAN_SEQ_NUM,TRUNC(TRAN_DATE) TRAN_DATE,to_char(tran_date,'HH24:MI:SS') TRAN_TIME,NVL(TRAN_AMOUNT,0) TRAN_AMOUNT , TRUNC(SYSDATE-TRAN_dATE)||' Days' Adging,"
					+ "NVL(DR_ACCT_NO,'N/A') DR_ACC_NO,NVL(TRAN_ACCT_NUM,'N/A') CR_ACC_NO,'N/A' VALUE_DATE,"
					+ "'N/A' TRAN_ID," + "'N/A' SENDER_REF_NUM," + "'N/A' TRAN_RESP_CODE "
					+ "FROM  rec_upiswt_swt_uben_DATA WHERE REC_FLG=0 AND DYN_UPI_MIS_REC_FLAG='1' AND DYN_UPHST_UPI_REC_FLAG='0'";

			try {
				List<TTUMExceptionReportDto> exceptionRecordList = getTTUMExceptionListFromQuery(sqlQuery);
				String fileName = " ";
				String reconReportName = genertateReportName(reconProcessDefMaster.getReconProcessName(), fileName,
						process);
				File getFileName = new File(reconReportName);
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(reconReportName))) {
					writer.write(
							"REMARKS , TRAN_SEQ_NUM , TRAN_DATE , TRAN_TIME , Adging , DR_ACC_NO , CR_ACC_NO , VALUE_DATE , TRAN_ID , SENDER_REF_NUM , TRAN_RESP_CODE");
					writer.newLine();
					for (TTUMExceptionReportDto report : exceptionRecordList) {
						writer.write(report.getRemarks() + ",  " + report.getTranSeqNum() + ",  " + report.getTranDate()
								+ ", " + report.getTranTime() + ",  " + report.getTranAmount() + ",  "
								+ report.getAging() + ",  " + report.getDrAccNo() + ",  " + report.getCrAccNo() + ",  "
								+ report.getValueDate() + ",  " + report.getTranId() + ",  " + report.getSenderRefNum()
								+ ",  " + report.getTranRespCode());
						writer.newLine();
					}
					writer.newLine();

					process.setReportStatus("Completed");
					ReportEntity newReconciliationReport = new ReportEntity();
					newReconciliationReport.setProcessId(reconProcessDefMaster.getReconProcessId());
					newReconciliationReport.setReportDate(LocalDate.now());
					newReconciliationReport.setReportFileName(reconProcessDefMaster.getReconProcessName());
					newReconciliationReport.setReportLocation(reconReportName);
					newReconciliationReport.setReportName(getFileName.getName());
					reportRepository.save(newReconciliationReport);
					logger.info("Reconciliation report created successfully at ::::::::" + reconReportName);
				} catch (Exception e) {
					logger.info("Error writing to CSV file: " + e.getMessage());
					process.setReportStatus("Error");
					process.setStatus("Error");
					reconBatchProcessEntityRepository.save(process);
				}

				reconBatchProcessEntityRepository.save(process);
			} catch (Exception e) {
				logger.info("ERRRRRRRRRRR :::::::::::::::::::::::::::" + e.getMessage());
			}
		} else if (reconProcessDefMaster.getReconProcessName().equalsIgnoreCase("EPIK_BC_RECON")) {
			writeEpikReconUnReconReversalAndCbsReconUnReconReversalReport(reconProcessDefMaster.getReconProcessId(),
					reconProcessDefMaster.getReconProcessName(), process);
		} else if (reconProcessDefMaster.getReconProcessName().equalsIgnoreCase("CKYC_")) {
			writeCkycReconReport(reconProcessDefMaster.getReconProcessId(), reconProcessDefMaster.getReconProcessName(),
					process);
		} else if (reconProcessDefMaster.getReconProcessName().equalsIgnoreCase("AEPS_ELMS")) {
			writeAeofElmsReconReport(reconProcessDefMaster.getReconProcessId(),
					reconProcessDefMaster.getReconProcessName(), process);
		}

	}

	private String generateAepsFileName(String fileName) {
		SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss-SS");
		String formatDate = date.format(new Date());
		String dataReportFileNamePath = aepsDataFilePath + fileName + formatDate + ".csv";
		return dataReportFileNamePath;
	}

	private List<AepsCbsReportDto> getAepsCbsData(String reportQuery) {
		return jdbcTemplate.query(reportQuery, new RowMapper<AepsCbsReportDto>() {

			@Override
			public AepsCbsReportDto mapRow(ResultSet rs, int rowNum) throws SQLException {
				AepsCbsReportDto data = new AepsCbsReportDto();
				data.setTransactionReferenceNo(rs.getString("TXN_REF_NO"));
				data.setAccountNumber(rs.getString("ACCOUNT_NUMBER"));
				data.setTransactionDate(rs.getString("TXN_DATE"));
				data.setTransactionPostDate(rs.getString("TXN_POST_DATE"));
				data.setTransactionTime(rs.getString("TXN_TIME"));
				data.setNarration(rs.getString("NARRATION"));
				data.setRemarks(rs.getString("REMARKS"));
				data.setDebit(rs.getString("DEBIT_AMT"));
				data.setCredit(rs.getString("CREDIT_AMT"));
				data.setTransactionSequenceNo(rs.getString("TRAN_SEQ_NUM"));
				data.setChecker(rs.getString("CHECKER"));
				return data;
			}
		});
	}

	private List<AepsEpikReportDto> getAepsEpikData(String reportQuery) {
		return jdbcTemplate.query(reportQuery, new RowMapper<AepsEpikReportDto>() {

			@Override
			public AepsEpikReportDto mapRow(ResultSet rs, int rowNum) throws SQLException {
				AepsEpikReportDto data = new AepsEpikReportDto();
				data.setAgentId(rs.getString("AGENT_ID"));
				data.setRequestReferenceNo(rs.getString("TRAN_SEQ_NUM"));
				data.setTransactionId(rs.getString("TRANSACTION_ID"));
				data.setAmount(rs.getDouble("TRAN_AMOUNT"));
				data.setTransactionType(rs.getString("TRANSACTION_TYPE"));
				data.setSourceBank(rs.getString("SOURCE_BANK"));
				data.setStatus(rs.getString("STATUS"));
				data.setNarration(rs.getString("NARATION"));
				data.setBalance(rs.getDouble("BALANCE"));
				data.setCreatedTs(rs.getString("CREATED_TS"));
				return data;
			}
		});
	}

	private List<TTUMExceptionReportDto> getTTUMExceptionListFromQuery(String exceptionQuery) {
		return jdbcTemplate.query(exceptionQuery, new RowMapper<TTUMExceptionReportDto>() {

			@Override
			public TTUMExceptionReportDto mapRow(ResultSet rs, int rowNum) throws SQLException {
				TTUMExceptionReportDto data = new TTUMExceptionReportDto();
				data.setRemarks(rs.getString("REMARKS"));
				data.setTranSeqNum(rs.getString("TRAN_SEQ_NUM"));
				data.setTranDate(rs.getDate("TRAN_DATE"));
				data.setTranTime(rs.getString("TRAN_TIME"));
				data.setTranAmount(rs.getDouble("TRAN_AMOUNT"));
				data.setAging(rs.getString("Adging"));
				data.setDrAccNo(rs.getString("DR_ACC_NO"));
				data.setCrAccNo(rs.getString("CR_ACC_NO"));
				data.setValueDate(rs.getString("VALUE_DATE"));
				data.setTranId(rs.getString("TRAN_ID"));
				data.setSenderRefNum(rs.getString("SENDER_REF_NUM"));
				data.setTranRespCode(rs.getString("TRAN_RESP_CODE"));
				return data;
			}

		});
	}

	@Override
	public ResponseEntity<RestWithStatusList> viewExtrationDetails(ReportDto extractionRequest) {
		List<ReconBatchProcessEntity> extractionReconData = reconBatchProcessEntityRepository
				.findByProcessTypeAndProcessIdAndInsertDate(extractionRequest.getReportType(),
						extractionRequest.getProcessId(), extractionRequest.getReportDate());
		logger.info("EXTRACTION DETAILS BY PROCESSID ::::::::::::::" + extractionReconData);

		RestWithStatusList restWithStatusList;
		List<Object> extractionList = new ArrayList<>();
		if (!extractionReconData.isEmpty()) {
			for (ReconBatchProcessEntity data : extractionReconData) {
				extractionList.add(data);
			}

			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Extraction Details Found.", extractionList);

		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Extraction Details Not Found !", extractionList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	private List<FileDataUpdateReportDto> getAepsBCReportData(String aepsReportQuery) {
		return jdbcTemplate.query(aepsReportQuery, new RowMapper<FileDataUpdateReportDto>() {
			@Override
			public FileDataUpdateReportDto mapRow(ResultSet rs, int rowNum) throws SQLException {
				FileDataUpdateReportDto fileDataUpdateReport = new FileDataUpdateReportDto();
				fileDataUpdateReport.setTranSeqNum(rs.getString("TRAN_SEQ_NUM"));
				fileDataUpdateReport.setFlag(rs.getString("FLAG"));
				fileDataUpdateReport.setShtdt(rs.getString("SHTDT"));
				fileDataUpdateReport.setAdjAmt(rs.getLong("ADJAMT"));
				fileDataUpdateReport.setShser(rs.getString("SHSER"));
				fileDataUpdateReport.setShcrd(rs.getString("SHCRD"));
				fileDataUpdateReport.setFileName(rs.getString("FILE_NAME"));
				fileDataUpdateReport.setReason(rs.getInt("REASON"));
				fileDataUpdateReport.setUrn(rs.getString("URN"));
				return fileDataUpdateReport;
			}
		});
	}

	@Override
	public void writeReversalReport(ReconFileDetailsMaster reconFileDetails,
			ReconBatchProcessEntity reconProcessManager) {
		writeEpikReconUnReconReversalAndCbsReconUnReconReversalReport(reconFileDetails.getReconFileId(),
				reconFileDetails.getReconFileName(), reconProcessManager);
	}

	private void writeEpikReconUnReconReversalAndCbsReconUnReconReversalReport(Long processId, String fileName,
			ReconBatchProcessEntity process) {

		List<ExceptionReconReportEntity> exceptionReportList = exceptionReconReportRepository
				.findByProcessId(processId);
		logger.info("exceptionReportList ::::::::::::::" + exceptionReportList);
		if (!exceptionReportList.isEmpty()) {
			for (ExceptionReconReportEntity reportData : exceptionReportList) {
				if (reportData.getFileName().equalsIgnoreCase("CBS Recon Report")
						|| reportData.getFileName().equalsIgnoreCase("CBS UnRecon Report")
						|| reportData.getFileName().equalsIgnoreCase("CBS_REVERSAL_REPORT")) {
					List<AepsCbsReportDto> aepsCbsReportData = getAepsCbsData(reportData.getReportQuery());

					String reconReportName = generateAepsFileName(reportData.getFileName());
					File getFileName = new File(reconReportName);

					try (BufferedWriter writer = new BufferedWriter(new FileWriter(reconReportName))) {
						writer.write(
								"TXN_REF_NO~ACCOUNT_NUMBER~TXN_DATE~TXN_POST_DATE~TXN_TIME~NARRATION~REMARKS~DEBIT~CREDIT~TRAN_SEQ_NUM~CHECKER");
						writer.newLine();
						for (AepsCbsReportDto cbsReport : aepsCbsReportData) {
							writer.write(cbsReport.getTransactionReferenceNo() + "~" + cbsReport.getAccountNumber()
									+ "~" + cbsReport.getTransactionDate() + "~" + cbsReport.getTransactionPostDate()
									+ "~" + cbsReport.getTransactionTime() + "~" + cbsReport.getNarration() + "~"
									+ cbsReport.getRemarks() + "~" + cbsReport.getDebit() + "~" + cbsReport.getCredit()
									+ "~" + cbsReport.getTransactionSequenceNo() + "~" + cbsReport.getChecker());
							writer.newLine();
						}
						writer.newLine();

						process.setReportStatus("Completed");
						ReportEntity newReconciliationReport = new ReportEntity();
						newReconciliationReport.setProcessId(processId);
						newReconciliationReport.setReportDate(LocalDate.now());
						newReconciliationReport.setReportFileName(fileName);
						newReconciliationReport.setReportLocation(reconReportName);
						newReconciliationReport.setReportName(getFileName.getName());
						reportRepository.save(newReconciliationReport);
						logger.info("Report created successfully ::::::::" + reportData.getFileName());
					} catch (Exception e) {
						logger.info("Error writing to CSV file: " + e.getMessage());
						process.setReportStatus("Error");
						process.setStatus("Error");
						reconBatchProcessEntityRepository.save(process);
					}

					reconBatchProcessEntityRepository.save(process);

				} else if (reportData.getFileName().equalsIgnoreCase("EPIK Recon Report")
						|| reportData.getFileName().equalsIgnoreCase("EPIK UnRecon Report")
						|| reportData.getFileName().equalsIgnoreCase("EPIK_REVERSAL_REPORT")) {
					List<AepsEpikReportDto> aepsEpikReportData = getAepsEpikData(reportData.getReportQuery());

					String reconReportName = generateAepsFileName(reportData.getFileName());
					File getFileName = new File(reconReportName);

					try (BufferedWriter writer = new BufferedWriter(new FileWriter(reconReportName))) {
						writer.write("AGENT_ID" + "~" + "TRAN_SEQ_NUM" + "~" + "TRANSACTION_ID" + "~" + "TRAN_AMOUNT"
								+ "~" + "TRANSACTION_TYPE" + "~" + "SOURCE_BANK" + "~" + "STATUS" + "~" + "NARATION"
								+ "~" + "BALANCE" + "~" + "CREATED_TS");
						writer.newLine();
						for (AepsEpikReportDto epikReport : aepsEpikReportData) {
							writer.write(epikReport.getAgentId() + "~" + epikReport.getRequestReferenceNo() + "~"
									+ epikReport.getTransactionId() + "~" + epikReport.getAmount() + "~"
									+ epikReport.getTransactionType() + "~" + epikReport.getSourceBank() + "~"
									+ epikReport.getStatus() + "~" + epikReport.getNarration() + "~"
									+ epikReport.getBalance() + "~" + epikReport.getCreatedTs());
							writer.newLine();
						}
						writer.newLine();

						process.setReportStatus("Completed");
						ReportEntity newReconciliationReport = new ReportEntity();
						newReconciliationReport.setProcessId(processId);
						newReconciliationReport.setReportDate(LocalDate.now());
						newReconciliationReport.setReportFileName(fileName);
						newReconciliationReport.setReportLocation(reconReportName);
						newReconciliationReport.setReportName(getFileName.getName());
						reportRepository.save(newReconciliationReport);
						logger.info("Report created successfully ::::::::" + reportData.getFileName());
					} catch (Exception e) {
						logger.info("Error writing to CSV file: " + e.getMessage());
						process.setReportStatus("Error");
						process.setStatus("Error");
						reconBatchProcessEntityRepository.save(process);
					}

					reconBatchProcessEntityRepository.save(process);

				}
			}
		} else {
			process.setReportStatus("Error");
			process.setStatus("Error");
			reconBatchProcessEntityRepository.save(process);
			logger.info("Report Query not found for process Id : " + processId);
		}

	}

	private void writeCkycReconReport(Long reconProcessId, String reconProcessName, ReconBatchProcessEntity process) {
		List<ExceptionReconReportEntity> reportData = exceptionReconReportRepository.findByProcessId(reconProcessId);

		if (!reportData.isEmpty()) {
			for (ExceptionReconReportEntity report : reportData) {
				List<CkycReconObReportDto> ckycObRecon = getCkycReconData(report.getReportQuery());
				String reconReportName = generateAepsFileName(report.getFileName());
				File getFileName = new File(reconReportName);

				try (BufferedWriter writer = new BufferedWriter(new FileWriter(reconReportName))) {

					writer.newLine();
					for (CkycReconObReportDto ckycReport : ckycObRecon) {
						writer.write(
								"TRAN_SEQ_NUM|ACCOUNT_OPEN_DATE|CKYC_STATUS|CKYC_NUMBER|BATCH_DATE|TAT|STATUS|CKYC_CATEGORY");
						writer.newLine();
						writer.write(ckycReport.getTranSeqNum() + "|" + ckycReport.getAccountOpenDate() + "|"
								+ ckycReport.getCkycStatus() + "|" + ckycReport.getCkycNumber() + "|"
								+ ckycReport.getBatchDate() + "|" + ckycReport.getTat() + "|" + ckycReport.getStatus()
								+ "|" + ckycReport.getCkycCategory());
					}
					writer.newLine();

					process.setReportStatus("Completed");
					ReportEntity newReconciliationReport = new ReportEntity();
					newReconciliationReport.setProcessId(reconProcessId);
					newReconciliationReport.setReportDate(LocalDate.now());
					newReconciliationReport.setReportFileName(reconProcessName);
					newReconciliationReport.setReportLocation(reconReportName);
					newReconciliationReport.setReportName(getFileName.getName());
					reportRepository.save(newReconciliationReport);
					logger.info("Report created successfully ::::::::" + report.getFileName());
				} catch (Exception e) {
					logger.info("Error writing to CSV file: " + e.getMessage());
					process.setReportStatus("Error");
					process.setStatus("Error");
					reconBatchProcessEntityRepository.save(process);
				}

				reconBatchProcessEntityRepository.save(process);
			}
		}

	}

	private List<CkycReconObReportDto> getCkycReconData(String reportQuery) {
		return jdbcTemplate.query(reportQuery, new RowMapper<CkycReconObReportDto>() {

			@Override
			public CkycReconObReportDto mapRow(ResultSet rs, int rowNum) throws SQLException {
				CkycReconObReportDto ckycReport = new CkycReconObReportDto();
				ckycReport.setTranSeqNum(rs.getString("TRAN_SEQ_NUM"));
				ckycReport.setAccountOpenDate(rs.getDate("ACCOUNT_OPEN_DATE"));
				ckycReport.setCkycStatus(rs.getString("CKYC_STATUS"));
				ckycReport.setCkycNumber(rs.getString("CKYC_NUMBER"));
				ckycReport.setBatchDate(rs.getDate("BATCH_DATE"));
				ckycReport.setTat(rs.getInt("TAT"));
				ckycReport.setStatus(rs.getString("STATUS"));
				ckycReport.setCkycCategory(rs.getString("CKYC_CATEGORY"));
				return ckycReport;
			}
		});
	}

	private void writeAeofElmsReconReport(Long reconProcessId, String reconProcessName,
			ReconBatchProcessEntity process) {

	}

	@Override
	public File generateManualFileForProcess(ReconFileDetailsMaster fileDetails) {
		SimpleDateFormat date = new SimpleDateFormat("ddMMYY");
		String formatDate = date.format(new Date());
		String fileName = "sample" + formatDate + ".csv";

		File makeNewFile = new File(fileDetails.getReconFileLocation() + "/" + fileName);

		List<ExceptionReconReportEntity> reportList = exceptionReconReportRepository
				.findByProcessId(fileDetails.getReconFileId());

		if (reportList.isEmpty()) {
			logger.info("Report Configuration not found for this process " + fileDetails.getReconFileId());
			return null;
		}

		for (ExceptionReconReportEntity report : reportList) {
			JasperReportHeader reportHeader = JdbcReportUtils.getReportHeader(jdbcTemplate, report.getReportHeader());
			List<JasperReportData> reportData = JdbcReportUtils.getReportData(jdbcTemplate, report.getReportQuery());

			if (reportData.isEmpty()) {
				logger.info("Report header or Report data is needed for Jasper report. Skipping report for process: "
						+ report.getProcessId());
				return null;
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(makeNewFile))) {
				writer.write(reportHeader.getReportHeader());
				writer.newLine();
				for (JasperReportData jsReport : reportData) {
					writer.write(jsReport.getRowData());
					writer.newLine();
				}
				writer.newLine();
			} catch (Exception e) {
				logger.info("Errorr");
			}

		}
		logger.info("MAKE File ::::::: { }" + makeNewFile);
		return makeNewFile;
	}

}
