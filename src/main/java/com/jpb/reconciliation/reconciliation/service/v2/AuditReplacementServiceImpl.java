package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditReplacementServiceImpl implements AuditReplacementService {

    private static final Logger logger = LoggerFactory.getLogger(AuditReplacementServiceImpl.class);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired private AuditReplacementRepository auditReplacementRepository;
    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private ReconBankMasterRepository reconBankMasterRepository;
    @Autowired private ReconPasswordManagerRepository reconPasswordManagerRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ── Schedule Pending — stores intent, replacement user NOT created yet ──────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> schedulePendingReplacement(
            Long originalUserId, String pendingEmail, String pendingFullName,
            String pendingMobile, String orderedBy, String reason, String scheduledBy) {

        Optional<ReconUser> originalOpt = reconUserRepository.findById(originalUserId);
        if (!originalOpt.isPresent()) {
            return fail("Original user not found: " + originalUserId);
        }
        ReconUser original = originalOpt.get();

        // Block duplicate pending
        if (auditReplacementRepository.existsByOriginalUserIdAndStatus(originalUserId, "PENDING")) {
            return fail("A pending replacement already exists for this user.");
        }
        if (auditReplacementRepository.existsByOriginalUserIdAndStatus(originalUserId, CommonConstants.ACTIVE)) {
            return fail("An active replacement already exists for this user.");
        }

        // Email uniqueness check (ignore BLOCKED users and former RESTORED replacements)
        Optional<ReconUser> emailOpt = reconUserRepository.findByEmail(pendingEmail.trim().toLowerCase());
        if (emailOpt.isPresent()) {
            boolean isFormerReplacement = auditReplacementRepository
                    .existsByReplacementUserIdAndStatus(emailOpt.get().getUserId(), "RESTORED");
            if (!isFormerReplacement && !"BLOCKED".equals(emailOpt.get().getStatus())) {
                return fail("Email '" + pendingEmail + "' is already registered.");
            }
        }

        AuditReplacement r = new AuditReplacement();
        r.setBankId(original.getBankId());
        r.setOriginalUserId(originalUserId);
        r.setReplacementUserId(0L); // sentinel: no replacement created yet
        r.setEntityType(original.getUserType());
        r.setStatus("PENDING");
        r.setReason(reason);
        r.setPendingEmail(pendingEmail.trim().toLowerCase());
        r.setPendingFullName(pendingFullName != null ? pendingFullName.trim() : null);
        r.setPendingMobile(pendingMobile);
        r.setPendingOrderedBy(orderedBy);
        r.setReplacedBy(scheduledBy);
        r.setReplacedAt(LocalDateTime.now());
        r.setCreatedAt(LocalDateTime.now());
        AuditReplacement saved = auditReplacementRepository.save(r);

        auditLog("AUDIT_REPLACEMENT", saved.getReplacementId(), "CREATE", null,
                "status=PENDING,originalUserId=" + originalUserId,
                scheduledBy, original.getUserType(), original.getBankId(),
                "Replacement scheduled for user " + original.getUsername());

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS,
                "Replacement scheduled. Will take effect when user goes INACTIVE.", Collections.singletonList(saved)));
    }

    // ── Cancel Pending — called on undo-inactivate ───────────────────────────────
    @Override
    @Transactional
    public void cancelPendingReplacement(Long originalUserId) {
        Optional<AuditReplacement> opt = auditReplacementRepository
                .findByOriginalUserIdAndStatus(originalUserId, "PENDING");
        opt.ifPresent(r -> {
            r.setStatus("CANCELLED");
            auditReplacementRepository.save(r);
            logger.info("PENDING replacement cancelled for originalUserId={}", originalUserId);
        });
    }

    // ── Finalize — called by scheduler when original goes INACTIVE ───────────────
    @Override
    @Transactional
    public void finalizePendingReplacement(Long originalUserId) {
        Optional<AuditReplacement> pendingOpt = auditReplacementRepository
                .findByOriginalUserIdAndStatus(originalUserId, "PENDING");
        if (!pendingOpt.isPresent()) return;

        AuditReplacement pending = pendingOpt.get();
        Optional<ReconUser> originalOpt = reconUserRepository.findById(originalUserId);
        if (!originalOpt.isPresent()) {
            pending.setStatus("CANCELLED");
            auditReplacementRepository.save(pending);
            return;
        }
        ReconUser original = originalOpt.get();

        String tempPassword = generatePassword(10);
        String username = deriveUsername(pending.getPendingEmail(),
                pending.getPendingFullName() != null ? pending.getPendingFullName() : "");

        // Check if former replacement with same email exists — reuse it
        Optional<ReconUser> existingOpt = reconUserRepository.findByEmail(pending.getPendingEmail());
        boolean reuseExisting = existingOpt.isPresent() &&
                auditReplacementRepository.existsByReplacementUserIdAndStatus(existingOpt.get().getUserId(), "RESTORED");

        ReconUser replacement;
        if (reuseExisting) {
            replacement = existingOpt.get();
            username = replacement.getUsername();
            replacement.setPasswordHash(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus("REQUEST");
            replacement.setUpdatedAt(LocalDateTime.now());
            replacement.setUpdatedBy(pending.getReplacedBy());
        } else {
            replacement = new ReconUser();
            replacement.setBankId(original.getBankId());
            replacement.setUsername(username);
            replacement.setFullName(pending.getPendingFullName() != null ? pending.getPendingFullName() : username);
            replacement.setEmail(pending.getPendingEmail());
            replacement.setMobileNumber(pending.getPendingMobile());
            replacement.setUserType(original.getUserType());
            replacement.setRoleId(original.getRoleId());
            replacement.setParentUserId(original.getParentUserId());
            replacement.setPasswordHash(passwordEncoder.encode(tempPassword));
            replacement.setPasswordSet(0);
            replacement.setStatus("REQUEST");
            replacement.setApprovedYn(CommonConstants.INACTIVE_N);
            replacement.setCreatedBy(pending.getReplacedBy());
            replacement.setCreatedAt(LocalDateTime.now());
        }
        ReconUser savedReplacement = reconUserRepository.saveAndFlush(replacement);

        // Save password history
        ReconPasswordManager pm = new ReconPasswordManager();
        pm.setReconUser(savedReplacement);
        pm.setUserPassword(savedReplacement.getPasswordHash());
        pm.setCreatedAt(LocalDateTime.now());
        pm.setCreatedBy(pending.getReplacedBy());
        pm.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pm);

        // Transfer children parentId from original → replacement
        List<ReconUser> children = reconUserRepository.findByParentUserId(original.getUserId());
        for (ReconUser child : children) {
            child.setParentUserId(savedReplacement.getUserId());
            child.setUpdatedAt(LocalDateTime.now());
            reconUserRepository.save(child);
        }

        pending.setReplacementUserId(savedReplacement.getUserId());
        pending.setStatus(CommonConstants.ACTIVE);
        auditReplacementRepository.save(pending);

        auditLog("AUDIT_REPLACEMENT", pending.getReplacementId(), "FINALIZE",
                "status=PENDING", "status=ACTIVE,replacementUserId=" + savedReplacement.getUserId(),
                CommonConstants.SYSTEM, original.getUserType(), original.getBankId(),
                "Replacement finalized: " + original.getUsername() + " → " + username);

        // Resolve bank/branch context once — used for entity code + verify link
        boolean isBranch = false;
        String entityCode = CommonConstants.SYSTEM;
        if (original.getBankId() != null) {
            Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findById(original.getBankId());
            if (bankOpt.isPresent()) {
                isBranch = bankOpt.get().getParentBankId() != null;
                entityCode = bankOpt.get().getBankCode();
            }
        }

        // Emails
        try {
            String orderedByFormatted = formatName(pending.getReplacedBy());
            emailService.sendReplacementOutgoingNotification(
                    original.getEmail(), original.getFullName(), entityCode,
                    orderedByFormatted, pending.getReason(),
                    savedReplacement.getFullName(), savedReplacement.getEmail(),
                    pending.getPendingOrderedBy());
        } catch (Exception e) {
            logger.warn("Outgoing replacement email failed: {}", e.getMessage());
        }

        try {
            String verifyPage = isBranch ? "/branch-verify-email" : "/verify-email";
            String verifyLink = frontendUrl + verifyPage + "?bankCode=" + entityCode + "&username=" + username;
            if ("BANK_ADMIN".equals(original.getUserType()) || "BRANCH_ADMIN".equals(original.getUserType())) {
                emailService.sendReplacementAdminWelcome(savedReplacement.getEmail(),
                        savedReplacement.getFullName(), entityCode,
                        username, tempPassword, verifyLink);
            } else {
                emailService.sendUserWelcomeReplacement(savedReplacement.getEmail(),
                        savedReplacement.getFullName(), entityCode,
                        isBranch ? "Branch Code" : "Bank Code", username, tempPassword, verifyLink,
                        "You have been assigned as a replacement user on ReconXpert.Ai.");
            }
        } catch (Exception e) {
            logger.warn("Welcome replacement email failed: {}", e.getMessage());
        }

        logger.info("Replacement finalized: {} → {}", original.getUsername(), username);
    }

    // ── On BLOCKED permanently — replacement becomes permanent ───────────────────
    @Override
    @Transactional
    public void onOriginalBlocked(Long originalUserId) {
        Optional<AuditReplacement> opt = auditReplacementRepository
                .findByOriginalUserIdAndStatus(originalUserId, CommonConstants.ACTIVE);
        opt.ifPresent(r -> {
            r.setStatus("FINALIZED");
            r.setFinalizedAt(LocalDateTime.now());
            r.setFinalizedBy(CommonConstants.SYSTEM);
            auditReplacementRepository.save(r);

            reconUserRepository.findById(r.getReplacementUserId()).ifPresent(rep -> {
                try {
                    emailService.sendReplacementBecamePermanent(rep.getEmail(), rep.getFullName());
                } catch (Exception e) {
                    logger.warn("sendReplacementBecamePermanent email failed: {}", e.getMessage());
                }

                // Mirror old backend (MainBank/BranchBank contact sync in BlockScheduleServiceImpl):
                // once the replacement is permanent, the institution's own PRIMARY contact row
                // is overwritten with the replacement's details, so RECON_BANK_MASTER reflects
                // who is actually running the bank/branch going forward.
                try {
                    reconUserRepository.findById(originalUserId).ifPresent(original -> {
                        if (original.getBankId() == null) return;
                        reconBankMasterRepository.findById(original.getBankId()).ifPresent(bank -> {
                            bank.setFullName(r.getPendingFullName() != null ? r.getPendingFullName() : rep.getUsername());
                            bank.setEmail(rep.getEmail());
                            bank.setMobileNumber(r.getPendingMobile() != null ? r.getPendingMobile() : rep.getMobileNumber());
                            bank.setBankAdminUsername(rep.getUsername());
                            bank.setUpdatedAt(LocalDateTime.now());
                            bank.setUpdatedBy(CommonConstants.SYSTEM);
                            reconBankMasterRepository.save(bank);
                            logger.info("[BLOCK-SYNC] RECON_BANK_MASTER {} contact updated to replacement: {}",
                                    bank.getBankCode(), rep.getUsername());
                        });
                    });
                } catch (Exception e) {
                    logger.warn("RECON_BANK_MASTER contact sync to replacement failed for originalUserId {}: {}",
                            originalUserId, e.getMessage());
                }
            });
            logger.info("Replacement FINALIZED (original BLOCKED): originalUserId={}", originalUserId);
        });
    }

    /**
     * A replacement is the person taking OVER from the blocked original — they are a distinct
     * individual who must survive the block, not be swept up by it. onOriginalBlocked() even makes
     * them the permanent admin, so blocking them in the same cascade would leave the institution
     * with a blocked permanent admin. Every block cascade skips them via this check.
     */
    @Override
    public boolean isActiveReplacementUser(Long userId) {
        return userId != null && auditReplacementRepository
                .findByReplacementUserIdAndStatus(userId, CommonConstants.ACTIVE).isPresent();
    }

    // ── On Original Reactivated — replacement tenure ends ───────────────────────
    /**
     * The original's reactivation has been SCHEDULED (they are ACTIVE_PENDING). Their replacement is
     * on the way out, so it is moved to INACTIVE_PENDING right now — the two countdowns then run in
     * parallel and both accounts read as pending, instead of the replacement staying ACTIVE until the
     * moment the original lands.
     *
     * Deliberately NO inactivateScheduledAt: the per-user scheduler skips rows without one, so it
     * won't treat this as an ordinary inactivation and fire replacement/delegation handling on the
     * replacement itself. The final flip to INACTIVE stays with onOriginalReactivated(), which runs
     * when the original actually becomes ACTIVE.
     */
    @Override
    @Transactional
    public void onOriginalReactivateScheduled(Long originalUserId) {
        auditReplacementRepository.findByOriginalUserIdAndStatus(originalUserId, CommonConstants.ACTIVE)
            .ifPresent(r -> reconUserRepository.findById(r.getReplacementUserId()).ifPresent(rep -> {
                if (!CommonConstants.ACTIVE.equals(rep.getStatus())) return;
                rep.setStatus("INACTIVE_PENDING");
                rep.setUpdatedAt(LocalDateTime.now());
                reconUserRepository.save(rep);
                logger.info("Original {} reactivation scheduled — replacement {} moved to INACTIVE_PENDING",
                        originalUserId, rep.getUserId());
            }));
    }

    /** The scheduled reactivation was cancelled, so the replacement is staying — put them back. */
    @Override
    @Transactional
    public void onOriginalReactivateCancelled(Long originalUserId) {
        auditReplacementRepository.findByOriginalUserIdAndStatus(originalUserId, CommonConstants.ACTIVE)
            .ifPresent(r -> reconUserRepository.findById(r.getReplacementUserId()).ifPresent(rep -> {
                if (!"INACTIVE_PENDING".equals(rep.getStatus())) return;
                rep.setStatus(CommonConstants.ACTIVE);
                rep.setInactivateScheduledAt(null);
                rep.setInactivateScheduledBy(null);
                rep.setUpdatedAt(LocalDateTime.now());
                reconUserRepository.save(rep);
                logger.info("Original {} reactivation cancelled — replacement {} restored to ACTIVE",
                        originalUserId, rep.getUserId());
            }));
    }

    @Override
    @Transactional
    public void onOriginalReactivated(Long originalUserId) {
        Optional<AuditReplacement> opt = auditReplacementRepository
                .findByOriginalUserIdAndStatus(originalUserId, CommonConstants.ACTIVE);
        opt.ifPresent(r -> {
            Optional<ReconUser> repOpt = reconUserRepository.findById(r.getReplacementUserId());
            repOpt.ifPresent(rep -> {
                // Transfer children back to original
                List<ReconUser> children = reconUserRepository.findByParentUserId(rep.getUserId());
                for (ReconUser child : children) {
                    child.setParentUserId(originalUserId);
                    child.setUpdatedAt(LocalDateTime.now());
                    reconUserRepository.save(child);
                }
                rep.setStatus(CommonConstants.INACTIVE);
                rep.setUpdatedAt(LocalDateTime.now());
                reconUserRepository.save(rep);

                try {
                    emailService.sendReplacementTenureEnded(rep.getEmail(), rep.getFullName());
                } catch (Exception e) {
                    logger.warn("sendReplacementTenureEnded email failed: {}", e.getMessage());
                }
            });

            r.setStatus("RESTORED");
            r.setRestoredAt(LocalDateTime.now());
            r.setRestoredBy(CommonConstants.SYSTEM);
            auditReplacementRepository.save(r);
            logger.info("Replacement RESTORED (original reactivated): originalUserId={}", originalUserId);
        });
    }

    // ── Existing CRUD (kept for API compatibility) ───────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createReplacement(AuditReplacement replacement, String replacedBy) {
        Optional<ReconUser> originalOpt = reconUserRepository.findById(replacement.getOriginalUserId());
        if (!originalOpt.isPresent()) return fail("Original user not found.");
        Optional<ReconUser> replacementOpt = reconUserRepository.findById(replacement.getReplacementUserId());
        if (!replacementOpt.isPresent()) return fail("Replacement user not found.");

        if (auditReplacementRepository.existsByOriginalUserIdAndStatus(replacement.getOriginalUserId(), CommonConstants.ACTIVE)) {
            return fail("An active replacement already exists for this user.");
        }

        ReconUser original = originalOpt.get();
        ReconUser rep = replacementOpt.get();
        original.setStatus(CommonConstants.INACTIVE); original.setUpdatedAt(LocalDateTime.now()); original.setUpdatedBy(replacedBy);
        rep.setStatus(CommonConstants.ACTIVE); rep.setUpdatedAt(LocalDateTime.now()); rep.setUpdatedBy(replacedBy);
        reconUserRepository.save(original);
        reconUserRepository.save(rep);

        replacement.setStatus(CommonConstants.ACTIVE);
        replacement.setReplacedAt(LocalDateTime.now());
        replacement.setReplacedBy(replacedBy);
        replacement.setCreatedAt(LocalDateTime.now());
        AuditReplacement saved = auditReplacementRepository.save(replacement);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS, "Replacement initiated.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getReplacementsByBankId(Long bankId) {
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Fetched.",
                auditReplacementRepository.findByBankId(bankId)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getReplacementsByOriginalUser(Long originalUserId) {
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Fetched.",
                auditReplacementRepository.findByOriginalUserId(originalUserId)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> finalizeReplacement(Long replacementId, String finalizedBy) {
        Optional<AuditReplacement> opt = auditReplacementRepository.findById(replacementId);
        if (!opt.isPresent()) return fail("Replacement not found: " + replacementId);
        AuditReplacement r = opt.get();
        if (!CommonConstants.ACTIVE.equals(r.getStatus())) return fail("Only ACTIVE replacements can be finalized.");
        r.setStatus("FINALIZED"); r.setFinalizedAt(LocalDateTime.now()); r.setFinalizedBy(finalizedBy);
        auditReplacementRepository.save(r);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Replacement finalized.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> restoreReplacement(Long replacementId, String restoredBy) {
        Optional<AuditReplacement> opt = auditReplacementRepository.findById(replacementId);
        if (!opt.isPresent()) return fail("Replacement not found: " + replacementId);
        AuditReplacement r = opt.get();
        if (!CommonConstants.ACTIVE.equals(r.getStatus())) return fail("Only ACTIVE replacements can be restored.");

        reconUserRepository.findById(r.getOriginalUserId()).ifPresent(u -> {
            u.setStatus(CommonConstants.ACTIVE); u.setUpdatedAt(LocalDateTime.now()); u.setUpdatedBy(restoredBy);
            reconUserRepository.save(u);
        });
        reconUserRepository.findById(r.getReplacementUserId()).ifPresent(rep -> {
            // finalizePendingReplacement() moved the original's team under the replacement.
            // Undoing the replacement has to move it back, or the whole team keeps reporting to
            // an INACTIVE user. onOriginalReactivated() — the other way out of an ACTIVE
            // replacement — already does this; this path did not.
            for (ReconUser child : reconUserRepository.findByParentUserId(rep.getUserId())) {
                child.setParentUserId(r.getOriginalUserId());
                child.setUpdatedAt(LocalDateTime.now());
                reconUserRepository.save(child);
            }
            rep.setStatus(CommonConstants.INACTIVE); rep.setUpdatedAt(LocalDateTime.now()); rep.setUpdatedBy(restoredBy);
            reconUserRepository.save(rep);

            try {
                emailService.sendReplacementTenureEnded(rep.getEmail(), rep.getFullName());
            } catch (Exception e) {
                logger.warn("sendReplacementTenureEnded email failed: {}", e.getMessage());
            }
        });

        r.setStatus("RESTORED"); r.setRestoredAt(LocalDateTime.now()); r.setRestoredBy(restoredBy);
        auditReplacementRepository.save(r);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Replacement restored.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getActiveReplacementForUser(Long originalUserId) {
        Optional<AuditReplacement> opt = auditReplacementRepository
                .findByOriginalUserIdAndStatus(originalUserId, CommonConstants.ACTIVE);
        if (!opt.isPresent()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RestWithStatusList(CommonConstants.FAILURE, "No active replacement found.", null));
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Found.", Collections.singletonList(opt.get())));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────
    private String generatePassword(int length) {
        int digits = 1000 + RNG.nextInt(9000); // 4-digit number: 1000–9999
        return "Recon@" + digits;
    }

    private String deriveUsername(String email, String fullName) {
        String base;
        if (fullName != null && fullName.trim().length() > 0) {
            String[] parts = fullName.trim().toLowerCase().replaceAll("[^a-z ]", "").split("\\s+");
            base = parts.length > 1 ? parts[0] + "." + parts[parts.length - 1] : parts[0];
        } else {
            base = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9.]", "");
        }
        String candidate = base;
        int suffix = 1;
        while (reconUserRepository.existsByUsername(candidate)) candidate = base + suffix++;
        return candidate;
    }


    private String formatName(String username) {
        if (username == null) return "";
        String[] parts = username.split("[.\\-_]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : username;
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

    private ResponseEntity<RestWithStatusList> fail(String msg) {
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.FAILURE, msg, null));
    }
}
