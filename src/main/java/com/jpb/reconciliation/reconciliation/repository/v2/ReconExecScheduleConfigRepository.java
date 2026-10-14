package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconExecScheduleConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RECON_EXEC_SCHEDULE_CONFIG.
 *
 * Entity   : ReconExecScheduleConfig
 * PK type  : Long (SCHEDULE_ID)
 * Schema   : JPB_RECON
 */
@Repository
public interface ReconExecScheduleConfigRepository
        extends JpaRepository<ReconExecScheduleConfig, Long> {

    /**
     * Fetch the schedule config for a given template.
     * UQ_RSCHED_TMPLT guarantees at most one row per TEMPLATE_ID.
     */
    Optional<ReconExecScheduleConfig> findByTemplateId(Long templateId);

    /**
     * Check whether a schedule config already exists for a template.
     * Used in the save logic to decide between insert and update.
     */
    boolean existsByTemplateId(Long templateId);

    /**
     * Fetch all schedule configs with a given active flag ('Y' or 'N').
     * Backed by IDX_RSCHED_ACTIVE.
     */
    List<ReconExecScheduleConfig> findByIsActive(String isActive);

    /**
     * Activate the schedule for a template in one update (no entity load required).
     */
    @Modifying
    @Query("UPDATE ReconExecScheduleConfig s SET s.isActive = 'Y' WHERE s.templateId = :templateId")
    int activateByTemplateId(@Param("templateId") Long templateId);

    /**
     * Deactivate the schedule for a template in one update.
     */
    @Modifying
    @Query("UPDATE ReconExecScheduleConfig s SET s.isActive = 'N' WHERE s.templateId = :templateId")
    int deactivateByTemplateId(@Param("templateId") Long templateId);

    /**
     * Delete schedule config by templateId (used when the parent template is hard-deleted
     * from Java layer; DB-level ON DELETE CASCADE handles it at DB layer).
     */
    void deleteByTemplateId(Long templateId);
}
