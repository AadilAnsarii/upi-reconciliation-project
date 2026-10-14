package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReportDto {
	private Long reportId;
	private Long processId;
	private String reportKey;
	private String reportQuery;
	private String reportName;
	private String reportFileName;
	private String reportSeprator;
	private String reportLocation;
	private LocalDate reportDate;
	private String reportType;
}
