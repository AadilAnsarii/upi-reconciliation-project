package com.jpb.reconciliation.reconciliation.dto;

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
public class ExtractionProcessManagerDto {

	private Long sequenceId;
	private Long processId;
	private String extractionStatus;
	private String segretionStatus;
	private String reportStatus;
	private String startTime;
	private String endTime;
	private String status;
	private String processName;
	private String processType;
	private String userName;
	private String dataCount;
	private String fileName;
}
