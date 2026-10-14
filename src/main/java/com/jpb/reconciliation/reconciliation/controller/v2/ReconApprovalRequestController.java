package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants; 
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconApprovalRequest;
import com.jpb.reconciliation.reconciliation.service.v2.ReconApprovalRequestService;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v2/approval")
@CrossOrigin(origins = "*")
public class ReconApprovalRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ReconApprovalRequestController.class);

    @Autowired
    private ReconApprovalRequestService reconApprovalRequestService;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Operation(summary = "Submit an approval request")
    @PostMapping(value = "/submit", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> submitRequest(@RequestBody ReconApprovalRequest request) {
        logger.info("Approval request submitted: entityType={}, entityId={}", request.getEntityType(), request.getEntityId());
        return reconApprovalRequestService.submitRequest(request);
    }

    /**
     * Read-only Approval Request History, scoped to the caller:
     *   KAL_ADMIN (no bank)          → every request, platform-wide.
     *   Bank / Branch Admin (bankId) → requests whose Maker belongs to their own institution.
     *   Any other user               → only the requests they raised.
     * The scope is derived from the JWT here, never from a parameter, so no one can widen it.
     */
    @Operation(summary = "Approval Request History (read-only, scoped to the caller)")
    @GetMapping(value = "/history", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<ReconUser> meOpt = (userDetails == null)
                ? Optional.empty()
                : reconUserRepository.findByUsername(userDetails.getUsername());
        if (!meOpt.isPresent()) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "No data", java.util.Collections.emptyList()));
        }
        ReconUser me = meOpt.get();
        boolean isAdmin = com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(me.getUserType());
        return reconApprovalRequestService.getHistoryFor(me.getUserId(), me.getBankId(), isAdmin);
    }

    @Operation(summary = "Get all pending approval requests")
    @GetMapping(value = "/pending", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getPendingRequests() {
        return reconApprovalRequestService.getPendingRequests();
    }

    @Operation(summary = "Get approval request by ID")
    @GetMapping(value = "/get/{requestId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestById(@PathVariable Long requestId) {
        return reconApprovalRequestService.getRequestById(requestId);
    }

    @Operation(summary = "Get approval requests by entity type")
    @GetMapping(value = "/get-by-entity-type", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestsByEntityType(@RequestParam String entityType) {
        return reconApprovalRequestService.getRequestsByEntityType(entityType);
    }

    @Operation(summary = "Get approval requests by maker ID")
    @GetMapping(value = "/get-by-maker/{makerId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestsByMakerId(@PathVariable Long makerId) {
        return reconApprovalRequestService.getRequestsByMakerId(makerId);
    }

    @Operation(summary = "Get approval requests by checker ID")
    @GetMapping(value = "/get-by-checker/{checkerId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestsByCheckerId(@PathVariable Long checkerId) {
        return reconApprovalRequestService.getRequestsByCheckerId(checkerId);
    }

    @Operation(summary = "Pending maker UPDATE requests this Checker must decide (bank/branch scoped)")
    @GetMapping(value = "/pending-updates", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getPendingUpdates(@AuthenticationPrincipal UserDetails userDetails) {
        return reconApprovalRequestService.getPendingUpdatesForChecker(userDetails.getUsername());
    }

    @Operation(summary = "Approve or reject a maker UPDATE request (applies the change on approval)")
    @PostMapping(value = "/decide-update/{requestId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> decideUpdate(
            @PathVariable Long requestId,
            @RequestParam String decision,
            @RequestParam(required = false, defaultValue = "") String remarks,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Decide update request {}: {} by {}", requestId, decision, userDetails.getUsername());
        return reconApprovalRequestService.decideUpdateRequest(requestId, userDetails.getUsername(), decision, remarks);
    }

    @Operation(summary = "Approve an approval request")
    @PostMapping(value = "/approve/{requestId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> approveRequest(
            @PathVariable Long requestId,
            @RequestParam Long checkerId,
            @RequestParam(required = false, defaultValue = "") String remarks) {
        logger.info("Approve request: {} by checkerId={}", requestId, checkerId);
        return reconApprovalRequestService.approveRequest(requestId, checkerId, remarks);
    }

    @Operation(summary = "Reject an approval request")
    @PostMapping(value = "/reject/{requestId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> rejectRequest(
            @PathVariable Long requestId,
            @RequestParam Long checkerId,
            @RequestParam(required = false, defaultValue = "") String remarks) {
        logger.info("Reject request: {} by checkerId={}", requestId, checkerId);
        return reconApprovalRequestService.rejectRequest(requestId, checkerId, remarks);
    }
}
