package com.jpb.reconciliation.reconciliation.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.ReconFieldMastService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller for Field Type master APIs.
 * Split from ReconFieldMastController as per Sir's Point 3 — separate controller per resource.
 */
@RestController
@RequestMapping(path = "/api/v2/field/field-types")
@RequiredArgsConstructor
@Tag(name = "Field Type Master", description = "APIs for fetching field types")
public class ReconFieldTypeMastController {

    private final ReconFieldMastService reconFieldMastService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET  /api/v2/field/field-types
    // Returns all rows from recon_field_type_mast
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Get all field types", description = "Returns all records from recon_field_type_mast")
    public ResponseEntity<RestWithMapStatusList> getAllFieldTypes() {
        return reconFieldMastService.getAllFieldTypes();
    }
}
