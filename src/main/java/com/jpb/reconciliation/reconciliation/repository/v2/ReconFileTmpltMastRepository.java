package com.jpb.reconciliation.reconciliation.repository.v2;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;

@Repository
public interface ReconFileTmpltMastRepository extends JpaRepository<ReconFileTmpltMast, Long> {

    // ── Finders used by service layer ───────────────────────────────────────

    ReconFileTmpltMast findByTemplateName(String templateName);

    Optional<ReconFileTmpltMast> findByTemplateCode(String templateCode);

    boolean existsByTemplateNameAndTenantId(String name, Long tenantId);

    /**
     * Duplicate-name check that ignores deleted templates. A soft-deleted (INACTIVE)
     * template is invisible in every list, so letting its name keep blocking new
     * templates meant users hit "Template already configured." with nothing on screen
     * to explain it. Deleting a template now frees its name.
     */
    @Query("SELECT t FROM ReconFileTmpltMast t WHERE UPPER(t.templateName) = UPPER(:name) "
         + "AND t.status <> 'INACTIVE'")
    List<ReconFileTmpltMast> findLiveByTemplateName(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM ReconFileTmpltMast t "
         + "WHERE UPPER(t.templateName) = UPPER(:name) AND t.tenantId = :tenantId "
         + "AND t.status <> 'INACTIVE'")
    boolean existsLiveByTemplateNameAndTenantId(@Param("name") String name,
                                                @Param("tenantId") Long tenantId);

    // ── Paginated list queries ───────────────────────────────────────────────

    /** Two-step fetch: first get IDs (count query friendly), then load full graph */
    @Query("SELECT t FROM ReconFileTmpltMast t WHERE t.status <> 'INACTIVE'")
    Page<ReconFileTmpltMast> findTemplates(Pageable pageable);

    @Query("SELECT DISTINCT t FROM ReconFileTmpltMast t " +
            "LEFT JOIN FETCH t.sourceSystem " +
            "LEFT JOIN FETCH t.fieldDetails fd " +
            "LEFT JOIN FETCH fd.fieldType " +
            "LEFT JOIN FETCH fd.fieldFormat " +
            "WHERE t IN :templates")
    List<ReconFileTmpltMast> fetchTemplateDetails(@Param("templates") List<ReconFileTmpltMast> templates);

    /**
     * Single template with sourceSystem + fields eagerly loaded.
     * Returns List (not Optional) to avoid NonUniqueResultException when
     * JOIN FETCH on fieldDetails produces multiple SQL rows (one per field).
     * DISTINCT removes duplicate template objects from Hibernate identity map.
     * Caller takes get(0) if list is non-empty.
     */
    @Query("SELECT DISTINCT t FROM ReconFileTmpltMast t " +
            "LEFT JOIN FETCH t.sourceSystem " +
            "LEFT JOIN FETCH t.fieldDetails fd " +
            "LEFT JOIN FETCH fd.fieldType " +
            "LEFT JOIN FETCH fd.fieldFormat " +
            "WHERE t.templateId = :id")
    List<ReconFileTmpltMast> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM ReconFileTmpltMast t WHERE t.status <> 'INACTIVE' ORDER BY t.templateName")
    List<ReconFileTmpltMast> findAllTemplates();

    @Query("SELECT t FROM ReconFileTmpltMast t WHERE t.status <> 'INACTIVE' AND UPPER(t.templateName) LIKE UPPER(CONCAT('%', :templateName, '%'))")
    Page<ReconFileTmpltMast> findByTemplateNameContainingIgnoreCase(@Param("templateName") String templateName, Pageable pageable);

    @Query("SELECT t FROM ReconFileTmpltMast t WHERE t.status <> 'INACTIVE' AND t.templateType = :templateType")
    Page<ReconFileTmpltMast> findByTemplateType(@Param("templateType") String templateType, Pageable pageable);

    @Query("SELECT t FROM ReconFileTmpltMast t WHERE t.status <> 'INACTIVE' AND UPPER(t.templateName) LIKE UPPER(CONCAT('%', :templateName, '%')) AND t.templateType = :templateType")
    Page<ReconFileTmpltMast> findByTemplateNameContainingIgnoreCaseAndTemplateType(
            @Param("templateName") String templateName, @Param("templateType") String templateType, Pageable pageable);

    @Query("SELECT t FROM ReconFileTmpltMast t " +
            "WHERE t.status <> 'INACTIVE' " +
            "AND (:status IS NULL   OR t.status       = :status) " +
            "AND (:type IS NULL     OR t.templateType = :type) " +
            "AND (:tenantId IS NULL OR t.tenantId     = :tenantId)")
    Page<ReconFileTmpltMast> findAllWithFilters(
            @Param("status")   String status,
            @Param("type")     String type,
            @Param("tenantId") Long tenantId,
            Pageable pageable);

    /** Count how many non-deleted templates are still using this SFTP server */
    @Query("SELECT COUNT(t) FROM ReconFileTmpltMast t " +
            "WHERE t.sftpServerId = :sftpServerId AND t.status <> 'INACTIVE'")
    long countBySftpServerId(@Param("sftpServerId") Long sftpServerId);
}
