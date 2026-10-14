package com.jpb.reconciliation.reconciliation.repository.forcematch;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.forcematch.ProcessDef;

@Repository
public interface ProcessDefRepository extends JpaRepository<ProcessDef, Long> {

    /** All configs for a given processId, sorted by execution order */
    @Query("SELECT p FROM ProcessDef p WHERE p.rmpProcessId = :pid ORDER BY p.rmpOrderOfExecution ASC NULLS LAST")
    List<ProcessDef> findByProcessIdOrdered(@Param("pid") Long processId);

    /** Filter by active/inactive status */
    List<ProcessDef> findByRmpActionConfigStatus(String status);

    /** Filter by processId + status */
    List<ProcessDef> findByRmpProcessIdAndRmpActionConfigStatus(Long processId, String status);

    /** Distinct process IDs — used for dropdown population */
    @Query("SELECT DISTINCT p.rmpProcessId FROM ProcessDef p ORDER BY p.rmpProcessId ASC")
    List<Long> findDistinctProcessIds();
}