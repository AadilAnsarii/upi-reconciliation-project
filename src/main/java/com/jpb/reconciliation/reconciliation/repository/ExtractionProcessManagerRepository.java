package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.REProcessManager;

@Repository
public interface ExtractionProcessManagerRepository extends JpaRepository<REProcessManager, Long> {

}
