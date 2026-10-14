package com.jpb.reconciliation.reconciliation.controller.forcematch;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.forcematch.ActionDef;
import com.jpb.reconciliation.reconciliation.service.forcematch.ActionDefService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller — Force Match Action Definitions
 *
 * Base URL : /api/v1/force-match/action-config
 *
 * ┌──────────┬──────────────────────────────────────┬─────────────────────────────┐
 * │ Method   │ URL                                  │ Purpose                     │
 * ├──────────┼──────────────────────────────────────┼─────────────────────────────┤
 * │ GET      │ /                                    │ All action definitions      │
 * │ GET      │ /{id}                                │ Single by PK                │
 * │ GET      │ /by-table/{tableName}                │ Filter by data table name   │
 * │ POST     │ /                                    │ Create                      │
 * │ PUT      │ /{id}                                │ Update                      │
 * │ DELETE   │ /{id}                                │ Delete                      │
 * └──────────┴──────────────────────────────────────┴─────────────────────────────┘
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/force-match/action-config")
@RequiredArgsConstructor
@Tag(name = "Force Match - Action Config", description = "CRUD for RCN_MANREC_ACTION_DEF_MAST")
public class ActionDefController {

    private final ActionDefService service;

    @Operation(summary = "Get all action definitions")
    @GetMapping
    public ResponseEntity<RestWithStatusList> getAll() {
        log.info("GET /api/v1/force-match/action-config");
        return ResponseEntity.ok(service.getAll());
    }

    @Operation(summary = "Get action definition by ID")
    @GetMapping("/{id}")
    public ResponseEntity<RestWithStatusList> getById(@PathVariable Long id) {
        log.info("GET /api/v1/force-match/action-config/{}", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Get action definitions by data table name")
    @GetMapping("/by-table/{tableName}")
    public ResponseEntity<RestWithStatusList> getByTable(@PathVariable String tableName) {
        return ResponseEntity.ok(service.getByTable(tableName));
    }

    @Operation(summary = "Create a new action definition")
    @PostMapping
    public ResponseEntity<RestWithStatusList> create(@RequestBody ActionDef request) {
        log.info("POST /api/v1/force-match/action-config debit={}", request.getRmtDebitAcct());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @Operation(summary = "Update an existing action definition")
    @PutMapping("/{id}")
    public ResponseEntity<RestWithStatusList> update(
            @PathVariable Long id,
            @RequestBody  ActionDef request) {
        log.info("PUT /api/v1/force-match/action-config/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @Operation(summary = "Delete an action definition")
    @DeleteMapping("/{id}")
    public ResponseEntity<RestWithStatusList> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/force-match/action-config/{}", id);
        return ResponseEntity.ok(service.delete(id));
    }
}
