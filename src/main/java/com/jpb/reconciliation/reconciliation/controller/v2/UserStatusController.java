package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.AuditReplacementService;
import com.jpb.reconciliation.reconciliation.service.v2.UserStatusService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/user-status")
@CrossOrigin(origins = "*")
public class UserStatusController {

    @Autowired private UserStatusService userStatusService;
    @Autowired private AuditReplacementService replacementService;

    @PostMapping("/{userId}/schedule-inactivate")
    public ResponseEntity<RestWithStatusList> scheduleInactivate(
            @PathVariable Long userId, Authentication auth) {
        return userStatusService.scheduleInactivate(userId, actor(auth));
    }

    @PostMapping("/{userId}/undo-inactivate")
    public ResponseEntity<RestWithStatusList> undoInactivate(
            @PathVariable Long userId, Authentication auth) {
        return userStatusService.undoInactivate(userId, actor(auth));
    }

    @PostMapping("/{userId}/schedule-reactivate")
    public ResponseEntity<RestWithStatusList> scheduleReactivate(
            @PathVariable Long userId, Authentication auth) {
        return userStatusService.scheduleReactivate(userId, actor(auth));
    }

    @PostMapping("/{userId}/undo-reactivate")
    public ResponseEntity<RestWithStatusList> undoReactivate(
            @PathVariable Long userId, Authentication auth) {
        return userStatusService.undoReactivate(userId, actor(auth));
    }

    @PostMapping("/{userId}/schedule-block")
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return userStatusService.scheduleBlock(userId, body.get("reason"), actor(auth));
    }

    @PostMapping("/{userId}/undo-block")
    public ResponseEntity<RestWithStatusList> undoBlock(
            @PathVariable Long userId, Authentication auth) {
        return userStatusService.undoBlock(userId, actor(auth));
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<RestWithStatusList> blockImmediate(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return userStatusService.blockImmediate(userId, body.get("reason"), actor(auth));
    }

    @PostMapping("/{userId}/unblock")
    public ResponseEntity<RestWithStatusList> unblock(
            @PathVariable Long userId, Authentication auth) {
        return userStatusService.unblock(userId, actor(auth));
    }

    // Schedule replacement pending (before inactivation confirmed)
    @PostMapping("/{userId}/schedule-replacement")
    public ResponseEntity<RestWithStatusList> scheduleReplacement(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return replacementService.schedulePendingReplacement(
                userId,
                body.get("pendingEmail"), body.get("pendingFullName"),
                body.get("pendingMobile"), body.get("orderedBy"),
                body.get("reason"), actor(auth));
    }

    private String actor(Authentication auth) {
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}
