package com.jpb.reconciliation.reconciliation.entity.v2;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RCN_RECON_USER")
public class ReconUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "BANK_ID")
    private Long bankId;

    @Column(name = "USERNAME", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "FULL_NAME", length = 200, nullable = false)
    private String fullName;

    @Column(name = "EMAIL", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "MOBILE_NUMBER", length = 20)
    private String mobileNumber;

    @Column(name = "USER_TYPE", length = 30, nullable = false)
    private String userType;

    @Column(name = "CONTACT_RANK", length = 10)
    private String contactRank;

    @Column(name = "ROLE_ID")
    private Long roleId;

    // Request-only: AddUser.jsx's Role dropdown sends the role's NAME (e.g. "MAKER"), not
    // its ID — resolved to roleId in NewReconUserServiceImpl.createUser(). "DEFAULT" means
    // no specific role. Never persisted itself.
    @JsonAlias("role")
    @Transient
    private String roleName;

    @Column(name = "PASSWORD_HASH", length = 255)
    private String passwordHash;

    @Column(name = "PASSWORD_SET", nullable = false)
    private Integer passwordSet = 0;

    @Column(name = "PASSWORD_UPDATED_AT")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "DESIGNATION", length = 100)
    private String designation;

    @Column(name = "DEPARTMENT", length = 150)
    private String department;

    // Employee classification — separate from USER_TYPE (which is the
    // institution-role: KAL_ADMIN/BANK_ADMIN/BRANCH_ADMIN/BANK_USER/BRANCH_USER)
    @Column(name = "EMPLOYMENT_TYPE", length = 20)
    private String employmentType;

    @Column(name = "EXTERNAL_DEPARTMENT_NAME", length = 200)
    private String externalDepartmentName;

    @Column(name = "EXTERNAL_SUPERVISOR_NAME", length = 100)
    private String externalSupervisorName;

    @Column(name = "EXTERNAL_SUPERVISOR_EMAIL", length = 150)
    private String externalSupervisorEmail;

    @Column(name = "EXTERNAL_SUPERVISOR_PHONE", length = 20)
    private String externalSupervisorPhone;

    // Default fallback if a code path creates a user without explicitly setting status.
    // PENDING_APPROVAL — ACTIVE_PENDING is reserved exclusively for the Inactive->reactivating
    // scheduling flow, never for a newly-created/not-yet-approved user.
    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "PENDING_APPROVAL";

    @Column(name = "PRE_BLOCK_STATUS", length = 20)
    private String preBlockStatus;

    @Column(name = "INACTIVATE_SCHEDULED_AT")
    private LocalDateTime inactivateScheduledAt;

    @Column(name = "INACTIVATE_SCHEDULED_BY", length = 100)
    private String inactivateScheduledBy;

    @Column(name = "REACTIVATE_SCHEDULED_AT")
    private LocalDateTime reactivateScheduledAt;

    @Column(name = "REACTIVATE_SCHEDULED_BY", length = 100)
    private String reactivateScheduledBy;

    @Column(name = "BLOCK_SCHEDULED_AT")
    private LocalDateTime blockScheduledAt;

    @Column(name = "BLOCK_SCHEDULED_BY", length = 100)
    private String blockScheduledBy;

    @Column(name = "BLOCK_REASON", length = 500)
    private String blockReason;

    @Column(name = "PARENT_USER_ID")
    private Long parentUserId;

    @Column(name = "PRE_DELEGATION_PARENT_ID")
    private Long preDelegationParentId;

    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLogin;

    @Column(name = "APPROVED_YN", length = 1, nullable = false)
    private String approvedYn = "N";

    @Column(name = "APPROVED_BY", length = 100)
    private String approvedBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    // Remembers the user's STATUS immediately before a bank-wide PRODUCT_EXPIRY_HOLD
    // was applied (all of the bank's products expired), so it can be restored exactly
    // if a product's validity is renewed within the grace period.
    @Column(name = "PRE_PRODUCT_HOLD_STATUS", length = 20)
    private String preProductHoldStatus;

    // ── Transient: replacement awareness for list/hierarchy screens ─────────────
    // Read-only, populated per-request by NewReconUserServiceImpl.enrichWithReplacement() —
    // never persisted. Mirrors ReconBankMasterController.getAllBankAdmins()'s per-admin-row
    // shape (replacementAdminRow/replacementStatus/replacedByUsername), but for a regular user
    // row (My Organization > Hierarchy / User Status). replacementAdminRow=true means THIS row
    // is itself someone else's active/finalized replacement; replacementStatus/replacedByUsername
    // are set instead when THIS user has an active/finalized replacement covering for them.
    @Transient private boolean replacementAdminRow;
    @Transient private String replacementStatus;
    @Transient private String replacedByUsername;
}
