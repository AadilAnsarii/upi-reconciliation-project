package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReconMenuMasterDto {

    private Long menuId;
    /** Permanent catalog identity of the menu being mapped. Authoritative — the backend validates
     *  and resolves against this, not against the (renameable) display name. */
    private String systemMenuCode;
    private String menuType;
    private String menuName;
    private String menuDescription;
    private String parentMenuCode;
    private String masterMenuParent;
    private String subMenuReq;
    private String menuUrl;
    private Long roleId;
    private Long productId;
    private Long menuProcessId;
    private String processType;
    private String reconFilePath;

    // 'V1' (RFD_FILE_ID, migrated from OCI) or 'V2' (TEMPLATE_ID, default for new rows).
    // See ReconMenuMaster.version.
    private String version;

    // Real backend REST API base for this menu's action (e.g. "/api/v2/extraction") — the
    // actual controller's own @RequestMapping, not the frontend page path (menuUrl). See
    // ReconMenuMaster.endPointUrl / MenuMasterServiceImpl.resolveEndPointUrl.
    private String endPointUrl;

    // true = the new custom Master/Main/Submenu creation path (no catalog lookup at all); absent
    // or false = today's catalog-driven Add Menu path, completely unchanged.
    private Boolean custom;

    // Custom path only — the chosen parent's MENU_ID, sent directly by the new Add Menu UI (no
    // name resolution needed, unlike parentMenuCode/masterMenuParent above). Null for a custom
    // Master (it has no parent).
    private Long parentMenuId;
}
