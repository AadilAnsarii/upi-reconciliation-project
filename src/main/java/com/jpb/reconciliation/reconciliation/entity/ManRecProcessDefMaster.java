package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "RCN_MANREC_PROCESS_DEF_MAST")
public class ManRecProcessDefMaster {

	@Id
	@Column(name = "RMP_ACTION_ID")
	private Long manRecActionId;

	@Column(name = "RMP_PROCESS_ID")
	private Long manRecProcessId;

	@Column(name = "RMP_TEMP1")
	private String manRecTemp1;

	@Column(name = "RMP_TEMP2")
	private String manRecTemp2;

	@Column(name = "RMP_TEMP3")
	private String manRecTemp3;

	@Column(name = "RMP_TEMP4")
	private String manRecTemp4;

	@Column(name = "RMP_ACTION_CATG_ID")
	private Long manRecActionCategoryId;

	@Column(name = "RMP_INST_CODE")
	private Long manRecInsertCode;

	@Column(name = "RMP_INS_USER")
	private Long manRecInsertUser;

	@Column(name = "RMP_LUPD_USER")
	private Long manRecLastUpdatedUser;

	@Column(name = "RMP_INS_DATE")
	private Date manRecInsertDate;

	@Column(name = "RMP_LUPD_DATE")
	private Date manRecLastUpdateDate;

	@Column(name = "RMP_MANREC_DESCRIPTION")
	private String manRecDescription;

	@Column(name = "RMP_ORDER_OF_EXECUTION")
	private String manRecOrderOfExecution;

	@Column(name = "RMP_MAPPING_LEVEL")
	private String manRecMappingLevel;

	@Column(name = "RMP_TRANSACTION_DAY")
	private String manRecTransactionDay;

	@Column(name = "RMP_TEMP_ID1")
	private String manRecTempId1;

	@Column(name = "RMP_TEMP_ID2")
	private String manRecTempId2;

	@Column(name = "RMP_TEMP_ID3")
	private String manRecTempId3;

	@Column(name = "RMP_TEMP_ID4")
	private String manRecTempId4;

	@Column(name = "RPM_FIELD1")
	private String manRecField1;

	@Column(name = "RPM_FIELD2")
	private String manRecField2;

	@Column(name = "RPM_FIELD3")
	private String manRecField3;

	@Column(name = "RPM_FIELD4")
	private String manRecField4;

	@Column(name = "RPM_EJ_ERRCHK_FLG")
	private String manRecEJErrorCheckFlag;

	@Column(name = "RPM_APPR_REQ_FLG")
	private String manRecApproverRequiredFlag;

	@Column(name = "RMP_DIFF_FLAG")
	private String manRecDiffFlag;

	@Column(name = "RMP_DIFF_AMT_EXPR")
	private String manRecDiffAmtExpression;

}
