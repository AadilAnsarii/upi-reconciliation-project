package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import lombok.Data;

@Data
public class ReportRequest {

	List<Report> reports;

	@Data
	public static class Report {
		private Long processId;
		private String reportName;
		private String reportLocation;
	}

}
