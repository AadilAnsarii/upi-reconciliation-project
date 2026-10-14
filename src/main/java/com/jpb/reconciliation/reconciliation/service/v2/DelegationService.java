package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

public interface DelegationService {

    /** Admin-initiated delegation (the "Confirm Delegation" modal flow) — transfers children
     *  to the chosen delegatee immediately and schedules the delegator's own inactivation,
     *  matching old backend's AddUserServiceImpl.delegateUser(). */
    ResponseEntity<RestWithStatusList> delegateNow(Long delegatorUserId, Long delegateeUserId,
                                                    String reason, String triggeredBy);

    /** Called when user goes INACTIVE — transfers children to parent, records delegation */
    void delegateOnInactivate(Long delegatorUserId, String triggeredBy);

    /** Called when a delegator's pending inactivation is undone before it finalizes —
     *  reverses the delegation immediately (children go back, delegation marked CANCELLED),
     *  mirroring AuditReplacementService.cancelPendingReplacement(). A no-op if the delegator
     *  has no ACTIVE delegation (e.g. inactivation wasn't triggered by a delegation at all). */
    void cancelDelegation(Long delegatorUserId, String cancelledBy);

    /** Called when user is reactivated — restores children back to original parent */
    void restoreDelegationOnReactivate(Long delegatorUserId, String triggeredBy);

    /** Manual admin-tooling restore of a specific delegation by its own ID, independent
     *  of any status change on the delegator (UserManagement.jsx's "Restore" button). */
    ResponseEntity<RestWithStatusList> restoreDelegationById(Long delegationId, String triggeredBy);

    /** Called when delegator's block is scheduled (BLOCK_PENDING) — notify delegatee */
    void notifyDelegateeBlockPending(Long delegatorUserId, String blockAt);

    /** Called when delegator is permanently BLOCKED — notify delegatee */
    void notifyDelegateeBlocked(Long delegatorUserId);

    /** Called when delegator is unblocked — notify delegatee */
    void notifyDelegateeUnblocked(Long delegatorUserId);
}
