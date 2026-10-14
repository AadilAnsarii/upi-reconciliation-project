package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "RCN_PROCESS_DEF_MAST")
@ToString(exclude = { "processmaster" })
public class ReconProcessDefMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq")
	@SequenceGenerator(name = "seq", sequenceName = "RecProcessManager", allocationSize = 1)
	@Column(name = "RPM_PROCESS_ID")
	private Long reconProcessId;

	@Column(name = "RPM_PROCESS_NAME")
	private String reconProcessName;

	@Column(name = "RPM_INPUT_COUNT")
	private Long reconInputCount;

	@Column(name = "RPM_TABLE_TYPE")
	private Long reconTableType;

	@Column(name = "RPM_FILE_TYPE1")
	private Long reconFileType1;

	@Column(name = "RPM_FILE_TYPE2")
	private Long reconFileType2;

	@Column(name = "RPM_FILE_TYPE3")
	private Long reconFileType3;

	@Column(name = "RPM_FILE_TYPE4")
	private Long reconFileType4;

	@Column(name = "RPM_TEMP1")
	private Long reconTemp1;

	@Column(name = "RPM_TEMP2")
	private Long reconTemp2;

	@Column(name = "RPM_TEMP3")
	private Long reconTemp3;

	@Column(name = "RPM_TEMP4")
	private Long reconTemp4;

	@Column(name = "RPM_MAST_TEMP")
	private Long reconMasterTemp;

	@Column(name = "RPM_REC_FLAG_NAME1")
	private String reconFlagName1;

	@Column(name = "RPM_REC_FLAG_NAME2")
	private String reconFlagName2;

	@Column(name = "RPM_REC_FLAG_NAME3")
	private String reconFlagName3;

	@Column(name = "RPM_REC_FLAG_NAME4")
	private String reconFlagName4;

	@Column(name = "RPM_DATA_TAB_NAME1")
	private String reconDataTableName1;

	@Column(name = "RPM_DATA_TAB_NAME2")
	private String reconDataTableName2;

	@Column(name = "RPM_DATA_TAB_NAME3")
	private String reconDataTableName3;

	@Column(name = "RPM_DATA_TAB_NAME4")
	private String reconDataTableName4;

	@Column(name = "RPM_INST_CODE")
	private Long reconInsertCode;

	@Column(name = "RPM_INS_USER")
	private Long reconInsertUser;

	@Column(name = "RPM_LUPD_USER")
	private Long reconLastUpdatedUser;

	@Column(name = "RPM_LUPD_DATE")
	private LocalDateTime reconLastUpdatedDate;

	@Column(name = "RPM_REC_MENU_FLAG")
	private String reconMenuFlag;

	@Column(name = "RPM_MANREC_FLAG")
	private String reconManRecFlag;

	@Column(name = "RPM_DISPUTE_FLAG")
	private String reconDisputeFlag;

	@Column(name = "RPM_ISS_ACQ_FLAG")
	private String reconIssAcqFlag;

	@Column(name = "RPM_INS_DATE")
	private LocalDateTime reconInsertDate;

	@Column(name = "RPM_MATCHING_TYPE")
	private String reconMatchingType;

	@Column(name = "RPM_INTERCHANGE_ID")
	private Long reconInterChnageId;

	@Column(name = "RPM_RETENTION_VOLUME")
	private Long reconRetentionVolume;

	@Column(name = "RPM_RETENTION_PERIOD")
	private Long reconRetentionPeriod;

	@Column(name = "RPM_EMAIL_SMS_FLAG")
	private String reconEmailSMSFlag;

	@Column(name = "RPM_MATCHING_FLAG")
	private String reconMatchigFlag;

	@Column(name = "RPM_MATCHING_FIELD1")
	private String reconMatchingField1;

	@Column(name = "RPM_MATCHING_FIELD2")
	private String reconMatchingField2;

	@Column(name = "RPM_MATCHING_FIELD3")
	private String reconMatchingField3;

	@Column(name = "RPM_MATCHING_FIELD4")
	private String reconMatchingField4;

	@Column(name = "RPM_INCHG_POSITION")
	private Long reconInterChangePosition;

	@Column(name = "RPM_PROCESS_JPBRPSL")
	private String reconProcessJPBRPSL;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROCESS_MAST_ID")
	@JsonBackReference
	private ProcessMasterEntity processmaster;
}
