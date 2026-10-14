package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProcessMasterDto {
	
	private Long processMastId;
	private String processType;
	private String longName;
	private String shrtName;
	private List<ProcessFileDetailsMasterDto> fileList;
	private List<ReconProcessMasterDto> processList;
}
