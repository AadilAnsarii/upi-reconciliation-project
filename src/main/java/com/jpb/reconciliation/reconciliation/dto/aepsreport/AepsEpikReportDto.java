package com.jpb.reconciliation.reconciliation.dto.aepsreport;

import lombok.Data;

@Data
public class AepsEpikReportDto {
	private String agentId;
	private String requestReferenceNo;
	private String transactionId;
	private Double amount;
	private String transactionType;
	private String sourceBank;
	private String status;
	private String narration;
	private Double balance;
	private String createdTs;

}
