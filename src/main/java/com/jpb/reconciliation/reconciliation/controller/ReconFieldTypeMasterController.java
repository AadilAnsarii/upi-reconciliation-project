package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.ReconFieldTypeMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/field-types")
@RequiredArgsConstructor
@Tag(name = "Recon Field Type Master", description = "APIs for managing reconciliation field types")
public class ReconFieldTypeMasterController {

    private final ReconFieldTypeMasterService service;

    @GetMapping
    @Operation(summary = "Get all field types", description = "Returns a list of all reconciliation field type master records")
    public ResponseEntity<RestWithStatusList> getAllFieldTypes() {
        return ResponseEntity.ok(service.getAllFieldTypes());
    }
}