package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.OtpManager;

@Repository
public interface OtpManagerRepository extends JpaRepository<OtpManager, Long> {

    // Find latest unused OTP for given email
    Optional<OtpManager> findTopByEmailIdAndIsUsedOrderByCreatedAtDesc(String emailId, String isUsed);

    // Mark all old OTPs for this email as used before generating new one.
    // @Transactional sits here rather than on the caller: OtpService drives this from private
    // helpers, and Spring's proxy would never apply the annotation there.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OtpManager o SET o.isUsed = 'Y' WHERE o.emailId = :emailId AND o.isUsed = 'N'")
    void invalidatePreviousOtps(@Param("emailId") String emailId);
}