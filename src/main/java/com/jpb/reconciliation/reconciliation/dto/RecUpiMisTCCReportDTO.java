package com.jpb.reconciliation.reconciliation.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class RecUpiMisTCCReportDTO {
	private String bankAdjRef;
	private String flag;
	private String shtDat;
	private BigDecimal adjAmt;
	private String shSer;
	private String utxId;
	private String reason;
	private String specifyOther;
}
