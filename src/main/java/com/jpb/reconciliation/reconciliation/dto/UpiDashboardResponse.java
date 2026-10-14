package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpiDashboardResponse {

    private Long      reconProcessId;
    private String    processName;
    private LocalDate tranDate;

    private KpiSummary kpiSummary;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class KpiSummary {

        // Card 1 — TOTAL TXNS
        private int    totalTxns;

        // Card 2 — AUTO-RECONCILED
        private int    autoReconciled;
        private double matchRatePercent;

        // Card 3 — EXCEPTIONS
        private int    exceptions;
        private String exceptionsNote;

        // Card 4 — FORCE MATCH ELIGIBLE
        private int    forceMatchEligible;   // TODO: wire table
        private String forceMatchNote;

        // Card 5 — PENDING TTUM APPROVAL
        private int    pendingTtumApproval;  // TODO: wire table
        private String pendingTtumNote;

        // Card 6 — SETTLEMENT NET
        private BigDecimal settlementNet;       // TODO: wire table
        private BigDecimal settlementExpected;
        private BigDecimal settlementDelta;

        // File-wise extraction counts
        private List<FileCount> fileWiseCount;
    }

    // ----------------------------------------------------------------
    //  One entry per RPM_FILE_TYPE slot (1-4)
    // ----------------------------------------------------------------
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FileCount {
        private int    fileSlot;            // 1, 2, 3, 4
        private Long   fileTypeProcessId;   // RPM_FILE_TYPEn value
        private String dataTableName;       // RPM_DATA_TAB_NAMEn
        private String fileName;            // RBP_FILE_NAME
        private int    dataCount;           // RBP_DATA_COUNT
        private String extStatus;           // RBP_EXT_STATUS
    }
}