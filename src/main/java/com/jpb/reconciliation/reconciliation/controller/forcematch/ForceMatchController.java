package com.jpb.reconciliation.reconciliation.controller.forcematch;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.forcematch.ForceMatchExecutionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller — Execute SP_FORCE_MATCH
 *
 * Base URL : /api/v1/force-match/execute
 *
 * ┌──────────┬───────────────────────────────────┬──────────────────────────────────┐
 * │ Method │ URL │ Purpose │
 * ├──────────┼───────────────────────────────────┼──────────────────────────────────┤
 * │ POST │ / │ Execute SP_FORCE_MATCH │
 * └──────────┴───────────────────────────────────┴──────────────────────────────────┘
 *
 * Request body: { "processId": "1001", "userId": 99 }
 *
 * Response (RestWithStatusList): { "status": CommonConstants.SUCCESS, "statusMsg": "Force
 * match executed successfully", "data": [{ "processId": "1001", "error": "OK",
 * "knockoffFlag": "A", "ttumId": 12345, "ttumError": 0 }] }
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/force-match/execute")
@RequiredArgsConstructor
@Tag(name = "Force Match - Execute", description = "Executes SP_FORCE_MATCH stored procedure")
public class ForceMatchController {

	private final ForceMatchExecutionService executionService;

	@Operation(summary = "Execute SP_FORCE_MATCH for a given process ID and user")
	@PostMapping
	public ResponseEntity<RestWithStatusList> execute(@RequestBody ExecuteRequest request, @AuthenticationPrincipal UserDetails userDetails) {
		return executionService.executeForceMatch(request.getProcessId(), userDetails);
	}

	// ── Inner request DTO (kept close to the controller; no extra file needed) ──
	@lombok.Data
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	public static class ExecuteRequest {
		private Long processId;
	}
}