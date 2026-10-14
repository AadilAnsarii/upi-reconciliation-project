package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement;
import com.jpb.reconciliation.reconciliation.service.v2.AuditReplacementService;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/replacement")
@CrossOrigin(origins = "*")
public class AuditReplacementController2 {

    private static final Logger logger = LoggerFactory.getLogger(AuditReplacementController2.class);

    @Autowired
    private AuditReplacementService auditReplacementService;

    @Operation(summary = "Create a user replacement")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createReplacement(
            @RequestBody AuditReplacement replacement,
            Authentication authentication) {
        String replacedBy = resolveUser(authentication);
        logger.info("Create replacement request: bankId={}, originalUserId={} by {}",
                replacement.getBankId(), replacement.getOriginalUserId(), replacedBy);
        return auditReplacementService.createReplacement(replacement, replacedBy);
    }

    @Operation(summary = "Get replacements by bank ID")
    @GetMapping(value = "/get-by-bank/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getReplacementsByBankId(@PathVariable Long bankId) {
        return auditReplacementService.getReplacementsByBankId(bankId);
    }

    @Operation(summary = "Get replacements by original user ID")
    @GetMapping(value = "/get-by-original-user/{originalUserId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getReplacementsByOriginalUser(@PathVariable Long originalUserId) {
        return auditReplacementService.getReplacementsByOriginalUser(originalUserId);
    }

    @Operation(summary = "Get active replacement for a user")
    @GetMapping(value = "/get-active/{originalUserId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getActiveReplacement(@PathVariable Long originalUserId) {
        return auditReplacementService.getActiveReplacementForUser(originalUserId);
    }

    @Operation(summary = "Finalize a replacement (original user stays INACTIVE permanently)")
    @PostMapping(value = "/finalize/{replacementId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> finalizeReplacement(
            @PathVariable Long replacementId,
            Authentication authentication) {
        String finalizedBy = resolveUser(authentication);
        logger.info("Finalize replacement: {} by {}", replacementId, finalizedBy);
        return auditReplacementService.finalizeReplacement(replacementId, finalizedBy);
    }

    @Operation(summary = "Restore a replacement (original user -> ACTIVE, replacement user -> INACTIVE)")
    @PostMapping(value = "/restore/{replacementId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> restoreReplacement(
            @PathVariable Long replacementId,
            Authentication authentication) {
        String restoredBy = resolveUser(authentication);
        logger.info("Restore replacement: {} by {}", replacementId, restoredBy);
        return auditReplacementService.restoreReplacement(replacementId, restoredBy);
    }

    private String resolveUser(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
    }
}
