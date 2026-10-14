package com.jpb.reconciliation.reconciliation.secondary.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.jpb.reconciliation.reconciliation.constants.FileProcessStatus;

import lombok.Data;

@Entity
@Table(name = "REC_FILE_PROCESS_STATUS")
@Data
public class FileProcessStatusEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_seq_pr")
	@SequenceGenerator(name = "file_seq_pr", sequenceName = "REC_FILE_PROCESS_STATUS_SEQ", allocationSize = 1)
	@Column(name = "FILE_ID")
	private Long fileId;

	@Column(name = "FILE_NAME", nullable = false)
	private String fileName;

	@Column(name = "FILE_PATH", nullable = false)
	private String filePath;

	@Column(name = "UPLOADED_BY", nullable = false)
	private String uploadedBy;

	@Column(name = "UPLOADED_DATE", nullable = false)
	private LocalDateTime uploadedDate;
    
	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false)
	private FileProcessStatus status;

	@Column(name = "CHECKER_ID")
	private String checkerId;

	@Column(name = "APPROVAL_DATE")
	private LocalDateTime approvalDate;

	@Column(name = "REJECTION_REASON")
	private String rejectionReason;

	@Column(name ="FILETYPE")
	private String fileType;
	
	@Column(name ="TOTAL_COUNT")
	private String totalCount;
	
	@Column(name ="TOTAL_AMOUNT")
	private String totalAmount;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "UPLOAD_DATA_STATUS")
	private FileProcessStatus uploadDataStatus;
}
