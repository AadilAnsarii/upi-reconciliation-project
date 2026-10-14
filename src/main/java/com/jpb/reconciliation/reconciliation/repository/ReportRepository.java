package com.jpb.reconciliation.reconciliation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReportEntity;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

	ReportEntity findByProcessId(Long processId);

	List<ReportEntity> findByReportKey(String reportKey);

	List<ReportEntity> findByReportDate(@Param("reportDate") LocalDate localDate);

	List<ReportEntity> findByProcessIdOrReportName(Long processId, String reportName);

	List<ReportEntity> findByProcessIdOrReportNameOrReportDate(Long processId, String reportName, LocalDate reportDate);

	List<ReportEntity> findByReportFileNameAndReportDate(String reportFileName, LocalDate reportDate);

}
