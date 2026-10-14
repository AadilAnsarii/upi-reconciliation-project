package com.jpb.reconciliation.reconciliation.entity.v2;


import java.time.LocalDateTime;
import java.util.Set;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.entity.ReconFileIngestConfig;
import com.jpb.reconciliation.reconciliation.entity.ReconSourceSystemMast;

import lombok.*;

@Getter
@Setter
@Entity
// name+tenant uniqueness is deliberately NOT declared here. A plain unique constraint
// also counts soft-deleted (INACTIVE) rows, so a deleted template kept its name reserved
// forever while being invisible in every list. It is enforced instead by the function-based
// index UQ_TMPLT_NAME_TENANT_LIVE (sql/uq_tmplt_name_tenant_live.sql), which indexes live
// rows only. Re-adding it here would make ddl-auto recreate the unscoped constraint.
@Table(name = "recon_file_tmplt_mast",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tmplt_code",        columnNames = "template_code")
    })
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"fieldDetails", "ingestConfigs"})
public class ReconFileTmpltMast {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FILE_TMPLT_MAST")
    @SequenceGenerator(name = "SEQ_FILE_TMPLT_MAST", sequenceName = "seq_file_tmplt_mast", allocationSize = 1)
    @Column(name = "template_id")
    @EqualsAndHashCode.Include
    private Long templateId;

    @Column(name = "template_code", nullable = false, length = 20, unique = true)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "template_type", nullable = false, length = 20)
    private String templateType;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_sys_id")
    private ReconSourceSystemMast sourceSystem;

    @Column(name = "stage_tab_name", length = 100)
    private String stageTabName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_encoding", length = 20)
    private String fileEncoding = "UTF-8";

    @Column(name = "has_header", length = 1)
    private String hasHeader = CommonConstants.ACTIVE_Y;

    @Column(name = "has_trailer", length = 1)
    private String hasTrailer = CommonConstants.INACTIVE_N;

    @Column(name = "header_line_count")
    private Integer headerLineCount = 1;

    @Column(name = "trailer_line_count")
    private Integer trailerLineCount = 0;

    @Column(name = "frequency_type", length = 20)
    private String frequencyType;

    @Column(name = "file_name_pattern", length = 200)
    private String fileNamePattern;

    @Column(name = "date_format", length = 30)
    private String dateFormat;

    @Column(name = "amount_format", length = 30)
    private String amountFormat;

    @Column(name = "dup_check_flag", length = 1)
    private String dupCheckFlag = CommonConstants.INACTIVE_N;

    @Column(name = "reversal_handling", length = 20)
    private String reversalHandling = "NONE";

    // CSV specific
    @Column(name = "delimiter", length = 5)
    private String delimiter;

    @Column(name = "text_qualifier", length = 5)
    private String textQualifier;

    // Fixed Width specific
    @Column(name = "record_length")
    private Integer recordLength;

    @Column(name = "padding_char", length = 1)
    private String paddingChar;

    // XML specific
    @Column(name = "xml_root_tag", length = 100)
    private String xmlRootTag;

    @Column(name = "xml_row_tag", length = 100)
    private String xmlRowTag;

    @Column(name = "xml_namespace", length = 200)
    private String xmlNamespace;
    
	@Column(name = "GL_FLAG")
	private String glFlag;

    // Reversal detail columns (conditional — populated when reversal_indicator = Y)
    @Column(name = "reversal_field_name", length = 100)
    private String reversalFieldName;

    @Column(name = "reversal_value", length = 100)
    private String reversalValue;

    @Column(name = "reversal_amt_handling", length = 20)
    private String reversalAmtHandling;  // negate / asis — HTML Section 1 revCond

    // Legacy columns — preserved for backward compatibility with old code paths
    @Column(name = "sub_template_id")               private Long   subTemplateId;
    @Column(name = "type_id")                        private Long   typeId;
    @Column(name = "column_count")                   private Long   columnCount;
    @Column(name = "exist_flag",         length = 1) private String existFlag;
    @Column(name = "reversal_indicator", length = 5) private String reversalIndicator;
    @Column(name = "data_reference_flag",length = 5) private String dataReferenceFlag;
    @Column(name = "onl_refund_flag",    length = 5) private String onlRefundFlag;
    @Column(name = "issacq_flag",        length = 5) private String issacqFlag;
    @Column(name = "data_table_ind",     length = 5) private String dataTableInd;
    @Column(name = "master_flag",        length = 5) private String masterFlag;
    @Column(name = "master_template_id", length = 20)private String masterTemplateId;
    @Column(name = "settlement_flag",    length = 5) private String settlementFlag;
    @Column(name = "product_type",       length = 50)private String productType;

    // Lifecycle
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // Audit
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("sequenceOrder ASC")
    private Set<ReconTmpltFieldDtls> fieldDetails;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonBackReference
    private Set<ReconFileIngestConfig> ingestConfigs;

    // Convenience alias — legacy code used insertCode; new code uses tenantId
    public Long getInsertCode()           { return this.tenantId; }
    public void setInsertCode(Long val)   { this.tenantId = val; }

    // Convenience: getReconTemplateId() alias so old service code doesn't break
    public Long getReconTemplateId()      { return this.templateId; }
    public void setReconTemplateId(Long v){ this.templateId = v; }
    
    @Column(name = "SFTP_SERVER_ID")
    private Long sftpServerId;
}
