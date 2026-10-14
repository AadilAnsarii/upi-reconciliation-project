package com.jpb.reconciliation.reconciliation.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class TTUMExceptionReportDto {
	
	 private String remarks;
	    private String tranSeqNum;
	    private Date tranDate;
	    private String tranTime;
	    private Double tranAmount;
	    private String aging;
	    private String drAccNo;
	    private String crAccNo;
	    private String valueDate;
	    private String tranId;
	    private String senderRefNum;
	    private String tranRespCode;
}
