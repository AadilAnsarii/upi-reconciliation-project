package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "RCN_MANREC_ACTION_DEF_MAST")
public class ManRecActionDefMaster {

	@Id
	@Column(name = "RMT_ACTION_ID")
	private Long manRecActionId;

	@Column(name = "RMT_DEBIT_ACCT")
	private String manRecDebitAccount;

	@Column(name = "RMT_CREDIT_ACCT")
	private String manRecCreditAccount;

	@Column(name = "RMT_NARRATION")
	private String narration;

	@Column(name = "RMT_REMARKS")
	private String remarks;

	@Column(name = "RMT_RULE_ID")
	private Long ruleId;

	@Column(name = "RMT_INST_CODE")
	private Long insertCode;

	@Column(name = "RMT_INS_USER")
	private Long insertUser;

	@Column(name = "RMT_LUPD_USER")
	private Long lastUpdatedUser;

	@Column(name = "RMT_INS_DATE")
	private Date insertDate;

	@Column(name = "RMT_LUPD_DATE")
	private Date lastUpdatedDate;

	@Column(name = "RMT_ACT_DATA_TBL")
	private String actionDataTable;

	@Column(name = "RMT_NARRATION_CNST")
	private String narrationCnst;

	@Column(name = "RMT_DC_IND")
	private String dcInd;

}
