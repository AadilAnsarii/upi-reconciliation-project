package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/audit-log")
@CrossOrigin(origins = "*")
public class AuditLogController {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);

    @Autowired
    private AuditLogService auditLogService;

    @Operation(summary = "Get audit logs by table name")
    @GetMapping(value = "/get-by-table", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getLogsByTable(@RequestParam String tableName) {
        logger.info("Get audit logs by table: {}", tableName);
        return auditLogService.getLogsByTable(tableName);
    }

    @Operation(summary = "Get audit logs by table and record ID")
    @GetMapping(value = "/get-by-record", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getLogsByRecord(
            @RequestParam String tableName,
            @RequestParam Long recordId) {
        logger.info("Get audit logs for table={} recordId={}", tableName, recordId);
        return auditLogService.getLogsByRecord(tableName, recordId);
    }

    @Operation(summary = "Get audit logs by actor user ID")
    @GetMapping(value = "/get-by-actor/{actorUserId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getLogsByActor(@PathVariable Long actorUserId) {
        return auditLogService.getLogsByActor(actorUserId);
    }

    @Operation(summary = "Get audit logs by bank ID")
    @GetMapping(value = "/get-by-bank/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getLogsByBankId(@PathVariable Long bankId) {
        return auditLogService.getLogsByBankId(bankId);
    }

    @Operation(summary = "Get audit logs within a date range (ISO LocalDateTime format)")
    @GetMapping(value = "/get-by-date-range", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getLogsByDateRange(
            @RequestParam String from,
            @RequestParam String to) {
        logger.info("Get audit logs from={} to={}", from, to);
        return auditLogService.getLogsByDateRange(from, to);
    }
}
