package com.jpb.reconciliation.reconciliation.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.MenuMasterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Menu Master APIs")
@RestController
@RequestMapping(path = "/api/v1")
@CrossOrigin(origins = "*")
public class MenuController {

    @Autowired
    MenuMasterService menuMasterService;

    @Operation(summary = "Add menu")
    @PostMapping(value = "/addmenu", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> addMasterMenu(@RequestBody ReconMenuMasterDto menuRequest,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force,
            @AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.addMenu(menuRequest, userDetails, force);
    }

    @Operation(summary = "Add menu — Admin-only, activates immediately")
    @PostMapping(value = "/addmenu-active", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> addMasterMenuActive(@RequestBody ReconMenuMasterDto menuRequest,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force,
            @AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.addMenuActive(menuRequest, userDetails, force);
    }

    @Operation(summary = "Get menu by ID")
    @GetMapping(value = "/getmenu/{menuId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMenu(@PathVariable Long menuId) {
        return menuMasterService.getMenus(menuId);
    }

    @GetMapping(value = "/getMenuBy-Role/{roleId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMenuByRole(@PathVariable Long roleId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long verifiedRoleId = menuMasterService.getVerifiedRoleId(userDetails.getUsername());
        if (!roleId.equals(verifiedRoleId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("UNAUTHORIZED_ACCESS",
                            "Requested menu role does not match user's verified role.", null));
        }
        return menuMasterService.getMenuByRole(verifiedRoleId);
    }

    @Operation(summary = "Get menus this role can actually see (Sidebar) — via C_ROLE_MENU_MAP, not RECON_MENU_MASTER.ROLE_ID directly")
    @GetMapping(value = "/getMenuBy-RolePrivileges/{roleId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMenusByRolePrivileges(@PathVariable Long roleId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long verifiedRoleId = menuMasterService.getVerifiedRoleId(userDetails.getUsername());
        if (!roleId.equals(verifiedRoleId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("UNAUTHORIZED_ACCESS",
                            "Requested menu role does not match user's verified role.", null));
        }
        return menuMasterService.getMenusByRolePrivileges(verifiedRoleId);
    }

    @Operation(summary = "Delete menu")
    @DeleteMapping(value = "/removemenu/{menuId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ResponseDto> removeMenu(@PathVariable Long menuId) {
        return menuMasterService.removeMenu(menuId);
    }

    @Operation(summary = "Preview the menus a delete would cascade to (a Master/Main's Mains/Submenus)")
    @GetMapping(value = "/menu/{menuId}/delete-chain", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getDeleteChain(@PathVariable Long menuId) {
        return menuMasterService.getDeleteChain(menuId);
    }

    @Operation(summary = "Update menu")
    @PutMapping(value = "/editmenu", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ResponseDto> updateMenu(@RequestBody ReconMenuMasterDto menuDto,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.updateMenu(menuDto, userDetails != null ? userDetails.getUsername() : null);
    }

    @Operation(summary = "Menus visible to the caller (Admin → institution; otherwise → own subtree only)")
    @GetMapping(value = "/menu/visible", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMenusVisibleTo(@AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.getMenusVisibleTo(userDetails.getUsername());
    }

    @GetMapping(value = "/getallmenu", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllMenus() {
        return menuMasterService.getAllMenus();
    }

    @Operation(summary = "Get menus scoped to a specific Bank/Branch")
    @GetMapping(value = "/menu/by-bank/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getMenusByBankId(@PathVariable Long bankId) {
        return menuMasterService.getMenusByBankId(bankId);
    }

    @Operation(summary = "Menu List only: every menu for this Bank/Branch, unfiltered by mapping status (no hide-until-mapped rule)")
    @GetMapping(value = "/menu/all-by-bank/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllMenusByBankId(@PathVariable Long bankId) {
        return menuMasterService.getAllMenusByBankId(bankId);
    }

    @Operation(summary = "Menu List only: unfiltered menus visible to the caller (Admin → institution; otherwise → own subtree only)")
    @GetMapping(value = "/menu/all-visible", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllMenusVisibleTo(@AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.getAllMenusVisibleTo(userDetails.getUsername());
    }

    @Operation(summary = "Submit menu for approval")
    @PutMapping(value = "/menu/{menuId}/submit", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> submitMenuForApproval(@PathVariable Long menuId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.submitForApproval(menuId, userDetails.getUsername());
    }

    @Operation(summary = "Approve menu")
    @PutMapping(value = "/menu/{menuId}/approve", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> approveMenu(@PathVariable Long menuId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.approveMenu(menuId, userDetails.getUsername());
    }

    @Operation(summary = "Pending menus visible to this Checker (bank/branch + product scope)")
    @GetMapping(value = "/menu/checker-queue", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getPendingMenusForChecker(@AuthenticationPrincipal UserDetails userDetails) {
        return menuMasterService.getPendingMenusForChecker(userDetails.getUsername());
    }

    @GetMapping(value = "/current-user", produces = CommonConstants.APPLICATION_JSON)
    public String getLoggedInUser(Principal principal) {
        return principal.getName();
    }
}
