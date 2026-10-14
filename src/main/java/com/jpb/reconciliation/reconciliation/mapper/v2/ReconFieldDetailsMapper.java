package com.jpb.reconciliation.reconciliation.mapper.v2;


import java.time.LocalDateTime;
import java.util.Date;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.FieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconFieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldFormatMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldTypeMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;

/**
 * Static mapper for field definition rows.
 *
 * All setters are aligned to ACTUAL entity fields in ReconTmpltFieldDtls:
 *   templateMast, fieldTypeMast, fieldFormatMast  — relationship fields
 *   colPosn        — sequence_order (NOT NULL Long)
 *   maxLength      — field_length
 *   keyIdentifier  — is_primary_key stored as Long 1/0
 *   mandatoryFlag  — is_mandatory Y/N
 *   shortName      — field label / display name
 *   fromPosition, toPosition — VARCHAR position columns
 *   fromPosn, toPosn         — duplicate VARCHAR position legacy columns
 *   matchingFieldFlag        — mirrors isReconKey
 *   alterFlag                — mirrors trimFlag
 *   colPosition              — VARCHAR display column
 *
 * New canonical columns (proper typed columns added in migration):
 *   fieldName, isReconKey, trimFlag, defaultValue,
 *   validationRegex, xmlXpath, excelColIndex
 */
public class ReconFieldDetailsMapper {

    private ReconFieldDetailsMapper() {}
    
    public static ReconFieldDetailsMaster mapFieldDtoToEntity(FieldConfigurationDto dto, ReconTemplateDetails template,
			ReconFieldTypeMaster fieldType, ReconFieldFormatMaster fieldFormat) {

		ReconFieldDetailsMaster entity = new ReconFieldDetailsMaster();

		entity.setReconColumnPosn(dto.getColumnPosition());
		entity.setReconShortName(dto.getFieldName());
		entity.setReconFromPosn(String.valueOf(dto.getFromPosition()));
		entity.setReconToPosn(String.valueOf(dto.getToPosition()));
		entity.setReconMaxLength(dto.getFieldLength());
		entity.setReconKeyIdentifier(CommonConstants.ACTIVE_Y.equalsIgnoreCase(dto.getKeyIdentity()) ? 1L : 0L);
		entity.setReconColumnOffset(dto.getColumnOffset());
		entity.setReconMandatoryFlag(dto.getQualifier());

		entity.setReconTemplateDetails(template);
		entity.setReconFieldTypeMaster(fieldType);
		entity.setReconFieldFormatMaster(fieldFormat);

		entity.setReconInsertDate(new Date());
		entity.setReconInsertUser(1L);
		entity.setReconInstanceCode(1L);
		entity.setReconSubTempId(1L);

		return entity;
	}

    public static ReconTmpltFieldDtls mapFieldDtoToEntity(ReconFieldConfigurationDto dto,
                                                           ReconFileTmpltMast template,
                                                           ReconFieldTypeMast  fieldType,
                                                           ReconFieldFormatMast fieldFormat) {
        if (null == dto) return null;

        ReconTmpltFieldDtls field = new ReconTmpltFieldDtls();

        // Relationships
        field.setTemplate(template);
        field.setFieldType(fieldType);
        field.setFieldFormat(fieldFormat);

        // New canonical columns
        field.setFieldName(dto.getFieldName());
        field.setIsReconKey(normaliseFlag(dto.getIsReconKey()));
        field.setTrimFlag(normaliseFlag(dto.getTrimFlag()));
        field.setDefaultValue(dto.getDefaultValue());
        field.setValidationRegex(dto.getValidationRegex());
        field.setXmlXpath(dto.getXmlXpath());
        field.setExcelColIndex(dto.getExcelColIndex());
        // Legacy: shortName = field label
        field.setShortName(dto.getFieldName());

        // Legacy: max length
        field.setFieldLength(null != dto.getFieldLength()
                ? Long.valueOf(dto.getFieldLength()) : null);

        // Field scale — decimal precision (e.g. 2 for ##.##)
        field.setFieldScale(dto.getFieldScale());

        // Legacy: primary key stored as Long 1 / 0
        field.setKeyIdentifier(
                CommonConstants.ACTIVE_Y.equalsIgnoreCase(dto.getIsPrimaryKey()) ? 1L : 0L);

        // Legacy: mandatory flag Y/N
        field.setIsMandatory(normaliseFlag(dto.getIsMandatory()));

        // Legacy: position columns stored as VARCHAR
        field.setFromPosition(null != dto.getPositionFrom()
                ? String.valueOf(dto.getPositionFrom()) : null);
        field.setToPosition(null != dto.getPositionTo()
                ? String.valueOf(dto.getPositionTo()) : null);

        // Legacy: matching field = recon key, alter flag = trim flag
        field.setMatchingFieldFlag(normaliseFlag(dto.getIsReconKey()));
        field.setAlterFlag(normaliseFlag(dto.getTrimFlag()));

        // Audit from parent template
        field.setCreatedAt(LocalDateTime.now());
        if (null != template) {
            field.setCreatedBy(template.getCreatedBy());
//            field.setIn(template.getInsertCode());
        }
        
        
        Long seqVal = null != dto.getFieldSequence()
                ? Long.valueOf(dto.getFieldSequence()) : 0L;
        field.setColPosition(seqVal);
        field.setColPosn(seqVal);
        field.setSequenceOrder(seqVal);
        return field;
    }

    private static String normaliseFlag(String value) {
        if (null == value) return CommonConstants.INACTIVE_N;
        String v = value.trim().toUpperCase();
        return (v.equals(CommonConstants.ACTIVE_Y) || v.equals("1") || v.equals("YES") || v.equals("TRUE"))
                ? CommonConstants.ACTIVE_Y : CommonConstants.INACTIVE_N;
    }
}
