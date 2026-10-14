package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "RCN_REPORT")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReportEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REPORT")
	@SequenceGenerator(name = "SEQ_REPORT", sequenceName = "SEQ_REPORT",allocationSize = 1)
	@Column(name = "RECON_ID")
	private Long reportId;

	@Column(name = "PROCESS_ID")
	private Long processId;
	
	@Column(name = "TEMPLATE_ID")
	private Long templateId;

	@Column(name = "REPORT_KEY")
	private String reportKey;

	@Column(name = "REPORT_HEADER")
	private String reportHeader;

	@Column(name = "REPORT_QUERY")
	private String reportQuery;

	@Column(name = "REPORT_NAME")
	private String reportName;

	@Column(name = "REPORT_FILE_NAME")
	private String reportFileName;

	@Column(name = "REPORT_SEPRATOR")
	private String reportSeprator;

	@Column(name = "REPORT_LOCATION")
	private String reportLocation;

	@Column(name = "REPORT_DATE")
	private LocalDate reportDate;
	
	@Column(name = "FILE_NAME")
	private String fileName;

}
