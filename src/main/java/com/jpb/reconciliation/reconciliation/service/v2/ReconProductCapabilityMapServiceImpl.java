package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconProductCapabilityMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconProductCapabilityMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconProductMasterRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReconProductCapabilityMapServiceImpl implements ReconProductCapabilityMapService {

    private static final Logger logger = LoggerFactory.getLogger(ReconProductCapabilityMapServiceImpl.class);

    private static final Set<String> ADMIN_USER_TYPES = new HashSet<>();
    static {
        ADMIN_USER_TYPES.add("KAL_ADMIN");
        ADMIN_USER_TYPES.add("BANK_ADMIN");
        ADMIN_USER_TYPES.add("BRANCH_ADMIN");
    }

    @Autowired
    private ReconProductCapabilityMapRepository capabilityRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconProductMasterRepository reconProductMasterRepository;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> grantCapability(Long productId, Long userId, String capabilityType,
            String actionType, boolean canDelegate, String grantedByUsername) {

        Optional<ReconUser> grantorOpt = reconUserRepository.findByUsername(grantedByUsername);
        Optional<ReconUser> granteeOpt = reconUserRepository.findById(userId);
        if (!grantorOpt.isPresent() || !granteeOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "Grantor or grantee user not found.", null), HttpStatus.NOT_FOUND);
        }
        Long grantedByUserId = grantorOpt.get().getUserId();

        boolean grantorIsAdmin = ADMIN_USER_TYPES.contains(grantorOpt.get().getUserType());

        if ("CHECKER".equals(capabilityType)) {
            // Segregation of duties: a Maker can never appoint a Checker for the same
            // product+action — only an Admin (root authority) may grant CHECKER capability.
            if (!grantorIsAdmin) {
                return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE,
                        "Only an Admin can grant Checker capability — a Maker cannot appoint their own Checker.", null),
                        HttpStatus.FORBIDDEN);
            }
        } else if ("MAKER".equals(capabilityType) && !grantorIsAdmin) {
            // Non-admin grantor delegating Maker capability: must hold an active,
            // delegate-eligible Maker grant of their own for this exact product+action,
            // and cannot pass the delegate-right onward themselves.
            Optional<ReconProductCapabilityMap> ownGrant = capabilityRepository
                    .findByProductIdAndUserIdAndActionTypeAndCapabilityTypeAndStatus(
                            productId, grantedByUserId, actionType, "MAKER", CommonConstants.ACTIVE);
            if (!ownGrant.isPresent() || !CommonConstants.ACTIVE_Y.equals(ownGrant.get().getCanDelegate())) {
                return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE,
                        "You do not hold delegable Maker capability for this product/action.", null),
                        HttpStatus.FORBIDDEN);
            }
            if (canDelegate) {
                // Delegation right is non-transferable beyond one hop unless re-granted by an Admin.
                canDelegate = false;
            }
        }

        Optional<ReconProductCapabilityMap> existing = capabilityRepository
                .findByProductIdAndUserIdAndActionTypeAndCapabilityTypeAndStatus(
                        productId, userId, actionType, capabilityType, CommonConstants.ACTIVE);
        if (existing.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE,
                    "This user already holds this exact capability.", null), HttpStatus.CONFLICT);
        }

        ReconProductCapabilityMap grant = new ReconProductCapabilityMap();
        grant.setProductId(productId);
        grant.setUserId(userId);
        grant.setCapabilityType(capabilityType);
        grant.setActionType(actionType);
        grant.setCanDelegate(canDelegate ? CommonConstants.ACTIVE_Y : CommonConstants.INACTIVE_N);
        grant.setGrantedBy(grantedByUserId);
        grant.setGrantedAt(LocalDateTime.now());
        grant.setStatus(CommonConstants.ACTIVE);
        ReconProductCapabilityMap saved = capabilityRepository.save(grant);
        logger.info("Capability granted: product={} user={} type={} action={} by={}",
                productId, userId, capabilityType, actionType, grantedByUserId);

        String productName = reconProductMasterRepository.findById(productId)
                .map(ReconProductMaster::getProductName).orElse("Product #" + productId);
        emailService.sendCapabilityGrantedNotification(
                granteeOpt.get().getEmail(), granteeOpt.get().getFullName(),
                productName, capabilityType, actionType, grantorOpt.get().getFullName(), canDelegate);

        return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Capability granted successfully.",
                java.util.Collections.singletonList(saved)), HttpStatus.CREATED);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> revokeCapability(Long capabilityId, String revokedByUsername,
            String reason, boolean force) {
        Optional<ReconUser> revokerOpt = reconUserRepository.findByUsername(revokedByUsername);
        if (!revokerOpt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "Revoking user not found.", null), HttpStatus.NOT_FOUND);
        }
        Long revokedByUserId = revokerOpt.get().getUserId();

        Optional<ReconProductCapabilityMap> opt = capabilityRepository.findById(capabilityId);
        if (!opt.isPresent()) {
            return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "Capability grant not found.", null), HttpStatus.NOT_FOUND);
        }
        ReconProductCapabilityMap existing = opt.get();
        if (!CommonConstants.ACTIVE.equals(existing.getStatus())) {
            return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "This capability is already revoked.", null), HttpStatus.CONFLICT);
        }

        Optional<ReconUser> holderOpt = reconUserRepository.findById(existing.getUserId());

        if (!force) {
            boolean holderIsAdmin = holderOpt.isPresent() && ADMIN_USER_TYPES.contains(holderOpt.get().getUserType());
            if (!holderIsAdmin) {
                long remaining = capabilityRepository.countByProductIdAndActionTypeAndCapabilityTypeAndStatus(
                        existing.getProductId(), existing.getActionType(), existing.getCapabilityType(), CommonConstants.ACTIVE);
                // The row being revoked is itself one of the counted active rows.
                if (remaining <= 1) {
                    return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE,
                            "Cannot revoke — this is the last active capability of its kind for this product. "
                            + "A Bank/Branch User must retain at least one.", null), HttpStatus.CONFLICT);
                }
            }
        }

        existing.setStatus("REVOKED");
        existing.setRevokedBy(revokedByUserId);
        existing.setRevokedAt(LocalDateTime.now());
        existing.setRevokeReason(reason != null ? reason : "MANUAL");
        capabilityRepository.save(existing);
        logger.info("Capability revoked: id={} by={} reason={}", capabilityId, revokedByUserId, existing.getRevokeReason());

        if (holderOpt.isPresent()) {
            String productName = reconProductMasterRepository.findById(existing.getProductId())
                    .map(ReconProductMaster::getProductName).orElse("Product #" + existing.getProductId());
            emailService.sendCapabilityRevokedNotification(
                    holderOpt.get().getEmail(), holderOpt.get().getFullName(),
                    productName, existing.getCapabilityType(), existing.getActionType(), existing.getRevokeReason());
        }

        return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Capability revoked successfully.", null), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getActiveCapabilities(Long productId, String actionType, String capabilityType) {
        List<ReconProductCapabilityMap> rows = capabilityRepository
                .findByProductIdAndActionTypeAndCapabilityTypeAndStatus(productId, actionType, capabilityType, CommonConstants.ACTIVE);
        return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Active capabilities fetched.", rows), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getUserCapabilities(Long userId) {
        List<ReconProductCapabilityMap> rows = capabilityRepository.findByUserIdAndStatus(userId, CommonConstants.ACTIVE);
        return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "User capabilities fetched.", rows), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RestWithStatusList> resolveApprover(Long productId, String actionType, Long makerUserId) {
        List<ReconProductCapabilityMap> checkerPool = capabilityRepository
                .findByProductIdAndActionTypeAndCapabilityTypeAndStatus(productId, actionType, "CHECKER", CommonConstants.ACTIVE);
        if (!checkerPool.isEmpty()) {
            return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Dedicated Checker pool found.", checkerPool), HttpStatus.OK);
        }

        // No dedicated Checker — walk up the GRANTED_BY chain from the maker until we run out.
        Long currentUserId = makerUserId;
        Long fallbackUserId = null;
        int hops = 0;
        while (hops < 10) { // safety bound against any accidental cycle
            List<ReconProductCapabilityMap> ownGrants = capabilityRepository
                    .findByUserIdAndProductIdAndActionTypeAndCapabilityTypeAndStatus(
                            currentUserId, productId, actionType, "MAKER", CommonConstants.ACTIVE);
            if (ownGrants.isEmpty()) {
                break;
            }
            Long grantor = ownGrants.get(0).getGrantedBy();
            if (grantor == null || grantor.equals(currentUserId)) {
                fallbackUserId = currentUserId;
                break;
            }
            fallbackUserId = grantor;
            currentUserId = grantor;
            hops++;
        }
        if (fallbackUserId == null) {
            return new ResponseEntity<>(new RestWithStatusList(CommonConstants.FAILURE, "No approver could be resolved.", null), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Fallback approver resolved (no dedicated Checker).",
                java.util.Collections.singletonList(fallbackUserId)), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> revokeAllForProduct(Long productId, String triggeredByUsername) {
        Optional<ReconUser> triggeredByOpt = reconUserRepository.findByUsername(triggeredByUsername);
        Long triggeredBy = triggeredByOpt.map(ReconUser::getUserId).orElse(null);
        List<ReconProductCapabilityMap> rows = capabilityRepository.findByProductIdAndStatus(productId, CommonConstants.ACTIVE);
        for (ReconProductCapabilityMap row : rows) {
            row.setStatus("REVOKED");
            row.setRevokedBy(triggeredBy);
            row.setRevokedAt(LocalDateTime.now());
            row.setRevokeReason("PRODUCT_DEACTIVATED");
        }
        capabilityRepository.saveAll(rows);
        logger.info("Revoked {} capability grants for productId={} (product deactivated)", rows.size(), productId);
        return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "All capabilities revoked for product.", null), HttpStatus.OK);
    }
}
