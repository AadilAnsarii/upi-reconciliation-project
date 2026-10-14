package com.jpb.reconciliation.reconciliation.entity.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AUDIT_USER_DELEGATION")
public class AuditUserDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DELEGATION_ID")
    private Long delegationId;

    @Column(name = "DELEGATOR_USER_ID", nullable = false)
    private Long delegatorUserId;

    @Column(name = "DELEGATEE_USER_ID", nullable = false)
    private Long delegateeUserId;

    @Column(name = "BANK_ID")
    private Long bankId;

    @Column(name = "REASON", length = 500)
    private String reason;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = CommonConstants.ACTIVE;

    @Column(name = "DELEGATED_AT")
    private LocalDateTime delegatedAt;

    @Column(name = "DELEGATED_BY", length = 100)
    private String delegatedBy;

    @Column(name = "RESTORED_AT")
    private LocalDateTime restoredAt;

    @Column(name = "RESTORED_BY", length = 100)
    private String restoredBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
