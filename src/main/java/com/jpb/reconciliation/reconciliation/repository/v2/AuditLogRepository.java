package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTableName(String tableName);

    List<AuditLog> findByRecordId(Long recordId);

    List<AuditLog> findByActorUserId(Long actorUserId);

    List<AuditLog> findByBankId(Long bankId);

    List<AuditLog> findByEntityType(String entityType);

    List<AuditLog> findByOperation(String operation);

    List<AuditLog> findByTableNameAndRecordId(String tableName, Long recordId);

    List<AuditLog> findByChangedAtBetween(LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByBankIdAndChangedAtBetween(Long bankId, LocalDateTime from, LocalDateTime to);
}
