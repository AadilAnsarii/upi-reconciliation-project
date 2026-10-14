package com.jpb.reconciliation.reconciliation.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class TTUMEntriesDto {

	    private Long rteTtumId;
	    private String rteSeqNum;
	    private String rteActionId;
	    private String rteDebit;
	    private String rteCredit;
	    private Long rteAmount;
	    private Long rteCurrCode;
	    private String rteNarration;
	    private String rtePremanrecFlg;
	    private String rteApprovalFlg;
	    private Date rteInsDate;
	    private Long rteProcessId;
	    private String rteBranchCode;
	    private String rteAck;
	    private String rteTtumProcessStatus;
	    private Date rteProcessDate;
	    private String rteProcessFileName;
	    private String rteTtumSrcTyp;
	    private String rteHashCardNum;
	    private Date rteTranDate;
	    private String rteTermId;
	    private String rteCardNum;
	    private Long rteFileId;
	    private String rteDownloadTime;
	    private String rteDebitNarr;
	    private String rteCreditNarr;
	    private String rteTranAcctType;
	    private String rteDebitType;
	    private Long rteFileSeqNum;
	    private String rteFileDate;
	    private Date rteIchgDate;
	    private String rteStatus;
	    private String rteFileInd;
	    private String rteCompId;
	    private String rteAepsAcctNum;
}
