package com.jpb.reconciliation.reconciliation.entity;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "RECON_MENU_MASTER")
public class ReconMenuMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MENU")
    @SequenceGenerator(name = "SEQ_MENU", sequenceName = "SEQ_MENU",allocationSize = 1)
    @Column(name = "MENU_ID")
    private Long menuId;

    // Permanent business identity of a menu. MENU_NAME is a display label and may be renamed at
    // any time, so no logic may key on it — validation, hierarchy resolution and duplicate
    // detection all key on this code instead. A catalog row defines the code; every bank-owned
    // row appended from that catalog row inherits it (so the same code repeats across banks and
    // products, and is unique only among catalog rows — see index UX_MENU_CATALOG_CODE).
    @Column(name = "SYSTEM_MENU_CODE")
    private String systemMenuCode;

    @Column(name = "MENU_TYPE")
    private String menuType;

    @Column(name = "MENU_NAME")
    private String menuName;

    @Column(name = "DESCRIPTION")
    private String menuDescription;

    @Column(name = "MENU_PARENT")
    private String parentMenuCode;

    @Column(name = "SUB_MENU_FLAG")
    private String subMenu;

    @Column(name = "MASTER_MENU_PARENT")
    private String masterMenuParent;

    @Column(name = "MENU_URL")
    private String menuUrl;

    // Real backend REST endpoint for this menu's action (only meaningful for a process-driven
    // Extraction/Reconciliation Submenu) — the actual @RequestMapping base of the controller
    // that runs it (e.g. "/api/v2/extraction", matching ExtractionAiController), not the
    // frontend page path. Set alongside VERSION in createCustomMenu, from the same real
    // controller-base constants used to build MENU_URL — see resolveEndPointUrl.
    @Column(name = "END_POINT_URL")
    private String endPointUrl;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "MODIFIED_BY")
    private String modifiedBy;

    @Column(name = "MODIFIED_DATE")
    private Timestamp modifiedDate;

    @Column(name = "INSERT_DATE")
    private Date insertDate;

    @Column(name = "LAST_UPDATED_USER")
    private Long lastUpdatedUser;

    @Column(name = "LAST_UPDATED_DATE")
    private Date lastUpdatedDate;

    @Column(name = "MENU_PROCESS_ID")
    private Long menuProcessId;

    @Column(name = "PROCESS_TYPE")
    private String processType;

    // ── Ownership and scope ──────────────────────────────────────────────────────
    // These answer two different questions, and C_ROLE_MENU_MAP answers a third:
    //     BANK_ID    : which institution this row belongs to
    //     PRODUCT_ID : which product it belongs to
    //     C_ROLE_MENU_MAP : which roles may see it
    //
    // A branch is its own RECON_BANK_MASTER row, so one BANK_ID covers banks and branches.
    // Replaces ROLE_ID, whose only real job was reaching the bank (bank -> users -> roles ->
    // menus). PRODUCT_ID cannot do this: RECON_PRODUCT_MASTER.PRODUCT_NAME is unique, so one
    // product row is shared by every bank that bought it — it identifies the product, never
    // the owner.
    //
    // NULL on both = a catalog (reference) row: registered for validation, owned by nobody,
    // never granted, so it never reaches a sidebar. Kal Admin's bootstrap menus also carry a
    // NULL BANK_ID (KAL_ADMIN has no bank) but are granted, which is what tells them apart.
    @Column(name = "BANK_ID")
    private Long bankId;

    // NULL means "no product restriction" — platform menus (Dashboard / My Organization /
    // Administration) that every product sees. Same "empty scope = unrestricted" convention
    // resolveProductScope()/isVisibleToChecker() already apply to C_ROLE_PRODUCT_MAP.
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "SUBMITTED_BY")
    private String submittedBy;

    @Column(name = "APPROVED_BY")
    private String approvedBy;

    // CommonConstants.ACTIVE_Y for a /user-portal twin auto-created by ReconRoleMasterServiceImpl.savePrivileges()
    // (see getOrCreateUserTwinMenu) — an internal implementation detail, never something an
    // Admin selects directly, so it's excluded from the Menu Access Tree / Menu List.
    @Column(name = "IS_PORTAL_TWIN")
    private String isPortalTwin;

    // For a twin row only — the MENU_ID of the original Admin-portal menu it was cloned from.
    // A twin's own MENU_ID reflects when it happened to be created (privilege-assignment time),
    // not the original's canonical bootstrap position — this lets the Sidebar sort a twin back
    // into that same canonical position instead of wherever it landed chronologically.
    @Column(name = "TWIN_OF_MENU_ID")
    private Long twinOfMenuId;

    // Internal, numeric mirror of the parentMenuCode/masterMenuParent hierarchy (MENU_PARENT /
    // MASTER_MENU_PARENT), which remain the wire contract read/written by the frontend and by
    // getMenusByRolePrivileges()/resolveUnmappedHiddenNames(). Exists so internal traversal can
    // resolve a parent by ID instead of by (possibly non-unique, possibly bank-ambiguous) name.
    // Plain Long, not @ManyToOne, matching TWIN_OF_MENU_ID precedent — avoids Hibernate lazy/
    // eager fetch surprises across the bulk hierarchy-scanning queries in
    // MenuMasterServiceImpl. NULL for a Master row (root) and for any row whose parent could
    // not be resolved. See sql/menu_parent_id_migration.sql.
    @Column(name = "PARENT_MENU_ID")
    private Long parentMenuId;

    // "CATALOG" (today's only path — appended from the BANK_ID-IS-NULL register) or "CUSTOM" (an
    // Admin-created Master/Main/Submenu with no catalog backing). Partitions which
    // validation/visibility rule a row is subject to, so custom-menu logic can never reach or
    // change a catalog-sourced row. See sql/menu_source_clickable_migration.sql.
    @Column(name = "MENU_SOURCE")
    private String menuSource;

    // CommonConstants.ACTIVE_Y/CommonConstants.INACTIVE_N, a synced mirror of "MENU_URL IS NOT NULL" — always backend-derived (never part of
    // the DTO, never client-writable), kept in sync at every write site that sets MENU_URL.
    @Column(name = "IS_CLICKABLE")
    private String isClickable;

    // Soft-delete flag, deliberately separate from STATUS (the maker-checker lifecycle field —
    // DRAFT/PENDING/ACTIVE/Y/REJECTED). CommonConstants.ACTIVE_Y = normal (default); CommonConstants.INACTIVE_N = deleted from Menu List's
    // point of view and from every duplicate-validation check, so the same menu can be freely
    // recreated. See sql/menu_active_yn_migration.sql.
    @Column(name = "ACTIVE_YN")
    private String activeYn;

    // CommonConstants.ACTIVE_Y marks a system menu — a row that is NOT a real application menu: it is hidden from every
    // list / tree / count / sidebar, is immutable (no edit/delete/approve from any screen), and is
    // managed only by the backend. Default Dashboard is the first; future ones (Maintenance /
    // License-Expired / …) reuse the same flag. Always backend-derived, never client-writable.
    // Use isSystemMenu()/isApplicationMenu() helpers rather than checking this directly.
    // See sql/default_dashboard_system_menu.sql.
    @Column(name = "IS_SYSTEM_MENU")
    private String isSystemMenu;

    // Which kind of system menu this is (NULL for a normal application menu). 'FALLBACK' = the
    // Default Dashboard. Lets the framework distinguish future system menus without new columns.
    @Column(name = "SYSTEM_MENU_TYPE")
    private String systemMenuType;

    // Which extraction/reconciliation pipeline this menu's MENU_PROCESS_ID resolves against:
    // 'V1' = RFD_FILE_ID (RCN_FILE_DTL_MAST / legacy file-driven config, migrated from OCI),
    // 'V2' = TEMPLATE_ID (RECON_FILE_TMPLT_MAST). Meaningful ONLY for a processType EXTRACTION
    // or RECONCILIATION row — every other menu (Master/Main with no process, Configuration,
    // Reports, My Organization, ...) stays NULL, same as it always has. No Java-side default:
    // the service layer sets "V2" explicitly, and only on the process-driven path.
    @Column(name = "VERSION")
    private String version;
}
