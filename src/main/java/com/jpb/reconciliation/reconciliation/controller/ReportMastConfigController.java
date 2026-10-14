package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.ReportMastConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.ReportMastConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/report-config")
@RequiredArgsConstructor
@Tag(name = "Report Master Config", description = "CRUD + Extraction/Recon template field APIs")
public class ReportMastConfigController {

    private final ReportMastConfigService service;

    @PostMapping
    @Operation(summary = "Create Report Config")
    public ResponseEntity<RestWithStatusList> createReportConfig(
            @RequestBody ReportMastConfigRequest request) {
        return service.createReportConfig(request);
    }

    @GetMapping
    @Operation(summary = "Get All Report Configs")
    public ResponseEntity<RestWithStatusList> getAllReportConfigs() {
        return service.getAllReportConfigs();
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get Report Config by ID")
    public ResponseEntity<RestWithStatusList> getReportConfigById(
            @Parameter(description = "Report ID", example = "10")
            @PathVariable Long reportId) {
        return service.getReportConfigById(reportId);
    }

    @PutMapping("/{reportId}")
    @Operation(summary = "Update Report Config")
    public ResponseEntity<RestWithStatusList> updateReportConfig(
            @PathVariable Long reportId,
            @RequestBody ReportMastConfigRequest request) {
        return service.updateReportConfig(reportId, request);
    }

    @DeleteMapping("/{reportId}")
    @Operation(summary = "Delete Report Config")
    public ResponseEntity<RestWithStatusList> deleteReportConfig(
            @PathVariable Long reportId) {
        return service.deleteReportConfig(reportId);
    }

    @GetMapping("/search")
    @Operation(summary = "Search Report Configs by report name or processId")
    public ResponseEntity<RestWithStatusList> searchReportConfigs(
            @RequestParam(required = false) String reportName,
            @RequestParam(required = false) String processId) {
        return service.searchReportConfigs(reportName, processId);
    }

    @GetMapping("/extraction/templates")
    @Operation(
        summary = "Extraction — Get ALL templates with fileDetails + fieldDetails",
        description = "Calls templateRepo.findAllTemplates() and for each template fetches " +
                      "its linked file (ReconFileDetailsMaster) and all fields " +
                      "(ReconFieldDetailsMaster ordered by column position). " +
                      "Returns a flat list — one entry per template."
    )
    public ResponseEntity<RestWithStatusList> getAllExtractionTemplatesWithFields() {
        return service.getAllExtractionTemplatesWithFields();
    }


    @GetMapping("/recon/processes")
    @Operation(
        summary = "Recon — Get ALL processes each with sourceFile + targetFile (templateDetails + fileDetails + fieldDetails)",
        description = "Calls processRepo.findAll() and for each process loads source and target " +
                      "templateDetails, fileDetails, and fieldDetails using your existing repositories."
    )
    public ResponseEntity<RestWithStatusList> getAllReconProcessesWithFiles() {
        return service.getAllReconProcessesWithFiles();
    }
}