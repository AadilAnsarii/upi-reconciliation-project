package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.service.v2.ReconRoleMasterService;
import com.jpb.reconciliation.reconciliation.service.v2.RoleCodeGeneratorService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recon-roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReconRoleMasterController {

    private final ReconRoleMasterService reconRoleMasterService;
    private final RoleCodeGeneratorService roleCodeGeneratorService;

    @GetMapping("/generate-code")
    public ResponseEntity<RestWithStatusList> generateRoleCode(@RequestParam String roleName) {
        String code = roleCodeGeneratorService.peekNextCode(roleName);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Role code reserved.",
                java.util.Collections.singletonList(code)));
    }

    // force=true means "the user saw the 'role already exists for this organization' prompt and
    // chose to continue". Default false: a same-name role in the same tenant returns
    // DUPLICATE_ROLE_EXISTS so the frontend can show the confirmation dialog.
    @PostMapping("/create")
    public ResponseEntity<RestWithStatusList> createRole(
            @RequestBody ReconRoleMaster role,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.createRole(role, userDetails.getUsername(), force);
    }

    @PostMapping("/create-active")
    public ResponseEntity<RestWithStatusList> createRoleActive(
            @RequestBody ReconRoleMaster role,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.createRoleActive(role, userDetails.getUsername(), force);
    }

    @GetMapping
    public ResponseEntity<RestWithStatusList> getAllRoles() {
        return reconRoleMasterService.getAllRoles();
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RestWithStatusList> getRoleById(@PathVariable Long roleId) {
        return reconRoleMasterService.getRoleById(roleId);
    }

    @GetMapping("/by-status")
    public ResponseEntity<RestWithStatusList> getRolesByStatus(@RequestParam String status) {
        return reconRoleMasterService.getRolesByStatus(status);
    }

    @GetMapping("/by-type")
    public ResponseEntity<RestWithStatusList> getRolesByType(@RequestParam String roleType) {
        return reconRoleMasterService.getRolesByType(roleType);
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RestWithStatusList> updateRole(
            @PathVariable Long roleId,
            @RequestBody ReconRoleMaster role,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.updateRole(roleId, role, userDetails.getUsername());
    }

    @PutMapping("/{roleId}/submit")
    public ResponseEntity<RestWithStatusList> submitForApproval(
            @PathVariable Long roleId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.submitForApproval(roleId, userDetails.getUsername());
    }

    @PutMapping("/{roleId}/approve")
    public ResponseEntity<RestWithStatusList> approveRole(
            @PathVariable Long roleId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.approveRole(roleId, userDetails.getUsername());
    }

    @PutMapping("/{roleId}/status")
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long roleId,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.updateStatus(roleId, status, userDetails.getUsername());
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<RestWithStatusList> deleteRole(@PathVariable Long roleId) {
        return reconRoleMasterService.deleteRole(roleId);
    }

    @GetMapping("/check-code")
    public ResponseEntity<RestWithStatusList> checkRoleCodeExists(@RequestParam String roleCode) {
        return reconRoleMasterService.checkRoleCodeExists(roleCode);
    }

    @GetMapping("/{roleId}/privileges")
    public ResponseEntity<RestWithStatusList> getPrivileges(@PathVariable Long roleId) {
        return reconRoleMasterService.getPrivileges(roleId);
    }

    @GetMapping("/{roleId}/locked-privileges")
    public ResponseEntity<RestWithStatusList> getLockedPrivileges(@PathVariable Long roleId) {
        return reconRoleMasterService.getLockedPrivileges(roleId);
    }

    /** The Bank/Branch that actually owns this role — see ReconRoleMasterService#getOwningBank. */
    @GetMapping("/{roleId}/owning-bank")
    public ResponseEntity<RestWithStatusList> getOwningBank(@PathVariable Long roleId) {
        return reconRoleMasterService.getOwningBank(roleId);
    }

    @PutMapping("/{roleId}/privileges")
    public ResponseEntity<RestWithStatusList> savePrivileges(
            @PathVariable Long roleId,
            @RequestBody java.util.List<Long> menuIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.savePrivileges(roleId, menuIds, userDetails.getUsername());
    }

    /** Roles the caller may see. Admin → their institution; anyone else → only their own subtree,
     *  never an ancestor's role and never a Checker role. Scope is taken from the JWT, so a client
     *  cannot widen it by passing a different bank. */
    @GetMapping("/visible")
    public ResponseEntity<RestWithStatusList> getRolesVisibleTo(@AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.getRolesVisibleTo(userDetails.getUsername());
    }

    @GetMapping("/by-bank/{bankId}")
    public ResponseEntity<RestWithStatusList> getRolesByBankId(@PathVariable Long bankId) {
        return reconRoleMasterService.getRolesByBankId(bankId);
    }

    @GetMapping("/{roleId}/products")
    public ResponseEntity<RestWithStatusList> getRoleProducts(@PathVariable Long roleId) {
        return reconRoleMasterService.getRoleProducts(roleId);
    }

    @PutMapping("/{roleId}/products")
    public ResponseEntity<RestWithStatusList> saveRoleProducts(
            @PathVariable Long roleId,
            @RequestBody java.util.List<Long> productIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.saveRoleProducts(roleId, productIds, userDetails.getUsername());
    }

    @PutMapping("/{roleId}/bank-type-scope")
    public ResponseEntity<RestWithStatusList> saveRoleBankTypeScope(
            @PathVariable Long roleId,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.saveRoleBankTypeScope(roleId, body.get("bankTypeScope"), userDetails.getUsername());
    }

    @GetMapping("/checker-queue")
    public ResponseEntity<RestWithStatusList> getPendingRolesForChecker(@AuthenticationPrincipal UserDetails userDetails) {
        return reconRoleMasterService.getPendingRolesForChecker(userDetails.getUsername());
    }
}
