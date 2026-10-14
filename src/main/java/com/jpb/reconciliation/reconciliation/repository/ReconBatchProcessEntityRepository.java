package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;

@Repository
public interface ReconBatchProcessEntityRepository extends JpaRepository<ReconBatchProcessEntity, Long> {

	ReconBatchProcessEntity findByProcessIdAndSequenceNo(Long processId, Long sequenceId);

	List<ReconBatchProcessEntity> findByProcessTypeAndProcessIdAndInsertDate(String reportType, Long processId,
			LocalDate reportDate);

	List<ReconBatchProcessEntity> findByProcessIdAndStatus(Long processId, String string);
	
	 // Latest EXTRACTION row for a given fileType processId (RPM_FILE_TYPE1/2/3/4)
    @Query("SELECT r FROM ReconBatchProcessEntity r " +
           "WHERE r.processId = :fileTypeProcessId " +
           "AND r.processType = 'EXTRACTION' " +
           "ORDER BY r.sequenceNo DESC")
    List<ReconBatchProcessEntity> findLatestExtractionByFileTypeProcessId(
            @Param("fileTypeProcessId") Long fileTypeProcessId,
            Pageable pageable);
 
    // Latest RECONCILIATION row for a given recon processId (RPM_PROCESS_ID)
    @Query("SELECT r FROM ReconBatchProcessEntity r " +
           "WHERE r.processId = :reconProcessId " +
           "AND r.processType = 'RECONCILIATION' " +
           "ORDER BY r.sequenceNo DESC")
    List<ReconBatchProcessEntity> findLatestReconByProcessId(
            @Param("reconProcessId") Long reconProcessId,
            Pageable pageable);
 
    // Latest RECONCILIATION row across all processes — used by /latest endpoint
    @Query("SELECT r FROM ReconBatchProcessEntity r " +
           "WHERE r.processType = 'RECONCILIATION' " +
           "ORDER BY r.sequenceNo DESC")
    List<ReconBatchProcessEntity> findLatestReconOverall(Pageable pageable);

    // Force-match eligible count: rows in REC_UPI_MIS_UBEN_DATA with tran_resp_code = 'RB'
    @Query(value = "SELECT COUNT(1) FROM REC_UPI_MIS_UBEN_DATA WHERE tran_resp_code = 'RB'",
           nativeQuery = true)
    int countForceMatchEligible();

	List<ReconBatchProcessEntity> findByTemplateIdAndStatus(Long templateId, String string);

	ReconBatchProcessEntity findByTemplateIdAndSequenceNo(Long templateId, Long sequenceNo);

}
