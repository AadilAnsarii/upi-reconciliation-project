package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.KalUserDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.KalAdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v2/admin/auth")
@CrossOrigin(origins = "*")
@Tag(name = "KalAdmin Auth V2", description = "KalInfotech internal admin account management")
public class KalAdminAuthController {

    @Autowired
    private KalAdminAuthService kalAdminAuthService;

    /**
     * POST /api/v2/admin/auth/create
     * Public endpoint — no JWT required.
     * Only @kalinfotech.com emails allowed (enforced in KalUserDto validation).
     *
     * Tables written:
     *   1. RCN_RECON_USER        — user record (USER_TYPE=KAL_ADMIN, STATUS=ACTIVE)
     *   2. RCN_RECON_PWD_MANAGER — password hash linked to user
     *   3. AUDIT_LOG             — creation audit trail
     * RECON_AUTH_TOKEN is NOT written here — only written on login/OTP flows.
     */
    @Operation(summary = "Register a new KalInfotech Admin account")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> create(@Valid @RequestBody KalUserDto dto) {
        return kalAdminAuthService.createKalAdmin(dto);
    }
}
