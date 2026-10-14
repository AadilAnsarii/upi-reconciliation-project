package com.jpb.reconciliation.reconciliation.entity.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RECON_ROLE_MASTER")
public class ReconRoleMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "ROLE_CODE", length = 30, unique = true, nullable = false)
    private String roleCode;

    @Column(name = "ROLE_NAME", length = 200, nullable = false)
    private String roleName;

    @Column(name = "ROLE_TYPE", length = 20, nullable = false)
    private String roleType;

    @Column(name = "ROLE_DESC", length = 500)
    private String roleDesc;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "SUBMITTED_BY", length = 100)
    private String submittedBy;

    @Column(name = "APPROVED_BY", length = 100)
    private String approvedBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    // Which Bank Type(s) (Issuer/Acquirer, comma-separated) this role is scoped to — only
    // meaningful when the owning bank/branch itself has more than one. Null/empty means
    // no restriction (defaults to whatever the bank/branch has).
    @Column(name = "BANK_TYPE_SCOPE", length = 50)
    private String bankTypeScope;
}
