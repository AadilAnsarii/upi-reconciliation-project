package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.OtpService;
import com.jpb.reconciliation.reconciliation.service.OtpService.OtpVerifyResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/otp")
@CrossOrigin(origins = "*")
public class OtpController {

    private static final Logger logger = LoggerFactory.getLogger(OtpController.class);

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconBankMasterRepository reconBankMasterRepository;

    // ───────────────── SEND OTP ─────────────────

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse("Email is required."));
        }

        try {
            otpService.generateAndSendOtp(email);
            logger.info("[OTP-SEND] OTP sent successfully to: {}", maskEmail(email));

            Map<String, Object> res = new HashMap<>();
            res.put(CommonConstants.SUCCESS, true);
            res.put("message", "OTP sent to " + maskEmail(email));
            return ResponseEntity.ok(res);

        } catch (EmailDeliveryException e) {
            // OTP was already rolled back inside OtpService — safe to return error
            logger.error("[OTP-SEND-FAIL] Email delivery failed for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("We could not deliver the OTP to your email address. " +
                            "Please check the address and try again. " +
                            "If the problem persists, contact support@kalinfotech.com"));
        } catch (Exception e) {
            logger.error("[OTP-SEND-FAIL] Unexpected error for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("An unexpected error occurred. Please try again."));
        }
    }

    // ───────────────── VERIFY OTP — JWT return karo on success ─────────────────

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse("Email and OTP are required."));
        }

        OtpVerifyResult result = otpService.verifyOtp(email, otp);

        if (result == OtpVerifyResult.SUCCESS) {

            // ── Resolve bankCode, branchCode, and JWT subject by email ──
            String resolvedBankCode   = null;
            String resolvedBranchCode = null;
            String jwtSubject         = email;

            java.util.Optional<ReconUser> userOpt = reconUserRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                ReconUser user = userOpt.get();
                jwtSubject = user.getUsername();

                // Activate user on first OTP login if still PENDING_APPROVAL
                if ("PENDING_APPROVAL".equals(user.getStatus())) {
                    user.setStatus(CommonConstants.ACTIVE);
                    user.setApprovedYn(CommonConstants.ACTIVE_Y);
                    reconUserRepository.save(user);
                }

                if (user.getBankId() != null) {
                    java.util.Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findById(user.getBankId());
                    if (bankOpt.isPresent()) {
                        ReconBankMaster bank = bankOpt.get();
                        resolvedBankCode = bank.getBankCode();
                        if ("BRANCH".equals(bank.getBankType())) {
                            resolvedBranchCode = bank.getBankCode();
                            if (bank.getParentBankId() != null) {
                                resolvedBankCode = reconBankMasterRepository.findById(bank.getParentBankId())
                                        .map(ReconBankMaster::getBankCode).orElse(null);
                            }
                        }
                    }
                }
                logger.info("[OTP-VERIFY] user='{}' userType='{}' bankCode='{}' branchCode='{}'",
                        jwtSubject, user.getUserType(), resolvedBankCode, resolvedBranchCode);
            }
            logger.info("[OTP-VERIFY] bankCode={} branchCode={} jwtSubject={} for {}",
                    resolvedBankCode, resolvedBranchCode, jwtSubject, maskEmail(email));

            // ── JWT generate karo resolved subject se ──
            UserDetails userDetails = User.builder()
                    .username(jwtSubject)
                    .password("")
                    .authorities(new ArrayList<>())
                    .build();

            String jwtRole      = (resolvedBranchCode != null) ? "BRANCH_ADMIN" : "BANK_ADMIN";
            String accessToken  = jwtHelper.generateToken(userDetails, jwtRole);
            String refreshToken = jwtHelper.generateTokenForRefresh(jwtSubject);

            Map<String, Object> res = new HashMap<>();
            res.put(CommonConstants.SUCCESS,      true);
            res.put("message",      "OTP verified successfully.");
            res.put("accessToken",  accessToken);
            res.put("refreshToken", refreshToken);
            res.put("email",        email);
            res.put("bankCode",     resolvedBankCode);
            if (resolvedBranchCode != null) {
                res.put("branchCode", resolvedBranchCode);
            }

            return ResponseEntity.ok(res);

        } else if (result == OtpVerifyResult.INVALID) {

            return ResponseEntity
                    .status(400)
                    .body(errorResponse("Invalid OTP. Please try again."));

        } else if (result == OtpVerifyResult.EXPIRED) {

            return ResponseEntity
                    .status(400)
                    .body(errorResponse("OTP has expired. Please request a new one."));

        } else if (result == OtpVerifyResult.NOT_FOUND) {

            return ResponseEntity
                    .status(400)
                    .body(errorResponse("No OTP found. Please request a new one."));

        } else if (result == OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED) {

            return ResponseEntity
                    .status(429)
                    .body(errorResponse("Too many incorrect attempts. Please request a new OTP."));

        } else {

            return ResponseEntity
                    .status(500)
                    .body(errorResponse("Something went wrong."));
        }
    }

    // ───────────────── RESEND OTP ─────────────────

    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendOtp(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse("Email is required."));
        }

        try {
            otpService.resendOtp(email);
            logger.info("[OTP-RESEND] OTP resent successfully to: {}", maskEmail(email));

            Map<String, Object> res = new HashMap<>();
            res.put(CommonConstants.SUCCESS, true);
            res.put("message", "New OTP sent to " + maskEmail(email));
            return ResponseEntity.ok(res);

        } catch (EmailDeliveryException e) {
            // OTP rollback already handled inside OtpService
            logger.error("[OTP-RESEND-FAIL] Email delivery failed for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("We could not deliver the OTP to your email address. " +
                            "Please check the address and try again."));
        } catch (Exception e) {
            logger.error("[OTP-RESEND-FAIL] Unexpected error for: {} | reason: {}", maskEmail(email), e.getMessage());
            return ResponseEntity
                    .status(500)
                    .body(errorResponse("An unexpected error occurred while resending OTP. Please try again."));
        }
    }

    // NOTE: the former POST /api/otp/set-password stub was removed. It had no caller (the real
    // password-set flow is POST /api/v2/auth/user/set-password in BankAuthController), persisted
    // nothing, always returned a fake CommonConstants.SUCCESS, and printed the plaintext password to the console.

    // ───────────────── HELPERS ─────────────────

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> res = new HashMap<>();
        res.put(CommonConstants.SUCCESS, false);
        res.put("message", message);
        return res;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        String local  = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "*****" + local.charAt(local.length() - 1) + domain;
    }
}