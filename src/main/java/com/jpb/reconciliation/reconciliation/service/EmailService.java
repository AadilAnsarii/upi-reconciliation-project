package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.Map;

/**
 * EmailService — sends transactional emails from ReconXpert.Ai
 *
 * All methods are fire-and-forget — they throw RuntimeException on failure
 * so callers can decide whether to propagate or swallow.
 */
public interface EmailService {

    /**
     * Send OTP email for Login verification flow.
     */
    void sendLoginOtp(String toEmail, String userName, String otpCode, int expiryMins);

    /**
     * Send OTP email for Forgot Password flow.
     */
    void sendForgotPasswordOtp(String toEmail, String userName, String otpCode, int expiryMins);

    /**
     * Send welcome email to Bank's Primary Contact (Bank Admin)
     * after KalInfotech Admin successfully onboards the bank.
     * Includes: Bank Code, Bank Admin ID, Default Password, Verify Link.
     */
    void sendBankAdminWelcome(String toEmail, String superUserName,
                              String bankName, String bankCode,
                              String BankAdminId, String defaultPassword,
                              String verifyLink);

    /**
     * Send status change notification email to Bank's Admin
     * whenever KalInfotech Admin changes the bank status.
     * e.g. ACTIVE → INACTIVE, BLOCKED, etc.
     */
    void sendStatusChangeNotification(String toEmail, String superUserName,
                                      String bankName, String bankCode,
                                      String oldStatus, String newStatus);

    /**
     * Send cascade status change notification to a Sub-Institute's primary contact.
     * Triggered when parent bank is BLOCKED / ACTIVE.
     * Email includes a note referencing the parent bank that caused the cascade.
     */
    void sendBranchBankStatusNotification(String toEmail, String contactName,
                                            String branchBankName, String branchBankCode,
                                            String oldStatus, String newStatus,
                                            String parentBankName, String parentBankCode);

    /**
     * Send block warning email to Bank's Admin when admin schedules permanent block.
     * Tells them: account will be permanently blocked in 24 hours, contact admin to cancel.
     */
    void sendBlockWarning(String toEmail, String superUserName,
                          String bankName, String bankCode,
                          String blockAt);

    /**
     * Send block cancelled email to Bank's Admin when admin undoes block.
     */
    void sendBlockCancelled(String toEmail, String superUserName,
                            String bankName, String bankCode,
                            String restoredStatus);

    /**
     * Send block cancelled email to a Sub-Institute when parent's block is undone.
     */
    void sendBranchBankBlockCancelled(String toEmail, String contactName,
                                        String branchBankName, String branchBankCode,
                                        String parentBankName, String parentBankCode);

    /**
     * Send block warning email to a Sub-Institute's primary contact.
     * Triggered at same time as parent bank block warning.
     */
    void sendBranchBankBlockWarning(String toEmail, String contactName,
                                      String branchBankName, String branchBankCode,
                                      String parentBankName, String parentBankCode,
                                      String blockAt);

    /**
     * Send profile update notification to the bank's primary contact
     * whenever KalInfotech Admin updates the bank profile.
     * changesBySections: section name → list of "Field: old → new" strings.
     * Only sections that actually changed are included; empty map means no email is sent.
     */
    void sendBankUpdateNotification(String toEmail, String contactName,
                                           String bankName, String bankCode,
                                           String updatedAt,
                                           Map<String, List<String>> changesBySections);

    /**
     * Sent when an admin schedules inactivation (INACTIVE_PENDING).
     * Tells the contact: your account will be inactivated at inactivateAt unless cancelled.
     */
    void sendInactivatePendingWarning(String toEmail, String contactName,
                                      String entityName, String entityCode,
                                      String inactivateAt);

    /**
     * Sent when admin undoes a pending inactivation.
     * Account restored to its previous (ACTIVE) status.
     */
    void sendInactivateCancelled(String toEmail, String contactName,
                                 String entityName, String entityCode);

    /**
     * Sent when the inactivation window expires and status flips to INACTIVE automatically.
     */
    void sendInactivatedNotification(String toEmail, String contactName,
                                     String entityName, String entityCode);

    /**
     * Sent when an admin schedules reactivation (ACTIVE_PENDING).
     * Tells the contact: your account will be reactivated at reactivateAt.
     */
    void sendReactivatePendingNotification(String toEmail, String contactName,
                                           String entityName, String entityCode,
                                           String reactivateAt);

    /**
     * Sent when admin undoes a pending reactivation.
     * Account restored to INACTIVE.
     */
    void sendReactivateCancelled(String toEmail, String contactName,
                                 String entityName, String entityCode);

    /**
     * Sent when the reactivation window expires and status flips to ACTIVE automatically.
     * username — the login username to show in the credential block.
     * loginUrl — the frontend login page URL for this entity type.
     */
    void sendReactivatedNotification(String toEmail, String contactName,
                                     String entityName, String entityCode,
                                     String username, String loginUrl);

    /**
     * Sent to the outgoing (original) admin/user when they are replaced.
     * replacementFullName and replacementEmail describe who has taken over.
     * orderedBy is who authorised the replacement (from the HandoverModal form).
     */
    void sendReplacementOutgoingNotification(String toEmail, String contactName,
                                             String entityCode, String replacedBy, String reason,
                                             String replacementFullName, String replacementEmail,
                                             String orderedBy);

    /**
     * Sent to the incoming (replacement) admin/user with their login credentials.
     */
    void sendReplacementWelcome(String toEmail, String contactName,
                                String entityCode, String username, String tempPassword);

    /**
     * Sent to replacement MainAdmin or BranchAdmin — onboarding-style email with verify link.
     * Includes: Bank Code, User ID, Default Password, Verify Email & Set Password button.
     */
    void sendReplacementAdminWelcome(String toEmail, String contactName,
                                     String bankCode, String userId,
                                     String defaultPassword, String verifyLink);

    /**
     * Sent to the temporary replacement (B) when original admin (A) is reactivated
     * and B's tenure ends automatically.
     */
    void sendReplacementTenureEnded(String toEmail, String contactName);

    /**
     * Sent to replacement (B) when original admin (A) is permanently blocked —
     * B is now the permanent admin.
     */
    void sendReplacementBecamePermanent(String toEmail, String contactName);

    /**
     * Sent to a newly created user (REC_USER) after an admin adds them via AddUser.
     * Includes: Bank Code or Branch Code (codeLabel), Username, Default Password, Verify Link.
     * verifyLink points to /user-verify?bankCode=&username= (branch users also get &branchCode=)
     */
    void sendUserWelcome(String toEmail, String fullName,
                         String code, String codeLabel, String username,
                         String defaultPassword, String verifyLink);

    /**
     * Same as sendUserWelcome but adds a replacement-context banner above the credentials.
     * replacementDescription explains the user is a replacement and what that means.
     */
    void sendUserWelcomeReplacement(String toEmail, String fullName,
                                    String code, String codeLabel, String username,
                                    String defaultPassword, String verifyLink,
                                    String replacementDescription);

    /**
     * Sent to the actor (parent/admin) who performed a status-change action on a child entity.
     * action: "Block Scheduled", "Inactivation Scheduled", "Block Cancelled", "Reactivated", etc.
     */
    void sendActorActionConfirmation(String toEmail, String actorName,
                                     String action, String targetName, String targetCode,
                                     String scheduledAt);

    // ── Delegation emails ──

    /** Sent to delegator when their work is delegated to a parent. */
    void sendDelegationToDelegate(String toEmail, String delegatorName,
                                  String delegateeName, String reason, String orgName);

    /** Sent to delegatee when they receive a delegation. */
    void sendDelegationToDelegatee(String toEmail, String delegateeName,
                                   String delegatorName, String reason);

    /** Sent to delegatee when delegator is scheduled for block (BLOCK_PENDING). */
    void sendDelegatorBlockPendingToDelegatee(String toEmail, String delegateeName,
                                              String delegatorName, String blockAt);

    /** Sent to delegatee when delegator is permanently blocked (BLOCKED). */
    void sendDelegatorBlockedToDelegatee(String toEmail, String delegateeName, String delegatorName);

    /** Sent to delegatee when delegator's block is undone (back to INACTIVE). */
    void sendDelegatorUnblockedToDelegatee(String toEmail, String delegateeName, String delegatorName);

    /** Sent to delegator when delegation is restored (delegator reactivated). */
    void sendDelegationRestoredToDelegator(String toEmail, String delegatorName, String delegateeName);

    /** Sent to delegatee when delegation ends because delegator was reactivated. */
    void sendDelegationRestoredToDelegatee(String toEmail, String delegateeName, String delegatorName);

    // ── Scheduler / cascade notification shortcuts ──

    /** Sent when a user is automatically blocked by the scheduler (24h window elapsed). */
    void sendBlockedNotification(String toEmail, String contactName);

    /** Tells a user their role — and therefore what they can do in the platform — has changed. */
    void sendRoleChangedNotification(String toEmail, String contactName,
                                     String oldRoleName, String newRoleName, String changedByName);

    // ── Maker-Checker workflow emails (Role / Menu / Product Capability) ──

    /** Sent to the Checker(s) when a Maker submits an item (Role or Menu) for approval. */
    void sendWorkflowSubmittedNotification(String toEmail, String recipientName,
                                           String itemType, String itemName, String itemCode,
                                           String submittedByName);

    /** Sent to the Maker when the Checker approves or rejects their submitted item. */
    void sendWorkflowDecisionNotification(String toEmail, String recipientName,
                                          String itemType, String itemName, String itemCode,
                                          String decision, String decidedByName);

    /** Sent to a user when they are granted a Maker/Checker product capability. */
    void sendCapabilityGrantedNotification(String toEmail, String recipientName,
                                           String productName, String capabilityType,
                                           String actionType, String grantedByName, boolean canDelegate);

    /** Sent to a user when their Maker/Checker product capability is revoked. */
    void sendCapabilityRevokedNotification(String toEmail, String recipientName,
                                           String productName, String capabilityType,
                                           String actionType, String reason);

    // ── Product-expiry grace-period emails ──

    /** Sent to a user when all of their bank's products have expired and they're put on hold. */
    void sendProductExpiryHoldNotification(String toEmail, String recipientName, long gracePeriodMinutes);

    /** Sent to a user when their bank renews a product within the grace period and access is restored. */
    void sendProductExpiryRestoredNotification(String toEmail, String recipientName);

    /** Sent to a user when the grace period elapses without renewal and their account becomes permanently inactive. */
    void sendProductExpiryFinalizedNotification(String toEmail, String recipientName);
}