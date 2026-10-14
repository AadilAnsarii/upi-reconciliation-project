
package com.jpb.reconciliation.reconciliation.dto.v2;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned by viewTemplate, searchTemplate, and getTemplateById.
 * Contains the template header plus all its active field definitions.
 * Serialised to Map<String,Object> by ResponseBuilder.toMap() before being placed
 * into RestWithMapStatusList.data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReconFileTemplateMastDto {

    // ── Template header ───────────────────────────────────────────────────────

    private Long   templateId;
    private String templateCode;
    private String templateName;
    private String templateType;
    private String description;
    private String sourceSystem;
    private String stageTabName;
    private String filePath;
    private String fileEncoding;
    private String hasHeader;
    private String hasTrailer;
    private Integer headerLineCount;
    private Integer trailerLineCount;
    private String frequencyType;
    private String fileNamePattern;
    private String dateFormat;
    private String amountFormat;
    private String dupCheckFlag;
    private String reversalHandling;
    private String reversalFieldName;
    private String reversalValue;
    private String reversalAmtHandling;  // negate / asis

    // CSV
    private String delimiter;
    private String textQualifier;

    // Fixed Width
    private Integer recordLength;
    private String  paddingChar;

    // XML
    private String xmlRootTag;
    private String xmlRowTag;
    private String xmlNamespace;

    // Lifecycle
    private String  status;
    private Integer version;
    private Long    tenantId;

    // Legacy fields
    private Long   subTemplateId;
    private Long   typeId;
    private Long   columnCount;
    private String reversalIndicator;
    private String dataReference;
    private String onlineRefund;
    private String settlementFlag;
    private String productType;

    // Audit
    private String        createdBy;
    private LocalDateTime createdAt;
    private String        updatedBy;
    private LocalDateTime updatedAt;

    // ── SFTP server details (populated when deliverMode = SFTP) ──────────────
    private SftpServerResponseDTO sftpServerDetails;

    // ── Field definitions (child rows) ───────────────────────────────────────

    private List<FieldDefinitionDTO> fieldDetails;

    // ── Inner DTO for a single field definition ───────────────────────────────

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldDefinitionDTO {

        private Long    fieldId;
        private Long    templateId;
        private String  fieldName;
        private String  fieldtype;       // matches request field name — from recon_field_type_mast
        private String  fieldFormat;     // matches request field name — from recon_field_format_mast
        private Long    fieldLength;
        private Integer fieldScale;
        private Integer fieldSequence;   // matches request field name — sequence order
        private Integer positionFrom;
        private Integer positionTo;
        private String  isPrimaryKey;    // Y / N
        private String  isReconKey;      // Y / N
        private String  isMandatory;     // Y / N
        private String  trimFlag;        // Y / N
        private String  defaultValue;
        private String  validationRegex;
        private String  xmlXpath;
        private Integer excelColIndex;
    }
}
