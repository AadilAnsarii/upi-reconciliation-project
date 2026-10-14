package com.jpb.reconciliation.reconciliation.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordServiceImpl.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Generate OTP and send email
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(ForgotPasswordRequestDto request) {
        // Null check — agar frontend ne galat field name bheja to NPE ki jagah clear error
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            logger.warn("Forgot password called with null/empty email");
            return ResponseEntity.badRequest()
                .body(new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "Email is required."));
        }
        String email = request.getEmail().trim().toLowerCase();

        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            logger.warn("Forgot password requested for unregistered email: {}", email);
            return ResponseEntity.ok(
                new ForgotPasswordResponseDto(CommonConstants.STATUS_404,
                    "This email address is not registered in our system. " +
                    "Please check and try again, or contact your administrator.")
            );
        }

        ReconUser user = userOpt.get();

        String otpCode = null;
        try {
            // Generate and store OTP FIRST — we'll roll it back if delivery fails
            otpCode = otpService.generateOtpForEmail(email);

            // Attempt email delivery — throws RuntimeException on failure
            emailService.sendForgotPasswordOtp(email, user.getUsername(), otpCode, otpService.getOtpExpiryMinutes());

            logger.info("OTP generated and delivered successfully to: {}", email);

        } catch (Exception e) {
            // ── Roll back: remove the undelivered OTP from memory ─────────────
            // Without this, a user could guess/brute-force an OTP that was
            // never actually delivered to their inbox.
            if (otpCode != null) {
                otpService.invalidateOtp(email);
                logger.warn("OTP invalidated for {} after failed email delivery.", email);
            }
            logger.error("Failed to send OTP to {}: {}", email, e.getMessage());
            return ResponseEntity.ok(
                new ForgotPasswordResponseDto("500",
                    "We were unable to deliver the OTP to your email address. " +
                    "Please check that your email is correct and try again. " +
                    "If the issue persists, contact support@kalinfotech.com")
            );
        }

        return ResponseEntity.ok(
            new ForgotPasswordResponseDto(CommonConstants.STATUS_200, "OTP sent successfully to " + email)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Verify OTP and reset password
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<ForgotPasswordResponseDto> resetPassword(ResetPasswordRequest request) {
        // Null-check all required fields — frontend field name mismatch would otherwise cause NPE
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            logger.warn("Reset password called with null/empty email");
            return ResponseEntity.badRequest()
                .body(new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "Email is required."));
        }
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            logger.warn("Reset password called with null/empty OTP");
            return ResponseEntity.badRequest()
                .body(new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "OTP is required."));
        }
        if (request.getNewPassword() == null || request.getConfirmNewPassword() == null) {
            logger.warn("Reset password called with null password field(s)");
            return ResponseEntity.badRequest()
                .body(new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "Password fields are required."));
        }

        String email      = request.getEmail().trim().toLowerCase();
        String otp        = request.getOtp().trim();
        String newPwd     = request.getNewPassword();
        String confirmPwd = request.getConfirmNewPassword();

        if (!newPwd.equals(confirmPwd)) {
            return ResponseEntity.badRequest()
                .body(new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "Passwords do not match."));
        }

        OtpVerifyResult result = otpService.verifyOtp(email, otp);
        switch (result) {
            case SUCCESS:
                break;
            case EXPIRED:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "OTP has expired. Please request a new one.")
                );
            case INVALID:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "Invalid OTP. Please check and try again.")
                );
            case MAX_ATTEMPTS_EXCEEDED:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "Too many incorrect attempts. Please request a new OTP.")
                );
            case NOT_FOUND:
            default:
                return ResponseEntity.ok(
                    new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "OTP not found or expired. Please request a new one.")
                );
        }

        Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.ok(new ForgotPasswordResponseDto(CommonConstants.STATUS_404, "User not found."));
        }

        ReconUser user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        reconUserRepository.save(user);

        logger.info("Password reset successfully for: {}", email);
        return ResponseEntity.ok(
            new ForgotPasswordResponseDto(CommonConstants.STATUS_200, "Password reset successful.")
        );
    }
}
