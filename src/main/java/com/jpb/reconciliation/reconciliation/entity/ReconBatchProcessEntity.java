package com.jpb.reconciliation.reconciliation.entity;
import java.time.LocalDate;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Table(name = "rcn_batch_process")
@Data
@Entity
public class ReconBatchProcessEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
	@SequenceGenerator(name = "sequence", sequenceName = "seq_ReconBatchProcessEntity", allocationSize = 1)
	@Column(name = "RBP_SNO")
	private Long sequenceNo;
	
	@Column(name = "RBP_PROCESS_ID")
	private Long processId;
	
	@Column(name = "TEMPLATE_ID")
	private Long templateId;
	
	@Column(name = "RBP_PROCESS_TYPE")
	private String processType;
	
	@Column(name = "RBP_START_TIME")
	private String startTime;
	
	@Column(name = "RBP_END_TIME")
	private String endTime;
	
	@Column(name = "RBP_STATUS")
	private String status;
	
	@Column(name = "RBP_FILE_NAME")
	private String fileName;
	
	@Column(name = "RBP_HEADER_DETAILS")
	private String headerDetails;
	
	@Column(name = "RBP_CTF_HEADER_DETAILS")
	private String controlFileHeaderDetails;
	
	@Column(name = "RBP_SEQ_HEADER_DETAILS")
	private Date seqHeaderDetails;
	
	@Column(name = "RBP_INST_CODE")
	private Long instCode;
	
	@Column(name = "RBP_INS_USER")
	private Long insertUser;
	
	@Column(name = "RBP_INS_DATE")
	private LocalDate insertDate;
	
	@Column(name = "RBP_EXT_STATUS")
	private String extractionStatus;
	
	@Column(name = "RBP_FILE_DATE")
	private String fileDate;

	@Column(name = "RBP_ERROR_DESC")
	private String errorDescription;
	
	@Column(name = "RBP_EXT_PROCEDURE_STATUS")
	private String extractionProcedureStatus;
	
	@Column(name = "RBP_SETL_PROCEDURE_STATUS")
	private String settleProcedureStatus;
	
	@Column(name = "RBP_DATA_COUNT")
	private String dataCount;
	
	@Column(name = "RBP_RECON_CNT1")
	private String reconDataCount;
	
	@Column(name = "RBP_RECON_STATUS")
	private String reconStatus;
	
	@Column(name = "SEGRETION_STATUS")
	private String segretionStatus;
		
	@Column(name = "REPORT_STATUS")
	private String reportStatus;

	
}
