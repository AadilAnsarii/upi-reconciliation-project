package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.BlockScheduleConstants;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.v2.ReconBankMasterService;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/bank")
@CrossOrigin(origins = "*")
public class ReconBankMasterController {

    private static final Logger logger = LoggerFactory.getLogger(ReconBankMasterController.class);

    @Autowired
    private ReconBankMasterService reconBankMasterService;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconBankMasterRepository reconBankMasterRepository;

    @Autowired
    private com.jpb.reconciliation.reconciliation.service.v2.EmailRegistryService emailRegistry;

    @Autowired
    private AuditReplacementRepository auditReplacementRepository;

    @Operation(summary = "Create a new bank")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createBank(
            @RequestBody ReconBankMaster bank,
            Authentication authentication) {
        String createdBy = resolveUser(authentication);
        logger.info("Create bank request: {} by {}", bank.getBankCode(), createdBy);
        return reconBankMasterService.createBank(bank, createdBy);
    }

    @Operation(summary = "Get all banks")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllBanks() {
        logger.info("Get all banks request received");
        return reconBankMasterService.getAllBanks();
    }

    @Operation(summary = "Get bank by ID")
    @GetMapping(value = "/get/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBankById(@PathVariable Long bankId) {
        logger.info("Get bank by ID: {}", bankId);
        return reconBankMasterService.getBankById(bankId);
    }

    @Operation(summary = "Get bank by code")
    @GetMapping(value = "/get-by-code/{bankCode}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBankByCode(@PathVariable String bankCode) {
        logger.info("Get bank by code: {}", bankCode);
        return reconBankMasterService.getBankByCode(bankCode);
    }

    @Operation(summary = "Get banks by status")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBanksByStatus(@RequestParam String status) {
        logger.info("Get banks by status: {}", status);
        return reconBankMasterService.getBanksByStatus(status);
    }

    @Operation(summary = "Get banks by type")
    @GetMapping(value = "/get-by-type", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBanksByType(@RequestParam String bankType) {
        logger.info("Get banks by type: {}", bankType);
        return reconBankMasterService.getBanksByType(bankType);
    }

    @Operation(summary = "Get branch banks by parent bank ID")
    @GetMapping(value = "/{parentBankId}/branch-banks", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getBranchBanks(@PathVariable Long parentBankId) {
        logger.info("Get branch banks for parent: {}", parentBankId);
        return reconBankMasterService.getBranchBanks(parentBankId);
    }

    @Operation(summary = "Update bank details")
    @PutMapping(value = "/update/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateBank(
            @PathVariable Long bankId,
            @RequestBody ReconBankMaster bank,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Update bank request for ID: {} by {}", bankId, updatedBy);
        return reconBankMasterService.updateBank(bankId, bank, updatedBy);
    }

    @Operation(summary = "Update bank status")
    @PatchMapping(value = "/update-status/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long bankId,
            @RequestParam String status,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Update bank status for ID: {} to {} by {}", bankId, status, updatedBy);
        return reconBankMasterService.updateStatus(bankId, status, updatedBy);
    }

    @Operation(summary = "Soft delete bank (sets INACTIVE)")
    @DeleteMapping(value = "/delete/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteBank(@PathVariable Long bankId) {
        logger.info("Delete bank request for ID: {}", bankId);
        return reconBankMasterService.deleteBank(bankId);
    }

    @Operation(summary = "Check if bank code exists")
    @GetMapping(value = "/check-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkBankCodeExists(@RequestParam String bankCode) {
        return reconBankMasterService.checkBankCodeExists(bankCode);
    }

    @Operation(summary = "Generate a unique 8-digit top-level bank code (preview code)")
    @GetMapping(value = "/generate-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> generateBankCode() {
        String code = reconBankMasterService.generateBankCodePublic();
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("bankCode", code);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank code generated.", java.util.Collections.singletonList(data)));
    }

    @Operation(summary = "Generate a unique branch code (first-4 of parent + timestamp last-4)")
    @GetMapping(value = "/generate-branch-code", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> generateBranchCode(@RequestParam Long parentBankId) {
        String code = reconBankMasterService.generateBranchCodePublic(parentBankId);
        if (code == null) {
            return ResponseEntity.internalServerError()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Could not generate a unique branch code.", null));
        }
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("branchCode", code);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Branch code generated.", java.util.Collections.singletonList(data)));
    }

    @Operation(summary = "Check if bank name exists")
    @GetMapping(value = "/check-name", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkBankNameExists(@RequestParam String bankName) {
        return reconBankMasterService.checkBankNameExists(bankName);
    }

    @Operation(summary = "Check if a primary/secondary contact email is already registered as an admin user or bank/branch contact")
    @GetMapping(value = "/check-email", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkEmailExists(@RequestParam String email) {
        // Only a NON-blocked holder makes the email unavailable — a blocked account is effectively
        // deleted and its email may be reused (EmailRegistryService = single source of truth).
        boolean exists = emailRegistry.isActivelyRegistered(email);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Operation(summary = "Block a bank (stores pre-block status)")
    @PostMapping(value = "/block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> blockBank(
            @PathVariable Long bankId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Block bank request for ID: {} by {}", bankId, updatedBy);
        return reconBankMasterService.blockBank(bankId, reason, updatedBy);
    }

    @Operation(summary = "Unblock a bank (restores pre-block status)")
    @PostMapping(value = "/unblock/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> unblockBank(
            @PathVariable Long bankId,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Unblock bank request for ID: {} by {}", bankId, updatedBy);
        return reconBankMasterService.unblockBank(bankId, updatedBy);
    }

    @Operation(summary = "Schedule bank inactivation at a future datetime (ISO format: 2025-01-15T10:30:00). Omit scheduledAt to schedule immediately (~30s, used by the admin-handover flow).")
    @PostMapping(value = "/schedule-inactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleInactivate(
            @PathVariable Long bankId,
            @RequestParam(required = false) String scheduledAt,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = (scheduledAt != null && !scheduledAt.trim().isEmpty())
                ? LocalDateTime.parse(scheduledAt) : LocalDateTime.now().plusSeconds(5);
        logger.info("Schedule inactivate for bankId: {} at {} by {}", bankId, dateTime, scheduledBy);
        return reconBankMasterService.scheduleInactivate(bankId, dateTime, scheduledBy);
    }

    @Operation(summary = "Schedule bank reactivation at a future datetime (ISO format: 2025-01-15T10:30:00). Omit scheduledAt to schedule immediately (~30s, used by the admin-handover flow).")
    @PostMapping(value = "/schedule-reactivate/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleReactivate(
            @PathVariable Long bankId,
            @RequestParam(required = false) String scheduledAt,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        LocalDateTime dateTime = (scheduledAt != null && !scheduledAt.trim().isEmpty())
                ? LocalDateTime.parse(scheduledAt) : LocalDateTime.now().plusSeconds(5);
        logger.info("Schedule reactivate for bankId: {} at {} by {}", bankId, dateTime, scheduledBy);
        return reconBankMasterService.scheduleReactivate(bankId, dateTime, scheduledBy);
    }

    @Operation(summary = "Schedule bank block at a future datetime (ISO format: 2025-01-15T10:30:00). Omit scheduledAt to schedule immediately (~30s, used by the admin-handover flow).")
    @PostMapping(value = "/schedule-block/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> scheduleBlock(
            @PathVariable Long bankId,
            @RequestParam(required = false) String scheduledAt,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String scheduledBy = resolveUser(authentication);
        // Blocking an institution is the most destructive action here: it cascades BLOCK_PENDING to
        // every branch and every user, and once it lands it is permanent (a live replacement is even
        // made permanent with it). It therefore gets the SAME grace window an individual user block
        // gets, read from the single source of truth BlockScheduleConstants.BLOCK_DELAY_MINUTES
        // (currently 1 minute for testing; set to 1440 for the 24-hour production window). That
        // window IS the undo, and the warning email tells recipients to contact their administrator
        // to cancel within it.
        //
        // This used to default to now()+5s, so the UI's Block button (which sends no scheduledAt)
        // blocked the whole institution five seconds later, with no chance to cancel.
        LocalDateTime dateTime = (scheduledAt != null && !scheduledAt.trim().isEmpty())
                ? LocalDateTime.parse(scheduledAt)
                : LocalDateTime.now().plusMinutes(BlockScheduleConstants.BLOCK_DELAY_MINUTES);
        logger.info("Schedule block for bankId: {} at {} by {}", bankId, dateTime, scheduledBy);
        return reconBankMasterService.scheduleBlock(bankId, dateTime, scheduledBy, reason);
    }

    @Operation(summary = "Cancel a scheduled bank status change (scheduleType: INACTIVATE / REACTIVATE / BLOCK)")
    @PostMapping(value = "/cancel-schedule/{bankId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> cancelSchedule(
            @PathVariable Long bankId,
            @RequestParam String scheduleType,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Cancel {} schedule for bankId: {} by {}", scheduleType, bankId, updatedBy);
        return reconBankMasterService.cancelSchedule(bankId, scheduleType, updatedBy);
    }

    @Operation(summary = "Get all bank admins with bank info (Kal Admin view)")
    @GetMapping(value = "/get-all-bank-admins", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllBankAdmins() {
        List<ReconUser> bankAdmins = reconUserRepository.findByUserType("BANK_ADMIN");
        List<Map<String, Object>> result = new ArrayList<>();
        for (ReconUser user : bankAdmins) {
            if (!"PRIMARY".equals(user.getContactRank()) && user.getContactRank() != null) continue;
            // A replacement whose cover has concluded (RESTORED — the original was reactivated and
            // is back in charge) is no longer an admin of this institution. They only ever appeared
            // here because finalizePendingReplacement gave them userType=BANK_ADMIN and this bank's
            // ID, and they carry no contactRank, so the PRIMARY filter above can't exclude them.
            // Drop them from My Organization; the user themselves still exists everywhere else.
            if (auditReplacementRepository
                    .findByReplacementUserIdAndStatus(user.getUserId(), "RESTORED").isPresent()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            ReconBankMaster bank = null;
            if (user.getBankId() != null) {
                bank = reconBankMasterRepository.findPrimaryById(user.getBankId()).orElse(null);
            }
            row.put("bankId", bank != null ? bank.getBankId() : null);
            row.put("bankCode", bank != null ? bank.getBankCode() : null);
            row.put("bankNameFull", bank != null ? bank.getBankName() : null);
            row.put("bankNameShort", bank != null ? bank.getBankNameShort() : null);
            row.put("status", bank != null ? bank.getStatus() : null);
            row.put("updatedAt", bank != null ? bank.getUpdatedAt() : null);
            row.put("blockReason", bank != null ? bank.getBlockReason() : null);
            row.put("blockScheduledAt", bank != null ? bank.getBlockScheduledAt() : null);
            row.put("inactivateScheduledAt", bank != null ? bank.getInactivateScheduledAt() : null);
            row.put("reactivateScheduledAt", bank != null ? bank.getReactivateScheduledAt() : null);
            row.put("preBlockStatus", bank != null ? bank.getPreBlockStatus() : null);
            row.put("adminId", user.getUserId());
            row.put("adminUsername", user.getUsername());
            row.put("primaryFullName", user.getFullName());
            row.put("primaryEmail", user.getEmail());
            row.put("primaryMobile", user.getMobileNumber());
            row.put("adminStatus", user.getStatus());
            // Every row that survives the RESTORED filter above belongs to a real admin of this
            // institution: an original, or an ACTIVE replacement (buttons suppressed via the
            // "Temporary" label), or a FINALIZED one (now the legitimate ongoing admin). For all
            // of them the bank-level cascade is the correct target, so none is an individual account.
            row.put("isIndividualAccount", false);

            // Is THIS admin currently covering for someone else? Their own row must be
            // flagged so the frontend shows "Temporary" and suppresses their buttons
            // while the cover is still reversible (ACTIVE), matching old-backend behavior.
            List<AuditReplacement> asReplacementOf = auditReplacementRepository
                    .findByReplacementUserIdAndStatusIn(user.getUserId(), Arrays.asList(CommonConstants.ACTIVE, "FINALIZED"));
            if (!asReplacementOf.isEmpty()) {
                AuditReplacement asRep = asReplacementOf.get(0);
                row.put("replacementAdminRow", true);
                row.put("replacementStatus", "FINALIZED".equals(asRep.getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
                row.put("replacedByUsername", null);
            } else {
                row.put("replacementAdminRow", false);
                // A replacement is "live" whether it's still ACTIVE (reversible — original can
                // still be reactivated) or FINALIZED (original was blocked, now permanent) —
                // these must be labeled differently, not both reported as PERMANENT.
                List<AuditReplacement> reps = auditReplacementRepository
                        .findByOriginalUserIdAndStatusIn(user.getUserId(), Arrays.asList(CommonConstants.ACTIVE, "FINALIZED"));
                // A FINALIZED replacement only still applies while the original remains BLOCKED
                // (the state finalize left them in). If a KAL Admin has since manually reactivated
                // this original back to a live status, the FINALIZED record is historical — showing
                // "Perm. Replaced" on a normal, currently-active admin is stale/wrong.
                if (!reps.isEmpty() && (CommonConstants.ACTIVE.equals(reps.get(0).getStatus()) || "BLOCKED".equals(user.getStatus()))) {
                    AuditReplacement rep = reps.get(0);
                    ReconUser repUser = reconUserRepository.findById(rep.getReplacementUserId()).orElse(null);
                    row.put("replacementStatus", "FINALIZED".equals(rep.getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
                    row.put("replacedByUsername", repUser != null ? repUser.getUsername() : null);
                } else {
                    row.put("replacementStatus", null);
                    row.put("replacedByUsername", null);
                }
            }
            result.add(row);
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank admins fetched", result));
    }

    @Operation(summary = "Get all branch admins — KalAdmin sees every branch across all banks, Bank Admin sees only their own bank's branches")
    @GetMapping(value = "/get-all-admins", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllBranchAdmins(Authentication authentication) {
        String username = resolveUser(authentication);
        ReconUser currentUser = reconUserRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.FAILURE, "User not found", null));
        }

        List<ReconBankMaster> branchBanks;
        if ("KAL_ADMIN".equals(currentUser.getUserType())) {
            // KalAdmin has no bankId of their own — return branches across every bank.
            branchBanks = reconBankMasterRepository.findAllPrimary().stream()
                    .filter(b -> b.getParentBankId() != null)
                    .collect(Collectors.toList());
        } else {
            if (currentUser.getBankId() == null) {
                return ResponseEntity.ok(new RestWithStatusList(CommonConstants.FAILURE, "User not found", null));
            }
            branchBanks = reconBankMasterRepository.findByParentBankId(currentUser.getBankId());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (ReconBankMaster branch : branchBanks) {
            ReconUser branchAdmin = reconUserRepository
                    .findByBankIdAndUserTypeAndContactRank(branch.getBankId(), "BRANCH_ADMIN", "PRIMARY")
                    .orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("branchId", branch.getBankId());
            row.put("branchCode", branch.getBankCode());
            row.put("branchNameFull", branch.getBankName());
            row.put("branchNameShort", branch.getBankNameShort());
            row.put("regCity", branch.getRegCity());
            row.put("commCity", branch.getRegCity());
            row.put("status", branch.getStatus());
            row.put("updatedAt", branch.getUpdatedAt());
            row.put("blockReason", branch.getBlockReason());
            row.put("blockScheduledAt", branch.getBlockScheduledAt());
            row.put("inactivateScheduledAt", branch.getInactivateScheduledAt());
            row.put("reactivateScheduledAt", branch.getReactivateScheduledAt());
            row.put("preBlockStatus", branch.getPreBlockStatus());
            AuditReplacement liveRep = null;
            if (branchAdmin != null) {
                row.put("adminId", branchAdmin.getUserId());
                row.put("branchAdminId", branchAdmin.getUserId());
                row.put("adminUsername", branchAdmin.getUsername());
                row.put("primaryFullName", branchAdmin.getFullName());
                row.put("primaryEmail", branchAdmin.getEmail());
                row.put("primaryMobile", branchAdmin.getMobileNumber());
                row.put("adminStatus", branchAdmin.getStatus());
                List<AuditReplacement> reps = auditReplacementRepository
                        .findByOriginalUserIdAndStatusIn(branchAdmin.getUserId(), Arrays.asList(CommonConstants.ACTIVE, "FINALIZED"));
                if (!reps.isEmpty()) {
                    liveRep = reps.get(0);
                    ReconUser repUser = reconUserRepository.findById(liveRep.getReplacementUserId()).orElse(null);
                    // Only badge the original's own row as replaced while that's still true: ACTIVE
                    // (in-flight, reversible) or FINALIZED with the original still actually BLOCKED.
                    // A KAL Admin manually reactivating the original after finalize makes the
                    // FINALIZED record historical — the row shouldn't keep reading "Perm. Replaced".
                    if (CommonConstants.ACTIVE.equals(liveRep.getStatus()) || "BLOCKED".equals(branchAdmin.getStatus())) {
                        row.put("replacementStatus", "FINALIZED".equals(liveRep.getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
                        row.put("replacedByUsername", repUser != null ? repUser.getUsername() : null);
                    } else {
                        row.put("replacementStatus", null);
                        row.put("replacedByUsername", null);
                    }
                } else {
                    row.put("replacementStatus", null);
                    row.put("replacedByUsername", null);
                }
            } else {
                row.put("adminId", null);
                row.put("branchAdminId", null);
                row.put("adminUsername", null);
                row.put("primaryFullName", null);
                row.put("primaryEmail", null);
                row.put("primaryMobile", null);
                row.put("adminStatus", null);
                row.put("replacementStatus", null);
                row.put("replacedByUsername", null);
            }
            row.put("replacementAdminRow", false);
            // This row is always the original (found via contactRank=PRIMARY, which a
            // replacement never has), so it's never an individual-replacement account.
            row.put("isIndividualAccount", false);
            result.add(row);

            // The replacement admin never has contactRank=PRIMARY (finalizePendingReplacement
            // doesn't set one), so findByBankIdAndUserTypeAndContactRank above can never find
            // them — surface them as their own row here instead, matching old-backend behavior.
            if (liveRep != null) {
                ReconUser repUser = reconUserRepository.findById(liveRep.getReplacementUserId()).orElse(null);
                if (repUser != null) {
                    Map<String, Object> repRow = new LinkedHashMap<>();
                    repRow.put("branchId", branch.getBankId());
                    repRow.put("branchCode", branch.getBankCode());
                    repRow.put("branchNameFull", branch.getBankName());
                    repRow.put("branchNameShort", branch.getBankNameShort());
                    repRow.put("regCity", branch.getRegCity());
                    repRow.put("commCity", branch.getRegCity());
                    repRow.put("status", branch.getStatus());
                    repRow.put("updatedAt", repUser.getUpdatedAt());
                    repRow.put("blockReason", branch.getBlockReason());
                    repRow.put("blockScheduledAt", branch.getBlockScheduledAt());
                    repRow.put("inactivateScheduledAt", branch.getInactivateScheduledAt());
                    repRow.put("reactivateScheduledAt", branch.getReactivateScheduledAt());
                    repRow.put("preBlockStatus", branch.getPreBlockStatus());
                    repRow.put("adminId", repUser.getUserId());
                    repRow.put("branchAdminId", repUser.getUserId());
                    repRow.put("adminUsername", repUser.getUsername());
                    repRow.put("primaryFullName", repUser.getFullName());
                    repRow.put("primaryEmail", repUser.getEmail());
                    repRow.put("primaryMobile", repUser.getMobileNumber());
                    repRow.put("adminStatus", repUser.getStatus());
                    repRow.put("replacementStatus", "FINALIZED".equals(liveRep.getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
                    repRow.put("replacedByUsername", null);
                    repRow.put("replacementAdminRow", true);
                    // ACTIVE is already fully handled (buttons suppressed via "Temporary"), and
                    // FINALIZED means they're now the legitimate ongoing admin — branch-level
                    // cascade is correct for them, same as any normal admin.
                    repRow.put("isIndividualAccount", false);
                    result.add(repRow);
                }
            }
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Branch admins fetched", result));
    }

    @Operation(summary = "Get current logged-in user's account status (for status guard polling)")
    @GetMapping(value = "/my-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> myStatus(Authentication authentication) {
        String username = resolveUser(authentication);
        Optional<ReconUser> userOpt = reconUserRepository.findByUsername(username);
        if (!userOpt.isPresent()) userOpt = reconUserRepository.findByEmail(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.FAILURE, "User not found", null));
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, userOpt.get().getStatus(), null));
    }

    @Operation(summary = "Upload bank or branch logo (JPG/TIF, max 2MB)")
    @PostMapping(value = "/upload-logo/{bankId}", consumes = "multipart/form-data", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> uploadLogo(
            @PathVariable Long bankId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        String uploadedBy = resolveUser(authentication);
        logger.info("Logo upload request for bank ID: {} by {}", bankId, uploadedBy);
        return reconBankMasterService.uploadLogo(bankId, file, uploadedBy);
    }

    @Operation(summary = "Serve bank or branch logo image by bank code")
    @GetMapping(value = "/logo/{bankCode}")
    public ResponseEntity<byte[]> getLogoImage(@PathVariable String bankCode) {
        logger.info("Logo image request for bank code: {}", bankCode);
        return reconBankMasterService.getLogoImage(bankCode);
    }

    private String resolveUser(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
    }
}
