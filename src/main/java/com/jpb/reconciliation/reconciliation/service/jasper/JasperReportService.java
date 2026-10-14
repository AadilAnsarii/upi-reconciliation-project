package com.jpb.reconciliation.reconciliation.service.jasper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportData;
import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportHeader;
import com.jpb.reconciliation.reconciliation.entity.ExceptionReconReportEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
import com.jpb.reconciliation.reconciliation.repository.ExceptionReconReportRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReportRepository;

@Service
public class JasperReportService {

	Logger logger = LoggerFactory.getLogger(JasperReportService.class);

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Value("${app.reconReport}")
	private String reconFileLoc;

	@Autowired
	ReportRepository reportRepository;

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@Autowired
	ExceptionReconReportRepository exceptionReconReportRepository;

	public void generateGlobalReconciliationReport(ReconProcessDefMaster reconProcessDefMaster,
			ReconBatchProcessEntity process) {
		List<ExceptionReconReportEntity> reportList = exceptionReconReportRepository
				.findByProcessId(reconProcessDefMaster.getReconProcessId());

		if (reportList.isEmpty()) {
			logger.info("Report Configuration not found for this process " + reconProcessDefMaster.getReconProcessId());
			return;
		}

		for (ExceptionReconReportEntity report : reportList) {
			JasperReportHeader reportHeader = getReportHeader(report.getReportHeader());
			List<JasperReportData> reportData = getReportData(report.getReportQuery());

			if (reportHeader == null || reportData.isEmpty()) {
				logger.info("Report header or Report data is needed for Jasper report. Skipping report for process: "
						+ report.getProcessId());
				continue;
			}

			String filePath = generateFileName(report.getFileName());

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				writer.write(reportHeader.getReportHeader());
				writer.newLine();
				for (JasperReportData jsReport : reportData) {
					writer.write(jsReport.getRowData());
					writer.newLine();
				}
				writer.newLine();

				// Saving Report
				File getFileName = new File(filePath);
				ReportEntity newReconciliationReport = new ReportEntity();
				newReconciliationReport.setProcessId(reconProcessDefMaster.getReconProcessId());
				newReconciliationReport.setReportDate(LocalDate.now());
				newReconciliationReport.setReportFileName(reconProcessDefMaster.getReconProcessName());
				newReconciliationReport.setReportLocation(filePath);
				newReconciliationReport.setReportName(getFileName.getName());
				reportRepository.save(newReconciliationReport);

				logger.info("Report exported successfully to: " + filePath);
			} catch (Exception e) {
				logger.info("Error writing to CSV file: " + e.getMessage());
				process.setReportStatus("Error");
				process.setStatus("Error");
				reconBatchProcessEntityRepository.save(process);
			}
//				Map<String, Object> parameters = new HashMap<>();
//				parameters.put("reportHeader", reportHeader.getReportHeader());
//				JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
//				InputStream jasperTemplateStream = getClass().getClassLoader()
//						.getResourceAsStream("jasperreports/recon-global-report.jrxml");
//				if (jasperTemplateStream == null) {
//					throw new JRException(
//							"JasperReports template not found in classpath: jasperreports/recon-global-report.jrxml");
//				}
//				JasperReport jasperReport = JasperCompileManager.compileReport(jasperTemplateStream);
//				JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
//				JRCsvExporter exporter = new JRCsvExporter();
//				exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
//				exporter.setExporterOutput(new SimpleWriterExporterOutput(filePath));
//				SimpleCsvExporterConfiguration configuration = new SimpleCsvExporterConfiguration();
//				configuration.setFieldDelimiter(",");
////				configuration.setRecordDelimiter("\n");
////				configuration.setForceFieldEnclosure("");
//				configuration.setFieldEnclosure("");
////				configuration.setRecordDelimiter("");
//				exporter.setConfiguration(configuration);
//				exporter.exportReport();

		}
		process.setReportStatus("Completed");
		reconBatchProcessEntityRepository.save(process);
	}

	private String generateFileName(String fileName) {
		SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss");
		String formatDate = date.format(new Date());
		String reportFilePath = reconFileLoc + fileName + formatDate + ".csv";
		return reportFilePath;
	}

	private JasperReportHeader getReportHeader(String reportHeaderSql) {
		try {
			return jdbcTemplate.queryForObject(reportHeaderSql, (rs, rowNum) -> {
				JasperReportHeader header = new JasperReportHeader();
				header.setReportHeader(rs.getString("HEADER"));
				return header;
			});
		} catch (Exception e) {
			logger.error("Error retrieving report header: " + reportHeaderSql, e);
			return null;
		}
	}

	private List<JasperReportData> getReportData(String reportQuerySql) {
		return jdbcTemplate.query(reportQuerySql, new RowMapper<JasperReportData>() {
			@Override
			public JasperReportData mapRow(ResultSet rs, int rowNum) throws SQLException {
				JasperReportData reportdata = new JasperReportData();
				reportdata.setRowData(rs.getString("DATA"));
				return reportdata;
			}
		});
	}

}