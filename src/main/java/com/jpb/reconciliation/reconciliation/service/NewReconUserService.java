package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public interface NewReconUserService {

    ResponseEntity<RestWithStatusList> createUser(ReconUser user, String createdBy);

    ResponseEntity<RestWithStatusList> getAllUsers();

    ResponseEntity<RestWithStatusList> getUserById(Long userId);

    ResponseEntity<RestWithStatusList> getUserByUsername(String username);

    ResponseEntity<RestWithStatusList> getUsersByBankId(Long bankId);

    /** Users the caller may see. Admin → their institution; anyone else → only their own subtree
     *  (never an ancestor or a sibling). Scope comes from the JWT and cannot be widened. */
    ResponseEntity<RestWithStatusList> getUsersVisibleTo(String username);

    ResponseEntity<RestWithStatusList> getUsersByBankCode(String bankCode);

    ResponseEntity<RestWithStatusList> getUsersByRoleId(Long roleId);

    ResponseEntity<RestWithStatusList> getUsersByStatus(String status);

    ResponseEntity<RestWithStatusList> getUsersByType(String userType);

    ResponseEntity<RestWithStatusList> updateUser(Long userId, ReconUser user, String updatedBy);

    /** Self-service Profile page: the ONLY field a logged-in user may edit about themselves. Applies
     *  immediately for every role (no admin/maker-checker gate) — unlike updateUser, this never
     *  touches role, name, email or any other field, so there is nothing here to route to a Checker. */
    ResponseEntity<RestWithStatusList> updateOwnMobileNumber(String username, String mobileNumber);

    ResponseEntity<RestWithStatusList> updateStatus(Long userId, String status, String updatedBy);

    ResponseEntity<RestWithStatusList> approveUser(Long userId, String approvedBy);

    ResponseEntity<RestWithStatusList> deleteUser(Long userId);

    ResponseEntity<RestWithStatusList> checkUsernameExists(String username);

    ResponseEntity<RestWithStatusList> checkEmailExists(String email);

    ResponseEntity<RestWithStatusList> blockUser(Long userId, String reason, String updatedBy);

    ResponseEntity<RestWithStatusList> unblockUser(Long userId, String updatedBy);

    ResponseEntity<RestWithStatusList> scheduleInactivate(Long userId, LocalDateTime scheduledAt, String scheduledBy);

    ResponseEntity<RestWithStatusList> scheduleReactivate(Long userId, LocalDateTime scheduledAt, String scheduledBy);

    ResponseEntity<RestWithStatusList> scheduleBlock(Long userId, LocalDateTime scheduledAt, String scheduledBy, String reason);

    ResponseEntity<RestWithStatusList> cancelSchedule(Long userId, String scheduleType, String updatedBy);

    /** PENDING_APPROVAL users visible to this Checker — same bank/branch-only + product-scope
     *  overlap rule as ReconRoleMasterService.getPendingRolesForChecker. */
    ResponseEntity<RestWithStatusList> getPendingUsersForChecker(String checkerUsername);
}
