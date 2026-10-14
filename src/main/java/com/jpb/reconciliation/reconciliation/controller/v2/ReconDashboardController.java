//package com.jpb.reconciliation.reconciliation.controller;
//
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.dto.UpiDashboardResponse;
//import com.jpb.reconciliation.reconciliation.service.DashboardService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Collections;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/recon/dashboard")
//@RequiredArgsConstructor
//@Tag(name = "Recon Dashboard", description = "KPI cards for any Recon process")
//public class ReconDashboardController {
//
//    private final DashboardService service;
//
//    // GET /api/v1/recon/dashboard/latest
//    @GetMapping("/latest")
//    @Operation(summary = "Latest dashboard across all recon processes")
//    public ResponseEntity<RestWithStatusList> getLatestDashboard() {
//        try {
//            UpiDashboardResponse data = service.getLatestDashboard();
//            if (data == null) {
//                return ResponseEntity.ok(buildFailure("No reconciliation data found."));
//            }
//            return ResponseEntity.ok(buildSuccess(data));
//        } catch (Exception e) {
//            log.error("Error in getLatestDashboard : {}", e.getMessage());
//            return ResponseEntity.ok(buildFailure(e.getMessage()));
//        }
//    }
//
//    // GET /api/v1/recon/dashboard/{processId}
//    @GetMapping("/{processId}")
//    @Operation(summary = "Dashboard by RPM_PROCESS_ID")
//    public ResponseEntity<RestWithStatusList> getDashboardByProcessId(
//            @Parameter(description = "RPM_PROCESS_ID from RCN_PROCESS_DEF_MAST")
//            @PathVariable Long processId) {
//        try {
//            UpiDashboardResponse data = service.getDashboardByProcessId(processId);
//            if (data == null) {
//                return ResponseEntity.ok(buildFailure("No data found for processId: " + processId));
//            }
//            return ResponseEntity.ok(buildSuccess(data));
//        } catch (Exception e) {
//            log.error("Error in getDashboardByProcessId processId={} : {}", processId, e.getMessage());
//            return ResponseEntity.ok(buildFailure(e.getMessage()));
//        }
//    }
//
//    // ------------------------------------------------------------------
//    private RestWithStatusList buildSuccess(Object data) {
//        return RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Request executed successfully")
//                .data(Collections.singletonList(data))
//                .build();
//    }
//
//    private RestWithStatusList buildFailure(String msg) {
//        return RestWithStatusList.builder()
//                .status(CommonConstants.FAILURE)
//                .statusMsg(msg)
//                .data(Collections.emptyList())
//                .build();
//    }
//}

