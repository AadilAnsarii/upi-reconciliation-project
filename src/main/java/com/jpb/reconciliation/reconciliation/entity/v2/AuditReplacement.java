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
@Table(name = "AUDIT_REPLACEMENT")
public class AuditReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REPLACEMENT_ID")
    private Long replacementId;

    @Column(name = "BANK_ID", nullable = false)
    private Long bankId;

    @Column(name = "ORIGINAL_USER_ID", nullable = false)
    private Long originalUserId;

    @Column(name = "REPLACEMENT_USER_ID", nullable = false)
    private Long replacementUserId;

    @Column(name = "ENTITY_TYPE", length = 20, nullable = false)
    private String entityType;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = CommonConstants.ACTIVE;

    @Column(name = "REASON", length = 500)
    private String reason;

    // Pending replacement info — stored before the replacement user is created
    @Column(name = "PENDING_EMAIL", length = 150)
    private String pendingEmail;

    @Column(name = "PENDING_FULL_NAME", length = 200)
    private String pendingFullName;

    @Column(name = "PENDING_MOBILE", length = 20)
    private String pendingMobile;

    @Column(name = "PENDING_ORDERED_BY", length = 200)
    private String pendingOrderedBy;

    @Column(name = "REPLACED_AT")
    private LocalDateTime replacedAt;

    @Column(name = "REPLACED_BY", length = 255)
    private String replacedBy;

    @Column(name = "FINALIZED_AT")
    private LocalDateTime finalizedAt;

    @Column(name = "FINALIZED_BY", length = 255)
    private String finalizedBy;

    @Column(name = "RESTORED_AT")
    private LocalDateTime restoredAt;

    @Column(name = "RESTORED_BY", length = 255)
    private String restoredBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
