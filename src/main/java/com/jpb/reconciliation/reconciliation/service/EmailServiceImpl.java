package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.exception.EmailDeliveryException;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    // ─────────────────────────────────────────────────────────────────────
    // SEND LOGIN OTP EMAIL
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void sendLoginOtp(String toEmail, String userName,
                             String otpCode, int expiryMins) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai - Your OTP for Login");
            helper.setText(buildLoginOtpEmailHtml(userName, otpCode, expiryMins), true);
            mailSender.send(message);
            logger.info("Login OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Login OTP email — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver login OTP email (messaging error): " + e.getMessage(), toEmail, e);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Login OTP email — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver login OTP email (unexpected error): " + e.getMessage(), toEmail, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND FORGOT PASSWORD OTP EMAIL
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void sendForgotPasswordOtp(String toEmail, String userName,
                                      String otpCode, int expiryMins) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Password Reset Verification Code");
            helper.setText(buildOtpEmailHtml(userName, otpCode, expiryMins), true);

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] OTP email — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver OTP email (messaging error): " + e.getMessage(), toEmail, e);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] OTP email — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to deliver OTP email (unexpected error): " + e.getMessage(), toEmail, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND Bank Admin WELCOME EMAIL
    // Includes: Bank Code, User ID, Default Password, Verify Link
    // @Async — runs in background, won't block the API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBankAdminWelcome(String toEmail, String superUserName,
                                     String bankName, String bankCode,
                                     String superUserId, String defaultPassword,
                                     String verifyLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Welcome! Your Bank Administrator Account Has Been Created");
            helper.setText(buildSuperUserWelcomeHtml(
                superUserName, bankName, bankCode,
                superUserId, defaultPassword, verifyLink), true);
            mailSender.send(message);
            logger.info("SuperUser welcome email sent to: {} | userId: {}", toEmail, superUserId);
        } catch (MessagingException e) {
            // @Async — exception won't reach caller; bankmust NOT be rolled back on email failure
            logger.error("[EMAIL-DELIVERY-FAIL] SuperUser welcome — recipient: {} | userId: {} | reason: {}", toEmail, superUserId, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] SuperUser welcome — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Login OTP Email
    // ─────────────────────────────────────────────────────────────────────
    private String buildLoginOtpEmailHtml(String userName, String otpCode, int expiryMins) {
        return "<div style='font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;border:1px solid #e5e7eb;border-radius:8px;'>"
            + "<div style='text-align:center;margin-bottom:24px;'>"
            + "<span style='font-size:20px;font-weight:600;color:#0f2137;'>ReconXpert.Ai</span><br/>"
            + "<span style='font-size:12px;color:#6b7280;'>by KalInfotech</span>"
            + "</div>"
            + "<p style='color:#374151;font-size:15px;'>Dear <strong>" + sanitize(userName) + "</strong>,</p>"
            + "<p style='color:#374151;font-size:15px;'>Your One-Time Password (OTP) for login is:</p>"
            + "<div style='text-align:center;margin:24px 0;'>"
            + "<span style='display:inline-block;font-size:36px;font-weight:700;letter-spacing:12px;"
            + "color:#0f2137;background:#f0f4ff;padding:16px 28px;border-radius:8px;'>"
            + sanitize(otpCode)
            + "</span>"
            + "</div>"
            + "<p style='color:#6b7280;font-size:13px;'>This OTP is valid for <strong>" + expiryMins + " minutes</strong> and can only be used once.</p>"
            + "<p style='color:#6b7280;font-size:13px;'>If you did not request this, please ignore this email.</p>"
            + "<hr style='border:none;border-top:1px solid #e5e7eb;margin:24px 0;'/>"
            + "<p style='color:#9ca3af;font-size:11px;text-align:center;'>ReconXpert.Ai | KalInfotech | Do not reply to this email</p>"
            + "</div>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — OTP Email
    // ─────────────────────────────────────────────────────────────────────
    private String buildOtpEmailHtml(String userName, String otpCode, int expiryMins) {
        return "<!DOCTYPE html>"
            + "<html lang='en'>"
            + "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Password Reset OTP</title></head>"
            + "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,Helvetica,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f9;padding:40px 0;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628 0%,#0f2137 50%,#152d47 100%);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px 40px 20px 40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px 0;'>Dear <strong>" + sanitize(userName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 28px 0;'>We have received a request to reset the password associated with your ReconXpert.Ai account. To proceed, please use the One-Time Password (OTP) provided below to verify your identity.</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:8px 0 28px 0;'>"
            + "<div style='background:#f8fafc;border:2px dashed #f59e0b;border-radius:12px;padding:24px 32px;display:inline-block;text-align:center;'>"
            + "<p style='margin:0 0 8px 0;font-size:12px;color:#94a3b8;text-transform:uppercase;letter-spacing:2px;'>Your One-Time Password</p>"
            + "<p style='margin:0;font-size:40px;font-weight:bold;color:#0a1628;letter-spacing:10px;'>" + sanitize(otpCode) + "</p>"
            + "<p style='margin:8px 0 0 0;font-size:12px;color:#ef4444;'>Valid for " + expiryMins + " minutes only</p>"
            + "</div></td></tr></table>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px 0;'>To reset your password:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 24px 0;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter this OTP on the password reset page</li>"
            + "<li>Set your new password (min 8 chars, 1 uppercase, 1 number, 1 special character)</li>"
            + "<li>Log in with your new credentials</li>"
            + "</ol>"
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:6px;padding:12px 16px;margin-bottom:24px;'>"
            + "<p style='margin:0;font-size:12px;color:#991b1b;'><strong>Security Notice:</strong> For your security, never share your OTP with anyone. KalInfotech will never ask you to disclose your OTP through email, phone, or any other communication channel. If you did not request a password reset, please disregard this email. Your account will remain secure.</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Bank Admin Welcome Email
    // Contains: Bank Code, User ID, Default Password, Verify button
    // ─────────────────────────────────────────────────────────────────────
    private String buildSuperUserWelcomeHtml(String name, String bankName,
                                              String bankCode, String superUserId,
                                              String defaultPassword, String verifyLink) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"

            // Greeting
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "Welcome to <strong>ReconXpert.Ai</strong>. "
            + "You have been successfully appointed as the <strong>Bank Administrator</strong> for "
            + "<strong>" + sanitize(bankName) + "</strong> by the KalInfotech Administration. "
            + "Please use the credentials below to activate your account."
            + "</p>"

            // Credentials box — Bank Code, User ID, Default Password
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>Bank Code:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(bankCode) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>User ID:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(superUserId) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"

            // Verify button
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Kindly click the button below to verify your email and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#0a1628,#152d47);color:#f59e0b;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Email &amp; Set Password</a>"
            + "</td></tr></table>"

            // Steps
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the login page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>Bank Code:</strong> " + sanitize(bankCode) + "</li>"
            + "<li>Enter your <strong>User ID:</strong> " + sanitize(superUserId) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"

            // Warning box
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete email verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This link expires in 48 hours. Do not share it with anyone.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND STATUS CHANGE NOTIFICATION EMAIL
    // Sent when Admin changes bank status: INACTIVE / BLOCKED / ACTIVE
    // @Async — fire and forget, won't block API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendStatusChangeNotification(String toEmail, String superUserName,
                                              String bankName, String bankCode,
                                              String oldStatus, String newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Account Status Update for " + bankName);
            helper.setText(buildStatusChangeHtml(superUserName, bankName, bankCode, oldStatus, newStatus), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Status change — recipient: {} | bank: {} | {} → {}", toEmail, bankCode, oldStatus, newStatus);
        } catch (MessagingException e) {
            // @Async — status update must NOT be blocked by email failure
            logger.error("[EMAIL-DELIVERY-FAIL] Status change — recipient: {} | bank: {} | {} → {} | reason: {}", toEmail, bankCode, oldStatus, newStatus, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Status change — unexpected error — recipient: {} | bank: {} | reason: {}", toEmail, bankCode, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Status Change Notification Email
    // ─────────────────────────────────────────────────────────────────────
    private String buildStatusChangeHtml(String name, String bankName,
                                          String bankCode,
                                          String oldStatus, String newStatus) {

        // Color + icon per status
        String statusColor, statusBg, statusIcon, statusMessage;
        switch (newStatus.toUpperCase()) {
            case CommonConstants.INACTIVE:
                statusColor  = "#6366f1";
                statusBg     = "rgba(99,102,241,0.1)";
                statusIcon   = "⏸";
                statusMessage = "Your account has been marked <strong>Inactive</strong>. "
                    + "You will not be able to access the platform until it is reactivated. "
                    + "Please contact KalInfotech Admin for assistance.";
                break;
            case "BLOCKED":
                statusColor  = "#ef4444";
                statusBg     = "rgba(239,68,68,0.1)";
                statusIcon   = "🚫";
                statusMessage = "Your account has been <strong>Blocked</strong> by KalInfotech Admin. "
                    + "Access to the ReconXpert.Ai platform has been permanently restricted. "
                    + "Please contact KalInfotech Admin immediately for clarification.";
                break;
            case CommonConstants.ACTIVE:
                statusColor  = "#22c55e";
                statusBg     = "rgba(34,197,94,0.1)";
                statusIcon   = "✅";
                statusMessage = "Your account has been <strong>Activated</strong>. "
                    + "You can now access the ReconXpert.Ai platform using your credentials.";
                break;
            default:
                statusColor  = "#f59e0b";
                statusBg     = "rgba(212,168,67,0.1)";
                statusIcon   = "ℹ";
                statusMessage = "Your bank status has been updated to <strong>" + sanitize(newStatus) + "</strong>.";
        }

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Status banner
            + "<tr><td style='background:" + statusBg + ";border-bottom:3px solid " + statusColor + ";padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:32px;'>" + statusIcon + "</p>"
            + "<p style='margin:8px 0 0;font-size:18px;font-weight:700;color:" + statusColor + ";'>Status Changed: " + sanitize(oldStatus) + " → " + sanitize(newStatus) + "</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;line-height:1.7;'>" + statusMessage + "</p>"

            // Bank details box
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + statusColor + ";border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 12px;font-size:13px;color:#475569;font-weight:700;text-transform:uppercase;letter-spacing:1px;'>Bank Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:160px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:600;padding:4px 0;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Previous Status</td>"
            + "<td style='font-size:13px;color:#64748b;padding:4px 0;'>" + sanitize(oldStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>New Status</td>"
            + "<td style='font-size:13px;font-weight:700;padding:4px 0;color:" + statusColor + ";'>" + sanitize(newStatus) + "</td></tr>"
            + "</table></div>"

            // Contact note
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Note:</strong> This is an automated notification from KalInfotech Admin. "
            + "If you have any questions, please contact us at <a href='mailto:support@kalinfotech.com' style='color:#f59e0b;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SUB-INSTITUTE CASCADE STATUS NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBranchBankStatusNotification(String toEmail, String contactName,
                                                   String branchBankName, String branchBankCode,
                                                   String oldStatus, String newStatus,
                                                   String parentBankName, String parentBankCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Branch Institution Status Update – " + branchBankName);
            helper.setText(buildSubInstituteStatusHtml(
                    contactName, branchBankName, branchBankCode,
                    oldStatus, newStatus, parentBankName, parentBankCode), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Sub-bnkitute cascade — recipient: {} | branch: {} | {} → {} | parent: {}",
                    toEmail, branchBankCode, oldStatus, newStatus, parentBankCode);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub-bnkitute cascade — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub-bnkitute cascade — unexpected — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        }
    }

    private String buildSubInstituteStatusHtml(String contactName,
                                                String branchBankName, String branchBankCode,
                                                String oldStatus, String newStatus,
                                                String parentBankName, String parentBankCode) {

        String statusColor, statusBg, statusIcon, statusHeading, statusMessage, actionNote;

        switch (newStatus.toUpperCase()) {
            case "BLOCKED":
                statusColor   = "#ef4444";
                statusBg      = "rgba(239,68,68,0.08)";
                statusIcon    = "🚫";
                statusHeading = "Platform Access Suspended";
                statusMessage = "We wish to inform you that your bank's access to the "
                    + "<strong>ReconXpert.Ai</strong> reconciliation platform has been <strong>suspended</strong> "
                    + "with immediate effect, in accordance with a compliance directive issued by "
                    + "<strong>KalInfotech Administration</strong> applicable to <strong>"
                    + sanitize(parentBankName) + "</strong> and all associated banks. "
                    + "During this period, platform login and all reconciliation operations will be unavailable.";
                actionNote    = "To understand the reason for this action or to request rebnkatement, "
                    + "please reach out to your designated KalInfotech Relationship Manager or write to us at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#f59e0b;'>support@kalinfotech.com</a>. "
                    + "Please quote your Bank Code when contacting support.";
                break;
            case CommonConstants.ACTIVE:
                statusColor   = "#22c55e";
                statusBg      = "rgba(34,197,94,0.08)";
                statusIcon    = "✅";
                statusHeading = "Platform Access Rebnkated";
                statusMessage = "We are pleased to inform you that your bank's access to the "
                    + "<strong>ReconXpert.Ai</strong> reconciliation platform has been <strong>rebnkated</strong>. "
                    + "This follows the reactivation of <strong>" + sanitize(parentBankName) + "</strong> "
                    + "by KalInfotech Administration. You may now resume normal platform operations "
                    + "using your existing Bank Admin credentials.";
                actionNote    = "If you experience any difficulty accessing the platform or require assistance, "
                    + "please contact our support team at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#f59e0b;'>support@kalinfotech.com</a>.";
                break;
            default:
                statusColor   = "#f59e0b";
                statusBg      = "rgba(212,168,67,0.08)";
                statusIcon    = "ℹ";
                statusHeading = "Bank Status Update";
                statusMessage = "Your bank's status on the ReconXpert.Ai platform has been updated "
                    + "to <strong>" + sanitize(newStatus) + "</strong> by KalInfotech Administration.";
                actionNote    = "For queries, contact us at "
                    + "<a href='mailto:support@kalinfotech.com' style='color:#f59e0b;'>support@kalinfotech.com</a>.";
        }

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Status banner
            + "<tr><td style='background:" + statusBg + ";border-bottom:3px solid " + statusColor + ";padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:36px;'>" + statusIcon + "</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:" + statusColor + ";'>" + statusHeading + "</p>"
            + "<p style='margin:0;font-size:13px;color:#64748b;'>"
            + sanitize(oldStatus) + " &nbsp;&#8594;&nbsp; <strong style='color:" + statusColor + ";'>" + sanitize(newStatus) + "</strong>"
            + "</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(contactName) + "</strong>,"
            + "<span style='font-size:12px;color:#94a3b8;font-weight:400;'> &nbsp;|&nbsp; Bank Admin, " + sanitize(branchBankName) + "</span></p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 28px;line-height:1.8;'>" + statusMessage + "</p>"

            // Bank details card
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + statusColor + ";border-radius:8px;padding:20px 24px;margin-bottom:16px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#94a3b8;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Your Bank Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;width:170px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(branchBankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(branchBankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Previous Status</td>"
            + "<td style='font-size:13px;color:#64748b;padding:5px 0;'>" + sanitize(oldStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Current Status</td>"
            + "<td style='font-size:13px;font-weight:700;padding:5px 0;color:" + statusColor + ";'>" + sanitize(newStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:5px 0;'>Effective</td>"
            + "<td style='font-size:13px;color:#1e293b;padding:5px 0;'>Immediately</td></tr>"
            + "</table></div>"

            // Issued by / reference
            + "<div style='background:#f0f4ff;border:1px solid #c7d2fe;border-radius:8px;padding:16px 20px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 10px;font-size:11px;color:#4338ca;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Issued By</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:170px;'>Issuing Authority</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>KalInfotech Administration</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Network Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(parentBankName)
            + " <span style='font-family:monospace;color:#6366f1;font-size:12px;'>(" + sanitize(parentBankCode) + ")</span></td></tr>"
            + "</table>"
            + "</div>"

            // Action note
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:14px 18px;'>"
            + "<p style='margin:0 0 4px;font-size:12px;color:#92400e;font-weight:700;'>Action Required</p>"
            + "<p style='margin:0;font-size:13px;color:#92400e;line-height:1.7;'>" + actionNote + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px 40px;'>"
            + "<table width='100%'><tr>"
            + "<td style='font-size:12px;color:#94a3b8;'>ReconXpert.Ai &nbsp;|&nbsp; KalInfotech</td>"
            + "<td style='font-size:12px;color:#94a3b8;text-align:right;'>This is a system-generated notification. Do not reply.</td>"
            + "</tr></table>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK WARNING — Bank Admin
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBlockWarning(String toEmail, String superUserName,
                                 String bankName, String bankCode,
                                 String blockAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Important Notice: Scheduled Account Block for " + bankName);
            helper.setText(buildBlockWarningHtml(superUserName, bankName, bankCode, blockAt), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Block warning — recipient: {} | bank: {} | blockAt: {}",
                    toEmail, bankCode, blockAt);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block warning — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block warning — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK WARNING — Sub-Institute Primary Contact
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBranchBankBlockWarning(String toEmail, String contactName,
                                             String branchBankName, String branchBankCode,
                                             String parentBankName, String parentBankCode,
                                             String blockAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Important Notice: Scheduled Account Block for " + branchBankName);
            helper.setText(buildSubBlockWarningHtml(contactName, branchBankName, branchBankCode,
                    parentBankName, parentBankCode, blockAt), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Sub block warning — recipient: {} | branch: {} | parent: {} | blockAt: {}",
                    toEmail, branchBankCode, parentBankCode, blockAt);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block warning — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block warning — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK CANCELLED — Bank Admin
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBlockCancelled(String toEmail, String superUserName,
                                   String bankName, String bankCode,
                                   String restoredStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Scheduled Account Block Cancelled – " + bankName);
            helper.setText(buildBlockCancelledHtml(superUserName, bankName, bankCode, restoredStatus), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Block cancelled — recipient: {} | bank: {}", toEmail, bankCode);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block cancelled — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Block cancelled — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK CANCELLED — Sub-Institute Primary Contact
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBranchBankBlockCancelled(String toEmail, String contactName,
                                               String branchBankName, String branchBankCode,
                                               String parentBankName, String parentBankCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Scheduled Account Block Cancelled – " + branchBankName);
            helper.setText(buildSubBlockCancelledHtml(contactName, branchBankName, branchBankCode,
                    parentBankName, parentBankCode), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Sub block cancelled — recipient: {} | branch: {} | parent: {}",
                    toEmail, branchBankCode, parentBankCode);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block cancelled — recipient: {} | branch: {} | reason: {}",
                    toEmail, branchBankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Sub block cancelled — unexpected — recipient: {} | reason: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Block Cancelled (Bank)
    // ─────────────────────────────────────────────────────────────────────
    private String buildBlockCancelledHtml(String name, String bankName,
                                             String bankCode, String restoredStatus) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Success banner
            + "<tr><td style='background:rgba(34,197,94,0.1);border-bottom:3px solid #22c55e;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>✅</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#15803d;'>Block Successfully Cancelled</p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'>Your account has been restored to <strong>" + sanitize(restoredStatus) + "</strong> status</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "We are pleased to inform you that the scheduled block of your bank's account on "
            + "<strong>ReconXpert.Ai</strong> has been <strong>successfully cancelled</strong> by KalInfotech Administration. "
            + "Your account is now fully restored and you may continue using the platform as normal."
            + "</p>"

            // Details box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #22c55e;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#166534;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Account Restoration Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#4ade80;padding:5px 0;width:170px;color:#166534;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Restored Status</td>"
            + "<td style='font-size:13px;font-weight:700;color:#15803d;padding:5px 0;'>" + sanitize(restoredStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Action By</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>KalInfotech Administration</td></tr>"
            + "</table></div>"

            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Note:</strong> No further action is required from your side. "
            + "If you have any questions, please contact us at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#f59e0b;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Block Cancelled (Sub-Institute)
    // ─────────────────────────────────────────────────────────────────────
    private String buildSubBlockCancelledHtml(String contactName,
                                                String branchBankName, String branchBankCode,
                                                String parentBankName, String parentBankCode) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Success banner
            + "<tr><td style='background:rgba(34,197,94,0.1);border-bottom:3px solid #22c55e;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>✅</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#15803d;'>Block Successfully Cancelled</p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'>Your account block has been called off — no action required</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(contactName) + "</strong>,"
            + "<span style='font-size:12px;color:#94a3b8;font-weight:400;'> &nbsp;|&nbsp; Bank Admin, " + sanitize(branchBankName) + "</span></p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "We are pleased to inform you that the scheduled block of your bank on "
            + "<strong>ReconXpert.Ai</strong> has been <strong>successfully cancelled</strong>. "
            + "This follows the cancellation of the block of your parent bank, "
            + "<strong>" + sanitize(parentBankName) + "</strong>, by KalInfotech Administration. "
            + "Your platform access and all reconciliation services remain fully active."
            + "</p>"

            // Details box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #22c55e;border-radius:8px;padding:20px 24px;margin-bottom:16px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#166534;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Your Bank Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;width:170px;'>Your Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(branchBankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Your Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(branchBankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#166534;padding:5px 0;'>Network Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(parentBankName)
            + " <span style='font-family:monospace;color:#6366f1;font-size:12px;'>(" + sanitize(parentBankCode) + ")</span></td></tr>"
            + "</table></div>"

            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>No action required.</strong> You may continue using the platform normally. "
            + "For any queries, contact us at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#f59e0b;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px 40px;'>"
            + "<table width='100%'><tr>"
            + "<td style='font-size:12px;color:#94a3b8;'>ReconXpert.Ai &nbsp;|&nbsp; KalInfotech</td>"
            + "<td style='font-size:12px;color:#94a3b8;text-align:right;'>This is a system-generated notification. Do not reply.</td>"
            + "</tr></table>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Bank Block Warning
    // ─────────────────────────────────────────────────────────────────────
    private String buildBlockWarningHtml(String name, String bankName,
                                         String bankCode, String blockAt) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Warning banner
            + "<tr><td style='background:rgba(239,68,68,0.08);border-bottom:3px solid #ef4444;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>🚫</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#991b1b;'>Account Block Scheduled</p>"
            + "<p style='margin:0;font-size:13px;color:#7f1d1d;'>Your account will be permanently blocked on <strong>" + sanitize(blockAt) + "</strong></p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "This is an urgent notice that your bank's account on <strong>ReconXpert.Ai</strong> has been "
            + "<strong>scheduled for permanent blocking</strong> by KalInfotech Administration. "
            + "Once blocked, all platform access and reconciliation services will be <strong>permanently suspended</strong> "
            + "and cannot be reversed."
            + "</p>"

            // Details box
            + "<div style='background:#fef2f2;border:1px solid #fecaca;border-left:4px solid #ef4444;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#991b1b;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Block Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;width:170px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Scheduled By</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>KalInfotech Administration</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Block Time</td>"
            + "<td style='font-size:13px;font-weight:700;color:#dc2626;padding:5px 0;'>" + sanitize(blockAt) + "</td></tr>"
            + "</table></div>"

            // Urgent action
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:6px;padding:16px 18px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 6px;font-size:13px;color:#991b1b;font-weight:700;'>⚡ Immediate Action Required</p>"
            + "<p style='margin:0;font-size:13px;color:#991b1b;line-height:1.7;'>"
            + "If you believe this is an error or wish to cancel the block, please contact KalInfotech Administration "
            + "<strong>immediately</strong> at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#dc2626;font-weight:600;'>support@kalinfotech.com</a>. "
            + "This action can only be cancelled before the scheduled block time."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Sub-Institute Block Warning
    // ─────────────────────────────────────────────────────────────────────
    private String buildSubBlockWarningHtml(String contactName,
                                            String branchBankName, String branchBankCode,
                                            String parentBankName, String parentBankCode,
                                            String blockAt) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Warning banner
            + "<tr><td style='background:rgba(239,68,68,0.08);border-bottom:3px solid #ef4444;padding:24px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:40px;'>🚫</p>"
            + "<p style='margin:10px 0 4px;font-size:20px;font-weight:700;color:#991b1b;'>Account Block Scheduled</p>"
            + "<p style='margin:0;font-size:13px;color:#7f1d1d;'>Your account will be permanently blocked on <strong>" + sanitize(blockAt) + "</strong></p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(contactName) + "</strong>,"
            + "<span style='font-size:12px;color:#94a3b8;font-weight:400;'> &nbsp;|&nbsp; Bank Admin, " + sanitize(branchBankName) + "</span></p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "This is an urgent notice that your bank's account on <strong>ReconXpert.Ai</strong> has been "
            + "<strong>scheduled for permanent blocking</strong>. This action has been triggered by the blocking "
            + "of your parent bank, <strong>" + sanitize(parentBankName) + "</strong>, by KalInfotech Administration."
            + "</p>"

            // Details box
            + "<div style='background:#fef2f2;border:1px solid #fecaca;border-left:4px solid #ef4444;border-radius:8px;padding:20px 24px;margin-bottom:16px;'>"
            + "<p style='margin:0 0 14px;font-size:11px;color:#991b1b;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Block Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;width:170px;'>Your Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:5px 0;'>" + sanitize(branchBankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Your Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:5px 0;letter-spacing:1px;'>" + sanitize(branchBankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#78716c;padding:5px 0;'>Block Time</td>"
            + "<td style='font-size:13px;font-weight:700;color:#dc2626;padding:5px 0;'>" + sanitize(blockAt) + "</td></tr>"
            + "</table></div>"

            // Issued by
            + "<div style='background:#f0f4ff;border:1px solid #c7d2fe;border-radius:8px;padding:16px 20px;margin-bottom:20px;'>"
            + "<p style='margin:0 0 10px;font-size:11px;color:#4338ca;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;'>Issued By</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:170px;'>Issuing Authority</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>KalInfotech Administration</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Network Bank</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(parentBankName)
            + " <span style='font-family:monospace;color:#6366f1;font-size:12px;'>(" + sanitize(parentBankCode) + ")</span></td></tr>"
            + "</table></div>"

            // Urgent action
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:6px;padding:16px 18px;'>"
            + "<p style='margin:0 0 6px;font-size:13px;color:#991b1b;font-weight:700;'>⚡ Immediate Action Required</p>"
            + "<p style='margin:0;font-size:13px;color:#991b1b;line-height:1.7;'>"
            + "If you wish to raise an objection or seek clarification, please contact KalInfotech Administration "
            + "<strong>before</strong> the scheduled block time at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#dc2626;font-weight:600;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px 40px;'>"
            + "<table width='100%'><tr>"
            + "<td style='font-size:12px;color:#94a3b8;'>ReconXpert.Ai &nbsp;|&nbsp; KalInfotech</td>"
            + "<td style='font-size:12px;color:#94a3b8;text-align:right;'>This is a system-generated notification. Do not reply.</td>"
            + "</tr></table>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // BANK PROFILE UPDATE NOTIFICATION
    // Sent to primary contact whenever Admin updates the bankprofile.
    // changesBySections maps section name → list of "Field: old → new" strings.
    // Only called when the map is non-empty (i.e. something actually changed).
    // @Async — fire and forget, update must NOT be blocked by email failure
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendBankUpdateNotification(String toEmail, String contactName,
                                                   String bankName, String bankCode,
                                                   String updatedAt,
                                                   Map<String, List<String>> changesBySections) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Bank Profile Update Notification – " + bankName);
            helper.setText(buildBankUpdateHtml(contactName, bankName, bankCode,
                    updatedAt, changesBySections), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Bank update notification — recipient: {} | bank: {} | sections: {}",
                    toEmail, bankCode, changesBySections.keySet());
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Bank update — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Bank update — unexpected — recipient: {} | bank: {} | reason: {}",
                    toEmail, bankCode, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Bank Profile Update Notification
    // Renders only the sections that actually changed, with before → after values
    // ─────────────────────────────────────────────────────────────────────
    private String buildBankUpdateHtml(String contactName, String bankName,
                                               String bankCode, String updatedAt,
                                               Map<String, List<String>> changesBySections) {

        // Build the change-details block: one coloured card per section
        StringBuilder changesHtml = new StringBuilder();
        String[] sectionColors = { "#0d9488", "#6366f1", "#f59e0b", "#ef4444" };
        String[] sectionBgs    = { "rgba(13,148,136,0.07)", "rgba(99,102,241,0.07)",
                                    "rgba(245,158,11,0.07)", "rgba(239,68,68,0.07)" };
        int colorIdx = 0;
        for (Map.Entry<String, List<String>> section : changesBySections.entrySet()) {
            String color = sectionColors[colorIdx % sectionColors.length];
            String bg    = sectionBgs[colorIdx % sectionBgs.length];
            colorIdx++;

            changesHtml.append("<div style='background:").append(bg)
                .append(";border:1px solid ").append(color)
                .append("33;border-left:4px solid ").append(color)
                .append(";border-radius:8px;padding:16px 20px;margin-bottom:14px;'>")
                .append("<p style='margin:0 0 10px;font-size:12px;color:").append(color)
                .append(";font-weight:700;text-transform:uppercase;letter-spacing:1px;'>")
                .append(sanitize(section.getKey())).append("</p>");

            for (String change : section.getValue()) {
                // Split at " → " to bold the two sides
                String sanitized = sanitize(change);
                String formatted = sanitized.replace(" &#x2192; ", "</span>"
                        + "<span style='color:#94a3b8;'> &#x2192; </span>"
                        + "<span style='color:#1e293b;font-weight:600;'>");
                // If it was a removal/addition line (starts with Added/Removed) give it a badge
                String badge = "";
                if (change.startsWith("Added:")) {
                    badge = "<span style='background:#dcfce7;color:#166534;font-size:10px;font-weight:700;"
                          + "padding:2px 7px;border-radius:20px;margin-right:8px;'>NEW</span>";
                } else if (change.startsWith("Removed:")) {
                    badge = "<span style='background:#fee2e2;color:#991b1b;font-size:10px;font-weight:700;"
                          + "padding:2px 7px;border-radius:20px;margin-right:8px;'>REMOVED</span>";
                }
                changesHtml.append("<p style='margin:0 0 6px;font-size:13px;color:#475569;'>")
                    .append(badge)
                    .append("<span style='color:#475569;'>").append(formatted).append("</span>")
                    .append("</p>");
            }
            changesHtml.append("</div>");
        }

        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Info banner
            + "<tr><td style='background:rgba(13,148,136,0.08);border-bottom:3px solid #0d9488;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:32px;'>🔔</p>"
            + "<p style='margin:8px 0 0;font-size:18px;font-weight:700;color:#0f766e;'>Bank Profile Updated</p>"
            + "<p style='margin:6px 0 0;font-size:13px;color:#0d9488;'>The following changes have been applied to your account</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 24px;line-height:1.8;'>"
            + "We wish to inform you that the profile of <strong>" + sanitize(bankName) + "</strong> "
            + "on the <strong>ReconXpert.Ai</strong> platform has been updated by KalInfotech Administration. "
            + "The specific changes are detailed below."
            + "</p>"

            // Bank meta card
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid #0d9488;border-radius:8px;padding:16px 20px;margin-bottom:24px;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:140px;'>Bank Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(bankName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Bank Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:700;padding:4px 0;letter-spacing:1px;'>" + sanitize(bankCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Updated On</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(updatedAt) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Updated By</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>KalInfotech Administration</td></tr>"
            + "</table></div>"

            // Change sections (dynamic)
            + "<p style='font-size:13px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:1px;margin:0 0 12px;'>Changes Made</p>"
            + changesHtml.toString()

            // Security note
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:14px 18px;margin-top:10px;'>"
            + "<p style='margin:0 0 4px;font-size:12px;color:#92400e;font-weight:700;'>Important Notice</p>"
            + "<p style='margin:0;font-size:13px;color:#92400e;line-height:1.7;'>"
            + "If you were not informed of this update or believe it was made in error, "
            + "please contact KalInfotech Administration immediately at "
            + "<a href='mailto:support@kalinfotech.com' style='color:#f59e0b;font-weight:600;'>support@kalinfotech.com</a> "
            + "and quote your Bank Code: <strong>" + sanitize(bankCode) + "</strong>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND INACTIVATE PENDING WARNING
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendInactivatePendingWarning(String toEmail, String contactName,
                                              String entityName, String entityCode,
                                              String inactivateAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Scheduled Account Inactivation – " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    CommonConstants.ACTIVE, "INACTIVE_PENDING",
                    "Your account has been scheduled for inactivation. Your account will automatically become <strong>Inactive</strong> on <strong>"
                    + sanitize(inactivateAt) + "</strong>. If this change was not authorized or requires review, please contact your administrator before the scheduled inactivation time.",
                    "#6366f1", "⏸");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Inactivate-pending warning sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Inactivate-pending warning — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND INACTIVATE CANCELLED
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendInactivateCancelled(String toEmail, String contactName,
                                         String entityName, String entityCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Scheduled Account Inactivation Cancelled – " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "INACTIVE_PENDING", CommonConstants.ACTIVE,
                    "We are pleased to inform you that the scheduled inactivation of your account has been cancelled by the administrator. "
                    + "Your account has been restored to <strong>Active</strong> status, and no further action is required.",
                    "#22c55e", "✅");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Inactivate-cancelled sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Inactivate-cancelled — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND INACTIVATED NOTIFICATION (auto-process fired)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendInactivatedNotification(String toEmail, String contactName,
                                             String entityName, String entityCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Account Successfully Inactivated – " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "INACTIVE_PENDING", CommonConstants.INACTIVE,
                    "This is to notify you that your account has been successfully changed to <strong>Inactive</strong> status. "
                    + "As a result, access to the ReconXpert.Ai platform has been suspended. "
                    + "If you require your account to be reactivated, please contact your administrator.",
                    "#6366f1", "⏸");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Inactivated notification sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Inactivated notification — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REACTIVATE PENDING NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReactivatePendingNotification(String toEmail, String contactName,
                                                   String entityName, String entityCode,
                                                   String reactivateAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Scheduled Account Reactivation – " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    CommonConstants.INACTIVE, "ACTIVE_PENDING",
                    "We are pleased to inform you that your account has been scheduled for reactivation. "
                    + "Your account status will automatically change to <strong>Active</strong> on <strong>"
                    + sanitize(reactivateAt) + "</strong>. No action is required from your side.",
                    "#22c55e", "🔄");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Reactivate-pending sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Reactivate-pending — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REACTIVATE CANCELLED
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReactivateCancelled(String toEmail, String contactName,
                                         String entityName, String entityCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Scheduled Account Reactivation Cancelled – " + entityName);
            String body = buildStatusPendingHtml(contactName, entityName, entityCode,
                    "ACTIVE_PENDING", CommonConstants.INACTIVE,
                    "This is to inform you that the scheduled reactivation of your account has been cancelled by the administrator. "
                    + "Your account will remain <strong>Inactive</strong> until further notice. "
                    + "If you believe this action was made in error or require assistance, please contact your administrator.",
                    "#6366f1", "⏸");
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Reactivate-cancelled sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Reactivate-cancelled — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REACTIVATED NOTIFICATION (auto-process fired)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReactivatedNotification(String toEmail, String contactName,
                                             String entityName, String entityCode,
                                             String username, String loginUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Your Account Has Been Reactivated");
            String safe     = sanitize(contactName);
            String safeOrg  = sanitize(entityName);
            String safeCode = sanitize(entityCode);
            String safeUser = sanitize(username);
            String safeUrl  = sanitize(loginUrl);
            String body = "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
                + "<tr><td align='center'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

                // Header
                + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
                + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
                + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
                + "</td></tr>"

                // Body
                + "<tr><td style='padding:40px;'>"

                // Greeting
                + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + safe + "</strong>,</p>"
                + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
                + "We are pleased to inform you that your ReconXpert.Ai account has been successfully reactivated. "
                + "You may now access the platform using your existing login credentials."
                + "</p>"

                // Credentials box — green like onboarding
                + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
                + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>✅ Your Login Credentials</p>"
                + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>ID / Code:</strong> "
                + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + safeCode + "</span></p>"
                + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>Username:</strong> "
                + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + safeUser + "</span></p>"
                + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Password:</strong> "
                + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;color:#94a3b8;'>--</span>"
                + "<span style='font-size:11px;color:#64748b;margin-left:8px;'>(use your existing password)</span></p>"
                + "</div>"

                // Login button
                + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
                + "<a href='" + safeUrl + "' style='background:linear-gradient(135deg,#0a1628,#152d47);color:#f59e0b;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Login to ReconXpert.Ai</a>"
                + "</td></tr></table>"

                // Steps note
                + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the button, on the login page:</p>"
                + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
                + "<li>Your <strong>ID / Code</strong> and <strong>Username</strong> will be pre-filled</li>"
                + "<li>Enter your <strong>existing password</strong> to continue</li>"
                + "</ol>"

                // Warning box
                + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
                + "<p style='margin:0;font-size:12px;color:#92400e;'>"
                + "<strong>Important:</strong> Please do not share your credentials with anyone. "
                + "Contact your administrator if you need assistance."
                + "</p>"
                + "</div>"

                + "</td></tr>"

                // Footer
                + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
                + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai.</p>"
                + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Reactivated notification sent to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Reactivated notification — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML TEMPLATE — Generic pending/transition email
    // ─────────────────────────────────────────────────────────────────────
    private String buildStatusPendingHtml(String contactName, String entityName, String entityCode,
                                           String oldStatus, String newStatus, String message,
                                           String accentColor, String icon) {
        String statusBg;
        switch (newStatus != null ? newStatus.toUpperCase() : "") {
            case "INACTIVE_PENDING": statusBg = "rgba(99,102,241,0.1)";  break;
            case CommonConstants.INACTIVE:         statusBg = "rgba(99,102,241,0.1)";  break;
            case "ACTIVE_PENDING":   statusBg = "rgba(34,197,94,0.1)";   break;
            case CommonConstants.ACTIVE:           statusBg = "rgba(34,197,94,0.1)";   break;
            case "BLOCK_PENDING":    statusBg = "rgba(239,68,68,0.08)";  break;
            case "BLOCKED":          statusBg = "rgba(239,68,68,0.08)";  break;
            default:                 statusBg = "rgba(212,168,67,0.1)";  break;
        }
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Status banner
            + "<tr><td style='background:" + statusBg + ";border-bottom:3px solid " + accentColor + ";padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:32px;'>" + icon + "</p>"
            + "<p style='margin:8px 0 0;font-size:18px;font-weight:700;color:" + accentColor + ";'>Status Changed: " + sanitize(oldStatus) + " → " + sanitize(newStatus) + "</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;line-height:1.7;'>" + message + "</p>"

            // Account details table
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + accentColor + ";border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 12px;font-size:13px;color:#475569;font-weight:700;text-transform:uppercase;letter-spacing:1px;'>Account Details</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:160px;'>Name</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:600;padding:4px 0;'>" + sanitize(entityName) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Code</td>"
            + "<td style='font-size:13px;color:#1e293b;font-family:monospace;font-weight:600;padding:4px 0;'>" + sanitize(entityCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Previous Status</td>"
            + "<td style='font-size:13px;color:#64748b;padding:4px 0;'>" + sanitize(oldStatus) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>New Status</td>"
            + "<td style='font-size:13px;font-weight:700;padding:4px 0;color:" + accentColor + ";'>" + sanitize(newStatus) + "</td></tr>"
            + "</table></div>"

            // Note box
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Note:</strong> This is an automated notification from KalInfotech Admin. "
            + "If you have any questions, please contact us at <a href='mailto:support@kalinfotech.com' style='color:#f59e0b;'>support@kalinfotech.com</a>."
            + "</p></div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REPLACEMENT OUTGOING NOTIFICATION (to old admin/user)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReplacementOutgoingNotification(String toEmail, String contactName,
                                                     String entityCode, String replacedBy, String reason,
                                                     String replacementFullName, String replacementEmail,
                                                     String orderedBy) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Account Replacement Notification – " + sanitize(entityCode));
            helper.setText(buildReplacementOutgoingHtml(contactName, entityCode, replacedBy, reason,
                    replacementFullName, replacementEmail, orderedBy), true);
            mailSender.send(message);
            logger.info("Replacement outgoing email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement outgoing — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND REPLACEMENT WELCOME (to new admin/user with credentials)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReplacementWelcome(String toEmail, String contactName,
                                        String entityCode, String username, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Welcome! Your Replacement Account Is Ready");
            helper.setText(buildReplacementWelcomeHtml(contactName, entityCode, username, tempPassword), true);
            mailSender.send(message);
            logger.info("Replacement welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement welcome — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML — Replacement Outgoing (old admin/user)
    // ─────────────────────────────────────────────────────────────────────
    private String buildReplacementOutgoingHtml(String contactName, String entityCode,
                                                 String replacedBy, String reason,
                                                 String replacementFullName, String replacementEmail,
                                                 String orderedBy) {
        String safeReason   = (reason != null && !reason.trim().isEmpty()) ? sanitize(reason) : "No reason provided";
        String repName      = (replacementFullName != null && !replacementFullName.trim().isEmpty()) ? sanitize(replacementFullName) : "—";
        String repEmail     = (replacementEmail != null && !replacementEmail.trim().isEmpty()) ? sanitize(replacementEmail) : "—";
        String safeOrdered  = (orderedBy != null && !orderedBy.trim().isEmpty()) ? sanitize(orderedBy) : "—";
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>This email is to inform you that your responsibilities on ReconXpert.Ai have been officially transferred to another user. As part of this transition, your account has been changed to <strong>Inactive</strong> status.</p>"
            + "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:8px;padding:20px 24px;margin:0 0 24px;'>"
            + "<p style='margin:0 0 10px;font-size:15px;font-weight:bold;color:#b91c1c;'>&#x26A0; Account Replaced</p>"
            + "<table cellpadding='0' cellspacing='0' width='100%'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;width:160px;'>Entity Code:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + sanitize(entityCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Actioned By:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + sanitize(replacedBy) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Ordered By:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + safeOrdered + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Replacement Name:</td><td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + repName + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Replacement Email:</td><td style='font-size:13px;color:#1e293b;'>" + repEmail + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:4px 0;'>Reason:</td><td style='font-size:13px;color:#1e293b;'>" + safeReason + "</td></tr>"
            + "</table></div>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 16px;'>If you believe this is an error, please contact your KalInfotech administrator immediately.</p>"
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'><strong>Note:</strong> This is an automated notification. Do not reply to this email.</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML — Replacement Welcome (new admin/user with credentials)
    // ─────────────────────────────────────────────────────────────────────
    private String buildReplacementWelcomeHtml(String contactName, String entityCode,
                                                String username, String tempPassword) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>Welcome to ReconXpert.Ai. You have been assigned as a replacement user, and your account has been successfully activated. Please use the credentials below to access your account.</p>"
            + "<div style='background:#f0fdf4;border-left:4px solid #16a34a;border-radius:8px;padding:24px;margin:0 0 24px;'>"
            + "<p style='margin:0 0 16px;font-size:15px;font-weight:bold;color:#15803d;'>&#x2705; Your Login Credentials</p>"
            + "<table cellpadding='0' cellspacing='0' width='100%'>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:6px 0;width:140px;'>Entity Code:</td>"
            + "<td style='font-size:13px;color:#1e293b;font-weight:bold;'>" + sanitize(entityCode) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:6px 0;'>Username:</td>"
            + "<td style='font-size:14px;color:#1e293b;font-weight:bold;letter-spacing:1px;'>" + sanitize(username) + "</td></tr>"
            + "<tr><td style='font-size:13px;color:#64748b;padding:6px 0;'>Temporary Password:</td>"
            + "<td><span style='background:#0a1628;color:#f59e0b;font-size:15px;font-weight:bold;letter-spacing:2px;padding:4px 12px;border-radius:6px;display:inline-block;'>" + sanitize(tempPassword) + "</span></td></tr>"
            + "</table></div>"
            + "<div style='background:#eff6ff;border-left:4px solid #3b82f6;border-radius:6px;padding:12px 16px;margin:0 0 16px;'>"
            + "<p style='margin:0;font-size:13px;color:#1e40af;'><strong>&#128274; Security:</strong> For security reasons, we strongly recommend changing your password immediately after your first login using the <strong>Forgot Password</strong> option.</p>"
            + "</div>"
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'><strong>Note:</strong> This is an automated notification. Do not share your credentials with anyone. Do not reply to this email.</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated notification from ReconXpert.Ai.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND Replacement Admin Welcome (onboarding-style with verify link)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendReplacementAdminWelcome(String toEmail, String contactName,
                                             String bankCode, String userId,
                                             String defaultPassword, String verifyLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Welcome! Your Replacement Administrator Account Has Been Created");
            helper.setText(buildReplacementAdminWelcomeHtml(contactName, bankCode, userId, defaultPassword, verifyLink), true);
            mailSender.send(message);
            logger.info("Replacement admin welcome email sent to: {} | userId: {}", toEmail, userId);
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement admin welcome — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML — Replacement Admin Welcome (same format as onboarding, replacement message)
    // ─────────────────────────────────────────────────────────────────────
    private String buildReplacementAdminWelcomeHtml(String name, String bankCode,
                                                     String userId, String defaultPassword,
                                                     String verifyLink) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(name) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "You have been designated as the <strong>Replacement Bank Administrator</strong> for your institution on ReconXpert.Ai. "
            + "To activate your account, please verify your email address and create a new password using the credentials below."
            + "</p>"
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>Bank Code:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(bankCode) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>User ID:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(userId) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Kindly click the button below to verify your email and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#0a1628,#152d47);color:#f59e0b;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Email &amp; Set Password</a>"
            + "</td></tr></table>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the login page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>Bank Code:</strong> " + sanitize(bankCode) + "</li>"
            + "<li>Enter your <strong>User ID:</strong> " + sanitize(userId) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete email verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    @Override
    public void sendReplacementTenureEnded(String toEmail, String contactName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Temporary Assignment Completed");
            helper.setText(buildReplacementTenureEndedHtml(contactName), true);
            mailSender.send(message);
            logger.info("Replacement tenure-ended email sent to {}", toEmail);
        } catch (Exception e) {
            logger.warn("Failed to send tenure-ended email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildReplacementTenureEndedHtml(String contactName) {
        String safe = sanitize(contactName);
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 16px;'>Dear <strong>" + safe + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;line-height:1.7;margin:0 0 20px;'>"
            + "This is to inform you that your temporary assignment on ReconXpert.Ai has now concluded. "
            + "The original administrator has been successfully reactivated, and your replacement account has been changed to <strong>Inactive</strong> status. "
            + "We appreciate your contribution during this assignment."
            + "</p>"
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:14px 18px;'>"
            + "<p style='margin:0;font-size:13px;color:#92400e;'>"
            + "If you believe this change was made in error or require clarification, please contact your KalInfotech Administrator."
            + "</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    @Override
    public void sendReplacementBecamePermanent(String toEmail, String contactName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Congratulations! You Are Now the Permanent Administrator");
            helper.setText(buildReplacementBecamePermanentHtml(contactName), true);
            mailSender.send(message);
            logger.info("Replacement became-permanent email sent to {}", toEmail);
        } catch (Exception e) {
            logger.warn("Failed to send became-permanent email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildReplacementBecamePermanentHtml(String contactName) {
        String safe = sanitize(contactName);
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 16px;'>Dear <strong>" + safe + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;line-height:1.7;margin:0 0 20px;'>"
            + "We are pleased to inform you that you have been appointed as the <strong>Permanent Administrator</strong> for your institution on ReconXpert.Ai. "
            + "Following the permanent removal of the previous administrator, your account has been granted full administrative privileges. "
            + "You may now continue managing your institution using your existing login credentials."
            + "</p>"
            + "<div style='background:#f0fdf4;border-left:4px solid #16a34a;border-radius:6px;padding:14px 18px;'>"
            + "<p style='margin:0;font-size:13px;color:#166534;'>"
            + "If you require any assistance, please contact the KalInfotech Support Team at <a href='mailto:support@kalinfotech.com' style='color:#16a34a;'>support@kalinfotech.com</a>."
            + "</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND USER WELCOME EMAIL
    // Sent to a newly created REC_USER after an admin adds them via AddUser.
    // Same template as Bank Admin welcome but with "User" description.
    // @Async — fire and forget, won't block API response
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendUserWelcome(String toEmail, String fullName,
                                String code, String codeLabel, String username,
                                String defaultPassword, String verifyLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Welcome! Your User Account Has Been Created");
            helper.setText(buildUserWelcomeHtml(fullName, code, codeLabel, username, defaultPassword, verifyLink), true);
            mailSender.send(message);
            logger.info("User welcome email sent to: {} | username: {}", toEmail, username);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] User welcome — recipient: {} | username: {} | reason: {}", toEmail, username, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] User welcome — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendUserWelcomeReplacement(String toEmail, String fullName,
                                           String code, String codeLabel, String username,
                                           String defaultPassword, String verifyLink,
                                           String replacementDescription) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Welcome! Your Replacement User Account Has Been Created");
            helper.setText(buildUserWelcomeReplacementHtml(fullName, code, codeLabel, username, defaultPassword, verifyLink, replacementDescription), true);
            mailSender.send(message);
            logger.info("Replacement user welcome email sent to: {} | username: {}", toEmail, username);
        } catch (MessagingException e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement user welcome — recipient: {} | username: {} | reason: {}", toEmail, username, e.getMessage());
        } catch (Exception e) {
            logger.error("[EMAIL-DELIVERY-FAIL] Replacement user welcome — unexpected error — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    private String buildUserWelcomeReplacementHtml(String fullName, String code, String codeLabel,
                                                    String username, String defaultPassword,
                                                    String verifyLink, String replacementDescription) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"

            // Greeting
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(fullName) + "</strong>,</p>"

            // Replacement banner
            + "<div style='background:#eff6ff;border:1px solid #bfdbfe;border-left:4px solid #2563eb;border-radius:8px;padding:14px 18px;margin-bottom:20px;'>"
            + "<p style='margin:0;font-size:14px;color:#1e40af;'>"
            + "<strong>Replacement Assignment:</strong> " + sanitize(replacementDescription)
            + "</p>"
            + "</div>"

            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "To activate your account, please verify your email address and create a secure password using the credentials below."
            + "</p>"

            // Credentials box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>" + sanitize(codeLabel) + ":</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(code) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>Username:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(username) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"

            // Verify button
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Click the button below to verify your account and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#0a1628,#152d47);color:#f59e0b;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Account &amp; Set Password</a>"
            + "</td></tr></table>"

            // Steps
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the verification page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>" + sanitize(codeLabel) + ":</strong> " + sanitize(code) + "</li>"
            + "<li>Enter your <strong>Username:</strong> " + sanitize(username) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"

            // Warning box
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    private String buildUserWelcomeHtml(String fullName, String code, String codeLabel,
                                        String username, String defaultPassword,
                                        String verifyLink) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='padding:40px 0;background:#f4f6f9;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p>"
            + "</td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"

            // Greeting
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(fullName) + "</strong>,</p>"
            + "<p style='font-size:14px;color:#64748b;margin:0 0 24px;'>"
            + "Welcome to <strong>ReconXpert.Ai</strong>. "
            + "Your user account has been successfully created by your administrator. "
            + "To activate your account, please verify your email address and create a secure password using the credentials provided below."
            + "</p>"

            // Credentials box
            + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #16a34a;border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;font-weight:bold;'>Your Login Credentials</p>"
            + "<p style='margin:10px 0 4px;font-size:13px;color:#166534;'><strong>" + sanitize(codeLabel) + ":</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(code) + "</span></p>"
            + "<p style='margin:0 0 4px;font-size:13px;color:#166534;'><strong>Username:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(username) + "</span></p>"
            + "<p style='margin:0;font-size:13px;color:#166534;'><strong>Default Password:</strong> "
            + "<span style='font-family:monospace;font-size:14px;letter-spacing:1px;'>" + sanitize(defaultPassword) + "</span></p>"
            + "</div>"

            // Verify button
            + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>Click the button below to verify your account and set your new password:</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding-bottom:28px;'>"
            + "<a href='" + sanitize(verifyLink) + "' style='background:linear-gradient(135deg,#0a1628,#152d47);color:#f59e0b;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:14px;font-weight:bold;letter-spacing:0.5px;display:inline-block;'>Verify Account &amp; Set Password</a>"
            + "</td></tr></table>"

            // Steps
            + "<p style='font-size:13px;color:#64748b;margin:0 0 8px;'>After clicking the link, on the verification page:</p>"
            + "<ol style='font-size:13px;color:#64748b;margin:0 0 20px;padding-left:20px;line-height:1.8;'>"
            + "<li>Enter your <strong>" + sanitize(codeLabel) + ":</strong> " + sanitize(code) + "</li>"
            + "<li>Enter your <strong>Username:</strong> " + sanitize(username) + "</li>"
            + "<li>Enter your <strong>Default Password:</strong> " + sanitize(defaultPassword) + "</li>"
            + "<li>Set a new password to activate your account</li>"
            + "</ol>"

            // Warning box
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:12px 16px;'>"
            + "<p style='margin:0;font-size:12px;color:#92400e;'>"
            + "<strong>Important:</strong> Your account will remain <strong>INACTIVE</strong> until you complete verification. "
            + "Please do not share your credentials with anyone."
            + "</p>"
            + "</div>"

            + "</td></tr>"

            // Footer
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACTOR ACTION CONFIRMATION — sent to parent/actor after every child action
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendActorActionConfirmation(String toEmail, String actorName,
                                             String action, String targetName, String targetCode,
                                             String scheduledAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Confirmation of Your Recent Activity – " + sanitize(action));
            helper.setText(buildActorConfirmationHtml(actorName, action, targetName, targetCode, scheduledAt), true);
            mailSender.send(message);
            logger.info("[ACTOR-CONFIRM] Sent to {} — action: {} on {}", toEmail, action, targetCode);
        } catch (Exception e) {
            logger.warn("[ACTOR-CONFIRM] Email failed for {} — action: {}: {}", toEmail, action, e.getMessage());
        }
    }

    private String buildActorConfirmationHtml(String actorName, String action,
                                               String targetName, String targetCode,
                                               String scheduledAt) {
        String actionColor = action.toLowerCase().contains("block") ? "#ef4444"
                           : action.toLowerCase().contains("inact") ? "#f97316"
                           : action.toLowerCase().contains("react") || action.toLowerCase().contains("cancel") ? "#22c55e"
                           : "#6366f1";
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628 0%,#0f2137 50%,#152d47 100%);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Action Confirmation</p>"
            + "</td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;margin:0 0 6px;'>Dear <strong>" + sanitize(actorName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;margin:0 0 24px;line-height:1.6;'>This email confirms that the following action has been successfully completed on your ReconXpert.Ai account.</p>"
            + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid " + actionColor + ";border-radius:8px;padding:20px 24px;margin-bottom:24px;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='padding:6px 0;width:40%;font-size:13px;color:#64748b;'>Action</td>"
            + "<td style='padding:6px 0;font-size:13px;font-weight:700;color:" + actionColor + ";'>" + sanitize(action) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>Target</td>"
            + "<td style='padding:6px 0;font-size:13px;font-weight:600;color:#1e293b;'>" + sanitize(targetName) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>Code</td>"
            + "<td style='padding:6px 0;font-size:13px;color:#475569;font-family:monospace;'>" + sanitize(targetCode) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>When</td>"
            + "<td style='padding:6px 0;font-size:13px;color:#475569;'>" + sanitize(scheduledAt) + "</td></tr>"
            + "</table></div>"
            + "<p style='font-size:12px;color:#94a3b8;margin:0;line-height:1.6;'>If you did not perform this action, please contact <strong>support@kalinfotech.com</strong> immediately.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:4px 0 0;font-size:11px;color:#cbd5e1;'>© KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // DELEGATION EMAILS
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Async
    public void sendDelegationToDelegate(String toEmail, String delegatorName,
                                         String delegateeName, String reason, String orgName) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | Work Delegation Notification");
            h.setText(buildDelegationToDelegatorHtml(delegatorName, delegateeName, reason, orgName), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[DELEGATION] sendDelegationToDelegate failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendDelegationToDelegatee(String toEmail, String delegateeName,
                                           String delegatorName, String reason) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | You Have Been Assigned a Work Delegation");
            h.setText(buildDelegationToDelegateeHtml(delegateeName, delegatorName, reason), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[DELEGATION] sendDelegationToDelegatee failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendDelegatorBlockPendingToDelegatee(String toEmail, String delegateeName,
                                                     String delegatorName, String blockAt) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | Notification: Scheduled Block for Delegated User");
            h.setText(buildDelegatorBlockPendingHtml(delegateeName, delegatorName, blockAt), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[DELEGATION] sendDelegatorBlockPendingToDelegatee failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendDelegatorBlockedToDelegatee(String toEmail, String delegateeName, String delegatorName) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | Notification: Delegated User Account Blocked");
            h.setText(buildDelegatorBlockedHtml(delegateeName, delegatorName), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[DELEGATION] sendDelegatorBlockedToDelegatee failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendDelegatorUnblockedToDelegatee(String toEmail, String delegateeName, String delegatorName) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | Notification: Delegated User Account Restored");
            h.setText(buildDelegatorUnblockedHtml(delegateeName, delegatorName), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[DELEGATION] sendDelegatorUnblockedToDelegatee failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendDelegationRestoredToDelegator(String toEmail, String delegatorName, String delegateeName) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | Welcome Back! Your Delegated Responsibilities Have Been Restored");
            h.setText(buildDelegationRestoredToDelegatorHtml(delegatorName, delegateeName), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[DELEGATION] sendDelegationRestoredToDelegator failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendDelegationRestoredToDelegatee(String toEmail, String delegateeName, String delegatorName) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | Delegation Completed – Team Successfully Restored");
            h.setText(buildDelegationRestoredToDelegateeHtml(delegateeName, delegatorName), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[DELEGATION] sendDelegationRestoredToDelegatee failed for {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildDelegationToDelegatorHtml(String delegatorName, String delegateeName, String reason, String orgName) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#0f2137,#152d47);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Work Delegation Confirmation</p></td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;'>Dear <strong>" + sanitize(delegatorName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>This is to inform you that your work responsibilities have been temporarily delegated, and your account has been changed to <strong>Inactive</strong> status.</p>"
            + "<div style='background:#fef3c7;border:1px solid #fcd34d;border-left:4px solid #f59e0b;border-radius:8px;padding:20px 24px;margin:20px 0;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='padding:6px 0;width:40%;font-size:13px;color:#64748b;'>Delegated To</td><td style='font-size:13px;font-weight:700;color:#d97706;'>" + sanitize(delegateeName) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>Organization</td><td style='font-size:13px;color:#1e293b;'>" + sanitize(orgName != null ? orgName : "") + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>Reason</td><td style='font-size:13px;color:#1e293b;'>" + sanitize(reason) + "</td></tr>"
            + "</table></div>"
            + "<p style='font-size:12px;color:#64748b;line-height:1.6;'>During this period, your assigned team members will be managed by <strong>" + sanitize(delegateeName) + "</strong> until your account is reactivated. If you require any clarification, please contact your administrator.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>ReconXpert.Ai — Automated Notification | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildDelegationToDelegateeHtml(String delegateeName, String delegatorName, String reason) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#0f2137,#152d47);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Work Delegation Received</p></td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;'>Dear <strong>" + sanitize(delegateeName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>You have been assigned responsibility for managing delegated work within ReconXpert.Ai. As part of this temporary assignment, the team members previously managed by <strong>" + sanitize(delegatorName) + "</strong> have been assigned to you.</p>"
            + "<div style='background:#dcfce7;border:1px solid #86efac;border-left:4px solid #22c55e;border-radius:8px;padding:20px 24px;margin:20px 0;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'>"
            + "<tr><td style='padding:6px 0;width:40%;font-size:13px;color:#64748b;'>Delegated From</td><td style='font-size:13px;font-weight:700;color:#16a34a;'>" + sanitize(delegatorName) + "</td></tr>"
            + "<tr><td style='padding:6px 0;font-size:13px;color:#64748b;'>Reason</td><td style='font-size:13px;color:#1e293b;'>" + sanitize(reason) + "</td></tr>"
            + "</table></div>"
            + "<p style='font-size:12px;color:#64748b;line-height:1.6;'>Once the original user is reactivated, all delegated responsibilities and team members will automatically be restored. No additional action is required at this time.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>ReconXpert.Ai — Automated Notification | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildDelegatorBlockPendingHtml(String delegateeName, String delegatorName, String blockAt) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#0f2137,#152d47);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Delegation Update — Block Scheduled</p></td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;'>Dear <strong>" + sanitize(delegateeName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>This is to inform you that <strong>" + sanitize(delegatorName) + "</strong>, whose responsibilities are currently delegated to you, has been scheduled for permanent account blocking.</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'><strong>Scheduled Date &amp; Time:</strong> " + sanitize(blockAt) + "</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>Your delegated responsibilities and management of the assigned team members will remain unchanged. No action is required from your side.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>ReconXpert.Ai — Automated Notification | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildDelegatorBlockedHtml(String delegateeName, String delegatorName) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#0f2137,#152d47);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Delegation Update — User Blocked</p></td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;'>Dear <strong>" + sanitize(delegateeName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>This is to notify you that the account of <strong>" + sanitize(delegatorName) + "</strong> has been permanently blocked. Your delegated responsibilities remain active, and the associated team members will continue to report to you until further administrative changes are made. No action is required at this time.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>ReconXpert.Ai — Automated Notification | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildDelegatorUnblockedHtml(String delegateeName, String delegatorName) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#0f2137,#152d47);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Delegation Update — User Unblocked</p></td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;'>Dear <strong>" + sanitize(delegateeName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>This is to inform you that <strong>" + sanitize(delegatorName) + "</strong> has been restored from <strong>Blocked</strong> status and is now <strong>Inactive</strong>. Your delegated responsibilities remain active, and you will continue managing the assigned team members until the delegation officially concludes. No further action is required.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>ReconXpert.Ai — Automated Notification | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildDelegationRestoredToDelegatorHtml(String delegatorName, String delegateeName) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#0f2137,#152d47);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>You Are Restored — Delegation Ended</p></td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;'>Dear <strong>" + sanitize(delegatorName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>Welcome back. Your account has been successfully reactivated, and your delegated responsibilities have been restored. Your assigned team members have now been automatically returned to your management. You may continue your responsibilities using your existing account.</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'><strong>Previous Delegate:</strong> " + sanitize(delegateeName) + "</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>ReconXpert.Ai — Automated Notification | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildDelegationRestoredToDelegateeHtml(String delegateeName, String delegatorName) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;'>"
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#0f2137,#152d47);padding:28px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:20px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:4px 0 0;font-size:12px;'>Delegation Ended</p></td></tr>"
            + "<tr><td style='padding:32px 40px 24px;'>"
            + "<p style='font-size:15px;color:#1e293b;'>Dear <strong>" + sanitize(delegateeName) + "</strong>,</p>"
            + "<p style='font-size:13px;color:#64748b;line-height:1.6;'>This is to inform you that the temporary delegation assigned to you has been completed. As <strong>" + sanitize(delegatorName) + "</strong> has been successfully reactivated, all associated team members have been automatically transferred back to their original manager. We appreciate your support during this temporary assignment. No further action is required from your side.</p>"
            + "</td></tr>"
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:11px;color:#94a3b8;'>ReconXpert.Ai — Automated Notification | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    // Prevent XSS — sanitize user input before putting in HTML
    private String sanitize(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    // ── Scheduler / cascade notification shortcuts ───────────────────────────────

    @Override
    @Async
    public void sendBlockedNotification(String toEmail, String contactName) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, fromName);
            h.setTo(toEmail);
            h.setSubject("ReconXpert.Ai | Your Account Has Been Blocked");
            h.setText(simpleNotice(contactName,
                    "Account Blocked",
                    "Your account on <strong>ReconXpert.Ai</strong> has been permanently blocked.",
                    "If you believe this is a mistake, please contact your administrator."), true);
            mailSender.send(msg);
        } catch (Exception e) {
            logger.warn("[SCHEDULER] sendBlockedNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAKER-CHECKER WORKFLOW EMAILS (Role / Menu / Product Capability)
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendWorkflowSubmittedNotification(String toEmail, String recipientName,
                                                  String itemType, String itemName, String itemCode,
                                                  String submittedByName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | " + sanitize(itemType) + " Awaiting Your Approval — " + sanitize(itemName));
            helper.setText(simpleNotice(recipientName,
                    itemType + " Submitted for Approval",
                    "<strong>" + sanitize(submittedByName) + "</strong> has submitted the " + sanitize(itemType).toLowerCase()
                        + " <strong>" + sanitize(itemName) + "</strong> (" + sanitize(itemCode) + ") for your review.",
                    "Please log in to ReconXpert.Ai and review this request from your Checker Queue."), true);
            mailSender.send(message);
            logger.info("[WORKFLOW] Submitted-notification sent to {} for {} {}", toEmail, itemType, itemCode);
        } catch (Exception e) {
            logger.warn("[WORKFLOW] sendWorkflowSubmittedNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendWorkflowDecisionNotification(String toEmail, String recipientName,
                                                 String itemType, String itemName, String itemCode,
                                                 String decision, String decidedByName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | " + sanitize(itemType) + " " + sanitize(decision) + " — " + sanitize(itemName));
            helper.setText(simpleNotice(recipientName,
                    itemType + " " + decision,
                    "Your " + sanitize(itemType).toLowerCase() + " <strong>" + sanitize(itemName) + "</strong> (" + sanitize(itemCode) + ") has been "
                        + "<strong>" + sanitize(decision).toLowerCase() + "</strong> by <strong>" + sanitize(decidedByName) + "</strong>.",
                    "APPROVED".equalsIgnoreCase(decision)
                        ? "It is now active and available for assignment."
                        : "Please review the details and resubmit if needed."), true);
            mailSender.send(message);
            logger.info("[WORKFLOW] Decision-notification ({}) sent to {} for {} {}", decision, toEmail, itemType, itemCode);
        } catch (Exception e) {
            logger.warn("[WORKFLOW] sendWorkflowDecisionNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendCapabilityGrantedNotification(String toEmail, String recipientName,
                                                  String productName, String capabilityType,
                                                  String actionType, String grantedByName, boolean canDelegate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | You've Been Granted " + sanitize(capabilityType) + " Access — " + sanitize(productName));
            helper.setText(simpleNotice(recipientName,
                    capabilityType + " Capability Granted",
                    "<strong>" + sanitize(grantedByName) + "</strong> has granted you <strong>" + sanitize(capabilityType) + "</strong> "
                        + "capability for <strong>" + sanitize(productName) + "</strong> (" + sanitize(actionType) + ")"
                        + (canDelegate ? ", including the right to grant this capability to others." : "."),
                    "Log in to ReconXpert.Ai to start using this access."), true);
            mailSender.send(message);
            logger.info("[WORKFLOW] Capability-granted notification sent to {} for product {}", toEmail, productName);
        } catch (Exception e) {
            logger.warn("[WORKFLOW] sendCapabilityGrantedNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendCapabilityRevokedNotification(String toEmail, String recipientName,
                                                  String productName, String capabilityType,
                                                  String actionType, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | " + sanitize(capabilityType) + " Access Revoked — " + sanitize(productName));
            helper.setText(simpleNotice(recipientName,
                    capabilityType + " Capability Revoked",
                    "Your <strong>" + sanitize(capabilityType) + "</strong> capability for <strong>" + sanitize(productName) + "</strong> ("
                        + sanitize(actionType) + ") has been revoked.",
                    "If you believe this is a mistake, please contact your administrator."), true);
            mailSender.send(message);
            logger.info("[WORKFLOW] Capability-revoked notification sent to {} for product {} (reason: {})", toEmail, productName, reason);
        } catch (Exception e) {
            logger.warn("[WORKFLOW] sendCapabilityRevokedNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRODUCT-EXPIRY GRACE-PERIOD EMAILS
    // ─────────────────────────────────────────────────────────────────────
    @Override
    @Async
    public void sendProductExpiryHoldNotification(String toEmail, String recipientName, long gracePeriodMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Your Access Is On Hold — Product Validity Expired");
            helper.setText(simpleNotice(recipientName,
                    "Account Temporarily On Hold",
                    "All products subscribed by your institution have reached the end of their validity period, "
                        + "so your account has been temporarily placed on hold.",
                    "If your institution renews at least one product within the next " + gracePeriodMinutes
                        + " minute(s), your access will be restored automatically. Otherwise, it will become "
                        + "permanently inactive and will require an administrator to reactivate it."), true);
            mailSender.send(message);
            logger.info("[PRODUCT-EXPIRY] Hold notification sent to {}", toEmail);
        } catch (Exception e) {
            logger.warn("[PRODUCT-EXPIRY] sendProductExpiryHoldNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendProductExpiryRestoredNotification(String toEmail, String recipientName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Access Restored — Product Renewed");
            helper.setText(simpleNotice(recipientName,
                    "Account Restored",
                    "Your institution has renewed a product within the grace period, so your account access has "
                        + "been automatically restored to its previous state.",
                    "You can log in to ReconXpert.Ai as usual."), true);
            mailSender.send(message);
            logger.info("[PRODUCT-EXPIRY] Restored notification sent to {}", toEmail);
        } catch (Exception e) {
            logger.warn("[PRODUCT-EXPIRY] sendProductExpiryRestoredNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendProductExpiryFinalizedNotification(String toEmail, String recipientName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Account Inactive — Grace Period Elapsed");
            helper.setText(simpleNotice(recipientName,
                    "Account Permanently Inactive",
                    "The grace period following your institution's product expiry has elapsed without a renewal, "
                        + "so your account has been made permanently inactive.",
                    "Please contact your administrator to reactivate your account once a product is renewed."), true);
            mailSender.send(message);
            logger.info("[PRODUCT-EXPIRY] Finalized notification sent to {}", toEmail);
        } catch (Exception e) {
            logger.warn("[PRODUCT-EXPIRY] sendProductExpiryFinalizedNotification failed for {}: {}", toEmail, e.getMessage());
        }
    }

    // Shared shell for all short notification emails (block, workflow, capability, product-expiry).
    // Kept deliberately aligned with the Bank Admin welcome mail so every transactional message
    // carries the same professional look: gold-on-navy header with the "Powered by KalInfotech"
    // subtitle, a prominent headline, a generously spaced body, the closing note in a highlighted
    // callout box, and a two-line footer. OTP mails intentionally use their own bespoke builders.
    @Override
    @Async
    public void sendRoleChangedNotification(String toEmail, String contactName,
                                            String oldRoleName, String newRoleName, String changedByName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("ReconXpert.Ai | Your Role Has Been Updated");
            String from = (oldRoleName == null || oldRoleName.trim().isEmpty()) ? "not previously set" : sanitize(oldRoleName);
            helper.setText(simpleNotice(contactName,
                    "Role Updated",
                    "Your role on <strong>ReconXpert.Ai</strong> has been changed from <strong>" + from
                        + "</strong> to <strong>" + sanitize(newRoleName) + "</strong> by <strong>"
                        + sanitize(changedByName) + "</strong>. Your access to the platform now follows the "
                        + "privileges attached to this new role, so the menus available to you may look different "
                        + "the next time you sign in.",
                    "If you were not expecting this change, please contact your administrator immediately."), true);
            mailSender.send(message);
            logger.info("[EMAIL-OK] Role-changed notification sent to: {} ({} -> {})", toEmail, oldRoleName, newRoleName);
        } catch (Exception e) {
            logger.warn("[EMAIL-FAIL] Role-changed notification — recipient: {} | reason: {}", toEmail, e.getMessage());
        }
    }

    private String simpleNotice(String contactName, String headline, String body, String note) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'><tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"

            // Header — matches the welcome mail
            + "<tr><td style='background:linear-gradient(135deg,#0a1628,#152d47);padding:32px 40px;text-align:center;'>"
            + "<h1 style='color:#f59e0b;margin:0;font-size:22px;letter-spacing:1px;'>ReconXpert.Ai</h1>"
            + "<p style='color:#94a3b8;margin:6px 0 0;font-size:13px;'>Powered by KalInfotech</p></td></tr>"

            // Body
            + "<tr><td style='padding:40px;'>"
            + "<p style='font-size:16px;color:#1e293b;margin:0 0 8px;'>Dear <strong>" + sanitize(contactName) + "</strong>,</p>"
            + "<h2 style='font-size:18px;color:#152d47;margin:0 0 16px;font-weight:bold;'>" + sanitize(headline) + "</h2>"
            + "<p style='font-size:14px;color:#475569;line-height:1.8;margin:0 0 24px;'>" + body + "</p>"

            // Closing note in a highlighted callout box
            + "<div style='background:#fef9ec;border-left:4px solid #f59e0b;border-radius:6px;padding:14px 18px;'>"
            + "<p style='margin:0;font-size:13px;color:#92400e;line-height:1.7;'>" + note + "</p>"
            + "</div>"
            + "</td></tr>"

            // Footer — matches the welcome mail
            + "<tr><td style='background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;'>"
            + "<p style='margin:0;font-size:12px;color:#94a3b8;'>This is an automated email from ReconXpert.Ai. Please do not reply.</p>"
            + "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e1;'>&#169; KalInfotech | support@kalinfotech.com</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }
}