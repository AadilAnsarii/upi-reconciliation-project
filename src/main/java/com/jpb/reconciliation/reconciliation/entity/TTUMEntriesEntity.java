package com.jpb.reconciliation.reconciliation.entity;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "rcn_ttum_entries")
@Data
@ToString
public class TTUMEntriesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TTUM")
	@SequenceGenerator(name = "SEQ_TTUM", sequenceName = "SEQ_TTUM",allocationSize = 1)
    @Column(name = "RTE_TTUM_ID")
    private Long rteTtumId;

    @Column(name = "RTE_SEQ_NUM")
    private String rteSeqNum;

    @Column(name = "RTE_ACTION_ID")
    private String rteActionId;

    @Column(name = "RTE_DEBIT")
    private String rteDebit;

    @Column(name = "RTE_CREDIT")
    private String rteCredit;

    @Column(name = "RTE_AMOUNT")
    private Long rteAmount;

    @Column(name = "RTE_CURR_CODE")
    private Long rteCurrCode;

    @Column(name = "RTE_NARRATION")
    private String rteNarration;

    @Column(name = "RTE_PREMANREC_FLG")
    private String rtePremanrecFlg;

    @Column(name = "RTE_APPROVAL_FLG")
    private String rteApprovalFlg;

    @Column(name = "RTE_INS_DATE")
    private Date rteInsDate;

    @Column(name = "RTE_PROCESS_ID")
    private Long rteProcessId;

    @Column(name = "RTE_BRANCH_CODE")
    private String rteBranchCode;

    @Column(name = "RTE_ACK")
    private String rteAck;

    @Column(name = "RTE_TTUM_PROCESS_STATUS")
    private String rteTtumProcessStatus;

    @Column(name = "RTE_PROCESS_DATE")
    private Date rteProcessDate;

    @Column(name = "RTE_PROCESS_FILE_NAME")
    private String rteProcessFileName;

    @Column(name = "RTE_TTUM_SRC_TYP")
    private String rteTtumSrcTyp;

    @Column(name = "RTE_HASH_CARD_NUM")
    private String rteHashCardNum;

    @Column(name = "RTE_TRAN_DATE")
    private Date rteTranDate;

    @Column(name = "RTE_TERM_ID")
    private String rteTermId;

    @Column(name = "RTE_CARD_NUM")
    private String rteCardNum;

    @Column(name = "RTE_FILE_ID")
    private Long rteFileId;

    @Column(name = "RTE_DOWNLOAD_TIME")
    private String rteDownloadTime;

    @Column(name = "RTE_DEBIT_NARR")
    private String rteDebitNarr;

    @Column(name = "RTE_CREDIT_NARR")
    private String rteCreditNarr;

    @Column(name = "RTE_TRAN_ACCT_TYPE")
    private String rteTranAcctType;

    @Column(name = "RTE_DEBIT_TYPE")
    private String rteDebitType;

    @Column(name = "RTE_FILE_SEQ_NUM")
    private Long rteFileSeqNum;

    @Column(name = "RTE_FILE_DATE")
    private String rteFileDate;

    @Column(name = "RTE_ICHG_DATE")
    private Date rteIchgDate;

    @Column(name = "RTE_STATUS")
    private String rteStatus;

    @Column(name = "RTE_FILE_IND")
    private String rteFileInd;

    @Column(name = "RTE_COMP_ID")
    private String rteCompId;

    @Column(name = "RTE_AEPS_ACCT_NUM")
    private String rteAepsAcctNum;

}
