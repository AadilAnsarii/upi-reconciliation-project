package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "PROCESS_MANAGER")
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class REProcessManager {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
	@SequenceGenerator(name = "sequence", sequenceName = "seq_ReconProcessManager", allocationSize = 1)
	private Long sequenceId;

	@Column(name = "PROCESS_ID")
	private Long processId;
	
	@Column(name = "EXTRACTION_STATUS")
	private String extractionStatus;
	
	@Column(name = "RECONCILIATION_STATUS")
	private String reconciliationStatus;
	
	@Column(name = "PROCESS_NAME")
	private String processName;
	
	@Column(name = "PROCESS_TYPE")
	private String processType;
	
	@Column(name = "SEGRETION_STATUS")
	private String segretionStatus;
	
	@Column(name = "USER_NAME")
	private String userName;
	
	@Column(name = "REPORT_STATUS")
	private String reportStatus;

	@Column(name = "START_TIME")
	private String startTime;

	@Column(name = "END_TIME")
	private String endTime;

	@Column(name = "STATUS")
	private String status;
	
	@Column(name = "DATA_COUNT")
	private String dataCount;
	
	@Column(name = "FILE_NAME")
	private String fileName;

	@CreatedDate
	@Column(updatable = false, name = "CREATED_AT")
	private LocalDateTime createdAt;

	@CreatedBy
	@Column(updatable = false, name = "CREATED_BY")
	private String createdBy;

	@LastModifiedDate
	@Column(insertable = false, name = "UPDATED_AT")
	private LocalDateTime updatedAt;

	@LastModifiedBy
	@Column(insertable = false, name = "UPDATED_BY")
	private String updatedBy;
}
