package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;

public class ReconMenuMasterMapper {

    public static ReconMenuMasterDto mapToMenuDto(ReconMenuMaster menu, ReconMenuMasterDto menuDto) {
        menuDto.setMenuId(menu.getMenuId());
        menuDto.setMenuType(menu.getMenuType());
        menuDto.setMenuName(menu.getMenuName());
        menuDto.setMenuDescription(menu.getMenuDescription());
        menuDto.setParentMenuCode(menu.getParentMenuCode());
        menuDto.setSubMenuReq(menu.getSubMenu());
        menuDto.setMenuUrl(menu.getMenuUrl());
        menuDto.setMenuProcessId(menu.getMenuProcessId());
        menuDto.setMasterMenuParent(menu.getMasterMenuParent());
        menuDto.setProductId(menu.getProductId());
        // Was never mapped here at all (predates this session) — AddMenu's mappedIds filter keys
        // on it (m.processType === effectiveModule) to hide already-mapped files/templates, so a
        // permanently-null processType silently made that dedup a no-op for every menu.
        menuDto.setProcessType(menu.getProcessType());
        // A custom Main/Submenu never sets parentMenuCode/masterMenuParent (only this numeric
        // pointer) — frontend tree-building (Sidebar/Navbar/PrivilegesAssign) needs it to nest
        // custom rows at all, so every raw-entity response must carry it through.
        menuDto.setParentMenuId(menu.getParentMenuId());
        menuDto.setVersion(menu.getVersion());
        menuDto.setEndPointUrl(menu.getEndPointUrl());
        return menuDto;
    }

    public static ReconMenuMaster mapToMenu(ReconMenuMasterDto menuDto, ReconMenuMaster menu) {
        menu.setMenuId(menuDto.getMenuId());
        menu.setMenuType(menuDto.getMenuType());
        menu.setMenuDescription(menuDto.getMenuDescription());
        menu.setParentMenuCode(menuDto.getParentMenuCode());
        menu.setMenuUrl(menuDto.getMenuUrl());
        menu.setMenuName(menuDto.getMenuName());
        menu.setSubMenu(menuDto.getSubMenuReq());
        menu.setMenuProcessId(menuDto.getMenuProcessId());
        menu.setProductId(menuDto.getProductId());
        if (menuDto.getEndPointUrl() != null) {
            menu.setEndPointUrl(menuDto.getEndPointUrl());
        }
        // Only overwrite when the caller actually sent one — keeps the entity's "V2" default
        // (or an already-persisted "V1") intact for every write path that doesn't set it.
        if (menuDto.getVersion() != null) {
            menu.setVersion(menuDto.getVersion());
        }
        return menu;
    }

    /**
     * reconFilePath is version-specific (RCN_FILE_DTL_MAST for V1, RECON_FILE_TMPLT_MAST for V2) —
     * callers resolve it against the right table via menu.getVersion() and pass the plain string,
     * so this mapper stays agnostic to which entity it came from.
     */
    public static ReconMenuMasterDto mapToMenuDtoWithFilePath(ReconMenuMasterDto reconMenuMasterDto,
            ReconMenuMaster menu, String reconFilePath) {
        reconMenuMasterDto.setMenuId(menu.getMenuId());
        reconMenuMasterDto.setMenuType(menu.getMenuType());
        reconMenuMasterDto.setMenuName(menu.getMenuName());
        reconMenuMasterDto.setMenuDescription(menu.getMenuDescription());
        reconMenuMasterDto.setParentMenuCode(menu.getParentMenuCode());
        reconMenuMasterDto.setSubMenuReq(menu.getSubMenu());
        reconMenuMasterDto.setMenuUrl(menu.getMenuUrl());
        reconMenuMasterDto.setMenuProcessId(menu.getMenuProcessId());
        reconMenuMasterDto.setMasterMenuParent(menu.getMasterMenuParent());
        reconMenuMasterDto.setProductId(menu.getProductId());
        reconMenuMasterDto.setParentMenuId(menu.getParentMenuId());
        reconMenuMasterDto.setVersion(menu.getVersion());
        reconMenuMasterDto.setProcessType(menu.getProcessType());
        reconMenuMasterDto.setReconFilePath(reconFilePath);
        reconMenuMasterDto.setEndPointUrl(menu.getEndPointUrl());
        return reconMenuMasterDto;
    }
}
