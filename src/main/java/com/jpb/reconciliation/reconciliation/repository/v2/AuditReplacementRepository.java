package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditReplacementRepository extends JpaRepository<AuditReplacement, Long> {

    List<AuditReplacement> findByBankId(Long bankId);

    List<AuditReplacement> findByOriginalUserId(Long originalUserId);

    List<AuditReplacement> findByReplacementUserId(Long replacementUserId);

    List<AuditReplacement> findByStatus(String status);

    List<AuditReplacement> findByBankIdAndStatus(Long bankId, String status);

    Optional<AuditReplacement> findByOriginalUserIdAndStatus(Long originalUserId, String status);

    // Any "live" replacement for the original — ACTIVE (reversible) or FINALIZED (permanent,
    // after the original was blocked). Used for admin-list display so the badge/label can
    // distinguish the two instead of only ever finding ACTIVE and mislabeling it PERMANENT.
    List<AuditReplacement> findByOriginalUserIdAndStatusIn(Long originalUserId, List<String> statuses);

    // Is this user CURRENTLY covering for someone else? (their own row must be flagged
    // replacementAdminRow=true so the frontend can suppress their buttons while temporary)
    List<AuditReplacement> findByReplacementUserIdAndStatusIn(Long replacementUserId, List<String> statuses);

    List<AuditReplacement> findByEntityType(String entityType);

    boolean existsByOriginalUserIdAndStatus(Long originalUserId, String status);

    boolean existsByReplacementUserIdAndStatus(Long replacementUserId, String status);

    Optional<AuditReplacement> findByReplacementUserIdAndStatus(Long replacementUserId, String status);
}
