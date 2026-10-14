package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class FileDataUpdateReportDto {
	 private String tranSeqNum;
	    private String flag;
	    private String shtdt; 
	    private Long adjAmt;
	    private String shser; 
	    private String shcrd; 
	    private String fileName;
	    private Integer reason; 
	    private String urn;
}
