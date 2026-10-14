package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.AuditUserDelegation;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditUserDelegationRepository extends JpaRepository<AuditUserDelegation, Long> {

    List<AuditUserDelegation> findByDelegatorUserId(Long delegatorUserId);

    List<AuditUserDelegation> findByDelegateeUserId(Long delegateeUserId);

    List<AuditUserDelegation> findByBankId(Long bankId);

    List<AuditUserDelegation> findByStatus(String status);

    Optional<AuditUserDelegation> findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(Long delegatorUserId, String status);

    Optional<AuditUserDelegation> findTopByDelegateeUserIdAndStatusOrderByCreatedAtDesc(Long delegateeUserId, String status);

    List<AuditUserDelegation> findByDelegatorUserIdAndStatus(Long delegatorUserId, String status);

    List<AuditUserDelegation> findByBankIdAndStatus(Long bankId, String status);
}
