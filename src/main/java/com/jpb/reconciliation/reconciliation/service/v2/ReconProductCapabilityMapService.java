package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

public interface ReconProductCapabilityMapService {

    /** Grant MAKER or CHECKER capability for a product+action-type to a user (pool-additive —
     *  does not remove the grantor's own capability). Enforces: grantor must hold an active
     *  MAKER capability with CAN_DELEGATE='Y' for this exact product+action to grant MAKER;
     *  only a user with no MAKER capability of their own for this product+action (i.e. an
     *  Admin, checked by caller) may grant CHECKER — a Maker can never create a Checker. */
    ResponseEntity<RestWithStatusList> grantCapability(Long productId, Long userId, String capabilityType,
            String actionType, boolean canDelegate, String grantedByUsername);

    /** Revoke a specific capability grant. If the target user is a non-admin (BANK_USER/BRANCH_USER)
     *  and this is their last active capability for this product+action, the revoke is blocked
     *  (min-1 rule) unless force=true (e.g. triggered by a block/product-deactivation cascade). */
    ResponseEntity<RestWithStatusList> revokeCapability(Long capabilityId, String revokedByUsername,
            String reason, boolean force);

    /** Pool lookup — active holders of a capability for a product+action. */
    ResponseEntity<RestWithStatusList> getActiveCapabilities(Long productId, String actionType, String capabilityType);

    /** Everything currently held by a user (profile / block-cascade use). */
    ResponseEntity<RestWithStatusList> getUserCapabilities(Long userId);

    /** Approval routing: returns the active CHECKER pool for productId+actionType; if empty,
     *  walks up the GRANTED_BY chain from makerUserId until it finds someone with no further
     *  grantor (the root/Admin), and returns that user as the fallback checker. */
    ResponseEntity<RestWithStatusList> resolveApprover(Long productId, String actionType, Long makerUserId);

    /** Cascade-revoke every active capability for a product (e.g. bank cancels a product subscription). */
    ResponseEntity<RestWithStatusList> revokeAllForProduct(Long productId, String triggeredByUsername);
}
