package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.AuditLogManager;

@Repository
public interface TestAuditLogManagerRepository extends JpaRepository<AuditLogManager, Long> {

    // Fetch all history by module (e.g. "CHECKER_ROLE" or "CHECKER_USER")
    List<AuditLogManager> findByModuleOrderByAuditDateTimeDesc(String module);

    // Fetch history by module + event (e.g. "Approved" / "Disapproved")
    List<AuditLogManager> findByModuleAndEventOrderByAuditDateTimeDesc(String module, String event);

    // Fetch all checker decisions (both ROLE and USER) — ordered by latest first
    List<AuditLogManager> findByModuleInOrderByAuditDateTimeDesc(List<String> modules);
}