package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;

import org.springframework.http.ResponseEntity;

public interface AuditLogService {

    void log(String tableName, Long recordId, String operation, Long actorUserId, String actorUsername,
             String actorType, String entityType, Long bankId, String oldValue, String newValue,
             String actionLabel, String ipAddress, String remarks);

    ResponseEntity<RestWithStatusList> getLogsByTable(String tableName);

    ResponseEntity<RestWithStatusList> getLogsByRecord(String tableName, Long recordId);

    ResponseEntity<RestWithStatusList> getLogsByActor(Long actorUserId);

    ResponseEntity<RestWithStatusList> getLogsByBankId(Long bankId);

    ResponseEntity<RestWithStatusList> getLogsByDateRange(String from, String to);
}
