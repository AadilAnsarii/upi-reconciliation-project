package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;

public interface BankAuthService {

    /** Step 0: check if user is NEW_USER (passwordSet=0) or OLD_USER */
    ResponseEntity<RestWithStatusList> verifyEmail(String username, String bankCode);

    /** Step 1: verify default credentials (BCrypt match from RCN_RECON_USER) */
    ResponseEntity<RestWithStatusList> verifyCredentials(String username, String bankCode, String defaultPassword);

    /** Step 2: set new password → status=VERIFIED, passwordSet=1, clear defaultPassword in bank */
    ResponseEntity<RestWithStatusList> setPassword(String username, String bankCode, String newPassword, String confirmPassword);

    /** Step 3a: password login → sends OTP (when enableOtp=true) */
    ResponseEntity<RestWithStatusList> loginSendOtp(String username, String password);

    /** Step 3b: verify OTP → status=ACTIVE → return JWT */
    ResponseEntity<RestWithStatusList> verifyOtp(String username, String otpCode, HttpServletResponse response);

    /** Step 3 (direct): password login → JWT immediately (when enableOtp=false) */
    ResponseEntity<RestWithStatusList> directLogin(String username, String password, HttpServletResponse response);

    /** Forgot password: send OTP to email */
    ResponseEntity<RestWithStatusList> forgotPasswordSendOtp(String email);

    /** Forgot password: verify OTP */
    ResponseEntity<RestWithStatusList> forgotVerifyOtp(String email, String otpCode);

    /** Forgot password: reset password after OTP verified */
    ResponseEntity<RestWithStatusList> forgotResetPassword(String email, String otpCode, String newPassword, String confirmPassword);
}
