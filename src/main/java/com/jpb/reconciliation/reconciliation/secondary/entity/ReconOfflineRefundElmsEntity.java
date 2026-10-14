package com.jpb.reconciliation.reconciliation.secondary.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "RCN_OFFLINE_REFUND_ELMS")
@Data
public class ReconOfflineRefundElmsEntity {
	
	@Id
	@Column(name = "TXN_REF_ID")
	private Long txnRefId;
	
	@Column(name = "ACCOUNT_NUMBER")
	private String accountNumber;
	
	@Column(name = "DEBIT_CREDIT_FLAG")
	private String debitCreditFlag;
	
	@Column(name = "NARRATION")
	private String narration;
	
	@Column(name = "INSERT_CODE")
	private String insertCode;
	
	@Column(name = "AMOUNT")
	private String amount;
	
	
}
