package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditUserDelegation;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditUserDelegationRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DelegationServiceImpl implements DelegationService {

    private static final Logger logger = LoggerFactory.getLogger(DelegationServiceImpl.class);

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private AuditUserDelegationRepository delegationRepository;
    @Autowired private com.jpb.reconciliation.reconciliation.repository.v2.AuditReplacementRepository auditReplacementRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private EmailService emailService;

    // ── Admin-initiated delegation (the "Confirm Delegation" modal) ──────────────
    // Mirrors old backend's AddUserServiceImpl.delegateUser(): transfers children
    // immediately, schedules the delegator's own inactivation (delegation is what
    // triggers it, not the reverse), and records the delegation right away —
    // it does NOT wait for the scheduler to finalize the inactivation.
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> delegateNow(Long delegatorUserId, Long delegateeUserId,
                                                           String reason, String triggeredBy) {
        Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
        if (!delegatorOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found: " + delegatorUserId, null));
        }
        Optional<ReconUser> delegateeOpt = reconUserRepository.findById(delegateeUserId);
        if (!delegateeOpt.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Delegatee not found: " + delegateeUserId, null));
        }
        ReconUser delegator = delegatorOpt.get();
        ReconUser delegatee = delegateeOpt.get();

        if (!CommonConstants.ACTIVE.equals(delegator.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "User must be ACTIVE to delegate. Current: " + delegator.getStatus(), null));
        }
        if (!delegationRepository.findByDelegatorUserIdAndStatus(delegatorUserId, CommonConstants.ACTIVE).isEmpty()) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "An active delegation already exists for this user.", null));
        }

        // Transfer children immediately
        List<ReconUser> children = reconUserRepository.findByParentUserId(delegatorUserId);
        for (ReconUser child : children) {
            child.setPreDelegationParentId(child.getParentUserId());
            child.setParentUserId(delegateeUserId);
            child.setUpdatedAt(LocalDateTime.now());
            reconUserRepository.save(child);
        }

        // Schedule the delegator's own inactivation
        delegator.setStatus("INACTIVE_PENDING");
        delegator.setInactivateScheduledAt(LocalDateTime.now());
        delegator.setInactivateScheduledBy(triggeredBy);
        delegator.setUpdatedAt(LocalDateTime.now());
        delegator.setUpdatedBy(triggeredBy);
        reconUserRepository.save(delegator);

        // Record delegation
        AuditUserDelegation delegation = new AuditUserDelegation();
        delegation.setDelegatorUserId(delegatorUserId);
        delegation.setDelegateeUserId(delegateeUserId);
        delegation.setBankId(delegator.getBankId());
        delegation.setReason(reason);
        delegation.setStatus(CommonConstants.ACTIVE);
        delegation.setDelegatedAt(LocalDateTime.now());
        delegation.setDelegatedBy(triggeredBy);
        delegation.setCreatedAt(LocalDateTime.now());
        delegation.setCreatedBy(triggeredBy);
        delegationRepository.save(delegation);

        auditLog("AUDIT_USER_DELEGATION", delegation.getDelegationId(), "CREATE", null,
                "delegatorUserId=" + delegatorUserId + ",delegateeUserId=" + delegateeUserId,
                triggeredBy, delegator.getUserType(), delegator.getBankId(),
                "Delegation: " + delegator.getUsername() + " -> " + delegatee.getUsername());

        try {
            emailService.sendDelegationToDelegate(delegator.getEmail(), delegator.getFullName(),
                    delegatee.getFullName(), reason, resolveOrgName(delegator));
        } catch (Exception e) {
            logger.warn("sendDelegationToDelegate failed: {}", e.getMessage());
        }
        try {
            emailService.sendDelegationToDelegatee(delegatee.getEmail(), delegatee.getFullName(),
                    delegator.getFullName(), reason);
        } catch (Exception e) {
            logger.warn("sendDelegationToDelegatee failed: {}", e.getMessage());
        }
        try {
            reconUserRepository.findByUsername(triggeredBy).ifPresent(actor ->
                    emailService.sendActorActionConfirmation(actor.getEmail(), actor.getFullName(),
                            "Delegation Initiated", delegator.getFullName(), delegator.getUsername(), null));
        } catch (Exception e) {
            logger.warn("Actor confirmation email failed: {}", e.getMessage());
        }

        logger.info("Delegation created (manual): {} -> {} ({} children transferred)",
                delegator.getUsername(), delegatee.getUsername(), children.size());

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS,
                "User delegated successfully.", Collections.singletonList(delegation)));
    }

    // ── Delegate: user goes INACTIVE → transfer children to parent (delegatee) ───
    @Override
    @Transactional
    public void delegateOnInactivate(Long delegatorUserId, String triggeredBy) {
        Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
        if (!delegatorOpt.isPresent()) return;

        // An explicit delegation (delegateNow) already transferred this user's children and
        // emailed both parties, leaving the account INACTIVE_PENDING. When the scheduler later
        // inactivates it, this auto-delegation must not run again: it would insert a second
        // ACTIVE row, resend both delegation emails, and overwrite PRE_DELEGATION_PARENT_ID on
        // every child — permanently breaking restore. delegateNow guards on the same condition.
        if (!delegationRepository.findByDelegatorUserIdAndStatus(delegatorUserId, CommonConstants.ACTIVE).isEmpty()) {
            logger.debug("delegateOnInactivate: delegatorUserId={} already has an ACTIVE delegation — skip", delegatorUserId);
            return;
        }

        // A replacement supersedes delegation: the successor inherits the team, not the parent.
        // Both run from the same scheduler tick (finalizePendingReplacement then this), and
        // whichever moves the children first wins. Without this guard the team ended up under
        // the parent and the freshly created replacement had nobody reporting to it.
        if (auditReplacementRepository.existsByOriginalUserIdAndStatus(delegatorUserId, "PENDING")
                || auditReplacementRepository.existsByOriginalUserIdAndStatus(delegatorUserId, CommonConstants.ACTIVE)) {
            logger.info("delegateOnInactivate: delegatorUserId={} has a replacement — the successor keeps the team, skipping delegation", delegatorUserId);
            return;
        }

        ReconUser delegator = delegatorOpt.get();
        Long parentId = delegator.getParentUserId();
        if (parentId == null) {
            logger.debug("delegateOnInactivate: delegatorUserId={} has no parent — skip delegation", delegatorUserId);
            return;
        }

        Optional<ReconUser> delegateeOpt = reconUserRepository.findById(parentId);
        if (!delegateeOpt.isPresent()) return;
        ReconUser delegatee = delegateeOpt.get();

        // Transfer children
        List<ReconUser> children = reconUserRepository.findByParentUserId(delegatorUserId);
        for (ReconUser child : children) {
            child.setPreDelegationParentId(child.getParentUserId());
            child.setParentUserId(parentId);
            child.setUpdatedAt(LocalDateTime.now());
            reconUserRepository.save(child);
        }

        // Record delegation
        AuditUserDelegation delegation = new AuditUserDelegation();
        delegation.setDelegatorUserId(delegatorUserId);
        delegation.setDelegateeUserId(parentId);
        delegation.setBankId(delegator.getBankId());
        delegation.setReason("User inactivated — children delegated to parent");
        delegation.setStatus(CommonConstants.ACTIVE);
        delegation.setDelegatedAt(LocalDateTime.now());
        delegation.setDelegatedBy(triggeredBy);
        delegation.setCreatedAt(LocalDateTime.now());
        delegation.setCreatedBy(triggeredBy);
        delegationRepository.save(delegation);

        auditLog("AUDIT_USER_DELEGATION", delegation.getDelegationId(), "CREATE",
                null, "delegatorUserId=" + delegatorUserId + ",delegateeUserId=" + parentId,
                triggeredBy, delegator.getUserType(), delegator.getBankId(),
                "Delegation: " + delegator.getUsername() + " → " + delegatee.getUsername());

        // Emails
        try {
            String orgName = resolveOrgName(delegator);
            emailService.sendDelegationToDelegate(delegator.getEmail(), delegator.getFullName(),
                    delegatee.getFullName(), "User inactivated", orgName);
        } catch (Exception e) {
            logger.warn("sendDelegationToDelegate failed: {}", e.getMessage());
        }
        try {
            emailService.sendDelegationToDelegatee(delegatee.getEmail(), delegatee.getFullName(),
                    delegator.getFullName(), "User inactivated");
        } catch (Exception e) {
            logger.warn("sendDelegationToDelegatee failed: {}", e.getMessage());
        }

        logger.info("Delegation created: {} → {} ({} children transferred)",
                delegator.getUsername(), delegatee.getUsername(), children.size());
    }

    // ── Cancel: delegator's pending inactivation was undone before it finalized ──
    // Mirrors AuditReplacementService.cancelPendingReplacement() — reverses the
    // delegation immediately instead of leaving it dangling as ACTIVE while the
    // delegator is actually back to ACTIVE.
    @Override
    @Transactional
    public void cancelDelegation(Long delegatorUserId, String cancelledBy) {
        Optional<AuditUserDelegation> activeOpt = delegationRepository
                .findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, CommonConstants.ACTIVE);
        if (!activeOpt.isPresent()) return;

        AuditUserDelegation delegation = activeOpt.get();
        Long delegateeId = delegation.getDelegateeUserId();

        // Move children back — same "only ones still parked here" guard as restore
        List<ReconUser> children = reconUserRepository.findByParentUserId(delegateeId);
        for (ReconUser child : children) {
            if (delegatorUserId.equals(child.getPreDelegationParentId())) {
                child.setParentUserId(delegatorUserId);
                child.setPreDelegationParentId(null);
                child.setUpdatedAt(LocalDateTime.now());
                reconUserRepository.save(child);
            }
        }

        delegation.setStatus("CANCELLED");
        delegation.setRestoredAt(LocalDateTime.now());
        delegation.setRestoredBy(cancelledBy);
        delegationRepository.save(delegation);

        auditLog("AUDIT_USER_DELEGATION", delegation.getDelegationId(), "UPDATE",
                "status=ACTIVE", "status=CANCELLED", cancelledBy, null, delegation.getBankId(),
                "Delegation cancelled (inactivation undone): delegatorUserId=" + delegatorUserId);

        logger.info("Delegation cancelled for delegatorUserId={} (inactivation undone)", delegatorUserId);
    }

    // ── Restore: user reactivated → children return to original parent ───────────
    @Override
    @Transactional
    public void restoreDelegationOnReactivate(Long delegatorUserId, String triggeredBy) {
        Optional<AuditUserDelegation> activeOpt = delegationRepository
                .findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, CommonConstants.ACTIVE);
        activeOpt.ifPresent(delegation -> restoreDelegation(delegation, triggeredBy));
    }

    // ── Manual restore by delegation ID (admin-tooling override, not tied to any
    //    status change) — same restore logic, just looked up by its own primary key. ──
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> restoreDelegationById(Long delegationId, String triggeredBy) {
        Optional<AuditUserDelegation> opt = delegationRepository.findById(delegationId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Delegation not found: " + delegationId, null));
        }
        AuditUserDelegation delegation = opt.get();
        if (!CommonConstants.ACTIVE.equals(delegation.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Only ACTIVE delegations can be restored.", null));
        }
        restoreDelegation(delegation, triggeredBy);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Delegation restored.", null));
    }

    private void restoreDelegation(AuditUserDelegation delegation, String triggeredBy) {
        Long delegatorUserId = delegation.getDelegatorUserId();
        Long delegateeId = delegation.getDelegateeUserId();

        // Restore children: those whose preDelegationParentId = delegatorUserId
        List<ReconUser> children = reconUserRepository.findByParentUserId(delegateeId);
        for (ReconUser child : children) {
            if (delegatorUserId.equals(child.getPreDelegationParentId())) {
                child.setParentUserId(delegatorUserId);
                child.setPreDelegationParentId(null);
                child.setUpdatedAt(LocalDateTime.now());
                reconUserRepository.save(child);
            }
        }

        // Bringing the delegation back means the delegator is back — so the inactivation that
        // triggered the hand-over has to be called off with it. Without this the scheduler still
        // fires ~30s later, marks the delegator INACTIVE and delegateOnInactivate hands the very
        // same children straight back out again, silently undoing this restore.
        //
        // Only a still-PENDING inactivation is cancelled. On the reactivate path the delegator is
        // already ACTIVE by the time we get here, so there is nothing to undo.
        reconUserRepository.findById(delegatorUserId).ifPresent(delegator -> {
            if (!"INACTIVE_PENDING".equals(delegator.getStatus())) return;
            delegator.setStatus(CommonConstants.ACTIVE);
            delegator.setInactivateScheduledAt(null);
            delegator.setInactivateScheduledBy(null);
            delegator.setUpdatedAt(LocalDateTime.now());
            delegator.setUpdatedBy(triggeredBy);
            reconUserRepository.save(delegator);
            logger.info("Delegation restore cancelled the pending inactivation of delegator {}", delegatorUserId);
        });

        delegation.setStatus("RESTORED");
        delegation.setRestoredAt(LocalDateTime.now());
        delegation.setRestoredBy(triggeredBy);
        delegationRepository.save(delegation);

        auditLog("AUDIT_USER_DELEGATION", delegation.getDelegationId(), "UPDATE",
                "status=ACTIVE", "status=RESTORED", triggeredBy, null, delegation.getBankId(),
                "Delegation restored: delegatorUserId=" + delegatorUserId);

        // Emails
        Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
        Optional<ReconUser> delegateeOpt = reconUserRepository.findById(delegateeId);

        delegatorOpt.ifPresent(delegator -> {
            try {
                emailService.sendDelegationRestoredToDelegator(delegator.getEmail(),
                        delegator.getFullName(),
                        delegateeOpt.map(ReconUser::getFullName).orElse("your deputy"));
            } catch (Exception e) {
                logger.warn("sendDelegationRestoredToDelegator failed: {}", e.getMessage());
            }
        });
        delegateeOpt.ifPresent(delegatee -> {
            try {
                emailService.sendDelegationRestoredToDelegatee(delegatee.getEmail(),
                        delegatee.getFullName(),
                        delegatorOpt.map(ReconUser::getFullName).orElse("the user"));
            } catch (Exception e) {
                logger.warn("sendDelegationRestoredToDelegatee failed: {}", e.getMessage());
            }
        });

        logger.info("Delegation restored for delegatorUserId={}", delegatorUserId);
    }

    // ── Notify delegatee: delegator BLOCK_PENDING ────────────────────────────────
    @Override
    public void notifyDelegateeBlockPending(Long delegatorUserId, String blockAt) {
        delegationRepository.findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, CommonConstants.ACTIVE)
                .ifPresent(d -> reconUserRepository.findById(d.getDelegateeUserId()).ifPresent(delegatee -> {
                    Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
                    try {
                        emailService.sendDelegatorBlockPendingToDelegatee(delegatee.getEmail(),
                                delegatee.getFullName(),
                                delegatorOpt.map(ReconUser::getFullName).orElse("the user"),
                                blockAt);
                    } catch (Exception e) {
                        logger.warn("sendDelegatorBlockPendingToDelegatee failed: {}", e.getMessage());
                    }
                }));
    }

    // ── Notify delegatee: delegator permanently BLOCKED ──────────────────────────
    @Override
    public void notifyDelegateeBlocked(Long delegatorUserId) {
        delegationRepository.findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, CommonConstants.ACTIVE)
                .ifPresent(d -> reconUserRepository.findById(d.getDelegateeUserId()).ifPresent(delegatee -> {
                    Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
                    try {
                        emailService.sendDelegatorBlockedToDelegatee(delegatee.getEmail(),
                                delegatee.getFullName(),
                                delegatorOpt.map(ReconUser::getFullName).orElse("the user"));
                    } catch (Exception e) {
                        logger.warn("sendDelegatorBlockedToDelegatee failed: {}", e.getMessage());
                    }
                }));
    }

    // ── Notify delegatee: delegator UNBLOCKED ───────────────────────────────────
    @Override
    public void notifyDelegateeUnblocked(Long delegatorUserId) {
        delegationRepository.findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(delegatorUserId, CommonConstants.ACTIVE)
                .ifPresent(d -> reconUserRepository.findById(d.getDelegateeUserId()).ifPresent(delegatee -> {
                    Optional<ReconUser> delegatorOpt = reconUserRepository.findById(delegatorUserId);
                    try {
                        emailService.sendDelegatorUnblockedToDelegatee(delegatee.getEmail(),
                                delegatee.getFullName(),
                                delegatorOpt.map(ReconUser::getFullName).orElse("the user"));
                    } catch (Exception e) {
                        logger.warn("sendDelegatorUnblockedToDelegatee failed: {}", e.getMessage());
                    }
                }));
    }

    private String resolveOrgName(ReconUser user) {
        return "ReconXpert.Ai";
    }

    private void auditLog(String table, Long recordId, String op, String oldVal, String newVal,
                           String actor, String actorType, Long bankId, String label) {
        try {
            AuditLog log = new AuditLog();
            log.setTableName(table); log.setRecordId(recordId);
            log.setOperation(op != null && op.length() > 10 ? op.substring(0, 10) : op);
            log.setOldValue(oldVal); log.setNewValue(newVal);
            log.setActorUsername(actor); log.setActorType(actorType); log.setBankId(bankId);
            log.setActionLabel(label); log.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) { logger.warn("AuditLog save failed: {}", e.getMessage()); }
    }
}
