package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

@Data
@Entity
@Table(name = "RCN_RULE_MAST")
@ToString
public class RcnRuleMast {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rcnRuleMastSeq")
    @SequenceGenerator(name = "rcnRuleMastSeq", sequenceName = "RCN_RULE_MAST_SEQ", allocationSize = 1)
    @Column(name = "RRM_RULE_ID")
    private Long rrmRuleId;

    /**
     * Template ID — links to a filter/template record.
     * Can be null when the row represents a cross-table matching rule.
     */
    @Column(name = "RRM_TMPLT_ID")
    private Long rrmTmpltId;

    /**
     * Foreign key to RCN_PROCESS_DEF_MAST.RPM_PROCESS_ID.
     * Kept as a plain Long (not a @ManyToOne) to avoid eager-load issues;
     * use a join-fetch query in the repository when the full process is needed.
     */
    @Column(name = "RRM_PROCESS_ID")
    private Long rrmProcessId;

    /**
     * Data table name this rule applies to (e.g. "REC_UPI_MIS_UBEN_DATA").
     * Max 30 chars per DDL.
     */
    @Column(name = "RRM_DATA_TBL_NAME", length = 30)
    private String rrmDataTblName;

    /**
     * Data table type code (e.g. "3" = third data source).
     * VARCHAR2(10) in DDL.
     */
    @Column(name = "RRM_DATA_TBL_TYP", length = 10)
    private String rrmDataTblTyp;

    /**
     * Rule execution priority (e.g. "1", "2", "3").
     * VARCHAR2(2) in DDL.
     */
    @Column(name = "RRM_PRIORITY", length = 2)
    private String rrmPriority;

    /**
     * Rule type code — "2" appears consistently in the matching rules.
     * VARCHAR2(2) in DDL.
     */
    @Column(name = "RRM_RULE_TYPE", length = 2)
    private String rrmRuleType;

    /**
     * SQL WHERE fragment used during matching
     * (e.g. "WHERE A.TRAN_SEQ_NUM=B.TRAN_SEQ_NUM AND A.TRAN_AMOUNT=B.TRAN_AMOUNT").
     * VARCHAR2(4000) in DDL.
     */
    @Column(name = "RRM_QUERY", length = 4000)
    private String rrmQuery;

    /**
     * Rule status flag (1 = active).
     */
    @Column(name = "RRM_RULE_STAT")
    private Integer rrmRuleStat;

    /**
     * Pipe-separated rank/matching field info for the first data source.
     * e.g. "TRAN_SEQ_NUM,TRAN_AMOUNT|TRAN_SEQ_NUM,TRAN_AMOUNT"
     * VARCHAR2(4000) in DDL.
     */
    @Column(name = "RRM_RANK_INFO1", length = 4000)
    private String rrmRankInfo1;

    /**
     * Pipe-separated rank/matching field info for the second data source.
     * VARCHAR2(300) in DDL.
     */
    @Column(name = "RRM_RANK_INFO2", length = 300)
    private String rrmRankInfo2;

    /**
     * Secondary match flag — defaults to 'N'.
     * CHAR(1) in DDL.
     */
    @Column(name = "RRM_SEC_MAT_FLG", length = 1, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private String rrmSecMatFlg = CommonConstants.INACTIVE_N;

    /**
     * Comma-separated table names involved in this matching rule.
     * e.g. "REC_UPI_MIS_UBEN_DATA,REC_UPHST_UPI_UBEN_DATA"
     * VARCHAR2(100) in DDL.
     */
    @Column(name = "RRM_TABLE_NAME", length = 100)
    private String rrmTableName;

    /**
     * Comma-separated dynamic reconciliation flag column names.
     * e.g. "DYN_UPI_MIS_REC_FLAG,DYN_UPHST_UPI_REC_FLAG"
     * VARCHAR2(100) in DDL.
     */
    @Column(name = "RRM_DYN_FLAG", length = 100)
    private String rrmDynFlag;

    /**
     * Comma-separated dynamic ID column names for each data source.
     * e.g. "DYN_UPI_MIS_ID_COL,DYN_UPHST_UPI_ID_COL"
     * VARCHAR2(100) in DDL.
     */
    @Column(name = "RRM_ID_COLS", length = 100)
    private String rrmIdCols;

    /**
     * Full WHERE clause used as a pre-filter / load condition for a data table.
     * Two distinct usages observed in the data:
     *   1. Matching rules  → stores the join condition (same as RRM_QUERY but as CLOB)
     *   2. Template/filter → stores a standalone filter e.g.
     *      "WHERE NVL(TXN_STATUS,'NA')='SUCCESS'"
     * Stored as Oracle SECUREFILE CLOB; mapped via Hibernate TextType so it behaves
     * like a regular String and avoids deprecated Clob/Blob handling.
     */
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "RRM_WHERE_CLAUSE")
    private String rrmWhereClause;
}