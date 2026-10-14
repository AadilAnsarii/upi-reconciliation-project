package com.jpb.reconciliation.reconciliation.repository;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconFileIngestConfig;

@Repository
public interface ReconFileIngestConfigRepository extends JpaRepository<ReconFileIngestConfig, Long> {

    ReconFileIngestConfig findByIngestConfigId(Long ingestConfigId);

    // Returns first match — for cases where only one file config per template exists
    ReconFileIngestConfig findByTemplate_TemplateId(Long templateId);

    // Returns all file configs for a template (use when multiple may exist)
    List<ReconFileIngestConfig> findAllByTemplate_TemplateId(Long templateId);

    @Query("SELECT f FROM ReconFileIngestConfig f WHERE f.tranFileFlag = :flag")
    List<ReconFileIngestConfig> findByTranFileFlag(@Param("flag") String flag);

    @Query("SELECT f FROM ReconFileIngestConfig f " +
            "WHERE (:templateId IS NULL OR f.template.templateId = :templateId) " +
            "AND (:fileName IS NULL OR LOWER(f.fileName) LIKE LOWER(CONCAT('%', :fileName, '%')))")
    Page<ReconFileIngestConfig> findByFilters(
            @Param("templateId") Long templateId,
            @Param("fileName")   String fileName,
            Pageable pageable);

    // Legacy alias — keeps old code compiling
    default ReconFileIngestConfig findByReconFileId(Long id) {
        return findByIngestConfigId(id);
    }

    default ReconFileIngestConfig findByReconTemplateDetails_ReconTemplateId(Long tId) {
        return findByTemplate_TemplateId(tId);
    }
}
