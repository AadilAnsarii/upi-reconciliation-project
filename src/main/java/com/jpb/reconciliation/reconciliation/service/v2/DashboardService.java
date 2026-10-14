//package com.jpb.reconciliation.reconciliation.service;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.sql.DataSource;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.EmptyResultDataAccessException;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//
//import com.jpb.reconciliation.reconciliation.dto.UpiDashboardResponse;
//import com.jpb.reconciliation.reconciliation.dto.UpiDashboardResponse.FileCount;
//import com.jpb.reconciliation.reconciliation.dto.UpiDashboardResponse.KpiSummary;
//import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
//import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
//import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
//import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Service
//
//public class DashboardService {
//
//	private final JdbcTemplate jdbcTemplate;
//
//	@Autowired
//	public DashboardService(DataSource dataSource, ReconProcessDefMasterRepository defMastRepo,
//			ReconBatchProcessEntityRepository batchRepo) {
//		this.jdbcTemplate = new JdbcTemplate(dataSource);
//		this.defMastRepo = defMastRepo;
//		this.batchRepo = batchRepo;
//	}
//
//	private static final PageRequest ONE = PageRequest.of(0, 1);
//
//	private final ReconProcessDefMasterRepository defMastRepo;
//	private final ReconBatchProcessEntityRepository batchRepo;
//
//	// ------------------------------------------------------------------
//	// 1. Search by reconProcessId (RPM_PROCESS_ID)
//	// ------------------------------------------------------------------
//	public UpiDashboardResponse getDashboardByProcessId(Long reconProcessId) {
//		try {
//			log.info("Dashboard request for reconProcessId={}", reconProcessId);
//			ReconProcessDefMaster def = defMastRepo.findById(reconProcessId).orElse(null);
//			if (def == null) {
//				log.warn("No process definition found for processId={}", reconProcessId);
//				return null;
//			}
//			return buildDashboard(def);
//		} catch (Exception e) {
//			log.error("Error fetching dashboard for processId={} : {}", reconProcessId, e.getMessage());
//			return null;
//		}
//	}
//
//	// ------------------------------------------------------------------
//	// 2. Latest dashboard
//	// ------------------------------------------------------------------
//	public UpiDashboardResponse getLatestDashboard() {
//		try {
//			log.info("Fetching latest recon dashboard");
//			List<ReconBatchProcessEntity> latestRecon = batchRepo.findLatestReconOverall(ONE);
//			if (latestRecon.isEmpty()) {
//				log.warn("No completed reconciliation process found.");
//				return null;
//			}
//			Long reconProcessId = latestRecon.get(0).getProcessId();
//			ReconProcessDefMaster def = defMastRepo.findById(reconProcessId).orElse(null);
//			if (def == null) {
//				log.warn("No process definition found for processId={}", reconProcessId);
//				return null;
//			}
//			return buildDashboard(def);
//		} catch (Exception e) {
//			log.error("Error fetching latest dashboard : {}", e.getMessage());
//			return null;
//		}
//	}
//
//	// ------------------------------------------------------------------
//	// Core builder
//	// ------------------------------------------------------------------
//	private UpiDashboardResponse buildDashboard(ReconProcessDefMaster def) {
//
//		// Latest RECONCILIATION row → RBP_RECON_CNT1 = autoReconciled
//		List<ReconBatchProcessEntity> reconRows = batchRepo.findLatestReconByProcessId(def.getReconProcessId(), ONE);
//		int autoReconciled = 0;
//		if (!reconRows.isEmpty() && reconRows.get(0).getReconDataCount() != null) {
//			if (Long.valueOf(815065384335L).equals(def.getReconProcessId())) {
//				try {
//					Long reconCount = jdbcTemplate.queryForObject(
//							"SELECT RECON_COUNT FROM RECON_COUNT WHERE PROCESS_ID = ?", Long.class, 815065384335L);
//					autoReconciled = (reconCount != null) ? reconCount.intValue() : 0;
//				} catch (EmptyResultDataAccessException ex) {
//					autoReconciled = 0;
//				} catch (Exception ex) {
//					log.error("Error fetching RECON_COUNT for processId=815065384335: {}", ex.getMessage());
//					autoReconciled = 0;
//				}
//			} else {
//				autoReconciled = parseCount(reconRows.get(0).getReconDataCount());
//			}
//		}
//
//		// Build file-wise count list + sum totalTxns
//		List<FileCount> fileWiseCount = new ArrayList<>();
//		int totalTxns = 0;
//
//		if (def.getReconFileType1() != null) {
//			FileCount fc = buildFileCount(1, def.getReconFileType1(), def.getReconDataTableName1());
//			totalTxns += fc.getDataCount();
//			fileWiseCount.add(fc);
//		}
//		if (def.getReconFileType2() != null) {
//			FileCount fc = buildFileCount(2, def.getReconFileType2(), def.getReconDataTableName2());
//			totalTxns += fc.getDataCount();
//			fileWiseCount.add(fc);
//		}
//		if (def.getReconFileType3() != null) {
//			FileCount fc = buildFileCount(3, def.getReconFileType3(), def.getReconDataTableName3());
//			totalTxns += fc.getDataCount();
//			fileWiseCount.add(fc);
//		}
//		if (def.getReconFileType4() != null) {
//			FileCount fc = buildFileCount(4, def.getReconFileType4(), def.getReconDataTableName4());
//			totalTxns += fc.getDataCount();
//			fileWiseCount.add(fc);
//		}
//
//		double matchRate = totalTxns > 0 ? Math.round((autoReconciled * 100.0 / totalTxns) * 10.0) / 10.0 : 0.0;
//
//		int exceptions = Math.max(totalTxns - autoReconciled, 0);
//
//		int forceMatchEligible = 0;
//		String forceMatchNote = "N/A";
//		if (Long.valueOf(815065384325L).equals(def.getReconProcessId())) {
//			forceMatchEligible = batchRepo.countForceMatchEligible();
//			forceMatchNote = "RB response code transactions";
//		}
//
//		KpiSummary kpi = KpiSummary.builder().totalTxns(totalTxns).autoReconciled(autoReconciled)
//				.matchRatePercent(matchRate).exceptions(exceptions)
//				.exceptionsNote("Across all " + def.getReconInputCount() + " sources")
//				.forceMatchEligible(forceMatchEligible).forceMatchNote(forceMatchNote).pendingTtumApproval(0) 
//				.pendingTtumNote("Maker-checker pending").settlementNet(BigDecimal.ZERO) 
//				.settlementExpected(BigDecimal.ZERO).settlementDelta(BigDecimal.ZERO).fileWiseCount(fileWiseCount)
//				.build();
//
//		return UpiDashboardResponse.builder().reconProcessId(def.getReconProcessId())
//				.processName(def.getReconProcessName()).tranDate(LocalDate.now().minusDays(1)).kpiSummary(kpi).build();
//	}
//
//	// ------------------------------------------------------------------
//	// Build one FileCount entry from latest EXTRACTION row
//	// ------------------------------------------------------------------
//	private FileCount buildFileCount(int slot, Long fileTypeProcessId, String dataTableName) {
//		try {
//			List<ReconBatchProcessEntity> rows = batchRepo.findLatestExtractionByFileTypeProcessId(fileTypeProcessId,
//					ONE);
//
//			if (!rows.isEmpty()) {
//				ReconBatchProcessEntity row = rows.get(0);
//				return FileCount.builder().fileSlot(slot).fileTypeProcessId(fileTypeProcessId)
//						.dataTableName(dataTableName).fileName(row.getFileName())
//						.dataCount(parseCount(row.getDataCount())).extStatus(row.getExtractionStatus()).build();
//			}
//		} catch (Exception e) {
//			log.error("Error fetching extraction for slot={} fileTypeProcessId={} : {}", slot, fileTypeProcessId,
//					e.getMessage());
//		}
//
//		// No batch record found for this slot
//		return FileCount.builder().fileSlot(slot).fileTypeProcessId(fileTypeProcessId).dataTableName(dataTableName)
//				.fileName(null).dataCount(0).extStatus("NOT_RUN").build();
//	}
//
//	private int parseCount(String val) {
//		if (val == null || val.trim().isEmpty())
//			return 0;
//		try {
//			return Integer.parseInt(val.trim());
//		} catch (NumberFormatException e) {
//			return 0;
//		}
//	}
//}


