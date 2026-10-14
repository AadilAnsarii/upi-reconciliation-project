package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;

@Repository
public interface ReconFieldDetailsMasterRepository 
        extends JpaRepository<ReconFieldDetailsMaster, Long> {

    // ✅ FIXED — EntityGraph avoids cartesian product duplicates
    @EntityGraph(attributePaths = {
        "reconFieldTypeMaster",
        "reconFieldFormatMaster",
        "reconTemplateDetails"
    })
    @Query("SELECT f FROM ReconFieldDetailsMaster f " +
           "WHERE f.reconTemplateDetails.reconTemplateId = :templateId " +
           "ORDER BY f.reconColumnPosn ASC")
    List<ReconFieldDetailsMaster> findFullFieldDetailsByTemplateId(
            @Param("templateId") Long templateId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ReconFieldDetailsMaster r " +
           "WHERE r.reconTemplateDetails.reconTemplateId = :templateId")
    void deleteByTemplateId(@Param("templateId") Long templateId);
}