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

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity → RCN_MANREC_PROCESS_DEF_MAST
 */
@Entity
@Table(name = "RCN_MANREC_PROCESS_DEF_MAST")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessDef {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proc_def_seq")
    @SequenceGenerator(name = "proc_def_seq", sequenceName = "RCN_PROC_DEF_SEQ", allocationSize = 1)
    @Column(name = "RMP_ACTION_ID")
    private Long rmpActionId;

    @Column(name = "RMP_PROCESS_ID", nullable = false)
    private Long rmpProcessId;

    @Column(name = "RMP_TEMP1",  length = 1)  private String rmpTemp1;
    @Column(name = "RMP_TEMP2",  length = 1)  private String rmpTemp2;
    @Column(name = "RMP_TEMP3",  length = 1)  private String rmpTemp3;
    @Column(name = "RMP_TEMP4",  length = 1)  private String rmpTemp4;

    @Column(name = "RMP_ACTION_CATG_ID")       private Long    rmpActionCatgId;
    @Column(name = "RMP_INST_CODE")            private Integer rmpInstCode;
    @Column(name = "RMP_INS_USER")             private Integer rmpInsUser;
    @Column(name = "RMP_LUPD_USER")            private Integer rmpLupdUser;
    @Column(name = "RMP_INS_DATE")             private LocalDateTime rmpInsDate;
    @Column(name = "RMP_LUPD_DATE")            private LocalDateTime rmpLupdDate;

    @Column(name = "RMP_MANREC_DESCRIPTION", length = 4000) private String rmpManrecDescription;
    @Column(name = "RMP_ORDER_OF_EXECUTION",  length = 20)  private String rmpOrderOfExecution;
    @Column(name = "RMP_MAPPING_LEVEL",        length = 20)  private String rmpMappingLevel;
    @Column(name = "RMP_TRANSACTION_DAY",      length = 20)  private String rmpTransactionDay;

    @Column(name = "RMP_TEMP_ID1", length = 20) private String rmpTempId1;
    @Column(name = "RMP_TEMP_ID2", length = 20) private String rmpTempId2;
    @Column(name = "RMP_TEMP_ID3", length = 20) private String rmpTempId3;
    @Column(name = "RMP_TEMP_ID4", length = 20) private String rmpTempId4;

    @Column(name = "RPM_FIELD1", length = 100)  private String rpmField1;
    @Column(name = "RPM_FIELD2", length = 100)  private String rpmField2;
    @Column(name = "RPM_FIELD3", length = 100)  private String rpmField3;
    @Column(name = "RPM_FIELD4", length = 100)  private String rpmField4;

    @Column(name = "RPM_EJ_ERRCHK_FLG", length = 1) private String rpmEjErrchkFlg;
    @Column(name = "RPM_APPR_REQ_FLG",  length = 1) private String rpmApprReqFlg;

    @Column(name = "RMP_DIFF_FLAG",           length = 1)  private String rmpDiffFlag;
    @Column(name = "RMP_DIFF_AMT_EXPR",       length = 50) private String rmpDiffAmtExpr;
    @Column(name = "RMP_ACTION_CONFIG_STATUS", length = 1) private String rmpActionConfigStatus;

    @PrePersist
    public void prePersist() {
        if (rmpInsDate == null)            rmpInsDate            = LocalDateTime.now();
        if (rmpLupdDate == null)           rmpLupdDate           = LocalDateTime.now();
        if (rmpActionConfigStatus == null) rmpActionConfigStatus = CommonConstants.ACTIVE_Y;
        if (rmpDiffFlag == null)           rmpDiffFlag           = CommonConstants.INACTIVE_N;
    }

    @PreUpdate
    public void preUpdate() { rmpLupdDate = LocalDateTime.now(); }
}
