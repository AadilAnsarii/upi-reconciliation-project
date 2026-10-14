package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Table(name = "RCN_TTUM_CONFIG_MAST")
@Data
@Entity
public class TTUMConfigMasterEntity {

	@Id
	@Column(name = "RTC_TTUM_CONFIG_ID")
	private Long ttumConfigId;

	@Column(name = "RTC_TTUM_DESCRIPTION")
	private String ttumDescription;

	@Column(name = "RTC_TTUM_ENTITY_ID")
	private Long ttumEntityId;

	@Column(name = "RTC_TTUM_PROCESS_ID")
	private Long ttumProcessId;

	@Column(name = "RTC_TTUM_TYPE_DESCRIPTION")
	private String ttumTypeDescription;

	@Column(name = "RTC_TTUM_TYPE")
	private String ttumType;

	@Column(name = "RTC_JRXML_ID")
	private Long jrxmlId;

	@Column(name = "RTC_OUTPUT_FORMAT")
	private String outputFormat;
     
	@Column(name = "RTC_INST_CODE")
	private Long insertCode;
    
	@Column(name = "RTC_TTUM_CAT_TYPE")
	private String ttumCatType;
    
	@Column(name = "RTC_SETTLE_FILE_ID")
	private String settleFileId;
    
	@Column(name = "RTC_IS_CBS_TTUM")
	private String isCBSTTUM;

}
