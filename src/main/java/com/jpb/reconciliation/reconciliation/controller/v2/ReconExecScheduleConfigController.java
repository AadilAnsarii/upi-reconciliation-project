package com.jpb.reconciliation.reconciliation.controller.v2;

import java.util.Collections;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigResponse;
import com.jpb.reconciliation.reconciliation.service.v2.ReconExecScheduleConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v2/templates/{templateId}/schedule-config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Execution Schedule Config v2", description = "Manage CRON / CYCLE / EVENT_BASED execution schedules per template")
public class ReconExecScheduleConfigController {

    private final ReconExecScheduleConfigService scheduleConfigService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v2/templates/{templateId}/schedule-config
    // Upsert — creates if absent, updates if already exists for this template.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Save or update schedule config for a template (upsert)")
    public ResponseEntity<RestWithStatusList> saveScheduleConfig(
            @PathVariable Long templateId,
            @Valid @RequestBody ScheduleConfigRequest request) {

        log.info("POST /api/v2/templates/{}/schedule-config", templateId);
        ScheduleConfigResponse response = scheduleConfigService.saveScheduleConfig(templateId, request);
        return buildResponse("Schedule config saved successfully.", response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v2/templates/{templateId}/schedule-config
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "Get schedule config for a template")
    public ResponseEntity<RestWithStatusList> getScheduleConfig(
            @PathVariable Long templateId) {

        log.info("GET /api/v2/templates/{}/schedule-config", templateId);
        ScheduleConfigResponse response = scheduleConfigService.getScheduleConfig(templateId);
        return buildResponse("Schedule config fetched successfully.", response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/v2/templates/{templateId}/schedule-config/activate
    // Sets IS_ACTIVE = 'Y' — enables automatic execution of this schedule.
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/activate")
    @Operation(summary = "Activate schedule for a template — sets IS_ACTIVE = Y")
    public ResponseEntity<RestWithStatusList> activateScheduleConfig(
            @PathVariable Long templateId) {

        log.info("PATCH /api/v2/templates/{}/schedule-config/activate", templateId);
        scheduleConfigService.activateSchedule(templateId);
        return buildResponse("Schedule activated successfully.", null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/v2/templates/{templateId}/schedule-config/deactivate
    // Sets IS_ACTIVE = 'N' — pauses automatic execution of this schedule.
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/deactivate")
    @Operation(summary = "Deactivate schedule for a template — sets IS_ACTIVE = N")
    public ResponseEntity<RestWithStatusList> deactivateScheduleConfig(
            @PathVariable Long templateId) {

        log.info("PATCH /api/v2/templates/{}/schedule-config/deactivate", templateId);
        scheduleConfigService.deactivateSchedule(templateId);
        return buildResponse("Schedule deactivated successfully.", null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v2/templates/{templateId}/schedule-config
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping
    @Operation(summary = "Delete schedule config for a template")
    public ResponseEntity<RestWithStatusList> deleteScheduleConfig(
            @PathVariable Long templateId) {

        log.info("DELETE /api/v2/templates/{}/schedule-config", templateId);
        scheduleConfigService.deleteScheduleConfig(templateId);
        return buildResponse("Schedule config deleted successfully.", null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helper — builds standard RestWithStatusList response
    // ─────────────────────────────────────────────────────────────────────────
    private ResponseEntity<RestWithStatusList> buildResponse(String message, Object data) {
        return ResponseEntity.ok(RestWithStatusList.builder()
                .status(CommonConstants.SUCCESS)
                .statusMsg(message)
                .data(null != data ? Collections.singletonList(data) : Collections.emptyList())
                .build());
    }
}
