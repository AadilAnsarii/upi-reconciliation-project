package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="SP_LOAD_MASTER")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoadMasterEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="LOAD_MASTER_ID")
	private Long  loadMasterId;
	
	@Column(name="RLM_SRC_TAB_NAME")
	private String  rlmSrcTablName;
	
	@Column(name="RLM_TRG_TAB_NAME")
	private String  rlmTrgTabName;
	
	@Column(name="RLM_SRC_TAB_FIELDS")
	private String  rlmSrcTabFields;
	
	@Column(name="RLM_TRG_TAB_FIELDS")
	private String  rlmTrgTabFields;
	
	@Column(name="RLM_WHERE_CLAUSE")
	private String  rlmWhereClause;
	
	@Column(name="RLM_MODULE_NAME")
	private String  rlmModuleName;
	
	@Column(name="RLM_LAST_PROCESS_DATE")
	private Date  rlmLastProcessDate;
	
	@Column(name="DATETIME_SEPARATOR")
	private String  dateTimeSeparator;
	
	@Column(name="RLM_CBS_FLAG")
	private boolean  rlmCbsFlag;
	
	@Column(name="RLM_ORG_RETURN_TRAN_TYPE")
	private String  rlmOrgReturnTranType;
	
	@Column(name="RLM_FILE_ID")
	private Long  rlmFileId;
	
	@Column(name="RLM_CBS_VALIDATION_FLAG")
	private String  rlmCBSValidationFlag;
	
	@Column(name="RLM_PRE_LAST_PROCESS_DATE")
	private Date  rlmPreLastProcessDate;
	
	@Column(name="RLM_STATUS")
	private String  rlmStatus;
	
	@Column(name="ROLLBACK_PROCESS_DATE")
	private Date  rollbackProcessDate;
	
	@Column(name="SEQUENCE_NO")
	private int  sequensceNo;
	
	
	

}
