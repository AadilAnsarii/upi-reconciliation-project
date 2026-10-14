package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.jpb.reconciliation.reconciliation.entity.RcnRuleMast;

@Repository
public interface RcnRuleMastRepository extends JpaRepository<RcnRuleMast, Long> {

    /** All matching rules for a process (RRM_PROCESS_ID is set, RRM_TMPLT_ID is null) */
    List<RcnRuleMast> findByRrmProcessIdAndRrmTmpltIdIsNull(Long processId);

    /** All template/filter rows for a process (RRM_TMPLT_ID is set) */
    List<RcnRuleMast> findByRrmTmpltIdAndRrmProcessIdIsNull(Long tmpltId);

    /** All rules (matching + filter) tied to a process */
    List<RcnRuleMast> findByRrmProcessId(Long processId);

    /** All template rows for a given data table */
    List<RcnRuleMast> findByRrmDataTblName(String dataTableName);

    /** Active matching rules ordered by priority */
    @Query("SELECT r FROM RcnRuleMast r " +
           "WHERE r.rrmProcessId = :processId " +
           "AND r.rrmRuleStat = 1 " +
           "AND r.rrmTmpltId IS NULL " +
           "ORDER BY r.rrmPriority ASC")
    List<RcnRuleMast> findActiveMatchingRulesByProcess(@Param("processId") Long processId);

    /** Delete all rules (matching + filter) for a process – used during update */
    @Modifying
    @Query("DELETE FROM RcnRuleMast r WHERE r.rrmProcessId = :processId")
    void deleteByRrmProcessId(@Param("processId") Long processId);
}