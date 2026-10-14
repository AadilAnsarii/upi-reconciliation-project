package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.PasswordPolicy; 
import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconAuthToken;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconAuthTokenRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.CustomUserDetailService;
import com.jpb.reconciliation.reconciliation.service.EmailService;
import com.jpb.reconciliation.reconciliation.service.OtpService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BankAuthServiceImpl implements BankAuthService {

    private static final Logger logger = LoggerFactory.getLogger(BankAuthServiceImpl.class);

    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private ReconBankMasterRepository reconBankMasterRepository;
    @Autowired private ReconPasswordManagerRepository reconPasswordManagerRepository;
    @Autowired private OtpService otpService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private ReconAuthTokenRepository reconAuthTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtHelper jwtHelper;
    @Autowired private CustomUserDetailService customUserDetailService;
    @Autowired private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ── Step 0: verify-email ────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> verifyEmail(String username, String bankCode) {
        Optional<ReconUser> userOpt = reconUserRepository.findByUsername(username.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            userOpt = reconUserRepository.findByEmail(username.trim().toLowerCase());
        }
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found.", null));
        }
        ReconUser user = userOpt.get();
        if ("BLOCKED".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("BLOCKED", "Account is BLOCKED. Contact administrator.", null));
        }
        if (CommonConstants.INACTIVE.equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList(CommonConstants.INACTIVE, "Account is INACTIVE. Contact administrator.", null));
        }
        String userType = user.getPasswordSet() != null && user.getPasswordSet() == 1 ? "OLD_USER" : "NEW_USER";
        Map<String, Object> payload = new HashMap<>();
        payload.put("userStatus", userType);
        payload.put("bankCode", bankCode);
        payload.put("username", user.getUsername());
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, userType, Collections.singletonList(payload)));
    }

    // ── Step 1: verify-credentials ──────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> verifyCredentials(String username, String bankCode, String defaultPassword) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }
        ReconUser user = userOpt.get();

        ResponseEntity<RestWithStatusList> statusGuard = guardStatus(user);
        if (statusGuard != null) return statusGuard;

        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            return ResponseEntity.ok(
                    new RestWithStatusList("ALREADY_VERIFIED", "Password already set. Proceeding to login.", null));
        }

        if (!passwordEncoder.matches(defaultPassword, user.getPasswordHash())) {
            auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN_FAIL",
                    null, "verify-credentials failed", username, user.getUserType(), user.getBankId(), "Invalid default password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }

        auditLog("RCN_RECON_USER", user.getUserId(), "VERIFY",
                null, "default credentials verified", username, user.getUserType(), user.getBankId(), "Step 1 passed");
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Credentials verified.", Collections.singletonList(user.getUsername())));
    }

    // ── Step 2: set-password ────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> setPassword(String username, String bankCode,
                                                           String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Passwords do not match.", null));
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, PasswordPolicy.REQUIREMENT, null));
        }

        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found.", null));
        }
        ReconUser user = userOpt.get();

        if (user.getPasswordSet() != null && user.getPasswordSet() == 1) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Password already set.", null));
        }

        // Check password not same as default
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "New password cannot be the same as the default password.", null));
        }

        // Check password history
        for (ReconPasswordManager pm : reconPasswordManagerRepository.findByReconUser(user)) {
            if (passwordEncoder.matches(newPassword, pm.getUserPassword())) {
                return ResponseEntity.badRequest()
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Password was used before. Choose a different one.", null));
            }
        }

        String oldHash = user.getPasswordHash();
        String oldStatus = user.getStatus();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordSet(1);
        user.setStatus(CommonConstants.VERIFIED);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(username);
        reconUserRepository.save(user);

        // Save to password history
        ReconPasswordManager pm = new ReconPasswordManager();
        pm.setReconUser(user);
        pm.setUserPassword(user.getPasswordHash());
        pm.setCreatedAt(LocalDateTime.now());
        pm.setCreatedBy(username);
        pm.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pm);

        // Update RECON_BANK_MASTER: clear default password + set status VERIFIED
        if (bankCode != null && !bankCode.trim().isEmpty()) {
            reconBankMasterRepository.findByBankCode(bankCode.trim()).ifPresent(bank -> {
                bank.setDefaultPassword(null);
                bank.setStatus(CommonConstants.VERIFIED);
                bank.setUpdatedAt(LocalDateTime.now());
                bank.setUpdatedBy(username);
                reconBankMasterRepository.save(bank);
            });
        }

        auditLog("RCN_RECON_USER", user.getUserId(), "UPDATE",
                "passwordSet=0,status=" + oldStatus,
                "passwordSet=1,status=VERIFIED",
                username, user.getUserType(), user.getBankId(), "Password set, status→VERIFIED");

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Password set successfully. Proceed to login.", null));
    }

    // ── Step 3a: login → send OTP ───────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> loginSendOtp(String username, String password) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }
        ReconUser user = userOpt.get();

        ResponseEntity<RestWithStatusList> guard = guardStatus(user);
        if (guard != null) return guard;

        if (user.getPasswordSet() == null || user.getPasswordSet() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Please set your password first.", null));
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN_FAIL",
                    null, null, username, user.getUserType(), user.getBankId(), "Wrong password on login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }

        // Generate OTP (in-memory) & send
        String otp = otpService.generateOtpForEmail(user.getEmail());
        emailService.sendLoginOtp(user.getEmail(), user.getFullName(), otp, otpService.getOtpExpiryMinutes());

        auditLog("RCN_RECON_USER", user.getUserId(), CommonConstants.LOGIN,
                null, "OTP sent", username, user.getUserType(), user.getBankId(), "Login step: OTP dispatched");

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "OTP sent to registered email.",
                Collections.singletonList(maskEmail(user.getEmail()))));
    }

    // ── Step 3b: verify OTP → status=ACTIVE → JWT ──────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> verifyOtp(String username, String otpCode, HttpServletResponse response) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found.", null));
        }
        ReconUser user = userOpt.get();

        OtpService.OtpVerifyResult otpResult = otpService.verifyOtp(user.getEmail(), otpCode.trim());
        switch (otpResult) {
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "No active OTP found. Please request again.", null));
            case EXPIRED:
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "OTP expired. Please login again.", null));
            case INVALID:
            case MAX_ATTEMPTS_EXCEEDED:
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid OTP.", null));
            default:
                break;
        }

        // First-time OTP login: VERIFIED → ACTIVE; subsequent logins already ACTIVE
        boolean firstLogin = CommonConstants.VERIFIED.equals(user.getStatus());
        if (firstLogin) {
            user.setStatus(CommonConstants.ACTIVE);
            user.setApprovedYn(CommonConstants.ACTIVE_Y);
            user.setApprovedBy(CommonConstants.SYSTEM);
        }
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        reconUserRepository.save(user);

        // Sync RECON_BANK_MASTER status → ACTIVE on first login
        if (firstLogin && user.getBankId() != null) {
            reconBankMasterRepository.findPrimaryById(user.getBankId()).ifPresent(bank -> {
                bank.setStatus(CommonConstants.ACTIVE);
                bank.setUpdatedAt(LocalDateTime.now());
                bank.setUpdatedBy(username);
                reconBankMasterRepository.save(bank);
            });
        }

        auditLog("RCN_RECON_USER", user.getUserId(), CommonConstants.LOGIN,
                null, "status→ACTIVE", username, user.getUserType(), user.getBankId(), "OTP verified, login successful");

        return ResponseEntity.ok(buildJwtResponse(user, response));
    }

    // ── Step 3 (direct login — no OTP) ─────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> directLogin(String username, String password, HttpServletResponse response) {
        Optional<ReconUser> userOpt = findUser(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }
        ReconUser user = userOpt.get();

        ResponseEntity<RestWithStatusList> guard = guardStatus(user);
        if (guard != null) return guard;

        if (user.getPasswordSet() == null || user.getPasswordSet() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Please set your password first.", null));
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLog("RCN_RECON_USER", user.getUserId(), "LOGIN_FAIL",
                    null, null, username, user.getUserType(), user.getBankId(), "Wrong password (direct-login)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }

        boolean firstLogin = CommonConstants.VERIFIED.equals(user.getStatus());
        if (firstLogin) {
            user.setStatus(CommonConstants.ACTIVE);
            user.setApprovedYn(CommonConstants.ACTIVE_Y);
            user.setApprovedBy(CommonConstants.SYSTEM);
        }
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        reconUserRepository.save(user);

        // Sync RECON_BANK_MASTER status → ACTIVE on first login
        if (firstLogin && user.getBankId() != null) {
            reconBankMasterRepository.findPrimaryById(user.getBankId()).ifPresent(bank -> {
                bank.setStatus(CommonConstants.ACTIVE);
                bank.setUpdatedAt(LocalDateTime.now());
                bank.setUpdatedBy(username);
                reconBankMasterRepository.save(bank);
            });
        }

        auditLog("RCN_RECON_USER", user.getUserId(), CommonConstants.LOGIN,
                null, "direct-login,status→ACTIVE", username, user.getUserType(), user.getBankId(), "Direct login successful");

        return ResponseEntity.ok(buildJwtResponse(user, response));
    }

    // ── Forgot password: send OTP ───────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> forgotPasswordSendOtp(String email) {
        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            // Don't reveal if email exists
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "If this email is registered, an OTP will be sent.", null));
        }
        ReconUser user = userOpt.get();
        if ("BLOCKED".equals(user.getStatus()) || CommonConstants.INACTIVE.equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Account is " + user.getStatus() + ". Contact administrator.", null));
        }

        String otp = otpService.generateOtpForEmail(user.getEmail());
        try {
            emailService.sendForgotPasswordOtp(user.getEmail(), user.getFullName(), otp, otpService.getOtpExpiryMinutes());
        } catch (Exception e) {
            otpService.invalidateOtp(user.getEmail());
            throw e;
        }

        auditLog("RCN_RECON_USER", user.getUserId(), "FORGOT_PWD",
                null, "OTP sent", email, user.getUserType(), user.getBankId(), "Forgot password OTP dispatched");

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "If this email is registered, an OTP will be sent.", null));
    }

    // ── Forgot password: verify OTP ─────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> forgotVerifyOtp(String email, String otpCode) {
        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid request.", null));
        }
        ReconUser user = userOpt.get();
        OtpService.OtpVerifyResult peekResult = otpService.peekOtp(user.getEmail(), otpCode.trim());
        if (peekResult != OtpService.OtpVerifyResult.SUCCESS) {
            String msg = peekResult == OtpService.OtpVerifyResult.EXPIRED
                    ? "OTP expired. Please request again." : "Invalid or expired OTP.";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, msg, null));
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "OTP verified.", null));
    }

    // ── Forgot password: reset password ─────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> forgotResetPassword(String email, String otpCode,
                                                                    String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Passwords do not match.", null));
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, PasswordPolicy.REQUIREMENT, null));
        }

        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid request.", null));
        }
        ReconUser user = userOpt.get();

        OtpService.OtpVerifyResult resetOtpResult = otpService.verifyOtp(user.getEmail(), otpCode.trim());
        if (resetOtpResult != OtpService.OtpVerifyResult.SUCCESS) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "OTP invalid or expired.", null));
        }

        // Check password history
        for (ReconPasswordManager pm : reconPasswordManagerRepository.findByReconUser(user)) {
            if (passwordEncoder.matches(newPassword, pm.getUserPassword())) {
                return ResponseEntity.badRequest()
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Password was used before. Choose a different one.", null));
            }
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordSet(1);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(email);
        reconUserRepository.save(user);

        ReconPasswordManager pm = new ReconPasswordManager();
        pm.setReconUser(user);
        pm.setUserPassword(user.getPasswordHash());
        pm.setCreatedAt(LocalDateTime.now());
        pm.setCreatedBy(email);
        pm.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pm);

        auditLog("RCN_RECON_USER", user.getUserId(), "RESET_PWD",
                null, "password reset via OTP", email, user.getUserType(), user.getBankId(), "Forgot password reset successful");

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Password reset successfully. You can now login.", null));
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private Optional<ReconUser> findUser(String identifier) {
        String id = identifier.trim().toLowerCase();
        Optional<ReconUser> opt = reconUserRepository.findByUsername(id);
        if (!opt.isPresent()) opt = reconUserRepository.findByEmail(id);
        return opt;
    }

    private ResponseEntity<RestWithStatusList> guardStatus(ReconUser user) {
        if ("BLOCKED".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("BLOCKED",
                            "Account is BLOCKED. Reason: " + (user.getBlockReason() != null ? user.getBlockReason() : "Contact administrator."), null));
        }
        if (CommonConstants.INACTIVE.equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList(CommonConstants.INACTIVE, "Account is INACTIVE. Contact administrator.", null));
        }
        // Scheduled for reactivation but not live yet — no login until it actually flips to ACTIVE.
        // (INACTIVE_PENDING / BLOCK_PENDING accounts are still functionally active, so they may sign
        // in; only ACTIVE_PENDING is a not-yet-usable account.)
        if ("ACTIVE_PENDING".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RestWithStatusList("ACTIVE_PENDING",
                            "Your account is scheduled for reactivation and is not active yet. "
                          + "Please wait until it becomes Active, then sign in again.", null));
        }
        return null;
    }

    private RestWithStatusList buildJwtResponse(ReconUser user, HttpServletResponse response) {
        UserDetails userDetails = customUserDetailService.loadUserByUsername(user.getUsername());
        String accessToken  = jwtHelper.generateToken(userDetails);
        String refreshToken = jwtHelper.generateTokenForRefresh(user.getUsername());

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(8 * 60 * 60);
        response.addCookie(refreshCookie);

        LocalDateTime now = LocalDateTime.now();

        // Invalidate any existing ACTIVE tokens for this user before saving new ones
        reconAuthTokenRepository.findByUserIdAndStatus(user.getUserId(), CommonConstants.ACTIVE)
                .forEach(t -> { t.setStatus("EXPIRED"); reconAuthTokenRepository.save(t); });

        ReconAuthToken accessEntry = new ReconAuthToken();
        accessEntry.setUserId(user.getUserId());
        accessEntry.setTokenType("ACCESS");
        accessEntry.setTokenValue(accessToken);
        accessEntry.setStatus(CommonConstants.ACTIVE);
        accessEntry.setExpiresAt(now.plusHours(1));
        accessEntry.setCreatedAt(now);
        reconAuthTokenRepository.save(accessEntry);

        ReconAuthToken refreshEntry = new ReconAuthToken();
        refreshEntry.setUserId(user.getUserId());
        refreshEntry.setTokenType("REFRESH");
        refreshEntry.setTokenValue(refreshToken);
        refreshEntry.setStatus(CommonConstants.ACTIVE);
        refreshEntry.setExpiresAt(now.plusHours(8));
        refreshEntry.setCreatedAt(now);
        reconAuthTokenRepository.save(refreshEntry);

        AuthResponse auth = new AuthResponse(
                accessToken, refreshToken,
                user.getUserId(), user.getRoleId(), user.getUserType(),
                user.getFullName(), user.getUsername(), user.getBankId());
        return new RestWithStatusList(CommonConstants.SUCCESS, "Login successful.", Collections.singletonList(auth));
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String name = email.substring(0, at);
        String domain = email.substring(at);
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    private void auditLog(String table, Long recordId, String operation,
                           String oldVal, String newVal, String actor,
                           String actorType, Long bankId, String label) {
        try {
            auditLogService.log(table, recordId, operation, null, actor,
                    actorType, null, bankId, oldVal, newVal, label, null, null);
        } catch (Exception e) {
            logger.warn("AuditLog save failed: {}", e.getMessage());
        }
    }
}
