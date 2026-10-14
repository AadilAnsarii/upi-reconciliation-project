package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "rcn_report_mast_config")
public class ExceptionReconReportEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REPORT_MAST_SEQ")
	@SequenceGenerator(name = "REPORT_MAST_SEQ", sequenceName = "REPORT_MAST_SEQ",allocationSize = 1)
	@Column(name = "REPORT_ID")
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
    
	@Column(name = "REPORT_DATE")
	private LocalDate reportDate;
	
	@Column(name = "FILE_NAME")
	private String fileName;
}
