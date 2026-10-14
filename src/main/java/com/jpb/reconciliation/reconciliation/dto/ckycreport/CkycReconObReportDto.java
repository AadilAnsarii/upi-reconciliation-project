package com.jpb.reconciliation.reconciliation.dto.ckycreport;

import java.util.Date;

import lombok.Data;

@Data
public class CkycReconObReportDto {

	private String tranSeqNum;
	private Date accountOpenDate;
	private String ckycStatus;
	private String ckycNumber;
	private Date batchDate;
	private int tat;
	private String status;
	private String ckycCategory;
}
