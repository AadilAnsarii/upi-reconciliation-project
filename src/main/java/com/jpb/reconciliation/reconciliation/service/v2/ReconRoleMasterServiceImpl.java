package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.CRoleMenuMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleProductMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReconRoleMasterServiceImpl implements ReconRoleMasterService {

    private static final Logger logger = LoggerFactory.getLogger(ReconRoleMasterServiceImpl.class);

    // Menu names ReconBankMasterServiceImpl.createDefaultAdminMenus grants a bootstrap
    // Bank/Branch Admin role at onboarding time — the Master/Main container names ("Dashboard",
    // "My Organization", "Administration") plus every item under them. A Bank Admin may add MORE
    // menus to a BRANCH_ADMIN_DEFAULT role, but never remove one of these — see
    // resolveLockedMenuIds/savePrivileges/getLockedPrivileges.
    private static final Set<String> BOOTSTRAP_ADMIN_MENU_NAMES = new LinkedHashSet<>(Arrays.asList(
            "Dashboard", "My Organization", "Administration",
            "Overview", "My Hierarchy", "User Status", "Branches", "Branch Onboarding", "Branch Admin Status",
            "Add User", "Add Role", "Add Menu", "User List", "Role List", "Menu List",
            "Handover & Delegation History", "Approval Request History", "Checker Dashboard"
    ));
    private static final Set<String> BOOTSTRAP_ROLE_TYPES = new LinkedHashSet<>(Arrays.asList(
            "BRANCH_ADMIN_DEFAULT", "BANK_ADMIN_DEFAULT"));

    // The role types that receive the Default Dashboard fallback — the /user-portal business roles
    // (same set as isUserPortalRole in savePrivileges). Admin/bootstrap roles
    // (KAL_ADMIN_DEFAULT / BANK_ADMIN_DEFAULT / BRANCH_ADMIN_DEFAULT) always carry their own
    // bootstrap menus and are deliberately excluded — they never get the fallback.
    private static final Set<String> BUSINESS_ROLE_TYPES = new LinkedHashSet<>(Arrays.asList(
            "RECON_USER", "BANK_USER", "BRANCH_USER"));

    // The permanent identity code of the Default Dashboard system menu (see SystemMenuCodes and
    // sql/default_dashboard_system_menu.sql).
    private static final String DEFAULT_DASHBOARD_CODE = "DEFAULT_DASHBOARD";

    // Lazily resolved once and cached for the process lifetime — the DD row's MENU_ID is a
    // sequence PK that never changes. NOTE: if the DD row is ever deleted and re-seeded (getting a
    // new MENU_ID), an application restart (or explicit reset) is required to refresh this.
    private volatile Long defaultDashboardMenuId;

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    private CRoleMenuMapRepository roleMenuMapRepository;

    @Autowired
    private ApprovalAuditRecorder approvalAuditRecorder;

    @Autowired
    private HierarchyScopeService hierarchyScopeService;

    @Autowired
    private WorkflowNotifier workflowNotifier;

    @Autowired
    private MenuMasterRepository menuMasterRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconBankMasterRepository reconBankMasterRepository;

    @Autowired
    private RoleCodeGeneratorService roleCodeGeneratorService;

    @Autowired
    private ReconRoleProductMapRepository roleProductMapRepository;

    @Autowired
    private EmailService emailService;

    private void notifyMakerOfDecision(String submittedByUsername, String itemName, String itemCode, String decision, String decidedBy) {
        if (submittedByUsername == null) return;
        Optional<ReconUser> maker = reconUserRepository.findByUsername(submittedByUsername);
        if (!maker.isPresent()) return;
        emailService.sendWorkflowDecisionNotification(
                maker.get().getEmail(), maker.get().getFullName(),
                "Role", itemName, itemCode, decision, decidedBy);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createRole(ReconRoleMaster role, String createdBy, boolean force) {
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role name is required.", null));
        }
        // Roles are tenant-scoped, not globally unique: Bank A and Bank B may each have a MAKER.
        // A same-name role in the SAME organization isn't an error — it returns DUPLICATE_ROLE_EXISTS
        // so the UI can ask "continue?". force=true means the user already confirmed.
        if (!force && roleExistsInTenant(role.getRoleName(), role.getRoleType(), createdBy)) {
            return ResponseEntity.ok(new RestWithStatusList("DUPLICATE_ROLE_EXISTS",
                    "This role already exists for this organization. Do you still want to continue?", null));
        }
        // Role code is never typed by hand — auto-generated from ROLE_CODE_SEQ/ROLE_CODE_CUSTOM_SEQ
        // (same as the old backend). Reuses the code the Add Role form already reserved/showed the
        // user, unless it's gone stale (taken by another role since then).
        role.setRoleCode(resolveRoleCode(role.getRoleCode(), role.getRoleName()));
        role.setStatus("DRAFT");
        role.setCreatedAt(LocalDateTime.now());
        role.setCreatedBy(createdBy);
        ReconRoleMaster saved = reconRoleMasterRepository.save(role);
        // A brand-new business role has no application menus yet, so give it the Default Dashboard
        // fallback immediately (runs in this @Transactional; skips admin/bootstrap role types).
        reconcileDefaultDashboard(saved.getRoleId());
        logger.info("ReconRoleMaster created: {} by {}", saved.getRoleCode(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS, "Role created successfully.", Collections.singletonList(saved)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createRoleActive(ReconRoleMaster role, String createdBy, boolean force) {
        Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(createdBy);
        if (!actorOpt.isPresent() || !com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(actorOpt.get().getUserType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Only a Bank/Branch/KAL Admin can create a Role that activates immediately.", null));
        }
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role name is required.", null));
        }
        // Tenant-scoped duplicate check (see createRole): same name in the same organization is
        // allowed after the user confirms the prompt (force=true).
        if (!force && roleExistsInTenant(role.getRoleName(), role.getRoleType(), createdBy)) {
            return ResponseEntity.ok(new RestWithStatusList("DUPLICATE_ROLE_EXISTS",
                    "This role already exists for this organization. Do you still want to continue?", null));
        }
        role.setRoleCode(resolveRoleCode(role.getRoleCode(), role.getRoleName()));
        // Not ACTIVE yet — a role with zero privileges is unusable. It only flips to ACTIVE
        // once the Admin actually assigns at least one privilege (see savePrivileges below).
        role.setStatus("DRAFT");
        role.setCreatedAt(LocalDateTime.now());
        role.setCreatedBy(createdBy);
        ReconRoleMaster saved = reconRoleMasterRepository.save(role);
        // Give a brand-new business role the Default Dashboard fallback immediately (skips
        // admin/bootstrap role types; runs in this @Transactional).
        reconcileDefaultDashboard(saved.getRoleId());
        logger.info("ReconRoleMaster created by Admin (awaiting privileges): {} by {}", saved.getRoleCode(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS, "Role created and activated.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllRoles() {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findAll();
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Roles fetched.", roles));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRoleById(Long roleId) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByStatus(String status) {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findByStatus(status);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Roles fetched by status.", roles));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByType(String roleType) {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findByRoleType(roleType);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Roles fetched by type.", roles));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateRole(Long roleId, ReconRoleMaster role, String updatedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();

        // Maker-checker on UPDATE: Admin applies immediately; a Maker's edit is held as a PENDING
        // approval request (proposed changes stashed, live role untouched) until a Checker approves.
        boolean isAdmin = reconUserRepository.findByUsername(updatedBy)
                .map(a -> com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(a.getUserType()))
                .orElse(false);
        if (!isAdmin) {
            java.util.Map<String, Object> changes = new java.util.LinkedHashMap<>();
            if (role.getRoleName() != null) changes.put("roleName", role.getRoleName());
            if (role.getRoleDesc() != null) changes.put("roleDesc", role.getRoleDesc());
            if (role.getRoleType() != null) changes.put("roleType", role.getRoleType());
            approvalAuditRecorder.recordSubmission(ApprovalAuditRecorder.ENTITY_ROLE, roleId,
                    ApprovalAuditRecorder.ACTION_UPDATE, updatedBy, ApprovalJson.write(changes));
            logger.info("Role update by Maker {} submitted for approval (role {})", updatedBy, roleId);
            return ResponseEntity.ok(new RestWithStatusList("SUBMITTED_FOR_APPROVAL",
                    "Your changes have been submitted to the Checker for approval.", null));
        }

        if (role.getRoleName() != null) existing.setRoleName(role.getRoleName());
        if (role.getRoleDesc() != null) existing.setRoleDesc(role.getRoleDesc());
        if (role.getRoleType() != null) existing.setRoleType(role.getRoleType());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> submitForApproval(Long roleId, String submittedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }
        // Only real application menus count — a role holding just the Default Dashboard fallback is
        // still unprivileged and must not be submittable.
        if (!hasApplicationMenuGrant(roleId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList(CommonConstants.FAILURE,
                            "Assign at least one privilege to this role before submitting for approval.", null));
        }

        ReconRoleMaster existing = opt.get();
        existing.setStatus("PENDING");
        existing.setSubmittedBy(submittedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(submittedBy);
        reconRoleMasterRepository.save(existing);
        approvalAuditRecorder.recordSubmission(ApprovalAuditRecorder.ENTITY_ROLE, roleId,
                ApprovalAuditRecorder.ACTION_CREATE, submittedBy);
        workflowNotifier.notifySubmission("Role", existing.getRoleName(), existing.getRoleCode(), submittedBy,
                checker -> isVisibleToChecker(checker, existing.getCreatedBy(), existing.getRoleId()));
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role submitted for approval.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> approveRole(Long roleId, String approvedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        String priorStatus = existing.getStatus();
        // approveRole doubles as "approve a PENDING role" (Checker decision) AND "reactivate an
        // INACTIVE role" (an unrelated Admin action) — both flip TO ACTIVE, so only the two
        // terminal-decision states are blocked here. A role a Checker already rejected cannot then
        // be approved by a second Checker; an already-ACTIVE role cannot be double-approved.
        if (CommonConstants.ACTIVE.equals(priorStatus) || "REJECTED".equals(priorStatus)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
        }
        // Atomic claim: if another Checker's decision landed between our read above and this write,
        // rowsUpdated is 0 and we must not proceed (first completed decision always wins).
        int rowsUpdated = reconRoleMasterRepository.compareAndSetStatus(roleId, priorStatus, CommonConstants.ACTIVE);
        if (rowsUpdated == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
        }
        existing = reconRoleMasterRepository.findById(roleId).orElse(existing);
        existing.setApprovedBy(approvedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(approvedBy);
        reconRoleMasterRepository.save(existing);
        approvalAuditRecorder.recordDecision(ApprovalAuditRecorder.ENTITY_ROLE, roleId, approvedBy, "APPROVED", null);
        notifyMakerOfDecision(existing.getSubmittedBy(), existing.getRoleName(), existing.getRoleCode(), "Approved", approvedBy);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role approved and activated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long roleId, String status, String updatedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        String submittedBy = existing.getSubmittedBy();
        if ("REJECTED".equalsIgnoreCase(status)) {
            // This is a Checker's disapprove decision on a pending role — only valid from PENDING.
            // Guards both the sequential race (a second Checker rejecting an already-decided role)
            // and the true-concurrent race (CAS below) so the first completed decision always wins.
            String priorStatus = existing.getStatus();
            if (!"PENDING".equals(priorStatus)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
            }
            int rowsUpdated = reconRoleMasterRepository.compareAndSetStatus(roleId, "PENDING", "REJECTED");
            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
            }
            existing = reconRoleMasterRepository.findById(roleId).orElse(existing);
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setUpdatedBy(updatedBy);
            reconRoleMasterRepository.save(existing);
            approvalAuditRecorder.recordDecision(ApprovalAuditRecorder.ENTITY_ROLE, roleId, updatedBy, "REJECTED", null);
            notifyMakerOfDecision(submittedBy, existing.getRoleName(), existing.getRoleCode(), "Rejected", updatedBy);
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role status updated.", null));
        }
        // Every other transition (e.g. an Admin's ACTIVE <-> INACTIVE toggle from ViewRole) is
        // unrelated to a Checker decision and keeps its original unconditional behaviour.
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role status updated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteRole(Long roleId) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        existing.setStatus(CommonConstants.INACTIVE);
        existing.setUpdatedAt(LocalDateTime.now());
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role deactivated successfully.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkRoleCodeExists(String roleCode) {
        boolean exists = reconRoleMasterRepository.existsByRoleCode(roleCode);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getPrivileges(Long roleId) {
        List<Long> menuIds = roleMenuMapRepository.findMenuIdsByRoleId(roleId);
        // The Menu Access Tree displays the ORIGINAL Admin-portal menu rows (twins are hidden
        // from it — see getMenusByBankId). If this role's C_ROLE_MENU_MAP points at a twin
        // (its own MENU_ID differs from the original's), translate it back to the original's
        // ID here, otherwise its checkbox would never show as checked even though the
        // privilege genuinely exists.
        List<Long> displayIds = menuMasterRepository.findAllById(menuIds).stream()
                // A system menu (e.g. Default Dashboard) is a hidden fallback grant, never a real
                // privilege — excluded so it never shows as a checked box or inflates the privilege
                // count (a DD-only role must read as 0 privileges, not 1).
                .filter(m -> !CommonConstants.ACTIVE_Y.equals(m.getIsSystemMenu()))
                .map(m -> CommonConstants.ACTIVE_Y.equals(m.getIsPortalTwin()) && m.getTwinOfMenuId() != null ? m.getTwinOfMenuId() : m.getMenuId())
                .collect(Collectors.toList());
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Privileges fetched.", displayIds));
    }

    /**
     * True when this role holds at least one real APPLICATION menu grant. System menus (the Default
     * Dashboard fallback, and any future system menu) do NOT count — a role holding only the Default
     * Dashboard is effectively unprivileged, so it must never read as "has privileges" for
     * submit-for-approval, activation, or any count. Keyed on IS_SYSTEM_MENU (via the menu rows), not
     * a bare grant-count.
     */
    private boolean hasApplicationMenuGrant(Long roleId) {
        List<Long> ids = roleMenuMapRepository.findMenuIdsByRoleId(roleId);
        if (ids.isEmpty()) return false;
        return menuMasterRepository.findAllById(ids).stream().anyMatch(m -> !CommonConstants.ACTIVE_Y.equals(m.getIsSystemMenu()));
    }

    /**
     * The Default Dashboard row's MENU_ID — lazily resolved once, then cached for the process
     * lifetime (the id is a sequence PK that never changes). Fails LOUD if the row is missing or
     * duplicated: a production deployment must have run sql/default_dashboard_system_menu.sql, and a
     * silent null here would break login/sidebar for every zero-menu role with nobody knowing why.
     */
    private Long resolveDefaultDashboardMenuId() {
        Long cached = defaultDashboardMenuId;
        if (cached != null) return cached;
        List<ReconMenuMaster> rows = menuMasterRepository
                .findBySystemMenuCodeAndIsSystemMenu(DEFAULT_DASHBOARD_CODE, CommonConstants.ACTIVE_Y);
        if (rows.isEmpty()) {
            logger.error("SYSTEM_MENU_NOT_FOUND: Default Dashboard row (SYSTEM_MENU_CODE={}) is missing "
                    + "— run sql/default_dashboard_system_menu.sql", DEFAULT_DASHBOARD_CODE);
            throw new IllegalStateException("SYSTEM_MENU_NOT_FOUND: Default Dashboard menu is not seeded.");
        }
        if (rows.size() > 1) {
            logger.error("SYSTEM_MENU_DUPLICATE: {} Default Dashboard rows found (expected 1) — data corruption",
                    rows.size());
            throw new IllegalStateException("SYSTEM_MENU_DUPLICATE: more than one Default Dashboard menu exists.");
        }
        Long id = rows.get(0).getMenuId();
        defaultDashboardMenuId = id;
        return id;
    }

    /**
     * Single source of truth for the Default Dashboard fallback grant. Churn-free: it ONLY ensures
     * the grant EXISTS for a business role, never deletes it — while a role has application menus the
     * fallback is simply hidden everywhere (IS_SYSTEM_MENU), not removed. Idempotent and
     * concurrent-safe: existence is checked first, and a duplicate-key from a racing transaction is
     * treated as success (the desired end state — "mapping exists" — has been reached).
     *
     * MUST run inside the caller's transaction (savePrivileges / createRole / removeMenu are all
     * @Transactional) so the grant commits atomically with the change that triggered it.
     *
     * Any future feature that manipulates C_ROLE_MENU_MAP directly (Clone/Import/Bulk Role, scripts)
     * must call this before commit rather than inserting the fallback by hand.
     */
    @Override
    public void reconcileDefaultDashboard(Long roleId) {
        Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findById(roleId);
        if (!roleOpt.isPresent()) return;
        // Only business/user roles get the fallback; admin & bootstrap roles never do.
        if (!BUSINESS_ROLE_TYPES.contains(roleOpt.get().getRoleType())) return;

        Long ddId = resolveDefaultDashboardMenuId();
        CRoleMenuMap.RoleMenuMapId id = new CRoleMenuMap.RoleMenuMapId(roleId, ddId);
        if (roleMenuMapRepository.existsById(id)) return;

        Optional<ReconMenuMaster> ddMenu = menuMasterRepository.findById(ddId);
        if (!ddMenu.isPresent()) return; // resolve already validated, but stay defensive
        CRoleMenuMap map = new CRoleMenuMap();
        map.setId(id);
        map.setRole(roleOpt.get());
        map.setMenu(ddMenu.get());
        map.setCreatedAt(LocalDateTime.now());
        map.setCreatedBy(CommonConstants.SYSTEM);
        try {
            roleMenuMapRepository.save(map);
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // A concurrent transaction inserted the same DD mapping first. The end state we wanted
            // ("mapping exists") is already true, so treat it as success rather than failing the save.
            logger.debug("Default Dashboard mapping already present for roleId={} (concurrent insert)", roleId);
        }
    }

    /**
     * True only when updatedBy is a BANK_ADMIN whose own bank is the parent of the branch that
     * owns this BRANCH_ADMIN_DEFAULT role. RECON_ROLE_MASTER carries no BANK_ID of its own, so the
     * branch is derived the only way it can be: from the branch-admin user(s) actually holding this
     * roleId (there is exactly one branch per bootstrap role, by construction — see
     * ReconBankMasterServiceImpl.createDefaultAdminMenus).
     */
    @Override
    public ResponseEntity<RestWithStatusList> getOwningBank(Long roleId) {
        List<ReconUser> holders = reconUserRepository.findByRoleId(roleId);
        if (holders.isEmpty() || holders.get(0).getBankId() == null) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role has no holder yet.", Collections.emptyList()));
        }
        Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findById(holders.get(0).getBankId());
        if (!bankOpt.isPresent()) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role has no holder yet.", Collections.emptyList()));
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Owning bank fetched.",
                Collections.singletonList(bankOpt.get())));
    }

    private boolean callerIsParentBankAdminOf(ReconRoleMaster role, String updatedBy) {
        Optional<ReconUser> callerOpt = reconUserRepository.findByUsername(updatedBy);
        if (!callerOpt.isPresent() || !"BANK_ADMIN".equals(callerOpt.get().getUserType())) return false;
        List<ReconUser> holders = reconUserRepository.findByRoleId(role.getRoleId());
        if (holders.isEmpty() || holders.get(0).getBankId() == null) return false;
        Optional<ReconBankMaster> branchOpt = reconBankMasterRepository.findById(holders.get(0).getBankId());
        return branchOpt.isPresent() && callerOpt.get().getBankId() != null
                && callerOpt.get().getBankId().equals(branchOpt.get().getParentBankId());
    }

    @Override
    public ResponseEntity<RestWithStatusList> getLockedPrivileges(Long roleId) {
        Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Locked privileges fetched.", Collections.emptyList()));
        }
        List<Long> locked = new ArrayList<>(resolveLockedMenuIds(roleOpt.get()));
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Locked privileges fetched.", locked));
    }

    /**
     * Menu IDs this role can never be edited to drop — the system-default set it was bootstrapped
     * with at onboarding (see ReconBankMasterServiceImpl.createDefaultAdminMenus). Only applies to
     * a BRANCH_ADMIN_DEFAULT/BANK_ADMIN_DEFAULT role; every other role has no locked menus at all.
     * Derived by name match against BOOTSTRAP_ADMIN_MENU_NAMES over the role's CURRENT grants,
     * rather than a stored flag — the bootstrap set is fixed and small, and this way it never goes
     * stale if the grant rows get recreated (savePrivileges always deletes+reinserts).
     */
    private Set<Long> resolveLockedMenuIds(ReconRoleMaster role) {
        if (role == null || !BOOTSTRAP_ROLE_TYPES.contains(role.getRoleType())) return Collections.emptySet();
        List<Long> currentMenuIds = roleMenuMapRepository.findMenuIdsByRoleId(role.getRoleId());
        if (currentMenuIds.isEmpty()) return Collections.emptySet();
        return menuMasterRepository.findAllById(currentMenuIds).stream()
                .filter(m -> BOOTSTRAP_ADMIN_MENU_NAMES.contains(m.getMenuName()))
                .map(ReconMenuMaster::getMenuId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> savePrivileges(Long roleId, List<Long> menuIds, String updatedBy) {
        Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }

        // A branch's own Branch_Admin bootstrap role is managed exclusively by that branch's
        // parent Bank Admin — never by the Branch Admin themselves, and never by anyone else. This
        // is a hard server-side gate, not just a UI omission: without it, a Branch Admin (or
        // anyone else who learns the roleId) could call this endpoint directly and edit their own
        // role's privileges regardless of what Role List / Privileges Assign choose to show them.
        if ("BRANCH_ADMIN_DEFAULT".equals(roleOpt.get().getRoleType())
                && !callerIsParentBankAdminOf(roleOpt.get(), updatedBy)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Only that branch's Bank Admin can manage this role's privileges.", null));
        }

        // Captured before deleteByRoleId below wipes the current grants — a BRANCH_ADMIN_DEFAULT/
        // BANK_ADMIN_DEFAULT role's system-default menus are always kept regardless of what the
        // caller submitted; a Bank Admin can add menus to this role but can never remove one of
        // these. Every other role type resolves to an empty set. Deliberately unioned in AFTER the
        // SOD check below, not before: the bootstrap set itself already contains both Checker
        // Dashboard and the Maker-side Add User/Role/Menu (createDefaultAdminMenus grants both,
        // bypassing this check entirely since bootstrap creation never goes through
        // savePrivileges) — validating the locked baseline against SOD here would make it
        // impossible to save ANY change to the role, forever. SOD only needs to stop the Admin's
        // OWN new additions from creating a fresh conflict; it was never enforced against — and
        // can't retroactively fix — the pre-existing bootstrap set they didn't create.
        Set<Long> lockedMenuIds = resolveLockedMenuIds(roleOpt.get());

        // Checker Dashboard and Maker-side menus (Maker Dashboard / Add Role / Add Menu / Add User)
        // are mutually exclusive — same SOD rule as product capability: a Checker can never also
        // be able to create records. Enforced here as the server-side source of truth; the
        // Privileges-assign screen also disables the opposing checkboxes for UX.
        // Exempt for BRANCH_ADMIN_DEFAULT/BANK_ADMIN_DEFAULT: a branch/bank's own Admin role is
        // deliberately both Maker and Checker by design (it already runs both sides of every
        // workflow for its institution — the bootstrap set itself grants Checker Dashboard AND
        // Add User/Role/Menu together), so SOD simply doesn't apply to this role type at all.
        boolean sodExempt = BOOTSTRAP_ROLE_TYPES.contains(roleOpt.get().getRoleType());
        if (menuIds != null && !sodExempt) {
            Set<String> requestedNames = menuIds.stream()
                    .map(menuMasterRepository::findById)
                    .filter(Optional::isPresent)
                    .map(o -> o.get().getMenuName())
                    .collect(Collectors.toSet());
            boolean hasChecker = requestedNames.contains("Checker Dashboard");
            boolean hasMaker = requestedNames.contains("Maker Dashboard") || requestedNames.contains("Add Role")
                    || requestedNames.contains("Add Menu") || requestedNames.contains("Add User");
            if (hasChecker && hasMaker) {
                return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                        "A role cannot have both Checker Dashboard and Maker-side privileges (Maker Dashboard / Add Role / Add Menu / Add User).",
                        null));
            }

            // Defence-in-depth: only a Bank/Branch/KAL Admin may grant Checker Dashboard. A Maker
            // (a non-admin holding Add Role) must never create a Checker — the Privileges screen
            // hides the option, and this stops a hand-crafted request from bypassing it.
            if (hasChecker) {
                Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(updatedBy);
                boolean actorIsAdmin = actorOpt.isPresent()
                        && com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(actorOpt.get().getUserType());
                if (!actorIsAdmin) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new RestWithStatusList(CommonConstants.FAILURE,
                            "Only a Bank or Branch Admin can create or assign a Checker role.", null));
                }
            }
        }

        // Now union the locked baseline back in, after SOD has already judged only what the Admin
        // actually submitted.
        if (!lockedMenuIds.isEmpty()) {
            Set<Long> submitted = menuIds == null ? Collections.emptySet() : new LinkedHashSet<>(menuIds);
            List<Long> merged = new ArrayList<>(submitted);
            for (Long lockedId : lockedMenuIds) {
                if (!submitted.contains(lockedId)) merged.add(lockedId);
            }
            menuIds = merged;
        }

        // Wipe only the APPLICATION-menu grants — the Default Dashboard system grant is preserved
        // across every save, so it is assigned once (at role creation / backfill) and never churned.
        roleMenuMapRepository.deleteApplicationGrantsByRoleId(roleId);

        // Custom roles (RECON_USER/BANK_USER/BRANCH_USER) sign in via the /user portal — but the
        // Menu Access Tree also offers Bank/Branch Admin's own bootstrap "My Organization"/
        // "Administration" items, whose URLs are hardcoded to the Admin's own portal
        // (/bank-admin/*, /branch-admin/*, /admin/*). Those routes don't exist under /user, so
        // for this role type we map a /user-URL "twin" of the menu instead — auto-created once,
        // then reused — leaving the Admin's own original menu row completely untouched.
        boolean isUserPortalRole = Arrays.asList("RECON_USER", "BANK_USER", "BRANCH_USER")
                .contains(roleOpt.get().getRoleType());

        if (menuIds != null) {
            for (Long menuId : menuIds) {
                Optional<ReconMenuMaster> menuOpt = menuMasterRepository.findById(menuId);
                if (!menuOpt.isPresent()) continue;
                ReconMenuMaster menu = menuOpt.get();

                if (isUserPortalRole && "Main".equals(menu.getMenuType())) {
                    ReconMenuMaster twin = getOrCreateUserTwinMenu(menu, roleId, updatedBy);
                    if (twin != null) {
                        menu = twin;
                        menuId = twin.getMenuId();
                    }
                }

                CRoleMenuMap map = new CRoleMenuMap();
                // @EmbeddedId is never auto-instantiated by Hibernate on insert in this setup —
                // leaving it null makes CompositeNestedGeneratedValueGenerator NPE while trying to
                // reflectively populate id.menuId from the @MapsId association. Set it explicitly.
                map.setId(new CRoleMenuMap.RoleMenuMapId(roleId, menuId));
                map.setRole(roleOpt.get());
                map.setMenu(menu);
                map.setCreatedAt(LocalDateTime.now());
                map.setCreatedBy(updatedBy);
                roleMenuMapRepository.save(map);
            }
        }

        // Keep the Default Dashboard fallback grant in sync for business roles — ensures it exists
        // (idempotent, never re-inserts). Runs in this same @Transactional so it commits atomically
        // with the grants above. ACTIVE logic below is deliberately untouched: DD is never part of
        // `menuIds` (the submitted application-menu list), so a DD-only role still has an empty
        // menuIds and stays DRAFT exactly as before.
        reconcileDefaultDashboard(roleId);

        // A Bank/Branch/KAL Admin never goes through maker-checker approval — the moment they
        // assign at least one privilege to a role they created, it goes straight ACTIVE. Until
        // then it stays DRAFT (a role with zero privileges is unusable, so it can't be ACTIVE).
        Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(updatedBy);
        boolean actorIsAdmin = actorOpt.isPresent()
                && com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(actorOpt.get().getUserType());
        if (actorIsAdmin && menuIds != null && !menuIds.isEmpty()) {
            ReconRoleMaster r = roleOpt.get();
            r.setStatus(CommonConstants.ACTIVE);
            r.setSubmittedBy(updatedBy);
            r.setApprovedBy(updatedBy);
            r.setUpdatedAt(LocalDateTime.now());
            r.setUpdatedBy(updatedBy);
            reconRoleMasterRepository.save(r);
        }

        logger.info("Privileges saved for roleId={}: {} menu(s) by {}", roleId, menuIds == null ? 0 : menuIds.size(), updatedBy);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Privileges saved.", null));
    }

    // Finds (or creates, once) the /user-portal equivalent of an Admin-portal menu — same
    // name/type/parent, URL rewritten to /user/... . Returns null if the menu's URL doesn't
    // match a known Admin-portal prefix (nothing to rewrite; caller keeps the original).
    private ReconMenuMaster getOrCreateUserTwinMenu(ReconMenuMaster original, Long forRoleId, String createdBy) {
        String twinUrl = rewriteToUserUrl(original.getMenuUrl());
        if (twinUrl == null) return null;

        // Scoped to the original's own bank. /user/add-user is the same URL for every institution,
        // so a name+url lookup returned whichever bank happened to twin it first — a Branch's role
        // then got granted the parent Bank's menu row.
        Optional<ReconMenuMaster> existing = menuMasterRepository
                .findByMenuNameAndMenuUrlAndBankId(original.getMenuName(), twinUrl, original.getBankId());
        if (existing.isPresent()) {
            ReconMenuMaster found = existing.get();
            // Self-heal rows created before IS_PORTAL_TWIN/TWIN_OF_MENU_ID existed, so they stop
            // showing up as duplicate entries and sort back into their canonical position.
            boolean dirty = false;
            if (!CommonConstants.ACTIVE_Y.equals(found.getIsPortalTwin())) { found.setIsPortalTwin(CommonConstants.ACTIVE_Y); dirty = true; }
            if (found.getTwinOfMenuId() == null) { found.setTwinOfMenuId(original.getMenuId()); dirty = true; }
            // Self-heal parentMenuId the same way — a twin row created before this field existed
            // should backfill it, mirroring the isPortalTwin/twinOfMenuId self-heal above.
            if (found.getParentMenuId() == null && original.getParentMenuId() != null) {
                found.setParentMenuId(original.getParentMenuId());
                dirty = true;
            }
            if (dirty) menuMasterRepository.save(found);
            return found;
        }

        ReconMenuMaster twin = new ReconMenuMaster();
        // A twin is the same logical menu, so it carries the same permanent identity.
        twin.setSystemMenuCode(original.getSystemMenuCode());
        twin.setMenuType(original.getMenuType());
        twin.setMenuName(original.getMenuName());
        twin.setMenuDescription(original.getMenuDescription());
        twin.setParentMenuCode(original.getParentMenuCode());
        // Mirrors the parentMenuCode copy above (and its existing masterMenuParent-not-copied
        // asymmetry, left as-is): the twin's parent pointer is the ORIGINAL's own parent row,
        // never a "twin of the parent" — nothing here tries to find/create one.
        twin.setParentMenuId(original.getParentMenuId());
        twin.setSubMenu(CommonConstants.INACTIVE_N);
        twin.setMenuUrl(twinUrl);
        twin.setStatus(CommonConstants.ACTIVE_Y);
        // A twin is the same menu on another portal, so it inherits the original's owner and
        // product scope. Leaving either null would leak it across banks/products.
        twin.setBankId(original.getBankId());
        twin.setProductId(original.getProductId());
        twin.setIsPortalTwin(CommonConstants.ACTIVE_Y);
        twin.setTwinOfMenuId(original.getMenuId());
        twin.setCreatedBy(createdBy);
        twin.setCreatedDate(new Date());
        twin.setInsertDate(new Date());
        return menuMasterRepository.save(twin);
    }

    private static final List<String> ADMIN_PORTAL_PREFIXES = Arrays.asList("/bank-admin", "/branch-admin", "/admin");

    private String rewriteToUserUrl(String url) {
        if (url == null || url.startsWith("/user")) return null; // nothing to twin
        for (String prefix : ADMIN_PORTAL_PREFIXES) {
            if (url.startsWith(prefix)) {
                return "/user" + url.substring(prefix.length());
            }
        }
        return null; // not a recognized Admin-portal URL — leave the original mapping as-is
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesVisibleTo(String username) {
        Optional<ReconUser> callerOpt = hierarchyScopeService.caller(username);
        if (!callerOpt.isPresent()) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Roles fetched.", Collections.emptyList()));
        }
        ReconUser caller = callerOpt.get();

        // An Admin owns the institution and must be able to manage Checker roles too.
        if (hierarchyScopeService.isAdmin(caller)) {
            if (caller.getBankId() == null) return getAllRoles();
            ResponseEntity<RestWithStatusList> ownBankRoles = getRolesByBankId(caller.getBankId());

            // A Bank Admin also manages each of their branches' Branch_Admin role directly (add
            // extra menus; the bootstrap set stays locked — see resolveLockedMenuIds) even though
            // it's otherwise a system/bootstrap role hidden everywhere else (isSystemAdminRole).
            // Nobody but this bank's own Bank Admin sees these rows: not Kal Admin (getAllRoles
            // above never calls this branch), not the Branch Admin themselves (they're not an
            // Admin-of-their-own-institution from this method's point of view the way a Bank Admin
            // is — they fall through to the "everyone else" path below), and not another bank.
            if ("BANK_ADMIN".equals(caller.getUserType())) {
                List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(caller.getBankId());
                if (!branches.isEmpty()) {
                    List<Long> branchIds = branches.stream().map(ReconBankMaster::getBankId).collect(Collectors.toList());
                    Set<Long> branchAdminRoleIds = reconUserRepository.findByBankIdIn(branchIds).stream()
                            .map(ReconUser::getRoleId).filter(Objects::nonNull)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    if (!branchAdminRoleIds.isEmpty()) {
                        List<ReconRoleMaster> branchAdminRoles = reconRoleMasterRepository.findByRoleIdIn(new ArrayList<>(branchAdminRoleIds)).stream()
                                .filter(r -> "BRANCH_ADMIN_DEFAULT".equals(r.getRoleType()))
                                .collect(Collectors.toList());
                        if (!branchAdminRoles.isEmpty()) {
                            List<Object> combined = new ArrayList<>();
                            if (ownBankRoles.getBody() != null && ownBankRoles.getBody().getData() != null) {
                                combined.addAll(ownBankRoles.getBody().getData());
                            }
                            combined.addAll(branchAdminRoles);
                            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Roles fetched.", combined));
                        }
                    }
                }
            }
            return ownBankRoles;
        }

        // Everyone else sees only their own subtree. Never a parent's or an Admin's role — the
        // Administration screens can replace/re-assign users, so exposing an ancestor's role would
        // let a child swap themselves into it.
        Set<Long> descendants = hierarchyScopeService.descendantUserIds(caller.getUserId());
        Set<Long> roleIds = descendants.isEmpty() ? new LinkedHashSet<>()
                : reconUserRepository.findAllById(descendants).stream()
                        .map(ReconUser::getRoleId).filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // Plus the roles they created themselves but haven't assigned to anyone yet.
        reconRoleMasterRepository.findByCreatedByIn(Collections.singletonList(username))
                .forEach(r -> roleIds.add(r.getRoleId()));

        List<ReconRoleMaster> roles = roleIds.isEmpty() ? Collections.emptyList()
                : reconRoleMasterRepository.findByRoleIdIn(new ArrayList<>(roleIds)).stream()
                        .filter(r -> !isSystemAdminRole(r))                          // never the bootstrap admin roles
                        .filter(r -> !hierarchyScopeService.isCheckerRole(r.getRoleId())) // a Maker never even sees a Checker role
                        .collect(Collectors.toList());

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Roles fetched.", roles));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByBankId(Long bankId) {
        List<ReconUser> bankUsers = reconUserRepository.findByBankId(bankId);

        List<Long> roleIds = bankUsers.stream()
                .map(ReconUser::getRoleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<String> usernames = bankUsers.stream()
                .map(ReconUser::getUsername).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<ReconRoleMaster> byUser = roleIds.isEmpty() ? Collections.emptyList() : reconRoleMasterRepository.findByRoleIdIn(roleIds);
        List<ReconRoleMaster> byCreator = usernames.isEmpty() ? Collections.emptyList() : reconRoleMasterRepository.findByCreatedByIn(usernames);

        // A role an admin created can already be assigned to users of a DIFFERENT bank/branch
        // (e.g. Bank Admin creates a role while onboarding a Branch). Such roles belong to that
        // branch's view, not this bank's — so drop anything from byCreator that is in use elsewhere.
        byCreator = byCreator.stream()
                .filter(r -> reconUserRepository.findByRoleId(r.getRoleId()).stream()
                        .allMatch(u -> bankId.equals(u.getBankId())))
                .collect(Collectors.toList());

        Map<Long, ReconRoleMaster> merged = new LinkedHashMap<>();
        byUser.forEach(r -> merged.put(r.getRoleId(), r));
        byCreator.forEach(r -> merged.put(r.getRoleId(), r));

        // Bootstrap admin roles (KalInfotech Admin, "Bank Admin - <code>", "Branch Admin - <code>")
        // are system-level and not something a Bank/Branch Admin manages from this screen — hide
        // them from the list.
        List<ReconRoleMaster> visible = merged.values().stream()
                .filter(r -> !isSystemAdminRole(r))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Roles fetched for bank.", visible));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getPendingRolesForChecker(String checkerUsername) {
        Optional<ReconUser> checkerOpt = reconUserRepository.findByUsername(checkerUsername);
        if (!checkerOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Checker not found: " + checkerUsername, null));
        }
        ReconUser checker = checkerOpt.get();
        List<ReconRoleMaster> pending = reconRoleMasterRepository.findByStatus("PENDING");
        List<ReconRoleMaster> visible = pending.stream()
                .filter(r -> isVisibleToChecker(checker, r.getCreatedBy(), r.getRoleId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Pending roles fetched.", visible));
    }

    // Shared scoping rule for the Checker Queue (Roles / Users / Menus all use this):
    //   - KAL_ADMIN sees everything platform-wide.
    //   - Everyone else only sees submissions from their OWN bank/branch (BANK_ID match) —
    //     a Bank-level submission never reaches a Branch checker and vice versa, since a
    //     branch has its own distinct BANK_ID row, never the parent bank's.
    //   - A Bank/Branch Admin sees everything at their own level, unfiltered by product.
    //   - A plain Checker additionally only sees items whose EFFECTIVE product scope
    //     (itemRoleId's own C_ROLE_PRODUCT_MAP) overlaps their own — an unrestricted scope
    //     on either side (no C_ROLE_PRODUCT_MAP rows) counts as visible to everyone.
    private boolean isVisibleToChecker(ReconUser checker, String submitterUsername, Long itemRoleId) {
        if ("KAL_ADMIN".equals(checker.getUserType())) return true;

        Optional<ReconUser> submitterOpt = reconUserRepository.findByUsername(submitterUsername);
        if (!submitterOpt.isPresent() || !Objects.equals(submitterOpt.get().getBankId(), checker.getBankId())) {
            return false;
        }

        boolean isAdmin = com.jpb.reconciliation.reconciliation.constants.UserConstants.isAdminUserType(checker.getUserType());
        if (isAdmin) return true;

        Set<Long> checkerScope = resolveProductScope(checker.getRoleId());
        Set<Long> itemScope = resolveProductScope(itemRoleId);
        return checkerScope.isEmpty() || itemScope.isEmpty() || !Collections.disjoint(checkerScope, itemScope);
    }

    private Set<Long> resolveProductScope(Long roleId) {
        if (roleId == null) return Collections.emptySet();
        return new java.util.HashSet<>(roleProductMapRepository.findProductIdsByRoleId(roleId));
    }

    // Tenant-scoped duplicate detection. Roles carry no BANK_ID column, so an organization is
    // derived from RCN_RECON_USER exactly like getRolesByBankId: a role "belongs" to the actor's
    // bank if it's assigned to one of that bank's users, or was created by someone from that bank
    // and isn't in use anywhere else. A name+type match within that set is a duplicate.
    // Kal Admin (no bankId) is scoped to roles it created itself.
    private boolean roleExistsInTenant(String roleName, String roleType, String createdBy) {
        if (roleName == null) return false;
        String targetName = roleName.trim();
        Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(createdBy);
        Long bankId = actorOpt.map(ReconUser::getBankId).orElse(null);

        List<ReconRoleMaster> candidates;
        if (bankId == null) {
            // Kal Admin / no bank context — scope to this creator's own roles.
            candidates = reconRoleMasterRepository.findByCreatedByIn(Collections.singletonList(createdBy));
        } else {
            List<ReconUser> bankUsers = reconUserRepository.findByBankId(bankId);
            List<Long> roleIds = bankUsers.stream()
                    .map(ReconUser::getRoleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            List<String> usernames = bankUsers.stream()
                    .map(ReconUser::getUsername).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!usernames.contains(createdBy)) usernames.add(createdBy);

            List<ReconRoleMaster> byUser = roleIds.isEmpty() ? Collections.emptyList()
                    : reconRoleMasterRepository.findByRoleIdIn(roleIds);
            List<ReconRoleMaster> byCreator = usernames.isEmpty() ? Collections.emptyList()
                    : reconRoleMasterRepository.findByCreatedByIn(usernames);
            // A creator's role already assigned to a DIFFERENT bank belongs to that bank, not here.
            byCreator = byCreator.stream()
                    .filter(r -> reconUserRepository.findByRoleId(r.getRoleId()).stream()
                            .allMatch(u -> bankId.equals(u.getBankId())))
                    .collect(Collectors.toList());

            Map<Long, ReconRoleMaster> merged = new LinkedHashMap<>();
            byUser.forEach(r -> merged.put(r.getRoleId(), r));
            byCreator.forEach(r -> merged.put(r.getRoleId(), r));
            candidates = new java.util.ArrayList<>(merged.values());
        }

        return candidates.stream().anyMatch(r ->
                r.getRoleName() != null && r.getRoleName().trim().equalsIgnoreCase(targetName)
                && (roleType == null || roleType.equalsIgnoreCase(r.getRoleType())));
    }

    // ROLE_CODE is now a flat sequential number (same generator as Add Role) so it can no
    // longer be used to spot bootstrap roles — matches by ROLE_NAME instead, which
    // ReconBankMasterServiceImpl.createDefaultAdminMenus / KalAdminAuthServiceImpl always set
    // to a fixed, recognizable pattern for these system-created roles.
    private boolean isSystemAdminRole(ReconRoleMaster r) {
        if (r == null || r.getRoleName() == null) return false;
        String name = r.getRoleName();
        return name.equals("KalInfotech Admin")
                || name.startsWith("Bank Admin - ")
                || name.startsWith("Branch Admin - ");
    }

    // Reuses the code the Add Role form already reserved (via /generate-code) and displayed to
    // the user as a disabled field, as long as it's still unused. If it went stale (someone else
    // took it, or none was sent), a fresh one is generated — mirrors the old backend's
    // peekNextCode()/generateNextCode() fallback pattern.
    private String resolveRoleCode(String reservedCode, String roleName) {
        if (reservedCode != null && !reservedCode.trim().isEmpty()
                && !reconRoleMasterRepository.existsByRoleCode(reservedCode.trim())) {
            return reservedCode.trim();
        }
        return roleCodeGeneratorService.generateNextCode(roleName);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRoleProducts(Long roleId) {
        try {
            List<Long> productIds = roleProductMapRepository.findProductIdsByRoleId(roleId);
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role products fetched.", productIds));
        } catch (Exception e) {
            // Most likely cause: C_ROLE_PRODUCT_MAP table missing — migration_role_product_map.sql
            // hasn't been run against this DB yet. Surface the real reason instead of a bare 500.
            logger.error("Failed to fetch role products for roleId={}: {}", roleId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Failed to fetch role products: " + e.getMessage(), null));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> saveRoleProducts(Long roleId, List<Long> productIds, String updatedBy) {
        try {
            if (!reconRoleMasterRepository.existsById(roleId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
            }
            roleProductMapRepository.deleteByRoleId(roleId);
            if (productIds != null) {
                for (Long productId : productIds) {
                    ReconRoleProductMap map = new ReconRoleProductMap();
                    map.setRoleId(roleId);
                    map.setProductId(productId);
                    map.setCreatedAt(LocalDateTime.now());
                    map.setCreatedBy(updatedBy);
                    roleProductMapRepository.save(map);
                }
            }
            logger.info("Products saved for roleId={}: {} product(s) by {}", roleId, productIds == null ? 0 : productIds.size(), updatedBy);
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Products saved.", null));
        } catch (Exception e) {
            logger.error("Failed to save role products for roleId={}: {}", roleId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Failed to save role products: " + e.getMessage(), null));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> saveRoleBankTypeScope(Long roleId, String bankTypeScope, String updatedBy) {
        Optional<ReconRoleMaster> roleOpt = reconRoleMasterRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster role = roleOpt.get();
        role.setBankTypeScope(bankTypeScope != null && !bankTypeScope.trim().isEmpty() ? bankTypeScope.trim() : null);
        role.setUpdatedAt(LocalDateTime.now());
        role.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(role);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank type scope saved.", null));
    }
}
