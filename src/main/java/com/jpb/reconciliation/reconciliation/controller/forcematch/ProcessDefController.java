package com.jpb.reconciliation.reconciliation.controller.forcematch;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.forcematch.ProcessDef;
import com.jpb.reconciliation.reconciliation.service.forcematch.ProcessDefService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller — Force Match Process Configuration
 *
 * Base URL : /api/v1/force-match/process-config
 *
 * ┌──────────┬──────────────────────────────────────┬─────────────────────────────┐
 * │ Method   │ URL                                  │ Purpose                     │
 * ├──────────┼──────────────────────────────────────┼─────────────────────────────┤
 * │ GET      │ /                                    │ All configs                 │
 * │ GET      │ /{id}                                │ Single by PK                │
 * │ GET      │ /process/{processId}                 │ All for a process           │
 * │ GET      │ /process-ids                         │ Distinct process IDs        │
 * │ GET      │ /status/{status}                     │ Filter by Y or N            │
 * │ POST     │ /                                    │ Create                      │
 * │ PUT      │ /{id}                                │ Update                      │
 * │ PATCH    │ /{id}/toggle-status                  │ Toggle Y ↔ N               │
 * │ DELETE   │ /{id}                                │ Delete                      │
 * └──────────┴──────────────────────────────────────┴─────────────────────────────┘
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/force-match/process-config")
@RequiredArgsConstructor
@Tag(name = "Force Match - Process Config", description = "CRUD for RCN_MANREC_PROCESS_DEF_MAST")
public class ProcessDefController {

    private final ProcessDefService service;

    @Operation(summary = "Get all process configurations")
    @GetMapping
    public ResponseEntity<RestWithStatusList> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @Operation(summary = "Get process config by primary key (rmpActionId)")
    @GetMapping("/{id}")
    public ResponseEntity<RestWithStatusList> getById(@PathVariable Long id) {
        log.info("GET /api/v1/force-match/process-config/{}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Get all configs for a given process ID")
    @GetMapping("/process/{processId}")
    public ResponseEntity<RestWithStatusList> getByProcessId(@PathVariable Long processId) {
        log.info("GET /api/v1/force-match/process-config/process/{}", processId);
        return ResponseEntity.ok(service.getByProcessId(processId));
    }

    @Operation(summary = "Get distinct process IDs (for dropdown population)")
    @GetMapping("/process-ids")
    public ResponseEntity<RestWithStatusList> getDistinctProcessIds() {
        return ResponseEntity.ok(service.getDistinctProcessIds());
    }

    @Operation(summary = "Filter configs by status (Y=active, N=inactive)")
    @GetMapping("/status/{status}")
    public ResponseEntity<RestWithStatusList> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @Operation(summary = "Create a new process configuration")
    @PostMapping
    public ResponseEntity<RestWithStatusList> create(@RequestBody ProcessDef request) {
        log.info("POST /api/v1/force-match/process-config processId={}", request.getRmpProcessId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @Operation(summary = "Update existing process configuration")
    @PutMapping("/{id}")
    public ResponseEntity<RestWithStatusList> update(
            @PathVariable Long id,
            @RequestBody  ProcessDef request) {
        log.info("PUT /api/v1/force-match/process-config/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @Operation(summary = "Toggle config status Y ↔ N")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<RestWithStatusList> toggleStatus(@PathVariable Long id) {
        log.info("PATCH /api/v1/force-match/process-config/{}/toggle-status", id);
        return ResponseEntity.ok(service.toggleStatus(id));
    }

    @Operation(summary = "Delete a process configuration")
    @DeleteMapping("/{id}")
    public ResponseEntity<RestWithStatusList> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/force-match/process-config/{}", id);
        return ResponseEntity.ok(service.delete(id));
    }
}
