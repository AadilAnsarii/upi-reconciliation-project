package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconApprovalRequest;

import org.springframework.http.ResponseEntity;

public interface ReconApprovalRequestService {

    ResponseEntity<RestWithStatusList> submitRequest(ReconApprovalRequest request);

    ResponseEntity<RestWithStatusList> getPendingRequests();

    ResponseEntity<RestWithStatusList> getRequestById(Long requestId);

    ResponseEntity<RestWithStatusList> getRequestsByEntityType(String entityType);

    ResponseEntity<RestWithStatusList> getRequestsByMakerId(Long makerId);

    ResponseEntity<RestWithStatusList> getRequestsByCheckerId(Long checkerId);

    ResponseEntity<RestWithStatusList> approveRequest(Long requestId, Long checkerId, String remarks);

    ResponseEntity<RestWithStatusList> rejectRequest(Long requestId, Long checkerId, String remarks);

    /** Read-only history scoped to the caller (admins → own bank; KAL_ADMIN → all; user → own). */
    ResponseEntity<RestWithStatusList> getHistoryFor(Long userId, Long bankId, boolean isAdmin);

    /** PENDING maker UPDATE requests a Checker must decide — scoped to their bank/branch
     *  (KAL_ADMIN sees all). Each row carries the entity name and the proposed field changes. */
    ResponseEntity<RestWithStatusList> getPendingUpdatesForChecker(String checkerUsername);

    /** Applies a Checker's decision on a maker UPDATE request. On APPROVED the stashed proposed
     *  changes are applied to the live entity; on REJECTED they are discarded. */
    ResponseEntity<RestWithStatusList> decideUpdateRequest(Long requestId, String checkerUsername,
                                                           String decision, String remarks);
}
