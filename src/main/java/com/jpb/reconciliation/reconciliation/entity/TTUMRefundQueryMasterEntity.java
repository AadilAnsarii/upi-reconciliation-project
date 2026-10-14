package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Table(name = "TTUM_REFUND_QUERY_MAST")
@Entity
public class TTUMRefundQueryMasterEntity {
	
	@Id
	@Column(name = "ACTION_ID")
	private String actionId;

	@Column(name = "PROCESS_ID")
	private Long processId;
    
	@Column(name = "TRQ_TTUM_DESC")
	private String ttumDescription;

	@Column(name = "TRQ_REFUND_QUERY", length = 4000)
	private String refundQuery;

	@Column(name = "TRQ_UPDATE_QUERY", length = 1000)
	private String updateQuery;

	@Column(name = "TRQ_FILE_NAME")
	private String fileName;

	@Column(name = "TRQ_IS_CSV")
	private String isCsv;

	@Column(name = "MODULE_NAME")
	private String moduleName;

	@Column(name = "IS_MASTER_TEMPLATE")
	private String isMasterTemplate;

	@Column(name = "FILE_TYPE")
	private String fileType;
	
}
