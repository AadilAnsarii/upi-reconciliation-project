package com.jpb.reconciliation.reconciliation.util;

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
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
import com.jpb.reconciliation.reconciliation.repository.ExceptionReconReportRepository;
import com.jpb.reconciliation.reconciliation.repository.ReportRepository;

@Service
public class CommonReport {

	Logger logger = LoggerFactory.getLogger(CommonReport.class);

	@Autowired
	ReportRepository reportRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Value("${app.reconReport}")
	private String reconFileLoc;

	@Autowired
	ExceptionReconReportRepository exceptionReconReportRepository;

	public Boolean generateGlobalReconciliationReport(ReconFileDetailsMaster reconFileDetails
			) {
		Boolean globalReportFlg = false;
		List<ExceptionReconReportEntity> reportList = exceptionReconReportRepository
				.findByProcessId(reconFileDetails.getReconFileId());

		if (reportList.isEmpty()) {
			logger.info("Report Configuration not found for this process " + reconFileDetails.getReconFileId());
			return globalReportFlg;
		}

		for (ExceptionReconReportEntity report : reportList) {
			JasperReportHeader reportHeader = getReportHeader(report.getReportHeader());
			List<JasperReportData> reportData = getReportData(report.getReportQuery());

			if (reportData.isEmpty()) {
				logger.info("Report header or Report data is needed for Jasper report. Skipping report for process: "
						+ report.getProcessId());
				return globalReportFlg;
			}

			String filePath = generateFileName(report.getFileName(), reconFileDetails);

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
				if (!reconFileDetails.getReconFileName().equalsIgnoreCase("AEPS CREDIT ADJUSTMENT")) {
					writer.write(reportHeader.getReportHeader());
					writer.newLine();
				}

				for (JasperReportData jsReport : reportData) {
					writer.write(jsReport.getRowData());
					writer.newLine();
				}
				writer.newLine();

				// Saving Report
				File getFileName = new File(filePath);
				ReportEntity newReconciliationReport = new ReportEntity();
				newReconciliationReport.setProcessId(reconFileDetails.getReconFileId());
				newReconciliationReport.setReportDate(LocalDate.now());
				newReconciliationReport.setReportFileName(reconFileDetails.getReconFileName());
				newReconciliationReport.setReportLocation(filePath);
				newReconciliationReport.setReportName(getFileName.getName());
				reportRepository.save(newReconciliationReport);

				logger.info("Report exported successfully to: " + filePath);
				globalReportFlg = true;
			} catch (Exception e) {
				logger.info("Error writing to CSV file: " + e.getMessage());
				globalReportFlg = false;
			}

		}
		return globalReportFlg;

	}

	private String generateFileName(String fileName, ReconFileDetailsMaster reconFileDetails) {
		SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss-SS");
		String formatDate = date.format(new Date());
		if (reconFileDetails.getReconFileName().equalsIgnoreCase("AEPS CREDIT ADJUSTMENT")) {
			List<String> getChannelIdList = getChannelId();
			if (getChannelIdList.isEmpty()) {
				logger.info("Channel id is null ::::::");
				String aepsReportFilePath = reconFileLoc + "AEPS_BC_PARTNER_CA" + formatDate + ".txt";
				return aepsReportFilePath;
			} else {
				String channnelId = getChannelIdList.get(0).trim();
				String aepsChannelIdReportFilePath = reconFileLoc + "AEPS_BC_PARTNER_CA" + channnelId + "_" + formatDate
						+ ".txt";
				return aepsChannelIdReportFilePath;
			}

		} else {
			String reportFilePath = reconFileLoc + fileName+ formatDate + ".csv";
			return reportFilePath;
		}

	}

	private List<String> getChannelId() {
		String sql = "SELECT CHANNEL_ID FROM REC_AEPS_BC_STAGE_T";

		return jdbcTemplate.queryForList(sql, String.class);
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
