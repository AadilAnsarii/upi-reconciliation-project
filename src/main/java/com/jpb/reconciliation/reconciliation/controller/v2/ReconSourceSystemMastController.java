package com.jpb.reconciliation.reconciliation.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.ReconSourceSystemMastService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Recon Source System API", description = "Operations for Recon Source System master data")
@RestController
@RequestMapping("/api/v2/source-systems")
@RequiredArgsConstructor
public class ReconSourceSystemMastController {

    private final ReconSourceSystemMastService service;

    // ─── GET ALL ACTIVE (default) ─────────────────────────────────────
    @Operation(summary = "Get all active Recon Source Systems",
               description = "Returns all source systems where isActive = 'Y'")
    @ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Active source systems fetched successfully")
    @GetMapping
    public ResponseEntity<RestWithStatusList> getActiveSources() {
        return service.getActiveSources();
    }

}