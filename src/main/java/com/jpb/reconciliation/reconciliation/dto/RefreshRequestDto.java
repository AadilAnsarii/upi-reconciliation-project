package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import lombok.Data;

@Data
public class RefreshRequestDto {
	
	List<ProcessManager> data;
	
	@Data
	public static class ProcessManager{
		private Long sequenceId;
		private Long processId;
	}

}
