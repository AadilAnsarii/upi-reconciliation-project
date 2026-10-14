package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import java.util.List;
import java.util.Optional;

public interface ReconPasswordManagerRepository extends JpaRepository<ReconPasswordManager, Long> {

    Optional<ReconPasswordManager> findByReconUser_UserId(Long userId);

    List<ReconPasswordManager> findByReconUser(ReconUser reconUser);

    Optional<ReconPasswordManager> findByToken(String token);

    void deleteByReconUser_UserId(Long userId);
}
