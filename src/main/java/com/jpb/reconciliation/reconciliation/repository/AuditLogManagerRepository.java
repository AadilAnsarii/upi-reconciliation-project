package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.AuditLogManager;

@Repository
public interface AuditLogManagerRepository extends JpaRepository<AuditLogManager, Long> {

}
