package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

import lombok.Data;

/**
 * Master request DTO that captures all 6 wizard steps:
 *  Step 1 – What to Reconcile
 *  Step 2 – Pick Source Files
 *  Step 3 – How to Match (Matching Rules)
 *  Step 4 – Filter Data
 *  Step 5 – Schedule & Run
 *  Step 6 – Review (read-only, no input)
 */
@Data
public class ReconConfigRequest {

    // ─────────────────────────────────────────────
    // STEP 1 – What to Reconcile
    // ─────────────────────────────────────────────

    /** Human-readable recon name, e.g. "AEPS Daily Recon" */
    @NotBlank(message = "Recon name is required")
    private String reconName;

    /** Auto-generated recon code, e.g. "RCN-00042" */
    private String reconCode;

    /** Recon type, e.g. "INWARD", "OUTWARD" */
    @NotBlank(message = "Recon type is required")
    private String reconType;

    /** Channel, e.g. "UPI", "AEPS", "ATM" */
    @NotBlank(message = "Channel is required")
    private String channel;

    /** Frequency: Daily / Weekly / Monthly */
    private String frequency;

    /** Date logic: Same Day (T+0), T+1, etc. */
    private String dateLogic;

    /** Matching type: One-to-One / One-to-Many / Many-to-Many */
    private String matchingType;

    /** Optional description / notes */
    private String description;

    /** Institution code */
    private Long instCode;

    /** Inserted-by user ID */
    private Long insUser;

    /** Process master ID (parent) */
    private Long processMastId;

    // ─────────────────────────────────────────────
    // STEP 2 – Pick Source Files
    // ─────────────────────────────────────────────

    /**
     * Source file definitions.
     * The UI shows Source A and Source B; can be extended to 4 sources.
     * Minimum 2 sources required.
     */
    @NotNull(message = "Source files are required")
    @Size(min = 2, max = 4, message = "At least 2 and at most 4 source files must be provided")
    @Valid
    private List<SourceFileRequest> sourceFiles;

    @Data
    public static class SourceFileRequest {
        /** 1 = Source A, 2 = Source B, 3 = Source C, 4 = Source D */
        @NotNull(message = "Source number is required")
        private Integer sourceNumber;

        /** FK to RCN_FILE_DETAILS_MAST */
        @NotNull(message = "File type ID is required")
        private Long fileTypeId;

        /** FK to RCN_TEMPLATE_DETAILS */
        @NotNull(message = "Template ID is required")
        private Long templateId;
    }

    // ─────────────────────────────────────────────
    // STEP 3 – How to Match
    // ─────────────────────────────────────────────

    /**
     * Matching rules between Source A and Source B fields.
     * At least one rule is required.
     */
    @NotNull(message = "At least one matching rule is required")
    @Size(min = 1, message = "At least one matching rule is required")
    @Valid
    private List<MatchingRuleRequest> matchingRules;

    @Data
    public static class MatchingRuleRequest {
        /** Rule sequence / priority – drives RRM_PRIORITY */
        @NotNull(message = "Rule priority is required")
        private Integer priority;

        /** Column name from Source A template */
        @NotBlank(message = "Source A field is required")
        private String sourceAField;

        /** EXACT / FUZZY / RANGE */
        @NotBlank(message = "Match type is required")
        private String matchType;

        /** Column name from Source B template */
        @NotBlank(message = "Source B field is required")
        private String sourceBField;

        /** Tolerance value (0 = exact) */
        private Integer tolerance = 0;

        /** Whether this rule is mandatory for a match */
        private Boolean required = true;
    }

    // ─────────────────────────────────────────────
    // STEP 4 – Filter Data
    // ─────────────────────────────────────────────

    /**
     * Pre-filter conditions applied before matching.
     * Maps to RRM_WHERE_CLAUSE in RCN_RULE_MAST template rows.
     * Optional – if empty, all records are processed.
     */
    @Valid
    private List<FilterConditionRequest> filterConditions;

    /** AND / OR – how multiple conditions are combined */
    private String filterLogic = "AND";

    @Data
    public static class FilterConditionRequest {
        /**
         * Which source the filter applies to:
         * "A" = Source A only, "B" = Source B only, "BOTH" = both
         */
        private String applyTo = "BOTH";

        /** Column name to filter on */
        @NotBlank(message = "Filter column is required")
        private String column;

        /** Operator: =, !=, >, <, IN, LIKE, IS NULL, IS NOT NULL */
        @NotBlank(message = "Filter operator is required")
        private String operator;

        /** Value(s) for the condition. Multiple values separated by comma for IN */
        private String value;
    }

    // ─────────────────────────────────────────────
    // STEP 5 – Schedule & Run
    // ─────────────────────────────────────────────

    /** Cron expression or time string, e.g. "0 0 2 * * ?" */
    private String scheduleTime;

    /** Whether to auto-run on schedule */
    private Boolean autoRun = false;

    /** Email/SMS notification flag: Y / N */
    private String emailSmsFlag = CommonConstants.INACTIVE_N;

    /** Manual reconciliation allowed: Y / N */
    private String manrecFlag = CommonConstants.INACTIVE_N;

    /** Retention period in days */
    private Long retentionPeriod;

    /** Retention volume (max records) */
    private Long retentionVolume;
}