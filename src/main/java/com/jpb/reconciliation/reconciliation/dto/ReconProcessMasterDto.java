package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;

import lombok.Data;

@Data
public class ReconProcessMasterDto {

	private Long reconProcessId;
	private String reconProcessName;
	private Long reconInputCount;
	private Long reconTableType;
	private Long reconFileType1;
	private Long reconFileType2;
	private Long reconFileType3;
	private Long reconFileType4;
	private Long reconTemp1;
	private Long reconTemp2;
	private Long reconTemp3;
	private Long reconTemp4;
	private Long reconMasterTemp;
	private String reconFlagName1;
	private String reconFlagName2;
	private String reconFlagName3;
	private String reconFlagName4;
	private String reconDataTableName1;
	private String reconDataTableName2;
	private String reconDataTableName3;
	private String reconDataTableName4;
	private Long reconInsertCode;
	private Long reconInsertUser;
	private Long reconLastUpdatedUser;
	private Date reconLastUpdatedDate;
	private String reconMenuFlag;
	private String reconManRecFlag;
	private String reconDisputeFlag;
	private String reconIssAcqFlag;
	private Date reconInsertDate;
	private String reconMatchingType;
	private Long reconInterChnageId;
	private Long reconRetentionVolume;
	private Long reconRetentionPeriod;
	private String reconEmailSMSFlag;
	private String reconMatchigFlag;
	private String reconMatchingField1;
	private String reconMatchingField2;
	private String reconMatchingField3;
	private String reconMatchingField4;
	private Long reconInterChangePosition;
	private String reconProcessJPBRPSL;
}
