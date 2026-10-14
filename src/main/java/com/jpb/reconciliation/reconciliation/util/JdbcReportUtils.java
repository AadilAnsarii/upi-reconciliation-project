package com.jpb.reconciliation.reconciliation.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportData;
import com.jpb.reconciliation.reconciliation.dto.jasperdto.JasperReportHeader;

public class JdbcReportUtils {

	private static final Logger logger = LoggerFactory.getLogger(JdbcReportUtils.class);

	@Autowired
	JdbcTemplate jdbcTemplate;

	public static JasperReportHeader getReportHeader(JdbcTemplate jdbcTemplate, String reportHeaderSql) {
		try {
			return jdbcTemplate.queryForObject(reportHeaderSql, (rs, rowNum) -> {
				JasperReportHeader header = new JasperReportHeader();
				header.setReportHeader(rs.getString("HEADER"));
				return header;
			});
		} catch (Exception e) {
			logger.error("Error retrieving report header: {}", reportHeaderSql, e);
			return null;
		}
	}

	public static List<JasperReportData> getReportData(JdbcTemplate jdbcTemplate, String reportQuerySql) {
		return jdbcTemplate.query(reportQuerySql, (rs, rowNum) -> {
			JasperReportData reportdata = new JasperReportData();
			reportdata.setRowData(rs.getString("DATA"));
			return reportdata;
		});
	}

}
