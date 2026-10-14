package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconBankMasterRepository extends JpaRepository<ReconBankMaster, Long> {

    // ── SECONDARY-safe overrides (always filter to PRIMARY rows for display) ──

    @Query("SELECT b FROM ReconBankMaster b WHERE b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findAllPrimary();

    @Query("SELECT b FROM ReconBankMaster b WHERE b.bankId = :bankId AND b.contactRank = 'PRIMARY'")
    Optional<ReconBankMaster> findPrimaryById(@Param("bankId") Long bankId);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.bankCode = :bankCode AND b.contactRank = 'PRIMARY'")
    Optional<ReconBankMaster> findByBankCode(@Param("bankCode") String bankCode);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.status = :status AND b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findByStatus(@Param("status") String status);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.bankType LIKE %:bankType% AND b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findByBankType(@Param("bankType") String bankType);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.parentBankId = :parentBankId AND b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findByParentBankId(@Param("parentBankId") Long parentBankId);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.createdBy = :createdBy AND b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findByCreatedBy(@Param("createdBy") String createdBy);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.bankName = :bankName AND b.contactRank = 'PRIMARY'")
    Optional<ReconBankMaster> findByBankName(@Param("bankName") String bankName);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.inactivateScheduledAt < :time AND b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findByInactivateScheduledAtBefore(@Param("time") LocalDateTime time);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.reactivateScheduledAt < :time AND b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findByReactivateScheduledAtBefore(@Param("time") LocalDateTime time);

    @Query("SELECT b FROM ReconBankMaster b WHERE b.blockScheduledAt < :time AND b.contactRank = 'PRIMARY'")
    List<ReconBankMaster> findByBlockScheduledAtBefore(@Param("time") LocalDateTime time);

    // ── Existence checks (check across all ranks — intentional) ──────────────

    boolean existsByBankCode(String bankCode);

    boolean existsByBankCodeAndContactRank(String bankCode, String contactRank);

    boolean existsByBankName(String bankName);

    boolean existsByEmail(String email);

    /** A contact email counts as taken only if a NON-blocked bank/branch contact row holds it —
     *  a BLOCKED institution is effectively deleted and its contact email may be reused. */
    boolean existsByEmailAndStatusNot(String email, String status);

    /** All contact rows holding this email — used to release a blocked holder's email on reuse. */
    List<ReconBankMaster> findAllByEmail(String email);

    // ── Raw access (needed for internal ops: SECONDARY row saves, etc.) ──────

    Optional<ReconBankMaster> findByBankCodeAndContactRank(String bankCode, String contactRank);

    List<ReconBankMaster> findAllByBankCode(String bankCode);
}
