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
@Table(name = "AUDIT_LOG")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "TABLE_NAME", length = 100, nullable = false)
    private String tableName;

    @Column(name = "RECORD_ID", nullable = false)
    private Long recordId;

    @Column(name = "OPERATION", length = 30, nullable = false)
    private String operation;

    @Column(name = "ACTOR_USER_ID")
    private Long actorUserId;

    @Column(name = "ACTOR_USERNAME", length = 200)
    private String actorUsername;

    @Column(name = "ACTOR_TYPE", length = 30)
    private String actorType;

    @Column(name = "ENTITY_TYPE", length = 50)
    private String entityType;

    @Column(name = "BANK_ID")
    private Long bankId;

    @Column(name = "CHANGED_AT")
    private LocalDateTime changedAt;

    @Lob
    @Column(name = "OLD_VALUE")
    private String oldValue;

    @Lob
    @Column(name = "NEW_VALUE")
    private String newValue;

    @Column(name = "ACTION_LABEL", length = 200)
    private String actionLabel;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;
}
