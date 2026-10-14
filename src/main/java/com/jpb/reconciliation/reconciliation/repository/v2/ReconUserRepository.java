package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconUserRepository extends JpaRepository<ReconUser, Long> {

    Optional<ReconUser> findByUsername(String username);

    /**
     * Atomically claims a Checker's decision on a pending user: only flips the status if it is
     * still what the caller last read. Two Checkers racing on the same user cannot both succeed —
     * the loser gets 0 rows affected instead of silently overwriting the first decision.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReconUser u SET u.status = :newStatus WHERE u.userId = :userId AND u.status = :expectedStatus")
    int compareAndSetStatus(@Param("userId") Long userId, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus);

    Optional<ReconUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** An email is "actively" registered only if a NON-blocked account holds it. A BLOCKED account
     *  is treated as effectively deleted, so its email may be reused for a fresh account. */
    boolean existsByEmailAndStatusNot(String email, String status);

    /** All login accounts holding this email — used to release (tombstone) a blocked holder's email
     *  when it is reused, so findByEmail() stays single-valued. */
    List<ReconUser> findAllByEmail(String email);

    List<ReconUser> findByStatus(String status);

    List<ReconUser> findByBankId(Long bankId);

    List<ReconUser> findByBankIdIn(List<Long> bankIds);

    List<ReconUser> findByUserType(String userType);

    List<ReconUser> findByApprovedYn(String approvedYn);

    List<ReconUser> findByBankIdAndStatus(Long bankId, String status);

    List<ReconUser> findByRoleId(Long roleId);

    Optional<ReconUser> findByUsernameAndStatus(String username, String status);

    Optional<ReconUser> findByUsernameAndApprovedYn(String username, String approvedYn);

    List<ReconUser> findByInactivateScheduledAtBefore(LocalDateTime time);

    List<ReconUser> findByReactivateScheduledAtBefore(LocalDateTime time);

    List<ReconUser> findByBlockScheduledAtBefore(LocalDateTime time);

    List<ReconUser> findByBankIdAndUserType(Long bankId, String userType);

    Optional<ReconUser> findByBankIdAndUserTypeAndContactRank(Long bankId, String userType, String contactRank);

    Optional<ReconUser> findByUsernameAndBankId(String username, Long bankId);

    List<ReconUser> findByParentUserId(Long parentUserId);

    Optional<ReconUser> findByEmailAndUserType(String email, String userType);
}
