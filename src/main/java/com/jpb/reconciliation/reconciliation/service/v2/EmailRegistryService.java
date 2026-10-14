package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for "is this email already taken?" across every onboarding / user-create
 * path (bank create, branch create, add user, Kal-admin create) and the /check-email endpoints.
 *
 * Business rule: a BLOCKED account is treated as effectively deleted, so its email may be reused for
 * a brand-new account. Every OTHER status (ACTIVE, INACTIVE, REQUEST, REJECTED, VERIFIED, …) still
 * blocks reuse. This mirrors the exception already applied on the replacement path
 * (AuditReplacementServiceImpl) and now makes the behaviour consistent everywhere.
 *
 * Because RCN_RECON_USER.EMAIL has no unique index but findByEmail() is single-valued, reusing a
 * blocked account's email would create two rows with the same email and break login / forgot-password
 * lookups. To prevent that, {@link #releaseBlockedHolders(String)} tombstones the blocked holder's
 * email at reuse time, keeping the live email unique.
 */
@Service
public class EmailRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(EmailRegistryService.class);
    private static final String BLOCKED = "BLOCKED";
    private static final String TOMBSTONE_PREFIX = "blocked+"; // e.g. blocked+244+aadil@x.com

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private ReconBankMasterRepository reconBankMasterRepository;

    /** Normalised (trimmed, lower-cased) form used for every lookup/compare. Null-safe. */
    public String norm(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /**
     * True when a NON-blocked login account or bank/branch contact already holds this email — i.e.
     * the email may NOT be reused. A blocked-only match returns false (reuse allowed).
     */
    public boolean isActivelyRegistered(String email) {
        String e = norm(email);
        if (e == null || e.isEmpty()) return false;
        return reconUserRepository.existsByEmailAndStatusNot(e, BLOCKED)
                || reconBankMasterRepository.existsByEmailAndStatusNot(e, BLOCKED);
    }

    /**
     * Release (tombstone) the email of any BLOCKED user / bank-contact row still holding it, so the
     * brand-new account about to take this email remains the single live holder. Call this ONLY
     * after {@link #isActivelyRegistered(String)} has returned false, immediately before persisting
     * the new account. No-op when nothing blocked holds the email.
     */
    public void releaseBlockedHolders(String email) {
        String e = norm(email);
        if (e == null || e.isEmpty()) return;

        for (ReconUser u : reconUserRepository.findAllByEmail(e)) {
            if (BLOCKED.equals(u.getStatus())) {
                u.setEmail(TOMBSTONE_PREFIX + u.getUserId() + "+" + e);
                reconUserRepository.save(u);
                logger.info("Released blocked user {} email for reuse (tombstoned): {}", u.getUserId(), e);
            }
        }
        for (ReconBankMaster b : reconBankMasterRepository.findAllByEmail(e)) {
            if (BLOCKED.equals(b.getStatus())) {
                b.setEmail(TOMBSTONE_PREFIX + b.getBankId() + "+" + e);
                reconBankMasterRepository.save(b);
                logger.info("Released blocked bank {} contact email for reuse (tombstoned): {}", b.getBankId(), e);
            }
        }
    }
}
