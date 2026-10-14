package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.BankAuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Unified 3-step auth for Bank Admin, Branch Admin, Bank User, Branch User.
 * All user types share this same flow — userType in RCN_RECON_USER differentiates them.
 *
 * Step 0  GET  /api/v2/auth/user/verify-email          → NEW_USER / OLD_USER
 * Step 1  POST /api/v2/auth/user/verify-credentials    → verify default password
 * Step 2  POST /api/v2/auth/user/set-password          → set new password → VERIFIED
 * Step 3a POST /api/v2/auth/user/login                 → password → OTP sent
 * Step 3b POST /api/v2/auth/user/verify-otp            → OTP verified → JWT + ACTIVE
 * Step 3  POST /api/v2/auth/user/direct-login          → password → JWT (OTP disabled)
 *         POST /api/v2/auth/user/forgot-password       → send OTP
 *         POST /api/v2/auth/user/forgot-verify-otp     → verify OTP
 *         POST /api/v2/auth/user/forgot-reset-password → reset password
 */
@RestController
@RequestMapping("/api/v2/auth/user")
@CrossOrigin(origins = "*")
public class BankAuthController {

    @Autowired
    private BankAuthService bankAuthService;

    /** Step 0: check if NEW_USER or OLD_USER */
    @GetMapping("/verify-email")
    public ResponseEntity<RestWithStatusList> verifyEmail(
            @RequestParam String username,
            @RequestParam(required = false) String bankCode) {
        return bankAuthService.verifyEmail(username, bankCode);
    }

    /** Step 1: verify default credentials (first-time login check) */
    @PostMapping("/verify-credentials")
    public ResponseEntity<RestWithStatusList> verifyCredentials(@RequestBody Map<String, String> body) {
        return bankAuthService.verifyCredentials(
                body.get("username"), body.get("bankCode"), body.get("defaultPassword"));
    }

    /** Step 2: set new password → status becomes VERIFIED */
    @PostMapping("/set-password")
    public ResponseEntity<RestWithStatusList> setPassword(@RequestBody Map<String, String> body) {
        return bankAuthService.setPassword(
                body.get("username"), body.get("bankCode"),
                body.get("newPassword"), body.get("confirmPassword"));
    }

    /** Step 3a: password login → OTP sent to email */
    @PostMapping("/login")
    public ResponseEntity<RestWithStatusList> login(@RequestBody Map<String, String> body) {
        return bankAuthService.loginSendOtp(body.get("username"), body.get("password"));
    }

    /** Step 3b: verify OTP → JWT returned, status becomes ACTIVE */
    @PostMapping("/verify-otp")
    public ResponseEntity<RestWithStatusList> verifyOtp(
            @RequestBody Map<String, String> body, HttpServletResponse response) {
        return bankAuthService.verifyOtp(body.get("username"), body.get("otpCode"), response);
    }

    /** Step 3 direct: password → JWT (when OTP is disabled for the bank) */
    @PostMapping("/direct-login")
    public ResponseEntity<RestWithStatusList> directLogin(
            @RequestBody Map<String, String> body, HttpServletResponse response) {
        return bankAuthService.directLogin(body.get("username"), body.get("password"), response);
    }

    /** Forgot password: send OTP to email */
    @PostMapping("/forgot-password")
    public ResponseEntity<RestWithStatusList> forgotPassword(@RequestBody Map<String, String> body) {
        return bankAuthService.forgotPasswordSendOtp(body.get("email"));
    }

    /** Forgot password: verify OTP */
    @PostMapping("/forgot-verify-otp")
    public ResponseEntity<RestWithStatusList> forgotVerifyOtp(@RequestBody Map<String, String> body) {
        return bankAuthService.forgotVerifyOtp(body.get("email"), body.get("otpCode"));
    }

    /** Forgot password: reset password after OTP verified */
    @PostMapping("/forgot-reset-password")
    public ResponseEntity<RestWithStatusList> forgotResetPassword(@RequestBody Map<String, String> body) {
        return bankAuthService.forgotResetPassword(
                body.get("email"), body.get("otpCode"),
                body.get("newPassword"), body.get("confirmPassword"));
    }
}
