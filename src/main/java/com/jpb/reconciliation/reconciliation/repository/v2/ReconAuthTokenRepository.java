package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconAuthToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconAuthTokenRepository extends JpaRepository<ReconAuthToken, Long> {

    Optional<ReconAuthToken> findByTokenValue(String tokenValue);

    Optional<ReconAuthToken> findByTokenValueAndStatus(String tokenValue, String status);

    List<ReconAuthToken> findByUserId(Long userId);

    List<ReconAuthToken> findByUserIdAndStatus(Long userId, String status);

    List<ReconAuthToken> findByUserIdAndTokenType(Long userId, String tokenType);

    Optional<ReconAuthToken> findByUserIdAndTokenTypeAndStatus(Long userId, String tokenType, String status);

    List<ReconAuthToken> findByStatusAndExpiresAtBefore(String status, LocalDateTime dateTime);
}
