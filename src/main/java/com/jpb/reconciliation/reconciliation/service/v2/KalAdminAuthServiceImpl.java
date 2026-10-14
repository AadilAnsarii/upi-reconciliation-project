package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.KalUserDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.CRoleMenuMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class KalAdminAuthServiceImpl implements KalAdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(KalAdminAuthServiceImpl.class);

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private EmailRegistryService emailRegistry;

    @Autowired
    private ReconPasswordManagerRepository reconPasswordManagerRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    private MenuMasterRepository menuMasterRepository;

    @Autowired
    private CRoleMenuMapRepository roleMenuMapRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String KAL_ADMIN_ROLE_CODE = "KAL_ADMIN";

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createKalAdmin(KalUserDto dto) {

        // ── Duplicate checks ──────────────────────────────────────────────────
        if (reconUserRepository.existsByUsername(dto.getUsername().trim().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList(CommonConstants.FAILURE,
                            "Username '" + dto.getUsername() + "' already exists.", null));
        }
        String emailLc = dto.getEmail().trim().toLowerCase();
        // A BLOCKED account is effectively deleted, so its email may be reused; every other status
        // still blocks reuse (EmailRegistryService = single source of truth).
        if (emailRegistry.isActivelyRegistered(emailLc)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList(CommonConstants.FAILURE,
                            "Email '" + dto.getEmail() + "' is already registered.", null));
        }
        emailRegistry.releaseBlockedHolders(emailLc);

        // ── Derive full name from username (e.g. "john.doe" → "John Doe") ────
        String username = dto.getUsername().trim().toLowerCase();
        String[] parts  = username.split("\\.");
        StringBuilder nameBuilder = new StringBuilder();
        for (String part : parts) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        String fullName = nameBuilder.toString();

        // ── BCrypt-encode the password ────────────────────────────────────────
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // ── 1. Save to RCN_RECON_USER ─────────────────────────────────────────
        ReconUser user = new ReconUser();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setMobileNumber(dto.getPhone().trim());
        user.setUserType("KAL_ADMIN");
        user.setDesignation(null);
        user.setDepartment(null);
        user.setStatus(CommonConstants.ACTIVE);
        user.setApprovedYn(CommonConstants.ACTIVE_Y);
        user.setPasswordHash(encodedPassword);
        user.setPasswordSet(1);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setBankId(null);

        // ── Assign KAL_ADMIN role from RECON_ROLE_MASTER ─────────────────────
        // Self-healing: if the role (or its menus) don't exist yet — e.g. after a
        // table truncate — recreate them here instead of leaving the user roleless.
        Long kalAdminRoleId = ensureKalAdminRoleAndMenus(CommonConstants.SYSTEM);
        user.setRoleId(kalAdminRoleId);

        user.setCreatedAt(LocalDateTime.now());
        // Self-registration — there's no prior actor, so the new user is their own
        // creator (matches the AuditLog entry below, which already uses `username` as actor).
        user.setCreatedBy(username);

        ReconUser savedUser = reconUserRepository.save(user);
        logger.info("KalAdmin created — userId={}, username={}", savedUser.getUserId(), username);

        // ── 2. Save to RCN_RECON_PWD_MANAGER ─────────────────────────────────
        ReconPasswordManager pwdManager = new ReconPasswordManager();
        pwdManager.setReconUser(savedUser);
        pwdManager.setUserPassword(encodedPassword);
        pwdManager.setExpirationDate(LocalDateTime.now().plusDays(90));
        pwdManager.setCreatedAt(LocalDateTime.now());
        pwdManager.setCreatedBy(username);

        reconPasswordManagerRepository.save(pwdManager);
        logger.info("Password record saved in RCN_RECON_PWD_MANAGER for userId={}", savedUser.getUserId());

        // ── 3. Save to AUDIT_LOG ──────────────────────────────────────────────
        AuditLog auditLog = new AuditLog();
        auditLog.setTableName("RCN_RECON_USER");
        auditLog.setRecordId(savedUser.getUserId());
        auditLog.setOperation("INSERT");
        auditLog.setActorUserId(savedUser.getUserId());
        auditLog.setActorUsername(username);
        auditLog.setActorType("KAL_ADMIN");
        auditLog.setEntityType("KAL_ADMIN");
        auditLog.setBankId(null);
        auditLog.setChangedAt(LocalDateTime.now());
        auditLog.setOldValue(null);
        auditLog.setNewValue("{\"username\":\"" + username + "\",\"email\":\"" + savedUser.getEmail()
                + "\",\"status\":\"ACTIVE\",\"userType\":\"KAL_ADMIN\"}");
        auditLog.setActionLabel("KalAdmin account created");
        auditLog.setRemarks("Self-registration via /api/v2/admin/auth/create");

        auditLogRepository.save(auditLog);
        logger.info("Audit log recorded for KalAdmin creation — userId={}", savedUser.getUserId());

        // RECON_AUTH_TOKEN is NOT inserted here — tokens are only created during login/OTP flows.

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS, "KalAdmin account created successfully.", new ArrayList<>()));
    }

    // ── Self-healing: ensure KAL_ADMIN role + its default menus exist ────────────
    // Mirrors ReconBankMasterServiceImpl.createDefaultAdminMenus() so a truncated
    // RECON_ROLE_MASTER / RECON_MENU_MASTER recreates itself on the next KalAdmin signup.
    private Long ensureKalAdminRoleAndMenus(String createdBy) {
        Optional<ReconRoleMaster> existingRole = reconRoleMasterRepository.findByRoleCode(KAL_ADMIN_ROLE_CODE);
        if (existingRole.isPresent()) {
            Long roleId = existingRole.get().getRoleId();
            // Role exists but menus may have been wiped separately — recreate if empty. Keyed on
            // grants, not ownership: KAL_ADMIN has no bank (setBankId(null)), so its menu rows
            // carry a NULL BANK_ID and cannot be found by owner.
            if (roleMenuMapRepository.findMenuIdsByRoleId(roleId).isEmpty()) {
                createDefaultKalAdminMenus(roleId, createdBy);
            }
            return roleId;
        }

        ReconRoleMaster role = new ReconRoleMaster();
        role.setRoleCode(KAL_ADMIN_ROLE_CODE);
        role.setRoleName("KalInfotech Admin");
        role.setRoleType("INTERNAL");
        role.setRoleDesc("Default KalInfotech Admin role — full platform access");
        role.setStatus(CommonConstants.ACTIVE);
        role.setCreatedBy(createdBy);
        role.setCreatedAt(LocalDateTime.now());
        ReconRoleMaster savedRole = reconRoleMasterRepository.save(role);
        logger.info("KAL_ADMIN role recreated — roleId={}", savedRole.getRoleId());

        createDefaultKalAdminMenus(savedRole.getRoleId(), createdBy);
        return savedRole.getRoleId();
    }

    private void createDefaultKalAdminMenus(Long roleId, String createdBy) {
        try {
            ReconMenuMaster myOrg = saveMenu(null, "Master", "My Organization", null, roleId, createdBy);
            String myOrgId = String.valueOf(myOrg.getMenuId());
            for (String[] item : Arrays.asList(
                new String[]{"Overview",          "/admin/my-organization/overview"},
                new String[]{"Banks & Branches",  "/admin/my-organization/bank-branches"},
                new String[]{"Bank Onboarding",   "/admin/bank-onboarding"},
                new String[]{"My Hierarchy",      "/admin/my-organization/hierarchy"},
                new String[]{"Admin Status",      "/admin/my-organization/admin-status"}
            )) {
                saveMenu(myOrgId, "Main", item[0], item[1], roleId, createdBy);
            }

            logger.info("Default KAL_ADMIN menus created for roleId={}", roleId);
        } catch (Exception e) {
            logger.error("Failed to create default KAL_ADMIN menus for roleId={}: {}", roleId, e.getMessage());
        }
    }

    private ReconMenuMaster saveMenu(String parentMenuCode, String menuType, String menuName,
                                     String menuUrl, Long roleId, String createdBy) {
        ReconMenuMaster m = new ReconMenuMaster();
        // Permanent identity — display names may be renamed, this must not be derived from them.
        m.setSystemMenuCode(com.jpb.reconciliation.reconciliation.constants.SystemMenuCodes.of(menuName));
        m.setMenuType(menuType);
        m.setMenuName(menuName);
        m.setMenuUrl(menuUrl);
        m.setParentMenuCode(parentMenuCode);
        m.setSubMenu(CommonConstants.INACTIVE_N);
        m.setStatus(CommonConstants.ACTIVE_Y);
        // KAL_ADMIN belongs to no institution, so these rows have no owner. They are reachable
        // only through the C_ROLE_MENU_MAP grant written below.
        m.setBankId(null);
        m.setParentMenuId(resolveParentMenuId(menuType, parentMenuCode, null));
        m.setCreatedBy(createdBy);
        m.setCreatedDate(new Date());
        m.setInsertDate(new Date());
        ReconMenuMaster saved = menuMasterRepository.save(m);

        // Also attach via C_ROLE_MENU_MAP — the Sidebar reads privileges from there (not
        // RECON_MENU_MASTER.ROLE_ID directly), mirroring ReconBankMasterServiceImpl.saveMenu().
        // This copy never wrote to C_ROLE_MENU_MAP at all, so KalAdmin's sidebar had no
        // privilege rows created for it on admin creation.
        ReconRoleMaster roleRef = reconRoleMasterRepository.findById(roleId).orElse(null);
        CRoleMenuMap map = new CRoleMenuMap();
        map.setId(new CRoleMenuMap.RoleMenuMapId(roleId, saved.getMenuId()));
        map.setRole(roleRef);
        map.setMenu(saved);
        map.setCreatedAt(LocalDateTime.now());
        map.setCreatedBy(createdBy);
        roleMenuMapRepository.save(map);

        return saved;
    }

    // Internal, additive mirror of parentMenuCode — see MenuMasterServiceImpl's identically-named
    // private helper (duplicated per this codebase's per-class-helper style). bankId is always
    // null here. createDefaultKalAdminMenus() passes a stringified MENU_ID (myOrgId) as
    // parentMenuCode for every Main it creates, so the name-lookup branch below never matches and
    // this reliably falls straight to the numeric-parse branch — no name-collision risk possible
    // for KAL_ADMIN rows. See sql/menu_parent_id_migration.sql.
    private Long resolveParentMenuId(String menuType, String parentMenuCode, Long bankId) {
        if ("Master".equals(menuType) || parentMenuCode == null) return null;
        String parentType = "Main".equals(menuType) ? "Master" : "Main";
        Optional<ReconMenuMaster> byName = menuMasterRepository
                .findAllByMenuNameAndMenuType(parentMenuCode, parentType).stream()
                .filter(p -> CommonConstants.ACTIVE_Y.equals(p.getStatus()))
                .filter(p -> Objects.equals(p.getBankId(), bankId))
                .filter(p -> bankId != null || !"CATALOG".equals(p.getCreatedBy()))
                .findFirst();
        if (byName.isPresent()) return byName.get().getMenuId();
        if ("Main".equals(menuType)) {
            try { return Long.parseLong(parentMenuCode); } catch (NumberFormatException ignored) {
                // parentMenuCode isn't numeric — not a legacy ID-based row, nothing to fall back to
            }
        }
        return null;
    }
}
