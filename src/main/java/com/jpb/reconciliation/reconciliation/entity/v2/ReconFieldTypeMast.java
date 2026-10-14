package com.jpb.reconciliation.reconciliation.entity.v2;


import java.time.LocalDateTime;
import java.util.Set;
import javax.persistence.*;

import lombok.*;

@Entity
@Table(name = "recon_field_type_mast")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "fieldDetails")
public class ReconFieldTypeMast {

    @Id
    @Column(name = "field_type_id")
    @EqualsAndHashCode.Include
    private Long fieldTypeId;

    @Column(name = "field_type_code", nullable = false, unique = true, length = 20)
    private String fieldTypeCode;

    @Column(name = "field_type_desc", nullable = false, length = 100)
    private String fieldTypeDesc;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "fieldType", fetch = FetchType.LAZY)
    private Set<ReconTmpltFieldDtls> fieldDetails;

    // Legacy alias
    public String getFieldTypeDes() { return this.fieldTypeDesc; }
    public String getRftFieldTypeDesc() { return this.fieldTypeDesc; }
}
