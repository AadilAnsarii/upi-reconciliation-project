package com.jpb.reconciliation.reconciliation.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconSourceSystemMast;

@Repository
public interface ReconSourceSystemMastRepository extends JpaRepository<ReconSourceSystemMast, Long> {

    Optional<ReconSourceSystemMast> findBySourceCode(String sourceCode);

    List<ReconSourceSystemMast> findByIsActive(String isActive);
}
