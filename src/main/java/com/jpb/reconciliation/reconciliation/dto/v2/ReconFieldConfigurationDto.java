package com.jpb.reconciliation.reconciliation.dto.v2;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single field definition row inside TemplateFieldDto.fieldDetails.
 * Every field maps to one row in recon_tmplt_field_dtls.
 *
 * Used by:
 *   ReconFieldDetailsMapper.mapFieldDtoToEntity()
 *   ReconTemplateDetailsServiceImpl.buildFieldEntities()
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReconFieldConfigurationDto {

    /**
     * Field display / column name.
     * Maps to: field_name (canonical) and tab_field_name (legacy)
     */
    private String fieldName;

    /**
     * Field data type description — must match recon_field_type_mast.field_type_desc
     * e.g. "String", "Number", "Date", "Decimal", "Boolean"
     * Looked up via: ReconFieldTypeMastRepository.findByFieldTypeDes()
     */
    private String fieldType;

    /**
     * Field format description — must match recon_field_format_mast.field_format_desc
     * e.g. "dd-MM-yyyy", "##.##", "VARCHAR"
     * Looked up via: ReconFieldFormatMastRepository.findByReconFieldFormatDesc()
     */
    private String fieldFormat;

    /**
     * Maximum character length of the field value.
     * Maps to: field_length
     */
    private Integer fieldLength;

    /**
     * Decimal scale (number of digits after decimal point).
     * Maps to: field_scale  — used when fieldtype = "Decimal"
     */
    private Integer fieldScale;

    /**
     * Sequence / column order in the file (1-based).
     * Maps to: sequence_order (canonical) and col_position (legacy)
     */
    private Integer fieldSequence;

    /**
     * Fixed Width: 1-based start position of this field in a record.
     * Maps to: position_from
     */
    private Integer positionFrom;

    /**
     * Fixed Width: 1-based end position (inclusive) of this field in a record.
     * Maps to: position_to
     */
    private Integer positionTo;

    /**
     * Y / N — whether this field is the primary key of the record.
     * Maps to: is_primary_key (canonical) and key_identifier (legacy, stored as 1/0)
     */
    private String isPrimaryKey;

    /**
     * Y / N — whether this field is used as a reconciliation matching key.
     * Maps to: is_recon_key (canonical) and matching_field_flag (legacy)
     */
    private String isReconKey;

    /**
     * Y / N — whether this field must be present and non-empty.
     * Maps to: is_mandatory (canonical) and mandatory_flag_old (legacy)
     */
    private String isMandatory;

    /**
     * Y / N — whether to trim leading/trailing whitespace from this field's value.
     * Maps to: trim_flag (canonical) and alter_flag (legacy)
     */
    private String trimFlag;

    /**
     * Default value to use when the field is empty in the file.
     * Maps to: default_value
     */
    private String defaultValue;

    /**
     * Optional Java regex pattern for field value validation.
     * Maps to: validation_regex
     */
    private String validationRegex;

    /**
     * XML only: element tag name or simple XPath expression for this field.
     * Maps to: xml_xpath
     * Example: "TransactionDate" or "Header/TxnDate"
     */
    private String xmlXpath;

    /**
     * Excel only: 0-based column index.
     * Maps to: excel_col_index
     */
    private Integer excelColIndex;
}
