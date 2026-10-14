package com.jpb.reconciliation.reconciliation.dto.aepsreport;

import lombok.Data;

@Data
public class AepsCbsReportDto {
	private String transactionReferenceNo;
	private String accountNumber;
	private String transactionDate;
	private String transactionPostDate;
	private String transactionTime;
	private String narration;
	private String remarks;
	private String debit;
	private String credit;
	private String transactionSequenceNo;
	private String checker;
}
