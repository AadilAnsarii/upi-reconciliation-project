package com.jpb.reconciliation.reconciliation.entity;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Table(name = "upi_p2m_txn_stg_tbl")
@Data
@Entity
public class UPITransactionStageEntity {
	
    @Id
    @Column(name = "TRANSACTION_ID", nullable = false)
    private String transactionId;
    
    @Column(name = "ORIGINAL_TRANSACTION_ID", nullable = false)
    private String originalTransactionId;

    @Column(name = "CREATED")
    private Timestamp created;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "STATEMENT_DESCRIPTOR")
    private String statementDescriptor;

    @Column(name = "APPLICATION_ID")
    private Long applicationId;

    @Column(name = "PAYER_ACCOUNT_NUMBER")
    private String payerAccountNumber;

    @Column(name = "PAYER_BANK_IFSC")
    private String payerBankIfsc;

    @Column(name = "PAYER_MOBILE_NUMBER")
    private String payerMobileNumber;

    @Column(name = "PAYEE_ACCOUNT_NUMBER")
    private String payeeAccountNumber;

    @Column(name = "PAYEE_BANK_IFSC")
    private String payeeBankIfsc;

    @Column(name = "PAYEE_MMID")
    private String payeeMmid;

    @Column(name = "PAYEE_NAME")
    private String payeeName;

    @Column(name = "NET_AMOUNT")
    private Long netAmount;

    @Column(name = "BATCH_ID")
    private String batchId;

    @Column(name = "INS_DATE")
    private Date insDate;

    @Column(name = "TXN_POSTING_REF_NO")	
    private Long txnPostingRefNo;

    @Column(name = "POSTING_STATUS")
    private String postingStatus;

    @Column(name = "DUPL_FLAG")
    private String duplFlag;

    @Column(name = "BATCH_PROCESS_ID")
    private String batchProcessId;

    @Column(name = "PAYEE_MERCHANT_ID")
    private String payeeMerchantId;

    @Column(name = "POSTING_BATCH_ID")
    private String postingBatchId;

    @Column(name = "REFERENCE_NUMBER")
    private String referenceNumber;

    @Column(name = "METHOD_TYPE")
    private Long methodType;

    @Column(name = "METHOD_SUB_TYPE")
    private Long methodSubType;

    @Column(name = "IDEMPOTENT_KEY")
    private String idempotentKey;
}
