package com.jpb.reconciliation.reconciliation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.entity.OtpManager;
import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;
import com.jpb.reconciliation.reconciliation.repository.OtpManagerRepository;

import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    @Autowired
    private JavaMailSender mailSender;

    // Use the configured from address — must match SMTP authenticated account
    @Value("${app.mail.from}")
    private String fromAddress;

    @Autowired
    private OtpManagerRepository otpManagerRepository;

    // Verification runs off this in-memory map — it holds the attempt counter and makes an OTP
    // one-time-use without a DB round trip on every keystroke.
    // Key = email (lowercase), Value = OtpEntry
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    // ─── Generate & Send OTP — ATOMIC ────────────────────────────────────────
    // OTP is stored first, then email is attempted.
    // If email delivery fails, the OTP is removed (rollback) so an
    // undelivered OTP can never be brute-forced or accidentally used.
    public void generateAndSendOtp(String email) {
        String key = email.toLowerCase();
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Store OTP tentatively
        otpStore.put(key, new OtpEntry(otp, expiry, 0));
        record(key, otp, expiry);

        try {
            sendOtpEmail(email, otp);
            logger.info("[OTP-OK] OTP generated and delivered to: {}", key);
        } catch (Exception e) {
            // ── Rollback: an OTP that never reached the user must not stay usable ──
            otpStore.remove(key);
            markUsed(key); // closes the audit row too — it can never be redeemed
            logger.error("[OTP-ROLLBACK] OTP removed for {} after email delivery failure: {}", key, e.getMessage());
            throw new EmailDeliveryException(
                "OTP could not be delivered to " + email + ". Please verify the email address and try again.",
                email, e);
        }
    }

    // Generates + stores OTP but returns the code so caller can send via EmailService
    public String generateOtpForEmail(String email) {
        String key = email.toLowerCase();
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStore.put(key, new OtpEntry(otp, expiry, 0));
        record(key, otp, expiry);
        return otp;
    }

    /**
     * Writes the OTP to RECON_OTP_MANAGER so there is a durable record of what was issued, to whom
     * and when — the in-memory map alone leaves no trail and is gone on restart.
     *
     * Any still-open OTP for this address is closed first: only one OTP may ever be redeemable at a
     * time, so issuing a new one must retire the old.
     *
     * Best-effort — a failure to write history must never stop a user from logging in.
     */
    private void record(String emailKey, String otp, LocalDateTime expiry) {
        try {
            otpManagerRepository.invalidatePreviousOtps(emailKey);
            OtpManager row = new OtpManager();
            row.setEmailId(emailKey);
            row.setOtpCode(otp);
            row.setExpiryTime(expiry);
            row.setIsUsed(CommonConstants.INACTIVE_N);
            row.setCreatedAt(LocalDateTime.now());
            otpManagerRepository.save(row);
        } catch (RuntimeException e) {
            logger.warn("[OTP-HISTORY] Could not record OTP for {}: {}", emailKey, e.getMessage());
        }
    }

    /** Closes the open OTP row for this address — redeemed, expired, or never delivered. */
    private void markUsed(String emailKey) {
        try {
            otpManagerRepository.invalidatePreviousOtps(emailKey);
        } catch (RuntimeException e) {
            logger.warn("[OTP-HISTORY] Could not close OTP row for {}: {}", emailKey, e.getMessage());
        }
    }

    public int getOtpExpiryMinutes() {
        return OTP_EXPIRY_MINUTES;
    }

    // ─── Verify OTP ──────────────────────────────────────────────────────────

    public OtpVerifyResult verifyOtp(String email, String submittedOtp) {
        String key = email.toLowerCase();
        OtpEntry entry = otpStore.get(key);

        if (entry == null) {
            return OtpVerifyResult.NOT_FOUND;
        }
        if (LocalDateTime.now().isAfter(entry.expiry)) {
            otpStore.remove(key);
            markUsed(key);
            return OtpVerifyResult.EXPIRED;
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(key);
            markUsed(key);
            return OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED;
        }
        if (!entry.otp.equals(submittedOtp)) {
            entry.attempts++;
            return OtpVerifyResult.INVALID;
        }

        otpStore.remove(key); // one-time use — remove after success
        markUsed(key);        // and mark it redeemed in the history table
        return OtpVerifyResult.SUCCESS;
    }

    // ─── Peek OTP (verify without consuming) ─────────────────────────────────
    // Use this when you need to validate the OTP but leave it for a subsequent step.
    public OtpVerifyResult peekOtp(String email, String submittedOtp) {
        String key = email.toLowerCase();
        OtpEntry entry = otpStore.get(key);
        if (entry == null) return OtpVerifyResult.NOT_FOUND;
        if (LocalDateTime.now().isAfter(entry.expiry)) {
            otpStore.remove(key);
            return OtpVerifyResult.EXPIRED;
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(key);
            return OtpVerifyResult.MAX_ATTEMPTS_EXCEEDED;
        }
        if (!entry.otp.equals(submittedOtp)) {
            entry.attempts++;
            return OtpVerifyResult.INVALID;
        }
        return OtpVerifyResult.SUCCESS; // does NOT remove — caller must use verifyOtp() to consume
    }

    // ─── Resend OTP ──────────────────────────────────────────────────────────

    public void resendOtp(String email) {
        generateAndSendOtp(email); // resets expiry and attempts
    }

    // ─── Invalidate OTP (call when email delivery fails) ─────────────────────
    // Removes the OTP from memory so an undelivered OTP cannot be used later.
    public void invalidateOtp(String email) {
        String key = email.toLowerCase();
        otpStore.remove(key);
        markUsed(key);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // always 6 digits
        return String.valueOf(otp);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@reconxpert.ai");
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai - Your OTP for Login");
            helper.setText(buildEmailHtml(otp), true);

            mailSender.send(message);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] OTP login email — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver OTP email to: " + toEmail, toEmail, e);
        }
    }

    // Java 8 compatible — plain string concatenation bnkead of text blocks
    private String buildEmailHtml(String otp) {
        return "<div style='font-family: Arial, sans-serif; max-width: 480px; margin: auto; padding: 32px; border: 1px solid #e5e7eb; border-radius: 8px;'>"
             + "  <div style='text-align: center; margin-bottom: 24px;'>"
             + "    <span style='font-size: 20px; font-weight: 600; color: #0f2137;'>ReconXpert.Ai</span><br/>"
             + "    <span style='font-size: 12px; color: #6b7280;'>by KalInfotech</span>"
             + "  </div>"
             + "  <p style='color: #374151; font-size: 15px;'>Your One-Time Password (OTP) for login is:</p>"
             + "  <div style='text-align: center; margin: 24px 0;'>"
             + "    <span style='display: inline-block; font-size: 36px; font-weight: 700; letter-spacing: 12px;"
             + "                 color: #0f2137; background: #f0f4ff; padding: 16px 28px; border-radius: 8px;'>"
             + otp
             + "    </span>"
             + "  </div>"
             + "  <p style='color: #6b7280; font-size: 13px;'>This OTP is valid for <strong>5 minutes</strong> and can only be used once.</p>"
             + "  <p style='color: #6b7280; font-size: 13px;'>If you did not request this, please ignore this email.</p>"
             + "  <hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'/>"
             + "  <p style='color: #9ca3af; font-size: 11px; text-align: center;'>ReconXpert.Ai | KalInfotech | Do not reply to this email</p>"
             + "</div>";
    }

    // ─── Inner Classes ────────────────────────────────────────────────────────

    private static class OtpEntry {
        String otp;
        LocalDateTime expiry;
        int attempts;

        OtpEntry(String otp, LocalDateTime expiry, int attempts) {
            this.otp = otp;
            this.expiry = expiry;
            this.attempts = attempts;
        }
    }

    public enum OtpVerifyResult {
        SUCCESS,
        INVALID,
        EXPIRED,
        NOT_FOUND,
        MAX_ATTEMPTS_EXCEEDED
    }
}