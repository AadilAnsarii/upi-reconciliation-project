package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement;

import org.springframework.http.ResponseEntity;

public interface AuditReplacementService {

    ResponseEntity<RestWithStatusList> createReplacement(AuditReplacement replacement, String replacedBy);

    ResponseEntity<RestWithStatusList> getReplacementsByBankId(Long bankId);

    ResponseEntity<RestWithStatusList> getReplacementsByOriginalUser(Long originalUserId);

    ResponseEntity<RestWithStatusList> finalizeReplacement(Long replacementId, String finalizedBy);

    ResponseEntity<RestWithStatusList> restoreReplacement(Long replacementId, String restoredBy);

    ResponseEntity<RestWithStatusList> getActiveReplacementForUser(Long originalUserId);

    ResponseEntity<RestWithStatusList> schedulePendingReplacement(
            Long originalUserId, String pendingEmail, String pendingFullName,
            String pendingMobile, String orderedBy, String reason, String scheduledBy);

    void cancelPendingReplacement(Long originalUserId);

    void finalizePendingReplacement(Long originalUserId);

    void onOriginalBlocked(Long originalUserId);

    /** True while this user is standing in for someone else (their replacement record is ACTIVE). */
    boolean isActiveReplacementUser(Long userId);

    void onOriginalReactivated(Long originalUserId);

    /** The original's reactivation was SCHEDULED — put their replacement into INACTIVE_PENDING so
     *  both accounts show their pending state at the same time. */
    void onOriginalReactivateScheduled(Long originalUserId);

    /** The scheduled reactivation was cancelled — put the replacement back to ACTIVE. */
    void onOriginalReactivateCancelled(Long originalUserId);
}
