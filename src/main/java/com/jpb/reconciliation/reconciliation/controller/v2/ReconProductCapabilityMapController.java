package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.ReconProductCapabilityMapService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/product-capability")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReconProductCapabilityMapController {

    private final ReconProductCapabilityMapService capabilityService;

    private String resolveUser(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
    }

    @PostMapping("/grant")
    public ResponseEntity<RestWithStatusList> grant(
            @RequestParam Long productId,
            @RequestParam Long userId,
            @RequestParam String capabilityType,
            @RequestParam String actionType,
            @RequestParam(defaultValue = "false") boolean canDelegate,
            Authentication authentication) {
        return capabilityService.grantCapability(productId, userId, capabilityType, actionType, canDelegate, resolveUser(authentication));
    }

    @PutMapping("/{capabilityId}/revoke")
    public ResponseEntity<RestWithStatusList> revoke(
            @PathVariable Long capabilityId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        return capabilityService.revokeCapability(capabilityId, resolveUser(authentication), reason, false);
    }

    @GetMapping("/pool")
    public ResponseEntity<RestWithStatusList> getPool(
            @RequestParam Long productId,
            @RequestParam String actionType,
            @RequestParam String capabilityType) {
        return capabilityService.getActiveCapabilities(productId, actionType, capabilityType);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<RestWithStatusList> getUserCapabilities(@PathVariable Long userId) {
        return capabilityService.getUserCapabilities(userId);
    }

    @GetMapping("/resolve-approver")
    public ResponseEntity<RestWithStatusList> resolveApprover(
            @RequestParam Long productId,
            @RequestParam String actionType,
            @RequestParam Long makerUserId) {
        return capabilityService.resolveApprover(productId, actionType, makerUserId);
    }
}
