package com.jpb.reconciliation.reconciliation.entity;


import java.time.LocalDateTime;
import java.util.Set;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;

import lombok.*;

@Entity
@Table(name = "recon_source_system_mast")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "templates")
public class ReconSourceSystemMast {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOURCE_SYS_MAST")
    @SequenceGenerator(name = "SEQ_SOURCE_SYS_MAST", sequenceName = "seq_source_system_mast", allocationSize = 1)
    @Column(name = "source_id")
    @EqualsAndHashCode.Include
    private Long sourceId;

    @Column(name = "source_code", nullable = false, unique = true, length = 20)
    private String sourceCode;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "is_active", length = 1)
    private String isActive = CommonConstants.ACTIVE_Y;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "sourceSystem", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ReconFileTmpltMast> templates;

}