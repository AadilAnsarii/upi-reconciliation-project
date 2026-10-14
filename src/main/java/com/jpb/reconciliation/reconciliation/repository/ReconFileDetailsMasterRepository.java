package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;

@Repository
public interface ReconFileDetailsMasterRepository extends JpaRepository<ReconFileDetailsMaster, Long> {

    ReconFileDetailsMaster findByReconFileId(Long processId);

    @Query("SELECT r FROM ReconFileDetailsMaster r WHERE r.rfdTranFileFlag = :flag")
    List<ReconFileDetailsMaster> findByRfdTranFileFlag(@Param("flag") String flag);

    // Original — single result (kept for other existing usages in your project)
    ReconFileDetailsMaster findByReconTemplateDetails_ReconTemplateId(Long reconTemp1);

    // NEW — returns ALL files for a template, safe when multiple files exist
    // Used by ReportMastConfigService to avoid NonUniqueResultException
    List<ReconFileDetailsMaster> findAllByReconTemplateDetails_ReconTemplateId(Long templateId);

    @Query("SELECT f FROM ReconFileDetailsMaster f WHERE "
            + "(:templateId IS NULL OR f.reconTemplateDetails.reconTemplateId = :templateId) AND "
            + "(:fileName IS NULL OR LOWER(f.reconFileName) LIKE LOWER(CONCAT('%', :fileName, '%')))")
    Page<ReconFileDetailsMaster> findByFilters(
            @Param("templateId") Long templateId,
            @Param("fileName") String fileName,
            Pageable pageable);
}