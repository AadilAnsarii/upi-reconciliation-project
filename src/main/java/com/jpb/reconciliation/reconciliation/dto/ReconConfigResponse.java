package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO returned after create / update / view of the full
 * 6-step recon configuration wizard.
 * Also drives the Step-6 "Review" screen JSON (CONFIG JSON block).
 */
@Data
@Builder
public class ReconConfigResponse {

    // ─────────────────────────────────────────────
    // STEP 1 – What to Reconcile
    // ─────────────────────────────────────────────
    private Long processId;
    private String reconName;
    private String reconCode;
    private String reconType;
    private String channel;
    private String frequency;
    private String dateLogic;
    private String matchingType;
    private String description;
    private Long instCode;
    private Long insUser;
    private LocalDateTime insDate;
    private Long lupdUser;
    private LocalDateTime lupdDate;
    private Long processMastId;

    // ─────────────────────────────────────────────
    // STEP 2 – Pick Source Files
    // ─────────────────────────────────────────────
    private List<SourceFileInfo> sourceFiles;

    @Data
    @Builder
    public static class SourceFileInfo {
        private Integer sourceNumber;        // 1=A, 2=B, 3=C, 4=D
        private Long fileTypeId;
        private String fileTypeName;
        private String shortName;
        private Long templateId;
        private String templateName;
        private String stagingTableName;     // RRM_DATA_TBL_NAME
        private String dataTableName;        // RPM_DATA_TAB_NAME*
        private String recFlagName;          // RPM_REC_FLAG_NAME*
    }

    // ─────────────────────────────────────────────
    // STEP 3 – How to Match
    // ─────────────────────────────────────────────

    /** Total matching rules count – shown in Review summary card */
    private Integer matchingRulesCount;
    private List<MatchingRuleInfo> matchingRules;

    @Data
    @Builder
    public static class MatchingRuleInfo {
        private Long ruleId;
        private Integer priority;
        private String sourceAField;
        private String matchType;
        private String sourceBField;
        private Integer tolerance;
        private Boolean required;
        /** Computed SQL fragment stored as RRM_QUERY */
        private String generatedQuery;
    }

    // ─────────────────────────────────────────────
    // STEP 4 – Filter Data
    // ─────────────────────────────────────────────

    /** Total filters count – shown in Review summary card */
    private Integer filtersCount;
    private String filterLogic;
    private List<FilterConditionInfo> filterConditions;

    @Data
    @Builder
    public static class FilterConditionInfo {
        private String applyTo;
        private String column;
        private String operator;
        private String value;
        /** Computed WHERE clause fragment stored as RRM_WHERE_CLAUSE */
        private String generatedWhereClause;
    }

    // ─────────────────────────────────────────────
    // STEP 5 – Schedule & Run
    // ─────────────────────────────────────────────
    private String scheduleTime;
    private Boolean autoRun;
    private String emailSmsFlag;
    private String manrecFlag;
    private Long retentionPeriod;
    private Long retentionVolume;

    // ─────────────────────────────────────────────
    // STEP 6 – Review (derived fields)
    // ─────────────────────────────────────────────

    /** Matching flow description, e.g. "Source A ↔ Source B via TRAN_SEQ_NUM" */
    private String matchingFlow;

    /**
     * Full config as a JSON-friendly summary string.
     * Front-end renders this in the CONFIG JSON dark block.
     */
    private Object configJson;
}