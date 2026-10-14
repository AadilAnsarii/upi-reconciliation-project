package com.jpb.reconciliation.reconciliation.entity.v2;


import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "recon_field_format_mast")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "fieldDetails")
public class ReconFieldFormatMast {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FIELD_FORMAT_MAST")
    @SequenceGenerator(name = "SEQ_FIELD_FORMAT_MAST", sequenceName = "seq_field_format_mast", allocationSize = 1)
    @Column(name = "field_format_id")
    @EqualsAndHashCode.Include
    private Long fieldFormatId;

    @Column(name = "field_type_id", nullable = false)
    private Long fieldTypeId;

    @Column(name = "field_format_code", nullable = false, unique = true, length = 20)
    private String fieldFormatCode;

    @Column(name = "field_format_desc", nullable = false, length = 100)
    private String fieldFormatDesc;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "fieldFormat", fetch = FetchType.LAZY)
    private Set<ReconTmpltFieldDtls> fieldDetails;

    // Legacy alias
    public String getReconFieldFormatDesc() { return this.fieldFormatDesc; }
    public Long getReconFieldFormatId()     { return this.fieldFormatId; }
}
