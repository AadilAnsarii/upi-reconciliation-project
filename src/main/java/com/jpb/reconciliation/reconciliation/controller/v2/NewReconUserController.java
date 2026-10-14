package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.service.NewReconUserService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v2/user")
@CrossOrigin(origins = "*")
public class NewReconUserController {

    private static final Logger logger = LoggerFactory.getLogger(NewReconUserController.class);

    @Autowired
    private NewReconUserService newReconUserService;

    @Operation(summary = "Create a new user")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createUser(
            @RequestBody ReconUser user,
            Authentication authentication) {
        String createdBy = resolveUser(authentication);
        logger.info("Create user request: {} by {}", user.getUsername(), createdBy);
        return newReconUserService.createUser(user, createdBy);
    }

    @Operation(summary = "Get all users")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllUsers() {
        return newReconUserService.getAllUsers();
    }

    @Operation(summary = "Users visible to the caller (Admin → institution; otherwise → own subtree only)")
    @GetMapping(value = "/visible", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUsersVisibleTo(Authentication authentication) {
        return newReconUserService.getUsersVisibleTo(resolveUser(authentication));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping(value = "/get/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUserById(@PathVariable Long userId) {
        return newReconUserService.getUserById(userId);
    }

    @Operation(summary = "Get user by username")
    @GetMapping(value = "/get-by-username", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUserByUsername(@RequestParam String username) {
        return newReconUserService.getUserByUsername(username);
    }

    @Operation(summary = "Get users by bank code")
    @GetMapping(value = "/get-by-bank/{bankCode}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUsersByBankCode(@PathVariable String bankCode) {
        return newReconUserService.getUsersByBankCode(bankCode);
    }

    @Operation(summary = "Get users by bank/branch ID")
    @GetMapping(value = "/get-by-bank-id/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUsersByBankId(@PathVariable Long bankId) {
        return newReconUserService.getUsersByBankId(bankId);
    }

    @Operation(summary = "Get users assigned to a role")
    @GetMapping(value = "/get-by-role/{roleId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUsersByRoleId(@PathVariable Long roleId) {
        return newReconUserService.getUsersByRoleId(roleId);
    }

    @Operation(summary = "Get users by status")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUsersByStatus(@RequestParam String status) {
        return newReconUserService.getUsersByStatus(status);
    }

    @Operation(summary = "Get users by type")
    @GetMapping(value = "/get-by-type", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getUsersByType(@RequestParam String userType) {
        return newReconUserService.getUsersByType(userType);
    }

    @Operation(summary = "Pending users visible to this Checker (bank/branch + product scope)")
    @GetMapping(value = "/checker-queue", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getPendingUsersForChecker(Authentication authentication) {
        return newReconUserService.getPendingUsersForChecker(resolveUser(authentication));
    }

    @Operation(summary = "Update user details")
    @PutMapping(value = "/update/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateUser(
            @PathVariable Long userId,
            @RequestBody ReconUser user,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Update user request for ID: {} by {}", userId, updatedBy);
        return newReconUserService.updateUser(userId, user, updatedBy);
    }

    @Operation(summary = "Self-service Profile page: update the logged-in user's own mobile number — the only field a user may edit about themselves. Applies immediately, no admin/maker-checker gate.")
    @PatchMapping(value = "/update-mobile", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateMyMobile(
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        String username = resolveUser(authentication);
        return newReconUserService.updateOwnMobileNumber(username, body.get("mobileNumber"));
    }

    @Operation(summary = "Update user status")
    @PatchMapping(value = "/update-status/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long userId,
            @RequestParam String status,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Update user status for ID: {} to {} by {}", userId, status, updatedBy);
        return newReconUserService.updateStatus(userId, status, updatedBy);
    }

    @Operation(summary = "Approve user")
    @PostMapping(value = "/approve/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> approveUser(
            @PathVariable Long userId,
            Authentication authentication) {
        String approvedBy = resolveUser(authentication);
        logger.info("Approve user request for ID: {} by {}", userId, approvedBy);
        return newReconUserService.approveUser(userId, approvedBy);
    }

    @Operation(summary = "Soft delete user")
    @DeleteMapping(value = "/delete/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteUser(@PathVariable Long userId) {
        return newReconUserService.deleteUser(userId);
    }

    @Operation(summary = "Check if username exists")
    @GetMapping(value = "/check-username", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkUsernameExists(@RequestParam String username) {
        return newReconUserService.checkUsernameExists(username);
    }

    @Operation(summary = "Check if email exists")
    @GetMapping(value = "/check-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkEmailExists(@RequestParam String email) {
        return newReconUserService.checkEmailExists(email);
    }

    @Operation(summary = "Block a user (stores pre-block status)")
    @PostMapping(value = "/block/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> blockUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Block user request for ID: {} by {}", userId, updatedBy);
        return newReconUserService.blockUser(userId, reason, updatedBy);
    }

    @Operation(summary = "Unblock a user (restores pre-block status)")
    @PostMapping(value = "/unblock/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> unblockUser(
            @PathVariable Long userId,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Unblock user request for ID: {} by {}", userId, updatedBy);
        return newReconUserService.unblockUser(userId, updatedBy);
    }

    @Operation(summary = "Schedule user inactivation at a future datetime (ISO format: 2025-01-15T10:30:00)")
    @PostMapping(value = "/schedule-inactivate/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleInactivate(
            @PathVariable Long userId,
            @RequestParam String scheduledAt,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = LocalDateTime.parse(scheduledAt);
        logger.info("Schedule inactivate for userId: {} at {} by {}", userId, scheduledAt, scheduledBy);
        return newReconUserService.scheduleInactivate(userId, dateTime, scheduledBy);
    }

    @Operation(summary = "Schedule user reactivation at a future datetime (ISO format: 2025-01-15T10:30:00)")
    @PostMapping(value = "/schedule-reactivate/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleReactivate(
            @PathVariable Long userId,
            @RequestParam String scheduledAt,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = LocalDateTime.parse(scheduledAt);
        logger.info("Schedule reactivate for userId: {} at {} by {}", userId, scheduledAt, scheduledBy);
        return newReconUserService.scheduleReactivate(userId, dateTime, scheduledBy);
    }

    @Operation(summary = "Schedule user block at a future datetime (ISO format: 2025-01-15T10:30:00)")
    @PostMapping(value = "/schedule-block/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long userId,
            @RequestParam String scheduledAt,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = LocalDateTime.parse(scheduledAt);
        logger.info("Schedule block for userId: {} at {} by {}", userId, scheduledAt, scheduledBy);
        return newReconUserService.scheduleBlock(userId, dateTime, scheduledBy, reason);
    }

    @Operation(summary = "Cancel a scheduled status change (scheduleType: INACTIVATE / REACTIVATE / BLOCK)")
    @PostMapping(value = "/cancel-schedule/{userId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> cancelSchedule(
            @PathVariable Long userId,
            @RequestParam String scheduleType,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Cancel {} schedule for userId: {} by {}", scheduleType, userId, updatedBy);
        return newReconUserService.cancelSchedule(userId, scheduleType, updatedBy);
    }

    private String resolveUser(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
    }
}
