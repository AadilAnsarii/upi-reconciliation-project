
package com.jpb.reconciliation.reconciliation.dto.v2;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconTemplateConfigRequest {

    // ── Dual-mode routing — configure (create) vs update-via-configure ──────────

    /**
     * Drives single-API dual-mode behaviour on POST /template-configure:
     *   null     → CREATE  — a brand-new template is configured.
     *   non-null → UPDATE  — the identified template is updated in-place.
     *
     * When null the field is simply omitted from the JSON payload.
     * The dedicated PUT /update-template/{id} endpoint remains available
     * for direct update flows (e.g. future frontend update screen).
     */
    private Long templateId;

    // ── Template header ───────────────────────────────────────────────────────

    /** Unique template name */
    private String templateName;

    /** CSV | XML | EXCEL | FIXED_WIDTH */
    private String templateType;

    /** Number of data columns in the file */
    private Integer columnCount;

    /** Y / N — whether reversals are present in the file */
    private String reversalIndicator;

    /** Y / N — data reference flag (legacy) */
    private String dataReference;

    /** Y / N — online refund flag (legacy) */
    private String onlineRefund;

    /** CSV delimiter  e.g. "," */
    private String delimiter;

    /** CSV text qualifier  e.g. "\"" */
    private String textQualifier;

    /** File encoding  e.g. "UTF-8" */
    private String fileEncoding;

    /** Date format pattern  e.g. "dd-MM-yyyy" */
    private String dateFormat;

    /** Amount format pattern  e.g. "##.##" */
    private String amountFormat;

    /** Source system code  e.g. "CBS", "NPCI" — maps to recon_source_system_mast */
    private String sourceSystem;

    /** Y / N — duplicate check flag */
    private String dupCheckFlag;

    /** NONE | MARK_REVERSAL | EXCLUDE */
    private String reversalHandling;

    /** Fixed Width: record length */
    private Integer recordLength;

    /** Fixed Width: padding character */
    private String paddingChar;

    /** XML root tag */
    private String xmlRootTag;

    /** XML row tag */
    private String xmlRowTag;

    /** XML namespace */
    private String xmlNamespace;

    private Long tenantId;
    private String createdBy;
    private String updatedBy;

    private String filePath;
    private String description;

    // ── New fields — aligned to HTML screen ──────────────────────────────────

    /** Y / N — whether file has a header row (Header Row Present) */
    private String hasHeader;

    /** Y / N — whether file has a trailer/footer row */
    private String hasTrailer;

    /** Number of header rows to skip at top of file */
    private Integer headerLineCount;

    /** Number of footer/trailer rows to skip at bottom of file */
    private Integer trailerLineCount;

    /** File delivery frequency — Daily / Weekly / Monthly / On-demand etc. */
    private String frequencyType;

    /** File naming pattern — e.g. TXN_{YYYYMMDD}_*.csv */
    private String fileNamePattern;

    /** Reversal field name — conditional, used when reversalIndicator = Y */
    private String reversalFieldName;

    /** Reversal field value — e.g. REV, CR — used when reversalIndicator = Y */
    private String reversalValue;

    /** Reversal Amount Handling — negate (flip sign) / asis (keep as-is) — HTML Section 1 revCond */
    private String reversalAmtHandling;

    // ── Sir's new task fields ─────────────────────────────────────────────────

    /** SAVE (status=ACTIVE) / DRAFT (status=DRAFT) */
    private String action;

    /** SFTP (auto-pickup) / MANUAL (user upload) */
    private String deliverMode;

    // ── Field definitions ────────────────────────────────────────────────────

    /** List of field configurations — must not be null or empty */
    private List<ReconFieldConfigurationDto> fieldDetails;

    private SftpServerRequestDTO sftpServerDetails;

    private ScheduleConfigRequest schedulerConfig;
}
