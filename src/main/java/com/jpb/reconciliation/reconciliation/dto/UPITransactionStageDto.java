package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;

import lombok.Data;

@Data
public class UPITransactionStageDto {
	private String transactionId;
	private Date tranDate;
	private String payerAccountNumber;
	private String payeeAccountNumber;
	private Double netAmount;
	private Date txnExtractionDate;
	private String payeeMerchantId;
	private String referenceNumber;
	private String postingBatchId;
	private String status;
	private Date postingDate;
	private Double totalAmount;
	private String batchPostingStatus;
	private String idempotentKey;
}
