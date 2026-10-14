package com.jpb.reconciliation.reconciliation.mapper.v2;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplatesDetailsDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconFileTemplateMastDto;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconTemplateConfigRequest;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;

/**
 * Static mapper — all four methods used by ReconTemplateDetailsServiceImpl.
 *
 *  mapToReconFileTmpltMast(dto, entity)           ← addTemplate
 *  mapTemplateDtoToFileTmpltMast(request, entity) ← saveTemplateAndFields
 *  toDTOList(entities)                            ← viewTemplate, searchTemplate
 *  toDTO(entity)                                  ← getTemplateById, toDTOList
 *
 * toFieldDTO() entity access aligned to ACTUAL ReconTmpltFieldDtls fields:
 *   getTemplateMast()     → templateId
 *   getFieldTypeMast()    → data type code
 *   getFieldFormatMast()  → format description
 *   getColPosn()          → sequenceOrder
 *   getMaxLength()        → fieldLength
 *   getShortName()        → fieldLabel
 */
public class ReconFileTemplateMastMapper {

    private ReconFileTemplateMastMapper() {}

    // =========================================================================
    // METHOD 2: mapTemplateDtoToFileTmpltMast
    // Called by: saveTemplateAndFields(TemplateFieldDto) — configure/create flow
    // =========================================================================

    public static ReconFileTmpltMast mapTemplateDtoToFileTmpltMast(ReconTemplateConfigRequest request,
                                                                    ReconFileTmpltMast entity) {
        if (null == request) return null;

        if (null != request.getTemplateName()) entity.setTemplateName(request.getTemplateName());
        if (null != request.getTemplateType()) entity.setTemplateType(request.getTemplateType());
        if (null != request.getColumnCount()) entity.setColumnCount(Long.valueOf(request.getColumnCount()));
        if (null != request.getReversalIndicator()) entity.setReversalIndicator(request.getReversalIndicator());
        if (null != request.getDataReference()) entity.setDataReferenceFlag(request.getDataReference());
        if (null != request.getOnlineRefund()) entity.setOnlRefundFlag(request.getOnlineRefund());
        if (null != request.getDelimiter()) entity.setDelimiter(request.getDelimiter());
        if (null != request.getTextQualifier()) entity.setTextQualifier(request.getTextQualifier());
        if (null != request.getFileEncoding()) entity.setFileEncoding(request.getFileEncoding());
        if (null != request.getDateFormat()) entity.setDateFormat(request.getDateFormat());
        if (null != request.getAmountFormat()) entity.setAmountFormat(request.getAmountFormat());
        if (null != request.getDupCheckFlag()) entity.setDupCheckFlag(request.getDupCheckFlag());
        if (null != request.getReversalHandling()) entity.setReversalHandling(request.getReversalHandling());
        if (null != request.getRecordLength()) entity.setRecordLength(request.getRecordLength());
        if (null != request.getPaddingChar()) entity.setPaddingChar(request.getPaddingChar());
        if (null != request.getXmlRootTag()) entity.setXmlRootTag(request.getXmlRootTag());
        if (null != request.getXmlRowTag()) entity.setXmlRowTag(request.getXmlRowTag());
        if (null != request.getXmlNamespace()) entity.setXmlNamespace(request.getXmlNamespace());
        if (null != request.getDescription()) entity.setDescription(request.getDescription());
        if (null != request.getFilePath()) entity.setFilePath(request.getFilePath());
        if (null != request.getTenantId()) entity.setTenantId(request.getTenantId());
        // sourceSystem FK entity set in service via repository lookup — not in mapper
        if (null != request.getCreatedBy()) entity.setCreatedBy(request.getCreatedBy());

        // New fields — aligned to HTML screen
        if (null != request.getHasHeader()) entity.setHasHeader(request.getHasHeader());
        if (null != request.getHasTrailer()) entity.setHasTrailer(request.getHasTrailer());
        if (null != request.getHeaderLineCount()) entity.setHeaderLineCount(request.getHeaderLineCount());
        if (null != request.getTrailerLineCount()) entity.setTrailerLineCount(request.getTrailerLineCount());
        if (null != request.getFrequencyType()) entity.setFrequencyType(request.getFrequencyType());
        if (null != request.getFileNamePattern()) entity.setFileNamePattern(request.getFileNamePattern());
        if (null != request.getReversalFieldName()) entity.setReversalFieldName(request.getReversalFieldName());
        if (null != request.getReversalValue()) entity.setReversalValue(request.getReversalValue());
        if (null != request.getReversalAmtHandling()) entity.setReversalAmtHandling(request.getReversalAmtHandling());

        // Safe defaults
        if (null == entity.getStatus()) entity.setStatus(CommonConstants.ACTIVE);
        if (null == entity.getVersion()) entity.setVersion(1);
        if (null == entity.getFileEncoding()) entity.setFileEncoding("UTF-8");
        if (null == entity.getHasHeader()) entity.setHasHeader(CommonConstants.ACTIVE_Y);
        if (null == entity.getHasTrailer()) entity.setHasTrailer(CommonConstants.INACTIVE_N);
        if (null == entity.getHeaderLineCount()) entity.setHeaderLineCount(1);
        if (null == entity.getTrailerLineCount()) entity.setTrailerLineCount(0);
        if (null == entity.getDupCheckFlag()) entity.setDupCheckFlag(CommonConstants.INACTIVE_N);

        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    // =========================================================================
    // METHOD 3: toDTOList
    // Called by: viewTemplate, searchTemplate
    // =========================================================================

    public static List<ReconFileTemplateMastDto> toDTOList(List<ReconFileTmpltMast> entities) {
        if (null == entities || entities.isEmpty()) return Collections.emptyList();
        List<ReconFileTemplateMastDto> result = new ArrayList<>();
        for (ReconFileTmpltMast entity : entities) {
            result.add(toDTO(entity));
        }
        return result;
    }

    // =========================================================================
    // METHOD 4: toDTO
    // Called by: getTemplateById (single), and internally by toDTOList
    // =========================================================================

    public static ReconFileTemplateMastDto toDTO(ReconFileTmpltMast entity) {
        if (null == entity) return null;

        ReconFileTemplateMastDto dto = ReconFileTemplateMastDto.builder()
                .templateId(entity.getTemplateId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .templateType(entity.getTemplateType())
                .description(entity.getDescription())
                .stageTabName(entity.getStageTabName())
                .filePath(entity.getFilePath())
                .fileEncoding(entity.getFileEncoding())
                .hasHeader(entity.getHasHeader())
                .hasTrailer(entity.getHasTrailer())
                .headerLineCount(entity.getHeaderLineCount())
                .trailerLineCount(entity.getTrailerLineCount())
                .frequencyType(entity.getFrequencyType())
                .fileNamePattern(entity.getFileNamePattern())
                .dateFormat(entity.getDateFormat())
                .amountFormat(entity.getAmountFormat())
                .dupCheckFlag(entity.getDupCheckFlag())
                .reversalHandling(entity.getReversalHandling())
                .reversalFieldName(entity.getReversalFieldName())
                .reversalValue(entity.getReversalValue())
                .reversalAmtHandling(entity.getReversalAmtHandling())
                .delimiter(entity.getDelimiter())
                .textQualifier(entity.getTextQualifier())
                .recordLength(entity.getRecordLength())
                .paddingChar(entity.getPaddingChar())
                .xmlRootTag(entity.getXmlRootTag())
                .xmlRowTag(entity.getXmlRowTag())
                .xmlNamespace(entity.getXmlNamespace())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .tenantId(entity.getTenantId())
                .subTemplateId(entity.getSubTemplateId())
                .typeId(entity.getTypeId())
                .columnCount(entity.getColumnCount())
                .reversalIndicator(entity.getReversalIndicator())
                .dataReference(entity.getDataReferenceFlag())
                .onlineRefund(entity.getOnlRefundFlag())
                .settlementFlag(entity.getSettlementFlag())
                .productType(entity.getProductType())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Source system — null-safe FK lookup: return sourceCode string only
        dto.setSourceSystem(null != entity.getSourceSystem()
                ? entity.getSourceSystem().getSourceCode() : null);

        // Field definitions — map all fields sorted by position
        Set<ReconTmpltFieldDtls> fieldSet = entity.getFieldDetails();
        if (null != fieldSet && !fieldSet.isEmpty()) {
            List<ReconFileTemplateMastDto.FieldDefinitionDTO> fieldDtos = new ArrayList<>();
            fieldSet.stream()
                    .sorted((a, b) -> {
                        if (null == a.getColPosition()) return 1;
                        if (null == b.getColPosition()) return -1;
                        return Long.compare(a.getColPosition(), b.getColPosition());
                    })
                    .forEach(f -> fieldDtos.add(toFieldDTO(f)));
            dto.setFieldDetails(fieldDtos);
        } else {
            dto.setFieldDetails(Collections.emptyList());
        }

        return dto;
    }

    // =========================================================================
    // PRIVATE — single field entity → FieldDefinitionDTO
    // Uses ACTUAL entity field names from ReconTmpltFieldDtls
    // =========================================================================

    private static ReconFileTemplateMastDto.FieldDefinitionDTO toFieldDTO(ReconTmpltFieldDtls f) {
        return ReconFileTemplateMastDto.FieldDefinitionDTO.builder()
                .fieldId(f.getFieldId())
                // templateId via relationship
                .templateId(null != f.getTemplate()
                        ? f.getTemplate().getTemplateId() : null)
                .fieldName(f.getFieldName())
                // fieldtype — matches request field name (was dataType)
                .fieldtype(null != f.getFieldType()
                        ? f.getFieldType().getFieldTypeDesc() : null)
                // fieldFormat — matches request field name (was format)
                .fieldFormat(null != f.getFieldFormat()
                        ? f.getFieldFormat().getFieldFormatDesc() : null)
                // fieldLength — stored in maxLength column
                .fieldLength(f.getFieldLength())
                .fieldScale(f.getFieldScale())
                // fieldSequence — matches request field name (was sequenceOrder)
                .fieldSequence(null != f.getColPosition()
                        ? f.getColPosition().intValue() : null)
                // positionFrom/To — stored as VARCHAR, parsed to Integer
                .positionFrom(parseIntSafe(f.getFromPosition()))
                .positionTo(parseIntSafe(f.getToPosition()))
                // isPrimaryKey — derived from keyIdentifier (Long 1/0)
                .isPrimaryKey(null != f.getKeyIdentifier() && f.getKeyIdentifier() > 0 ? CommonConstants.ACTIVE_Y : CommonConstants.INACTIVE_N)
                // isReconKey — new canonical column
                .isReconKey(f.getIsReconKey())
                // isMandatory — mandatoryFlag column
                .isMandatory(f.getIsMandatory())
                // trimFlag — new canonical column
                .trimFlag(f.getTrimFlag())
                .defaultValue(f.getDefaultValue())
                .validationRegex(f.getValidationRegex())
                .xmlXpath(f.getXmlXpath())
                .excelColIndex(f.getExcelColIndex())
                .build();
    }

    private static Integer parseIntSafe(String value) {
        // FIX: isBlank() → null check + trim().isEmpty() (Java 8 compatible)
        if (null == value || value.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}