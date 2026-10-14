package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconProductCapabilityMap;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconProductCapabilityMapRepository extends JpaRepository<ReconProductCapabilityMap, Long> {

    // Pool lookup — e.g. "who are the active CHECKERs for product X, action ROLE"
    List<ReconProductCapabilityMap> findByProductIdAndActionTypeAndCapabilityTypeAndStatus(
            Long productId, String actionType, String capabilityType, String status);

    // Everything currently held by a specific user (for block/reclaim + profile screens)
    List<ReconProductCapabilityMap> findByUserIdAndStatus(Long userId, String status);

    // Prevent duplicate active grants of the exact same capability to the same user
    Optional<ReconProductCapabilityMap> findByProductIdAndUserIdAndActionTypeAndCapabilityTypeAndStatus(
            Long productId, Long userId, String actionType, String capabilityType, String status);

    // Fallback-checker lookup: "who granted this maker their capability" (walk up one level)
    List<ReconProductCapabilityMap> findByUserIdAndProductIdAndActionTypeAndCapabilityTypeAndStatus(
            Long userId, Long productId, String actionType, String capabilityType, String status);

    // Empty-pool detection before a self-revoke (min-1 rule for non-admin users)
    long countByProductIdAndActionTypeAndCapabilityTypeAndStatus(
            Long productId, String actionType, String capabilityType, String status);

    // Everything granted by a specific user (to cascade-revoke sub-grants when a grantor is blocked, if ever needed)
    List<ReconProductCapabilityMap> findByGrantedByAndStatus(Long grantedBy, String status);

    // Bulk revoke-on-product-deactivation
    List<ReconProductCapabilityMap> findByProductIdAndStatus(Long productId, String status);

    // Grace-period sweep — every SUSPENDED row regardless of product
    List<ReconProductCapabilityMap> findByStatus(String status);
}
