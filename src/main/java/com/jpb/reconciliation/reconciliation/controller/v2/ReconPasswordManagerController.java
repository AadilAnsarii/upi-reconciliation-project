package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.ReconPasswordManagerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/password")
public class ReconPasswordManagerController {

    @Autowired
    private ReconPasswordManagerService reconPasswordManagerService;

    @PostMapping("/set/{userId}")
    public ResponseEntity<RestWithStatusList> setPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        String updatedBy  = body.getOrDefault("updatedBy", CommonConstants.SYSTEM);
        return reconPasswordManagerService.setPassword(userId, newPassword, updatedBy);
    }

    @PostMapping("/change/{userId}")
    public ResponseEntity<RestWithStatusList> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String updatedBy   = body.getOrDefault("updatedBy", "SELF");
        return reconPasswordManagerService.changePassword(userId, oldPassword, newPassword, updatedBy);
    }

    @PostMapping("/forgot/{userId}")
    public ResponseEntity<RestWithStatusList> generateResetToken(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> body) {
        String createdBy = (body != null) ? body.getOrDefault("createdBy", CommonConstants.SYSTEM) : CommonConstants.SYSTEM;
        return reconPasswordManagerService.generateResetToken(userId, createdBy);
    }

    @PostMapping("/reset")
    public ResponseEntity<RestWithStatusList> resetPasswordByToken(
            @RequestBody Map<String, String> body) {
        String token       = body.get("token");
        String newPassword = body.get("newPassword");
        return reconPasswordManagerService.resetPasswordByToken(token, newPassword);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<RestWithStatusList> getByUserId(@PathVariable Long userId) {
        return reconPasswordManagerService.getByUserId(userId);
    }
}
