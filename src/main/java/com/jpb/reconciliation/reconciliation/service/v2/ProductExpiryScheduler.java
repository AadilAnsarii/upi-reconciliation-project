package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.ProductExpiryConstants;
import com.jpb.reconciliation.reconciliation.entity.v2.CBankProductMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconProductCapabilityMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.CBankProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconProductCapabilityMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cascades RECON_PRODUCT_CAPABILITY_MAP + RCN_RECON_USER status changes off
 * C_BANK_PRODUCT_MAP.VALID_TO expiry, with a grace period
 * (ProductExpiryConstants.GRACE_PERIOD_MINUTES) before anything becomes permanent.
 * Every transition is written to AUDIT_LOG and, where a specific user is affected,
 * an email notification is sent.
 *
 * Runs every 30s (same cadence as StatusSchedulerService) — cheap no-op passes
 * are fine since datasets here are small.
 */
@Service
public class ProductExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ProductExpiryScheduler.class);
    private static final String SYSTEM_ACTOR = CommonConstants.SYSTEM;

    @Autowired
    private CBankProductMapRepository bankProductMapRepository;

    @Autowired
    private ReconProductCapabilityMapRepository capabilityRepository;

    @Autowired
    private ReconBankMasterRepository reconBankMasterRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processProductExpiryAndGrace() {
        try {
            detectNewlyExpiredProducts();
            detectRenewedProducts();
            finalizeElapsedGracePeriods();
        } catch (Exception e) {
            logger.error("[PRODUCT-EXPIRY] Scheduler pass failed: {}", e.getMessage(), e);
        }
    }

    // ── Step 1: ACTIVE product whose VALID_TO has passed -> EXPIRED, suspend its capabilities ──
    private void detectNewlyExpiredProducts() {
        LocalDate today = LocalDate.now();
        List<CBankProductMap> allMappings = bankProductMapRepository.findAll();
        for (CBankProductMap mapping : allMappings) {
            if (!CommonConstants.ACTIVE.equals(mapping.getStatus())) continue;
            if (mapping.getValidTo() == null || !mapping.getValidTo().isBefore(today)) continue;

            mapping.setStatus("EXPIRED");
            bankProductMapRepository.save(mapping);
            logger.info("[PRODUCT-EXPIRY] Product {} expired for bankId={} (validTo={})",
                    mapping.getProductId(), mapping.getBankId(), mapping.getValidTo());
            auditLogService.log("C_BANK_PRODUCT_MAP", mapping.getId(), "EXPIRE", null, SYSTEM_ACTOR,
                    SYSTEM_ACTOR, "PRODUCT_MAP", mapping.getBankId(), CommonConstants.ACTIVE, "EXPIRED",
                    "Product Expired", SYSTEM_ACTOR, "validTo=" + mapping.getValidTo());

            suspendCapabilitiesForProduct(mapping.getProductId());
            checkAndApplyBankWideHold(mapping.getBankId());
        }
    }

    private void suspendCapabilitiesForProduct(Long productId) {
        List<ReconProductCapabilityMap> active = capabilityRepository.findByProductIdAndStatus(productId, CommonConstants.ACTIVE);
        for (ReconProductCapabilityMap cap : active) {
            cap.setStatus(ProductExpiryConstants.CAPABILITY_STATUS_SUSPENDED);
            cap.setSuspendedAt(LocalDateTime.now());
            capabilityRepository.save(cap);
            auditLogService.log("RECON_PRODUCT_CAPABILITY_MAP", cap.getCapabilityId(), "SUSPEND", null, SYSTEM_ACTOR,
                    SYSTEM_ACTOR, "CAPABILITY", null, CommonConstants.ACTIVE, ProductExpiryConstants.CAPABILITY_STATUS_SUSPENDED,
                    "Product Expired", SYSTEM_ACTOR, "productId=" + productId);
        }
        if (!active.isEmpty()) {
            logger.info("[PRODUCT-EXPIRY] Suspended {} capability grant(s) for productId={}", active.size(), productId);
        }
    }

    private void checkAndApplyBankWideHold(Long bankId) {
        List<CBankProductMap> bankMappings = bankProductMapRepository.findByBankId(bankId);
        boolean anyStillActive = bankMappings.stream().anyMatch(m -> CommonConstants.ACTIVE.equals(m.getStatus()));
        if (anyStillActive) return;

        reconBankMasterRepository.findById(bankId).ifPresent(bank -> {
            if (bank.getAllProductsExpiredAt() != null) return; // already applied
            bank.setAllProductsExpiredAt(LocalDateTime.now());
            reconBankMasterRepository.save(bank);

            List<ReconUser> users = reconUserRepository.findByBankId(bankId);
            for (ReconUser user : users) {
                if (ProductExpiryConstants.USER_STATUS_PRODUCT_EXPIRY_HOLD.equals(user.getStatus())) continue;
                String previousStatus = user.getStatus();
                user.setPreProductHoldStatus(previousStatus);
                user.setStatus(ProductExpiryConstants.USER_STATUS_PRODUCT_EXPIRY_HOLD);
                reconUserRepository.save(user);

                auditLogService.log("RCN_RECON_USER", user.getUserId(), "HOLD", null, SYSTEM_ACTOR,
                        SYSTEM_ACTOR, "USER", bankId, previousStatus, ProductExpiryConstants.USER_STATUS_PRODUCT_EXPIRY_HOLD,
                        "All Products Expired", SYSTEM_ACTOR, null);

                if (user.getEmail() != null) {
                    emailService.sendProductExpiryHoldNotification(
                            user.getEmail(), user.getFullName(), ProductExpiryConstants.GRACE_PERIOD_MINUTES);
                }
            }
            logger.info("[PRODUCT-EXPIRY] Bank {} has zero active products — {} user(s) put on PRODUCT_EXPIRY_HOLD",
                    bankId, users.size());
        });
    }

    // ── Step 2: EXPIRED product whose VALID_TO was extended back into the future -> restore ──
    private void detectRenewedProducts() {
        LocalDate today = LocalDate.now();
        List<CBankProductMap> allMappings = bankProductMapRepository.findAll();
        for (CBankProductMap mapping : allMappings) {
            if (!"EXPIRED".equals(mapping.getStatus())) continue;
            if (mapping.getValidTo() != null && mapping.getValidTo().isBefore(today)) continue; // still expired

            mapping.setStatus(CommonConstants.ACTIVE);
            bankProductMapRepository.save(mapping);
            logger.info("[PRODUCT-EXPIRY] Product {} renewed for bankId={} (validTo={})",
                    mapping.getProductId(), mapping.getBankId(), mapping.getValidTo());
            auditLogService.log("C_BANK_PRODUCT_MAP", mapping.getId(), "RENEW", null, SYSTEM_ACTOR,
                    SYSTEM_ACTOR, "PRODUCT_MAP", mapping.getBankId(), "EXPIRED", CommonConstants.ACTIVE,
                    "Product Renewed", SYSTEM_ACTOR, "validTo=" + mapping.getValidTo());

            restoreCapabilitiesForProduct(mapping.getProductId());
            restoreBankWideHoldIfInGrace(mapping.getBankId());
        }
    }

    private void restoreCapabilitiesForProduct(Long productId) {
        List<ReconProductCapabilityMap> suspended = capabilityRepository
                .findByProductIdAndStatus(productId, ProductExpiryConstants.CAPABILITY_STATUS_SUSPENDED);
        for (ReconProductCapabilityMap cap : suspended) {
            cap.setStatus(CommonConstants.ACTIVE);
            cap.setSuspendedAt(null);
            capabilityRepository.save(cap);
            auditLogService.log("RECON_PRODUCT_CAPABILITY_MAP", cap.getCapabilityId(), "RESTORE", null, SYSTEM_ACTOR,
                    SYSTEM_ACTOR, "CAPABILITY", null, ProductExpiryConstants.CAPABILITY_STATUS_SUSPENDED, CommonConstants.ACTIVE,
                    "Product Renewed", SYSTEM_ACTOR, "productId=" + productId);
        }
        if (!suspended.isEmpty()) {
            logger.info("[PRODUCT-EXPIRY] Restored {} capability grant(s) for productId={}", suspended.size(), productId);
        }
    }

    // Only restore if this bank's hold is still within the grace window (i.e. not
    // yet permanently finalized — finalizeElapsedGracePeriods() clears the anchor
    // timestamp once grace elapses, which is what stops a late renewal from
    // auto-restoring already-finalized users).
    private void restoreBankWideHoldIfInGrace(Long bankId) {
        reconBankMasterRepository.findById(bankId).ifPresent(bank -> {
            if (bank.getAllProductsExpiredAt() == null) return;

            List<CBankProductMap> bankMappings = bankProductMapRepository.findByBankId(bankId);
            boolean anyActive = bankMappings.stream().anyMatch(m -> CommonConstants.ACTIVE.equals(m.getStatus()));
            if (!anyActive) return;

            bank.setAllProductsExpiredAt(null);
            reconBankMasterRepository.save(bank);

            List<ReconUser> users = reconUserRepository.findByBankId(bankId);
            int restored = 0;
            for (ReconUser user : users) {
                if (!ProductExpiryConstants.USER_STATUS_PRODUCT_EXPIRY_HOLD.equals(user.getStatus())) continue;
                String restoredStatus = user.getPreProductHoldStatus() != null ? user.getPreProductHoldStatus() : CommonConstants.ACTIVE;
                user.setStatus(restoredStatus);
                user.setPreProductHoldStatus(null);
                reconUserRepository.save(user);
                restored++;

                auditLogService.log("RCN_RECON_USER", user.getUserId(), "RESTORE", null, SYSTEM_ACTOR,
                        SYSTEM_ACTOR, "USER", bankId, ProductExpiryConstants.USER_STATUS_PRODUCT_EXPIRY_HOLD, restoredStatus,
                        "Product Renewed Within Grace", SYSTEM_ACTOR, null);

                if (user.getEmail() != null) {
                    emailService.sendProductExpiryRestoredNotification(user.getEmail(), user.getFullName());
                }
            }
            logger.info("[PRODUCT-EXPIRY] Bank {} renewed within grace period — {} user(s) restored", bankId, restored);
        });
    }

    // ── Step 3: grace period elapsed without renewal -> make it permanent ──
    private void finalizeElapsedGracePeriods() {
        LocalDateTime now = LocalDateTime.now();

        List<ReconProductCapabilityMap> suspended = capabilityRepository.findByStatus(ProductExpiryConstants.CAPABILITY_STATUS_SUSPENDED);
        for (ReconProductCapabilityMap cap : suspended) {
            if (cap.getSuspendedAt() == null) continue;
            long minutesElapsed = ChronoUnit.MINUTES.between(cap.getSuspendedAt(), now);
            if (minutesElapsed < ProductExpiryConstants.GRACE_PERIOD_MINUTES) continue;

            cap.setStatus("REVOKED");
            cap.setRevokedAt(now);
            cap.setRevokeReason(ProductExpiryConstants.REVOKE_REASON_PRODUCT_DEACTIVATED);
            capabilityRepository.save(cap);
            auditLogService.log("RECON_PRODUCT_CAPABILITY_MAP", cap.getCapabilityId(), "REVOKE", null, SYSTEM_ACTOR,
                    SYSTEM_ACTOR, "CAPABILITY", null, ProductExpiryConstants.CAPABILITY_STATUS_SUSPENDED, "REVOKED",
                    "Grace Period Elapsed", SYSTEM_ACTOR, null);
            logger.info("[PRODUCT-EXPIRY] Grace period elapsed — capabilityId={} permanently REVOKED", cap.getCapabilityId());
        }

        List<ReconBankMaster> banksWithHold = reconBankMasterRepository.findAll().stream()
                .filter(b -> b.getAllProductsExpiredAt() != null)
                .collect(java.util.stream.Collectors.toList());
        for (ReconBankMaster bank : banksWithHold) {
            long minutesElapsed = ChronoUnit.MINUTES.between(bank.getAllProductsExpiredAt(), now);
            if (minutesElapsed < ProductExpiryConstants.GRACE_PERIOD_MINUTES) continue;

            List<ReconUser> users = reconUserRepository.findByBankId(bank.getBankId());
            int finalized = 0;
            for (ReconUser user : users) {
                if (!ProductExpiryConstants.USER_STATUS_PRODUCT_EXPIRY_HOLD.equals(user.getStatus())) continue;
                user.setStatus(CommonConstants.INACTIVE);
                user.setPreProductHoldStatus(null);
                reconUserRepository.save(user);
                finalized++;

                auditLogService.log("RCN_RECON_USER", user.getUserId(), "FINALIZE", null, SYSTEM_ACTOR,
                        SYSTEM_ACTOR, "USER", bank.getBankId(), ProductExpiryConstants.USER_STATUS_PRODUCT_EXPIRY_HOLD, CommonConstants.INACTIVE,
                        "Grace Period Elapsed", SYSTEM_ACTOR, null);

                if (user.getEmail() != null) {
                    emailService.sendProductExpiryFinalizedNotification(user.getEmail(), user.getFullName());
                }
            }
            // Clear the anchor so a later renewal does NOT auto-restore — once grace
            // has elapsed, restoration requires a manual admin action.
            bank.setAllProductsExpiredAt(null);
            reconBankMasterRepository.save(bank);
            logger.info("[PRODUCT-EXPIRY] Grace period elapsed for bankId={} — {} user(s) permanently INACTIVE",
                    bank.getBankId(), finalized);
        }
    }
}
