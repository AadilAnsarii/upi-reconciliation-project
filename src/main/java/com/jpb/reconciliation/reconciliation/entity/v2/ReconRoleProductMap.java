package com.jpb.reconciliation.reconciliation.entity.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Restricts a Role to specific Products (e.g. a MAKER role that can only work on UPI) —
 * a plain single-ID entity with ROLE_ID/PRODUCT_ID as ordinary columns, deliberately NOT an
 * @EmbeddedId/@MapsId composite key like CRoleMenuMap was, since that pattern crashed with a
 * PropertyAccessException on this DB (see ReconRoleMasterServiceImpl.savePrivileges fix).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "C_ROLE_PRODUCT_MAP", uniqueConstraints = @UniqueConstraint(columnNames = {"ROLE_ID", "PRODUCT_ID"}))
public class ReconRoleProductMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ROLE_ID", nullable = false)
    private Long roleId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;
}
