package com.jpb.reconciliation.reconciliation.entity.v2;


import java.time.LocalDateTime;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;

@Entity
@Table(name = "recon_tmplt_field_dtls",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tfd_name_template",
        columnNames = {"template_id", "field_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"template", "fieldType", "fieldFormat"})
public class ReconTmpltFieldDtls {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TMPLT_FIELD_DTLS")
    @SequenceGenerator(name = "SEQ_TMPLT_FIELD_DTLS", sequenceName = "seq_tmplt_field_dtls", allocationSize = 1)
    @Column(name = "field_id")
    @EqualsAndHashCode.Include
    private Long fieldId;

    @ManyToOne(fetch = FetchType.EAGER)  // CHANGE LAZY TO EAGER
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnoreProperties({"fieldDetails"})
    private ReconFileTmpltMast template;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_label", length = 150)
    private String fieldLabel;

    @ManyToOne(fetch = FetchType.EAGER)  // CHANGE LAZY TO EAGER
    @JoinColumn(name = "field_type_id", nullable = false)
    @JsonIgnoreProperties({"fieldDetails"})
    private ReconFieldTypeMast fieldType;

    @ManyToOne(fetch = FetchType.EAGER)  // CHANGE LAZY TO EAGER
    @JoinColumn(name = "field_format_id", nullable = false)
    @JsonIgnoreProperties({"fieldDetails"})
    private ReconFieldFormatMast fieldFormat;

    @Column(name = "field_length")
    private Long fieldLength;

    @Column(name = "field_scale")
    private Integer fieldScale;

    @Column(name = "sequence_order", nullable = false)
    private Long sequenceOrder;

    @Column(name = "position_from")
    private Integer positionFrom;

    @Column(name = "position_to")
    private Integer positionTo;

    @Column(name = "is_primary_key", length = 1)
    private String isPrimaryKey = "N";

    @Column(name = "is_recon_key", length = 1)
    private String isReconKey = "N";

    @Column(name = "is_mandatory", length = 1)
    private String isMandatory = "N";

    @Column(name = "trim_flag", length = 1)
    private String trimFlag = "N";

    @Column(name = "default_value", length = 200)
    private String defaultValue;

    @Column(name = "validation_regex", length = 500)
    private String validationRegex;

    @Column(name = "xml_xpath", length = 500)
    private String xmlXpath;

    @Column(name = "excel_col_index")
    private Integer excelColIndex;

    // Legacy columns — preserved for backward compatibility
    @Column(name = "col_position")              private Long   colPosition;
    @Column(name = "sub_temp_id")               private Long   subTempId;
    @Column(name = "short_name",   length = 100)private String shortName;
    @Column(name = "tab_field_name",length = 200)private String tabFieldName;
    @Column(name = "from_position",length = 20) private String fromPosition;
    @Column(name = "to_position",  length = 20) private String toPosition;
    @Column(name = "key_identifier")            private Long   keyIdentifier;
    @Column(name = "col_offset",   length = 20) private String colOffset;
    @Column(name = "mandatory_flag_old",length=1)private String mandatoryFlagOld;
    @Column(name = "rank_identifier",length = 20)private String rankIdentifier;
    @Column(name = "alter_flag",   length = 1)  private String alterFlag;
    @Column(name = "display_field_flag",length=1)private String displayFieldFlag;
    @Column(name = "report_field_flag",length=1) private String reportFieldFlag;
    @Column(name = "matching_field_flag",length=1)private String matchingFieldFlag;

    /**
     * Soft-delete marker. 'Y' once the owning template has been deleted; 'N' (or null,
     * for rows written before this column was mapped) means live. Every read path must
     * exclude 'Y' — see ReconTmpltFieldDtlsRepository.findActiveFieldsByTemplateId.
     */
    @Column(name = "is_deleted", length = 1)     private String isDeleted;

    // Audit
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Convenience aliases — legacy code used reconFieldId, reconColumnPosn etc.
    public Long getReconFieldId()         { return this.fieldId; }
    public void setReconFieldId(Long v)   { this.fieldId = v; }

    public Long getReconColumnPosn()      { return this.sequenceOrder; }
    public void setReconColumnPosn(Long v){ this.sequenceOrder = v; }

    public String getReconTabFieldName()  { return this.fieldName; }
    public String getReconShortName()     { return this.fieldLabel; }

    public ReconFileTmpltMast getReconTemplateDetails()         { return this.template; }
    public void setReconTemplateDetails(ReconFileTmpltMast t)   { this.template = t; }

    public ReconFieldTypeMast  getReconFieldTypeMaster()        { return this.fieldType; }
    public ReconFieldFormatMast getReconFieldFormatMaster()     { return this.fieldFormat; }
    
    
    
    
    @Column(name = "COL_POSN")              private Long   colPosn;
}
