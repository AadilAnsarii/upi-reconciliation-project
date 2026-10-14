package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ExceptionReconReportEntity;

@Repository
public interface ExceptionReconReportRepository extends JpaRepository<ExceptionReconReportEntity, Long> {

	List<ExceptionReconReportEntity> findByProcessId(Long long1);

	ExceptionReconReportEntity findByReportId(Long reportId);

	/**
	 * Search by report name (FILE_NAME) and/or processId — both optional.
	 */
	@Query("SELECT r FROM ExceptionReconReportEntity r WHERE "
			+ "(:reportName IS NULL OR UPPER(r.fileName) LIKE UPPER(CONCAT('%', :reportName, '%'))) AND "
			+ "(:processId  IS NULL OR CAST(r.processId AS string) = :processId)")
	List<ExceptionReconReportEntity> searchByNameAndProcess(@Param("reportName") String reportName,
			@Param("processId") String processId);

	List<ExceptionReconReportEntity> findByTemplateId(Long templateId);

}
