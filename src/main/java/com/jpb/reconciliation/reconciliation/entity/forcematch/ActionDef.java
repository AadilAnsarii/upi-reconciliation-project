package com.jpb.reconciliation.reconciliation.entity.forcematch;


import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity → RCN_MANREC_ACTION_DEF_MAST
 */
@Entity
@Table(name = "RCN_MANREC_ACTION_DEF_MAST")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ActionDef {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "action_def_seq")
    @SequenceGenerator(name = "action_def_seq", sequenceName = "RCN_ACTION_DEF_SEQ", allocationSize = 1)
    @Column(name = "RMT_ACTION_ID")
    private Long rmtActionId;

    @Column(name = "RMT_DEBIT_ACCT",  length = 20)  private String rmtDebitAcct;
    @Column(name = "RMT_CREDIT_ACCT", length = 20)  private String rmtCreditAcct;
    @Column(name = "RMT_NARRATION",   length = 4000) private String rmtNarration;
    @Column(name = "RMT_REMARKS",     length = 50)   private String rmtRemarks;
    @Column(name = "RMT_RULE_ID")                    private Long   rmtRuleId;
    @Column(name = "RMT_INST_CODE")                  private Integer rmtInstCode;
    @Column(name = "RMT_INS_USER")                   private Integer rmtInsUser;
    @Column(name = "RMT_LUPD_USER")                  private Integer rmtLupdUser;
    @Column(name = "RMT_INS_DATE")                   private LocalDateTime rmtInsDate;
    @Column(name = "RMT_LUPD_DATE")                  private LocalDateTime rmtLupdDate;

    @Column(name = "RMT_ACT_DATA_TBL",          length = 30)  private String rmtActDataTbl;
    @Column(name = "RMT_NARRATION_CNST",         length = 40)  private String rmtNarrationCnst;
    @Column(name = "RMT_DC_IND",                 length = 2)   private String rmtDcInd;
    @Column(name = "RMT_NARRATION_CNST_CREDIT",  length = 100) private String rmtNarrationCnstCredit;
    @Column(name = "RMT_DELIMITER",              length = 10)  private String rmtDelimiter;
    @Column(name = "RMT_CREDIT_NARRATION",       length = 100) private String rmtCreditNarration;
    @Column(name = "RMT_NARRATION_CNST_DEBIT",   length = 100) private String rmtNarrationCnstDebit;
    @Column(name = "RMT_DEBIT_NARRATION",        length = 100) private String rmtDebitNarration;

    @PrePersist
    public void prePersist() {
        if (rmtInsDate == null)   rmtInsDate   = LocalDateTime.now();
        if (rmtLupdDate == null)  rmtLupdDate  = LocalDateTime.now();
        if (rmtDelimiter == null) rmtDelimiter = "/";
    }

    @PreUpdate
    public void preUpdate() { rmtLupdDate = LocalDateTime.now(); }
}
