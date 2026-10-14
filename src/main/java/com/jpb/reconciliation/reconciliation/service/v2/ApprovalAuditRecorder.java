package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconApprovalRequest;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconApprovalRequestRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Writes the maker-checker audit trail into RECON_APPROVAL_REQUEST.
 *
 * The role/menu/user maker-checker flows each carried their own SUBMITTED_BY / APPROVED_BY
 * columns but never recorded a request row, so the table stayed empty and there was no single
 * place to see who submitted what and who decided it. This records both halves of every
 * maker-checker cycle, keyed on (ENTITY_TYPE, ENTITY_ID).
 *
 * Recording is deliberately best-effort: an audit write must never roll back or block the
 * business operation that triggered it. The caller's transaction owns the real change.
 */
@Component
public class ApprovalAuditRecorder {

    public static final String ENTITY_ROLE = "ROLE";
    public static final String ENTITY_MENU = "MENU";
    public static final String ENTITY_USER = "USER";

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";

    private static final String STATUS_PENDING = "PENDING";

    private static final Logger logger = LoggerFactory.getLogger(ApprovalAuditRecorder.class);

    @Autowired
    private ReconApprovalRequestRepository approvalRequestRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    /**
     * Opens (or refreshes) the PENDING request row for an entity a maker just submitted.
     *
     * Re-submitting the same entity — a rejected role that was fixed and sent again — reuses
     * the open row rather than stacking duplicates, since only one decision can ever be
     * outstanding for a given entity at a time.
     */
    public void recordSubmission(String entityType, Long entityId, String actionType, String makerUsername) {
        recordSubmission(entityType, entityId, actionType, makerUsername, null);
    }

    /**
     * As {@link #recordSubmission(String, Long, String, String)} but also stashes the maker's
     * proposed field changes (JSON) — used by the UPDATE flow, where the live entity is left
     * untouched until a Checker approves and the stashed changes are applied.
     *
     * @return the saved request's REQUEST_ID, or null if it couldn't be recorded.
     */
    public Long recordSubmission(String entityType, Long entityId, String actionType,
                                 String makerUsername, String proposedChanges) {
        try {
            Long makerId = resolveUserId(makerUsername);
            if (entityId == null || makerId == null) {
                logger.warn("Skipping approval audit for {}={}: unresolved maker '{}'", entityType, entityId, makerUsername);
                return null;
            }

            ReconApprovalRequest request = approvalRequestRepository
                    .findByEntityTypeAndEntityIdAndStatus(entityType, entityId, STATUS_PENDING)
                    .orElseGet(ReconApprovalRequest::new);

            request.setEntityType(entityType);
            request.setEntityId(entityId);
            request.setActionType(actionType);
            request.setMakerId(makerId);
            request.setSubmittedAt(LocalDateTime.now());
            request.setStatus(STATUS_PENDING);
            request.setProposedChanges(proposedChanges);
            // A fresh submission is undecided again — clear any leftover decision fields.
            request.setCheckerId(null);
            request.setCheckedAt(null);
            request.setDecision(null);
            request.setRemarks(null);

            return approvalRequestRepository.save(request).getRequestId();
        } catch (RuntimeException e) {
            logger.error("Failed to record approval submission for {}={}", entityType, entityId, e);
            return null;
        }
    }

    /**
     * Closes the open PENDING request for an entity with the checker's decision.
     *
     * @param decision APPROVED or REJECTED — also becomes the row's STATUS.
     */
    public void recordDecision(String entityType, Long entityId, String checkerUsername, String decision, String remarks) {
        try {
            Optional<ReconApprovalRequest> opt = approvalRequestRepository
                    .findByEntityTypeAndEntityIdAndStatus(entityType, entityId, STATUS_PENDING);
            if (!opt.isPresent()) {
                // An Admin acting as their own maker never goes through PENDING, so there is
                // genuinely nothing to close. Not an error.
                logger.debug("No pending approval request to close for {}={}", entityType, entityId);
                return;
            }

            ReconApprovalRequest request = opt.get();
            request.setCheckerId(resolveUserId(checkerUsername));
            request.setCheckedAt(LocalDateTime.now());
            request.setDecision(decision);
            request.setStatus(decision);
            request.setRemarks(remarks);
            approvalRequestRepository.save(request);
        } catch (RuntimeException e) {
            logger.error("Failed to record approval decision for {}={}", entityType, entityId, e);
        }
    }

    private Long resolveUserId(String username) {
        if (username == null) return null;
        return reconUserRepository.findByUsername(username).map(ReconUser::getUserId).orElse(null);
    }
}
