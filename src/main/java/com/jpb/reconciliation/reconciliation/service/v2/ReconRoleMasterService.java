package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;

import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReconRoleMasterService {

    ResponseEntity<RestWithStatusList> createRole(ReconRoleMaster role, String createdBy, boolean force);

    /** Admin-only: creates a Role and activates it immediately, skipping the
     *  DRAFT -> PENDING -> Checker-approval flow entirely. Rejected if the
     *  caller isn't an Admin (KAL_ADMIN / BANK_ADMIN / BRANCH_ADMIN). */
    ResponseEntity<RestWithStatusList> createRoleActive(ReconRoleMaster role, String createdBy, boolean force);

    ResponseEntity<RestWithStatusList> getAllRoles();

    ResponseEntity<RestWithStatusList> getRoleById(Long roleId);

    ResponseEntity<RestWithStatusList> getRolesByStatus(String status);

    ResponseEntity<RestWithStatusList> getRolesByType(String roleType);

    ResponseEntity<RestWithStatusList> updateRole(Long roleId, ReconRoleMaster role, String updatedBy);

    ResponseEntity<RestWithStatusList> submitForApproval(Long roleId, String submittedBy);

    ResponseEntity<RestWithStatusList> approveRole(Long roleId, String approvedBy);

    ResponseEntity<RestWithStatusList> updateStatus(Long roleId, String status, String updatedBy);

    ResponseEntity<RestWithStatusList> deleteRole(Long roleId);

    ResponseEntity<RestWithStatusList> checkRoleCodeExists(String roleCode);

    /** Menu IDs currently assigned to this role (its privileges). */
    ResponseEntity<RestWithStatusList> getPrivileges(Long roleId);

    /** Replaces this role's privilege set with exactly the given menu IDs. For a
     *  BRANCH_ADMIN_DEFAULT/BANK_ADMIN_DEFAULT role, its system-default menus (see
     *  getLockedPrivileges) are always kept even if missing from menuIds — a Bank Admin may add
     *  menus to the role but can never remove the ones it was bootstrapped with. */
    ResponseEntity<RestWithStatusList> savePrivileges(Long roleId, List<Long> menuIds, String updatedBy);

    /** Ensures the Default Dashboard fallback grant exists for a business role (idempotent, never
     *  removes it). Exposed so menu deletion (MenuMasterServiceImpl.removeMenu) can re-run it inside
     *  its own transaction after a delete. Single source of truth for the fallback grant — no other
     *  code should insert the Default Dashboard mapping by hand. Skips admin/bootstrap role types. */
    void reconcileDefaultDashboard(Long roleId);

    /** Menu IDs in this role that are the system-default bootstrap set (Dashboard, My Organization,
     *  Administration, …) and therefore read-only — empty for any role that isn't a
     *  BRANCH_ADMIN_DEFAULT/BANK_ADMIN_DEFAULT bootstrap role. */
    ResponseEntity<RestWithStatusList> getLockedPrivileges(Long roleId);

    /** Roles belonging to a specific Bank/Branch — derived from RCN_RECON_USER
     *  (no BANK_ID column on RECON_ROLE_MASTER itself). Covers both roles already
     *  assigned to one of that bank's users, and roles created by one of that
     *  bank's users but not yet assigned to anyone. */
    ResponseEntity<RestWithStatusList> getRolesByBankId(Long bankId);

    /** Roles the caller is allowed to see. An Admin sees their institution's roles; anyone else
     *  sees only the roles of their own subtree (never a parent's or an Admin's), and never a
     *  Checker role. Scope comes from the JWT, so it cannot be widened by the client. */
    ResponseEntity<RestWithStatusList> getRolesVisibleTo(String username);

    /** Product IDs this role is restricted to (C_ROLE_PRODUCT_MAP). Empty means
     *  no restriction has been configured yet. */
    ResponseEntity<RestWithStatusList> getRoleProducts(Long roleId);

    /** Replaces this role's product restriction set with exactly the given product IDs. */
    ResponseEntity<RestWithStatusList> saveRoleProducts(Long roleId, List<Long> productIds, String updatedBy);

    /** Sets which Bank Type(s) (Issuer/Acquirer, comma-separated) this role is scoped to. */
    ResponseEntity<RestWithStatusList> saveRoleBankTypeScope(Long roleId, String bankTypeScope, String updatedBy);

    /** PENDING roles visible to this Checker — scoped to their own bank/branch (never a
     *  different one), and for a plain Checker (not an Admin) further scoped to only roles
     *  whose own product restriction overlaps their own. KAL_ADMIN sees everything. */
    ResponseEntity<RestWithStatusList> getPendingRolesForChecker(String checkerUsername);

    /** The Bank/Branch that actually owns this role, resolved from one of its current holders
     *  (RECON_ROLE_MASTER has no BANK_ID of its own). This is normally the caller's own
     *  institution, but for a BRANCH_ADMIN_DEFAULT role it's a CHILD branch — a Bank Admin
     *  managing that role's privileges must see the BRANCH's own onboarded products/bank type,
     *  not their own. Empty when the role has no holder yet (a brand-new, unassigned role). */
    ResponseEntity<RestWithStatusList> getOwningBank(Long roleId);
}
