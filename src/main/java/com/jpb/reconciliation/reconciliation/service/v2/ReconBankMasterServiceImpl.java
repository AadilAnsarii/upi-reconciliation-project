package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditLog;
import com.jpb.reconciliation.reconciliation.entity.v2.CBankProductMap;
import com.jpb.reconciliation.reconciliation.entity.v2.CRoleMenuMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditLogRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CBankProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconProductMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReconBankMasterServiceImpl implements ReconBankMasterService {

    private static final Logger logger = LoggerFactory.getLogger(ReconBankMasterServiceImpl.class);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RNG = new SecureRandom();

    @Autowired private ReconBankMasterRepository reconBankMasterRepository;
    @Autowired private ReconRoleMasterRepository reconRoleMasterRepository;
    @Autowired private MenuMasterRepository menuMasterRepository;
    @Autowired private CRoleMenuMapRepository roleMenuMapRepository;
    @Autowired private RoleCodeGeneratorService roleCodeGeneratorService;
    @Autowired private ReconUserRepository reconUserRepository;
    @Autowired private EmailRegistryService emailRegistry;
    @Autowired private HierarchyScopeService hierarchyScope;
    @Autowired private ReconPasswordManagerRepository reconPasswordManagerRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private CBankProductMapRepository bankProductMapRepository;
    @Autowired private ReconProductMasterRepository productMasterRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private AuditReplacementService replacementService;
    @Autowired private DelegationService delegationService;
    @Autowired private com.jpb.reconciliation.reconciliation.repository.v2.AuditReplacementRepository auditReplacementRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.logo.upload-dir:/home/reconapp/kalrecon/bank_logos/}")
    private String logoUploadDir;

    private static final List<String> ALLOWED_LOGO_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/tiff", "image/tif"
    );
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2 MB

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createBank(ReconBankMaster bank, String createdBy) {
        // Name uniqueness is intentionally not enforced here — matches old backend
        // behavior (MainBankServiceImpl/BranchBankServiceImpl.checkNameExists()),
        // where the same institution name is always allowed for both banks and branches.

        // ── Server-side email-exists validation (mirrors /check-email) ─────────
        // Checked against both RCN_RECON_USER (login accounts) and RECON_BANK_MASTER
        // (contact rows for other banks/branches) — an email registered in either
        // place must not be reused for a new onboarding.
        // A BLOCKED account/institution is effectively deleted, so its email may be reused; every
        // other status still blocks reuse. EmailRegistryService is the single source of truth.
        String primaryEmailLc = bank.getEmail() != null ? bank.getEmail().trim().toLowerCase() : null;
        if (primaryEmailLc != null && !primaryEmailLc.isEmpty() && emailRegistry.isActivelyRegistered(primaryEmailLc)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "\"" + primaryEmailLc + "\" is already registered.", null));
        }
        String secondaryEmailLc = bank.getSecondaryEmail() != null ? bank.getSecondaryEmail().trim().toLowerCase() : null;
        if (secondaryEmailLc != null && !secondaryEmailLc.isEmpty() && emailRegistry.isActivelyRegistered(secondaryEmailLc)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "\"" + secondaryEmailLc + "\" is already registered.", null));
        }

        // ── Primary and Secondary contact details must never be identical ──────
        if (primaryEmailLc != null && primaryEmailLc.equals(secondaryEmailLc)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Primary and Secondary email cannot be the same.", null));
        }
        String primaryMobile = bank.getMobileNumber() != null ? bank.getMobileNumber().trim() : null;
        String secondaryMobile = bank.getSecondaryMobileNumber() != null ? bank.getSecondaryMobileNumber().trim() : null;
        if (primaryMobile != null && !primaryMobile.isEmpty() && primaryMobile.equals(secondaryMobile)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Primary and Secondary mobile number cannot be the same.", null));
        }
        String primaryAltMobile = bank.getAltMobileNumber() != null ? bank.getAltMobileNumber().trim() : null;
        String secondaryAltMobile = bank.getSecondaryAltMobile() != null ? bank.getSecondaryAltMobile().trim() : null;
        if (primaryAltMobile != null && !primaryAltMobile.isEmpty() && primaryAltMobile.equals(secondaryAltMobile)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Primary and Secondary alternate mobile number cannot be the same.", null));
        }
        String primaryFullNameLc = bank.getFullName() != null ? bank.getFullName().trim().toLowerCase() : null;
        String secondaryFullNameLc = bank.getSecondaryFullName() != null ? bank.getSecondaryFullName().trim().toLowerCase() : null;
        if (primaryFullNameLc != null && !primaryFullNameLc.isEmpty() && primaryFullNameLc.equals(secondaryFullNameLc)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Primary and Secondary contact name cannot be the same.", null));
        }

        // ── Within the same contact, Mobile and Alternate Mobile must never be identical ──
        if (primaryMobile != null && !primaryMobile.isEmpty() && primaryMobile.equals(primaryAltMobile)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Alternate mobile number cannot be the same as Mobile number.", null));
        }
        if (secondaryMobile != null && !secondaryMobile.isEmpty() && secondaryMobile.equals(secondaryAltMobile)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Alternate mobile number cannot be the same as Mobile number.", null));
        }

        // All validation passed. If either contact email is being reused from a BLOCKED account,
        // release (tombstone) that blocked holder's email now so the new bank/admin rows below stay
        // the single live holder of it (findByEmail must remain single-valued).
        emailRegistry.releaseBlockedHolders(primaryEmailLc);
        emailRegistry.releaseBlockedHolders(secondaryEmailLc);

        // ── Determine branch vs bank ─────────────────────────────────────────
        boolean isBranch     = bank.getParentBankId() != null;
        String adminUserType = isBranch ? "BRANCH_ADMIN" : "BANK_ADMIN";
        bank.setBankLevel(isBranch ? "BRANCH" : "BANK");

        // ── Resolve / generate BANK_CODE ─────────────────────────────────────
        // Use frontend's pre-generated preview code if it's an unused 8-digit
        // code — keeps the code shown during onboarding identical to what gets
        // persisted. Applies to both Bank and Branch creation.
        String generatedCode;
        String frontendCode = bank.getBankCode();
        boolean frontendCodeUsable = frontendCode != null && frontendCode.matches("\\d{8}")
                && !reconBankMasterRepository.existsByBankCodeAndContactRank(frontendCode, "PRIMARY");
        if (frontendCodeUsable) {
            generatedCode = frontendCode;
        } else if (isBranch) {
            // Fallback: generate using parent bank's first-4 prefix + timestamp last-4
            generatedCode = generateBranchCode(bank.getParentBankId());
        } else {
            generatedCode = generateBankCode();
        }
        bank.setBankCode(generatedCode);

        mapAddressAndPhoneTransients(bank);

        bank.setContactRank("PRIMARY");
        bank.setCreatedAt(LocalDateTime.now());
        bank.setCreatedBy(createdBy);
        if (bank.getStatus() == null) bank.setStatus("REQUEST");

        // ── Resolve KalAdmin userId (stored as parentUserId on admin users) ──
        Long kalAdminUserId = reconUserRepository.findByUsername(createdBy)
                .map(ReconUser::getUserId).orElse(null);

        // ── Save PRIMARY bank row ────────────────────────────────────────────
        ReconBankMaster savedPrimaryBank = reconBankMasterRepository.save(bank);
        saveProductMappings(savedPrimaryBank.getBankId(), bank, createdBy);

        Long adminRoleId = createDefaultAdminMenus(generatedCode, savedPrimaryBank.getBankId(), isBranch, createdBy);

        saveAuditLog("RECON_BANK_MASTER", savedPrimaryBank.getBankId(), "CREATE", null, null,
                createdBy, adminUserType, savedPrimaryBank.getBankId(),
                "Bank " + (isBranch ? "(Branch) " : "") + generatedCode + " onboarded");

        // ── Save SECONDARY bank row (if secondary contact provided) ──────────
        if (bank.getSecondaryEmail() != null && !bank.getSecondaryEmail().trim().isEmpty()) {
            ReconBankMaster secondaryRow = new ReconBankMaster();
            secondaryRow.setBankCode(generatedCode);
            secondaryRow.setBankName(bank.getBankName());
            secondaryRow.setBankType(bank.getBankType());
            secondaryRow.setParentBankId(bank.getParentBankId());
            secondaryRow.setContactRank("SECONDARY");
            secondaryRow.setFullName(bank.getSecondaryFullName());
            secondaryRow.setEmail(bank.getSecondaryEmail().trim().toLowerCase());
            secondaryRow.setMobileNumber(bank.getSecondaryMobileNumber());
            secondaryRow.setAltMobileNumber(bank.getSecondaryAltMobile());
            secondaryRow.setStatus(CommonConstants.INACTIVE);
            secondaryRow.setCreatedAt(LocalDateTime.now());
            secondaryRow.setCreatedBy(createdBy);
            reconBankMasterRepository.save(secondaryRow);
            logger.info("Secondary bank row saved for bankCode={}", generatedCode);
        }

        // ── Create PRIMARY admin user in RCN_RECON_USER ──────────────────────
        if (bank.getEmail() != null && !bank.getEmail().trim().isEmpty()) {
            try {
                String defaultPwd = generatePassword(10);
                String username   = deriveUsername(bank.getEmail(), bank.getFullName());

                ReconUser primaryUser = buildAdminUser(
                        savedPrimaryBank.getBankId(), bank.getFullName(), bank.getEmail(),
                        bank.getMobileNumber(), username, adminUserType, "PRIMARY",
                        defaultPwd, "REQUEST", createdBy);
                primaryUser.setParentUserId(kalAdminUserId);

                primaryUser.setRoleId(adminRoleId);

                ReconUser savedPrimaryUser = reconUserRepository.saveAndFlush(primaryUser);
                savePasswordHistory(savedPrimaryUser, createdBy);

                savedPrimaryBank.setBankAdminUsername(username);
                savedPrimaryBank.setDefaultPassword(passwordEncoder.encode(defaultPwd));
                reconBankMasterRepository.save(savedPrimaryBank);

                saveAuditLog("RCN_RECON_USER", savedPrimaryUser.getUserId(), "CREATE", null,
                        "userType=" + adminUserType + ",contactRank=PRIMARY,status=REQUEST",
                        createdBy, adminUserType, savedPrimaryBank.getBankId(),
                        "Primary " + adminUserType + " created: " + username);

                String verifyPage = isBranch ? "/branch-verify-email" : "/verify-email";
                String verifyLink = frontendUrl + verifyPage
                        + "?bankCode=" + generatedCode
                        + "&username=" + username;
                emailService.sendBankAdminWelcome(
                        savedPrimaryUser.getEmail(), savedPrimaryUser.getFullName(),
                        savedPrimaryBank.getBankName(), generatedCode,
                        username, defaultPwd, verifyLink);

                // ── Create SECONDARY admin user in RCN_RECON_USER ────────────
                if (bank.getSecondaryEmail() != null && !bank.getSecondaryEmail().trim().isEmpty()) {
                    String secUsername   = deriveUsername(bank.getSecondaryEmail(), bank.getSecondaryFullName());
                    String secDefaultPwd = generatePassword(10);

                    ReconUser secondaryUser = buildAdminUser(
                            savedPrimaryBank.getBankId(), bank.getSecondaryFullName(), bank.getSecondaryEmail(),
                            bank.getSecondaryMobileNumber(), secUsername, adminUserType, "SECONDARY",
                            secDefaultPwd, CommonConstants.INACTIVE, createdBy);
                    secondaryUser.setParentUserId(kalAdminUserId);
                    secondaryUser.setRoleId(adminRoleId);

                    ReconUser savedSecondaryUser = reconUserRepository.saveAndFlush(secondaryUser);
                    savePasswordHistory(savedSecondaryUser, createdBy);

                    saveAuditLog("RCN_RECON_USER", savedSecondaryUser.getUserId(), "CREATE", null,
                            "userType=" + adminUserType + ",contactRank=SECONDARY,status=INACTIVE",
                            createdBy, adminUserType, savedPrimaryBank.getBankId(),
                            "Secondary " + adminUserType + " created (INACTIVE): " + secUsername);

                    logger.info("Secondary {} created (INACTIVE): {} for bank {}", adminUserType, secUsername, generatedCode);
                }

                logger.info("Primary {} created and welcome email sent: {} for bank {}", adminUserType, username, generatedCode);
            } catch (Exception e) {
                logger.error("Failed to create admin users for bank {}: {}", generatedCode, e.getMessage(), e);
            }
        }

        logger.info("ReconBankMaster created: {} by {}", generatedCode, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS,
                        (isBranch ? "Branch" : "Bank") + " created successfully.", Collections.singletonList(savedPrimaryBank)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllBanks() {
        List<ReconBankMaster> banks = reconBankMasterRepository.findAllPrimary();
        banks.forEach(this::enrichWithProducts);
        banks.forEach(this::enrichWithReplacement);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Banks fetched.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBankById(Long bankId) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findPrimaryById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster found = opt.get();
        enrichWithProducts(found);
        enrichForEditAndDetail(found);
        enrichWithReplacement(found);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank found.", Collections.singletonList(found)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBankByCode(String bankCode) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findByBankCode(bankCode);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with code: " + bankCode, null));
        }
        ReconBankMaster found = opt.get();
        enrichWithProducts(found);
        enrichForEditAndDetail(found);
        enrichWithReplacement(found);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank found.", Collections.singletonList(found)));
    }

    /**
     * Surfaces the CURRENTLY acting admin's contact info on a bank/branch row for "View" and list
     * screens (OrgOverview/OrgHierarchy/OrgBankBranches — they all read fullName/email/
     * mobileNumber/bankAdminUsername straight off this row, plus replacementStatus/
     * replacedByUsername to label it "(Replaced)"; see ReconBankMasterController
     * .getAllBankAdmins() for the per-admin-row equivalent of this same shape).
     *
     * While a replacement is ACTIVE (the common state right after a replace, before the original
     * is ever permanently blocked), RECON_BANK_MASTER's own stored contact columns still hold the
     * ORIGINAL's info — they are only overwritten once AuditReplacementServiceImpl
     * .onOriginalBlocked() finalizes the replacement (original permanently BLOCKED). Every "View"
     * screen that read the bank row directly therefore showed stale, original data throughout
     * that whole ACTIVE window. This closes that gap by mirroring the same field-mapping
     * onOriginalBlocked() uses, in-memory only (never persisted) and only while ACTIVE — once
     * FINALIZED, the bank row's own columns are already correct and this leaves them untouched.
     */
    private void enrichWithReplacement(ReconBankMaster bank) {
        bank.setReplacementAdminRow(false);
        bank.setReplacementStatus(null);
        bank.setReplacedByUsername(null);
        if (bank.getBankAdminUsername() == null) return;
        reconUserRepository.findByUsername(bank.getBankAdminUsername()).ifPresent(admin ->
                auditReplacementRepository.findByOriginalUserIdAndStatus(admin.getUserId(), CommonConstants.ACTIVE)
                        .ifPresent(rep -> reconUserRepository.findById(rep.getReplacementUserId()).ifPresent(repUser -> {
                            bank.setFullName(rep.getPendingFullName() != null ? rep.getPendingFullName() : repUser.getUsername());
                            bank.setEmail(repUser.getEmail());
                            bank.setMobileNumber(rep.getPendingMobile() != null ? rep.getPendingMobile() : repUser.getMobileNumber());
                            bank.setBankAdminUsername(repUser.getUsername());
                            bank.setReplacementStatus(CommonConstants.ACTIVE);
                            bank.setReplacedByUsername(repUser.getUsername());
                        })));
    }

    // Populates fields that are stored separately/combined in the DB but that the
    // Detail view and Edit form need split back out — secondary contact (a separate
    // row), the registered address (surfaced whole into Line 1, no lossy re-split of
    // free text), and the registered phone (split back into code/city/number, the
    // reverse of mapAddressAndPhoneTransients()'s join).
    private void enrichForEditAndDetail(ReconBankMaster bank) {
        reconBankMasterRepository.findByBankCodeAndContactRank(bank.getBankCode(), "SECONDARY")
                .ifPresent(sec -> {
                    bank.setSecondaryFullName(sec.getFullName());
                    bank.setSecondaryEmail(sec.getEmail());
                    bank.setSecondaryMobileNumber(sec.getMobileNumber());
                    bank.setSecondaryAltMobile(sec.getAltMobileNumber());
                });

        if (bank.getRegAddress() != null && !bank.getRegAddress().isEmpty()) {
            bank.setRegAddressLine1(bank.getRegAddress());
        }

        if (bank.getRegPhone() != null && bank.getRegPhone().contains("-")) {
            String[] parts = bank.getRegPhone().split("-", 3);
            if (parts.length == 3) {
                bank.setRegPhoneCode(parts[0]);
                bank.setRegCityCode(parts[1]);
                bank.setRegPhone(parts[2]);
            }
        }
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBanksByStatus(String status) {
        List<ReconBankMaster> banks = reconBankMasterRepository.findByStatus(status);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Banks fetched by status.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBanksByType(String bankType) {
        List<ReconBankMaster> banks = reconBankMasterRepository.findByBankType(bankType);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Banks fetched by type.", banks));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBranchBanks(Long parentBankId) {
        List<ReconBankMaster> banks = reconBankMasterRepository.findByParentBankId(parentBankId);
        banks.forEach(this::enrichWithProducts);
        banks.forEach(this::enrichWithReplacement);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Branch banks fetched.", banks));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateBank(Long bankId, ReconBankMaster bank, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        boolean isBranch = existing.getParentBankId() != null;
        String adminUserType = isBranch ? "BRANCH_ADMIN" : "BANK_ADMIN";

        // ── Server-side email-exists validation (mirrors /check-email; excludes this record's own current email) ──
        String newPrimaryEmail = bank.getEmail() != null ? bank.getEmail().trim().toLowerCase() : null;
        if (newPrimaryEmail != null && !newPrimaryEmail.isEmpty()
                && !newPrimaryEmail.equalsIgnoreCase(existing.getEmail())
                && emailRegistry.isActivelyRegistered(newPrimaryEmail)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "\"" + newPrimaryEmail + "\" is already registered.", null));
        }
        ReconBankMaster secondaryRow = reconBankMasterRepository
                .findByBankCodeAndContactRank(existing.getBankCode(), "SECONDARY").orElse(null);
        String newSecondaryEmail = bank.getSecondaryEmail() != null ? bank.getSecondaryEmail().trim().toLowerCase() : null;
        if (newSecondaryEmail != null && !newSecondaryEmail.isEmpty()
                && (secondaryRow == null || !newSecondaryEmail.equalsIgnoreCase(secondaryRow.getEmail()))
                && emailRegistry.isActivelyRegistered(newSecondaryEmail)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "\"" + newSecondaryEmail + "\" is already registered.", null));
        }

        mapAddressAndPhoneTransients(bank);

        // ── Snapshot OLD values for the change-notification diff ─────────────
        enrichWithProducts(existing);
        final String oldBankNameShort = existing.getBankNameShort();
        final String oldRegAddress    = existing.getRegAddress();
        final String oldRegCity       = existing.getRegCity();
        final String oldRegState      = existing.getRegState();
        final String oldRegCountry    = existing.getRegCountry();
        final String oldRegPhone      = existing.getRegPhone();
        final String oldFullName      = existing.getFullName();
        final String oldEmail         = existing.getEmail();
        final String oldMobile        = existing.getMobileNumber();
        final String oldAltMobile     = existing.getAltMobileNumber();
        final Boolean oldMfa          = existing.getEnableMfa();
        final Boolean oldOtp          = existing.getEnableOtp();
        final Boolean oldHrms         = existing.getEnableHrms();
        final String oldSecFullName   = secondaryRow != null ? secondaryRow.getFullName() : null;
        final String oldSecEmail      = secondaryRow != null ? secondaryRow.getEmail() : null;
        final String oldSecMobile     = secondaryRow != null ? secondaryRow.getMobileNumber() : null;
        final String oldSecAltMobile  = secondaryRow != null ? secondaryRow.getAltMobileNumber() : null;
        final List<String> oldProducts = existing.getSelectedProducts() != null
                ? new ArrayList<>(existing.getSelectedProducts()) : new ArrayList<>();

        // ── Apply updates to PRIMARY row (protected fields — code/status/logo/admin
        //    username/default password/created* — are never touched here) ─────────
        if (bank.getBankName() != null)       existing.setBankName(bank.getBankName());
        if (bank.getBankNameShort() != null)  existing.setBankNameShort(bank.getBankNameShort());
        if (bank.getBankCategory() != null)   existing.setBankCategory(bank.getBankCategory());
        if (bank.getBankType() != null)       existing.setBankType(bank.getBankType());
        if (bank.getRegAddress() != null)     existing.setRegAddress(bank.getRegAddress());
        if (bank.getRegCity() != null)        existing.setRegCity(bank.getRegCity());
        if (bank.getRegState() != null)       existing.setRegState(bank.getRegState());
        if (bank.getRegCountry() != null)     existing.setRegCountry(bank.getRegCountry());
        if (bank.getRegPhone() != null)       existing.setRegPhone(bank.getRegPhone());
        if (bank.getFullName() != null)       existing.setFullName(bank.getFullName());
        if (newPrimaryEmail != null && !newPrimaryEmail.isEmpty()) existing.setEmail(newPrimaryEmail);
        if (bank.getMobileNumber() != null)   existing.setMobileNumber(bank.getMobileNumber());
        if (bank.getAltMobileNumber() != null) existing.setAltMobileNumber(bank.getAltMobileNumber());
        if (bank.getEnableMfa() != null)      existing.setEnableMfa(bank.getEnableMfa());
        if (bank.getEnableOtp() != null)      existing.setEnableOtp(bank.getEnableOtp());
        if (bank.getEnableHrms() != null)     existing.setEnableHrms(bank.getEnableHrms());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);

        // ── Sync the linked PRIMARY admin ReconUser's contact info (non-fatal) ───
        try {
            reconUserRepository.findByBankIdAndUserTypeAndContactRank(bankId, adminUserType, "PRIMARY")
                    .ifPresent(admin -> {
                        boolean changed = false;
                        if (bank.getFullName() != null && !bank.getFullName().equals(admin.getFullName())) {
                            admin.setFullName(bank.getFullName()); changed = true;
                        }
                        if (newPrimaryEmail != null && !newPrimaryEmail.isEmpty() && !newPrimaryEmail.equalsIgnoreCase(admin.getEmail())) {
                            admin.setEmail(newPrimaryEmail); changed = true;
                        }
                        if (bank.getMobileNumber() != null && !bank.getMobileNumber().equals(admin.getMobileNumber())) {
                            admin.setMobileNumber(bank.getMobileNumber()); changed = true;
                        }
                        if (changed) {
                            admin.setUpdatedAt(LocalDateTime.now());
                            admin.setUpdatedBy(updatedBy);
                            reconUserRepository.save(admin);
                        }
                    });
        } catch (Exception e) {
            logger.warn("Primary admin contact sync failed for bankId {}: {}", bankId, e.getMessage());
        }

        // ── Update / create SECONDARY row + sync its admin account ───────────────
        if (newSecondaryEmail != null && !newSecondaryEmail.isEmpty()) {
            if (secondaryRow != null) {
                if (bank.getSecondaryFullName() != null) secondaryRow.setFullName(bank.getSecondaryFullName());
                secondaryRow.setEmail(newSecondaryEmail);
                if (bank.getSecondaryMobileNumber() != null) secondaryRow.setMobileNumber(bank.getSecondaryMobileNumber());
                if (bank.getSecondaryAltMobile() != null) secondaryRow.setAltMobileNumber(bank.getSecondaryAltMobile());
                secondaryRow.setUpdatedAt(LocalDateTime.now());
                secondaryRow.setUpdatedBy(updatedBy);
                reconBankMasterRepository.save(secondaryRow);

                try {
                    reconUserRepository.findByBankIdAndUserTypeAndContactRank(bankId, adminUserType, "SECONDARY")
                            .ifPresent(admin -> {
                                boolean changed = false;
                                if (bank.getSecondaryFullName() != null && !bank.getSecondaryFullName().equals(admin.getFullName())) {
                                    admin.setFullName(bank.getSecondaryFullName()); changed = true;
                                }
                                if (!newSecondaryEmail.equalsIgnoreCase(admin.getEmail())) {
                                    admin.setEmail(newSecondaryEmail); changed = true;
                                }
                                if (bank.getSecondaryMobileNumber() != null && !bank.getSecondaryMobileNumber().equals(admin.getMobileNumber())) {
                                    admin.setMobileNumber(bank.getSecondaryMobileNumber()); changed = true;
                                }
                                if (changed) {
                                    admin.setUpdatedAt(LocalDateTime.now());
                                    admin.setUpdatedBy(updatedBy);
                                    reconUserRepository.save(admin);
                                }
                            });
                } catch (Exception e) {
                    logger.warn("Secondary admin contact sync failed for bankId {}: {}", bankId, e.getMessage());
                }
            } else {
                // No secondary contact existed before — create it now (row + INACTIVE admin), mirroring createBank()
                try {
                    ReconBankMaster newSecondaryRow = new ReconBankMaster();
                    newSecondaryRow.setBankCode(existing.getBankCode());
                    newSecondaryRow.setBankName(existing.getBankName());
                    newSecondaryRow.setBankType(existing.getBankType());
                    newSecondaryRow.setParentBankId(existing.getParentBankId());
                    newSecondaryRow.setContactRank("SECONDARY");
                    newSecondaryRow.setFullName(bank.getSecondaryFullName());
                    newSecondaryRow.setEmail(newSecondaryEmail);
                    newSecondaryRow.setMobileNumber(bank.getSecondaryMobileNumber());
                    newSecondaryRow.setAltMobileNumber(bank.getSecondaryAltMobile());
                    newSecondaryRow.setStatus(CommonConstants.INACTIVE);
                    newSecondaryRow.setCreatedAt(LocalDateTime.now());
                    newSecondaryRow.setCreatedBy(updatedBy);
                    reconBankMasterRepository.save(newSecondaryRow);

                    String secUsername   = deriveUsername(newSecondaryEmail, bank.getSecondaryFullName());
                    String secDefaultPwd = generatePassword(10);
                    ReconUser secondaryUser = buildAdminUser(bankId, bank.getSecondaryFullName(), newSecondaryEmail,
                            bank.getSecondaryMobileNumber(), secUsername, adminUserType, "SECONDARY",
                            secDefaultPwd, CommonConstants.INACTIVE, updatedBy);
                    reconUserRepository.findByBankIdAndUserTypeAndContactRank(bankId, adminUserType, "PRIMARY")
                            .ifPresent(primaryAdmin -> secondaryUser.setParentUserId(primaryAdmin.getParentUserId()));
                    ReconUser savedSecondaryUser = reconUserRepository.saveAndFlush(secondaryUser);
                    savePasswordHistory(savedSecondaryUser, updatedBy);
                    logger.info("Secondary {} created (INACTIVE) via update: {} for bank {}", adminUserType, secUsername, existing.getBankCode());
                } catch (Exception e) {
                    logger.error("Failed to create secondary contact during update for bankId {}: {}", bankId, e.getMessage(), e);
                }
            }
        }

        // ── Products ───────────────────────────────────────────────────────────
        if (bank.getSelectedProducts() != null && !bank.getSelectedProducts().isEmpty()) {
            saveProductMappings(bankId, bank, updatedBy);
        }

        // ── Build per-section diff & notify primary contact (only if something changed) ──
        Map<String, List<String>> sections = new LinkedHashMap<>();
        List<String> addressDiffs = new ArrayList<>();
        diffF("Institution Name (Short)", oldBankNameShort, existing.getBankNameShort(), addressDiffs);
        diffF("Registered Address", oldRegAddress, existing.getRegAddress(), addressDiffs);
        diffF("City", oldRegCity, existing.getRegCity(), addressDiffs);
        diffF("State", oldRegState, existing.getRegState(), addressDiffs);
        diffF("Country", oldRegCountry, existing.getRegCountry(), addressDiffs);
        diffF("Phone", oldRegPhone, existing.getRegPhone(), addressDiffs);
        if (!addressDiffs.isEmpty()) sections.put("Registered Address", addressDiffs);

        List<String> contactDiffs = new ArrayList<>();
        diffF("Primary Full Name", oldFullName, existing.getFullName(), contactDiffs);
        diffF("Primary Email", oldEmail, existing.getEmail(), contactDiffs);
        diffF("Primary Mobile", oldMobile, existing.getMobileNumber(), contactDiffs);
        diffF("Primary Alt. Mobile", oldAltMobile, existing.getAltMobileNumber(), contactDiffs);
        String finalSecFullName  = secondaryRow != null ? secondaryRow.getFullName() : bank.getSecondaryFullName();
        String finalSecEmail     = secondaryRow != null ? secondaryRow.getEmail() : newSecondaryEmail;
        String finalSecMobile    = secondaryRow != null ? secondaryRow.getMobileNumber() : bank.getSecondaryMobileNumber();
        String finalSecAltMobile = secondaryRow != null ? secondaryRow.getAltMobileNumber() : bank.getSecondaryAltMobile();
        diffF("Secondary Full Name", oldSecFullName, finalSecFullName, contactDiffs);
        diffF("Secondary Email", oldSecEmail, finalSecEmail, contactDiffs);
        diffF("Secondary Mobile", oldSecMobile, finalSecMobile, contactDiffs);
        diffF("Secondary Alt. Mobile", oldSecAltMobile, finalSecAltMobile, contactDiffs);
        if (!contactDiffs.isEmpty()) sections.put("Contact Details", contactDiffs);

        List<String> securityDiffs = new ArrayList<>();
        diffB("Multi-Factor Authentication", oldMfa, existing.getEnableMfa(), securityDiffs);
        diffB("OTP Verification", oldOtp, existing.getEnableOtp(), securityDiffs);
        diffB("HRMS Integration", oldHrms, existing.getEnableHrms(), securityDiffs);
        if (!securityDiffs.isEmpty()) sections.put("Security & Compliance Settings", securityDiffs);

        List<String> productDiffs = diffProducts(oldProducts, bank.getSelectedProducts());
        if (!productDiffs.isEmpty()) sections.put("Product Subscriptions & Validity Dates", productDiffs);

        String updatedAtFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        if (!sections.isEmpty()) {
            try {
                if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                    emailService.sendBankUpdateNotification(existing.getEmail(), existing.getFullName(),
                            existing.getBankName(), existing.getBankCode(), updatedAtFormatted, sections);
                }
            } catch (Exception e) {
                logger.warn("Bank update notification email failed for {}: {}", existing.getBankCode(), e.getMessage());
            }
        }
        notifyActor(updatedBy, "Updated", existing.getBankName(), existing.getBankCode(), null);

        String changeSummary = sections.values().stream().flatMap(List::stream).collect(Collectors.joining("; "));
        saveAuditLog("RECON_BANK_MASTER", bankId, "UPDATE", null,
                changeSummary.isEmpty() ? "No field changes" : changeSummary,
                updatedBy, adminUserType, bankId, (isBranch ? "Branch" : "Bank") + " updated");
        logger.info("ReconBankMaster updated: {} by {}", bankId, updatedBy);
        enrichWithProducts(existing);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS,
                (isBranch ? "Branch" : "Bank") + " updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status, String updatedBy) {
        // INACTIVE_PENDING / ACTIVE_PENDING / BLOCK_PENDING are intentionally NOT
        // settable here — each requires a scheduledAt time, which only
        // scheduleInactivate()/scheduleReactivate()/scheduleBlock() capture.
        // Setting them directly would leave the bank stuck forever, since the
        // scheduler only picks up rows with a real *_SCHEDULED_AT value.
        List<String> validStatuses = Arrays.asList(
                "REQUEST", CommonConstants.VERIFIED, CommonConstants.ACTIVE, CommonConstants.INACTIVE, "BLOCKED");
        if (status == null || !validStatuses.contains(status.toUpperCase())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Invalid status. Allowed: " + validStatuses, null));
        }
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        String oldStatus = existing.getStatus();
        String newStatus = status.toUpperCase();

        // BLOCKED is permanent — only unblockBank() can restore
        if ("BLOCKED".equals(oldStatus)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "This institution is permanently BLOCKED. Its status cannot be changed.", null));
        }

        // Transition validation
        Map<String, List<String>> allowedTransitions = new LinkedHashMap<>();
        allowedTransitions.put("REQUEST",          Arrays.asList(CommonConstants.VERIFIED, CommonConstants.ACTIVE, CommonConstants.INACTIVE, "BLOCKED"));
        allowedTransitions.put(CommonConstants.VERIFIED,         Arrays.asList(CommonConstants.ACTIVE, CommonConstants.INACTIVE, "BLOCKED"));
        allowedTransitions.put(CommonConstants.ACTIVE,           Arrays.asList(CommonConstants.INACTIVE, "BLOCKED"));
        allowedTransitions.put(CommonConstants.INACTIVE,         Arrays.asList(CommonConstants.ACTIVE, "BLOCKED"));
        // No entries for INACTIVE_PENDING / ACTIVE_PENDING / BLOCK_PENDING as
        // sources — a bank in a PENDING state must leave it via
        // cancelSchedule(), which properly clears the *_SCHEDULED_AT fields.
        // Allowing a plain updateStatus() override here would leave those
        // fields stale, and the scheduler would later re-trigger the pending
        // action since it only checks *_SCHEDULED_AT, not the current status.
        List<String> allowed = allowedTransitions.getOrDefault(oldStatus, new ArrayList<>());
        if (!allowed.contains(newStatus)) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Cannot change status from '" + oldStatus + "' to '" + newStatus + "'. Allowed: " + allowed, null));
        }

        // Reactivation cooldown — can't go ACTIVE within 30s of becoming INACTIVE
        if (CommonConstants.ACTIVE.equals(newStatus) && CommonConstants.INACTIVE.equals(oldStatus) && existing.getInactivatedAt() != null) {
            LocalDateTime allowedAfter = existing.getInactivatedAt().plusSeconds(30);
            if (LocalDateTime.now().isBefore(allowedAfter)) {
                long secsLeft = java.time.Duration.between(LocalDateTime.now(), allowedAfter).getSeconds();
                return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                        "Cannot mark Active yet. Institution was recently made Inactive. Please wait " + secsLeft + " more second(s).", null));
            }
        }

        existing.setStatus(newStatus);
        if (CommonConstants.INACTIVE.equals(newStatus)) {
            existing.setInactivatedAt(LocalDateTime.now());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);

        // Sync status to this institution's admin users (BANK_ADMIN / BRANCH_ADMIN)
        syncAdminStatus(bankId, newStatus, updatedBy);

        saveAuditLog("RECON_BANK_MASTER", bankId, "STATUS_CHANGE", oldStatus, newStatus, updatedBy, null, bankId,
                "Status changed: " + oldStatus + " → " + newStatus);
        logger.info("ReconBankMaster status updated to {} for ID: {} by {}", newStatus, bankId, updatedBy);

        // Notify institution primary contact + actor
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendStatusChangeNotification(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), oldStatus, newStatus);
            }
        } catch (Exception e) {
            logger.warn("Status change email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(updatedBy, "Status Changed to " + newStatus, existing.getBankName(), existing.getBankCode(), null);

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank status updated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteBank(Long bankId) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        String prevStatus = existing.getStatus();
        existing.setStatus(CommonConstants.INACTIVE);
        existing.setUpdatedAt(LocalDateTime.now());
        reconBankMasterRepository.save(existing);
        syncAdminStatus(bankId, CommonConstants.INACTIVE, CommonConstants.SYSTEM);
        saveAuditLog("RECON_BANK_MASTER", bankId, "DELETE", prevStatus, CommonConstants.INACTIVE, null, null, bankId,
                "Bank soft-deleted (INACTIVE)");
        logger.info("ReconBankMaster soft-deleted (INACTIVE): {}", bankId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank deactivated successfully.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkBankCodeExists(String bankCode) {
        boolean exists = reconBankMasterRepository.existsByBankCode(bankCode);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkBankNameExists(String bankName) {
        boolean exists = reconBankMasterRepository.existsByBankName(bankName);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, exists ? "EXISTS" : "AVAILABLE", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> blockBank(Long bankId, String reason, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank is already BLOCKED.", null));
        }
        String preBlock = existing.getStatus();
        existing.setPreBlockStatus(preBlock);
        existing.setStatus("BLOCKED");
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);

        // Cascade BLOCKED to all users of this institution
        cascadeBlockUsersNow(bankId, reason, updatedBy);

        // If top-level bank: cascade BLOCKED to all branches + their users
        if (existing.getParentBankId() == null) {
            List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(bankId);
            for (ReconBankMaster branch : branches) {
                if ("BLOCKED".equals(branch.getStatus())) continue;
                branch.setPreBlockStatus(branch.getStatus());
                branch.setStatus("BLOCKED");
                branch.setBlockReason(reason);
                branch.setUpdatedAt(LocalDateTime.now());
                branch.setUpdatedBy(updatedBy);
                reconBankMasterRepository.save(branch);
                cascadeBlockUsersNow(branch.getBankId(), reason, updatedBy);
                try {
                    if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                        emailService.sendBranchBankStatusNotification(branch.getEmail(), branch.getFullName(),
                                branch.getBankName(), branch.getBankCode(), branch.getPreBlockStatus(), "BLOCKED",
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Branch block cascade email failed for {}: {}", branch.getBankCode(), e.getMessage());
                }
            }
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "BLOCK", preBlock, "BLOCKED", updatedBy, null, bankId,
                "Bank BLOCKED. Reason: " + reason);
        logger.info("ReconBankMaster BLOCKED: bankId={} preBlockStatus={} by {}", bankId, preBlock, updatedBy);

        // Notify institution primary contact + actor
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendStatusChangeNotification(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), preBlock, "BLOCKED");
            }
        } catch (Exception e) {
            logger.warn("Block email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(updatedBy, "Blocked", existing.getBankName(), existing.getBankCode(), null);

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank blocked successfully.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> unblockBank(Long bankId, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        if (!"BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank is not currently BLOCKED.", null));
        }
        String restoreStatus = existing.getPreBlockStatus() != null ? existing.getPreBlockStatus() : CommonConstants.ACTIVE;
        existing.setStatus(restoreStatus);
        existing.setPreBlockStatus(null);
        existing.setBlockReason(null);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);

        // Restore all BLOCKED users of this institution to their pre-block status
        cascadeUnblockUsers(bankId, updatedBy);

        // If top-level bank: restore blocked branches + their users
        if (existing.getParentBankId() == null) {
            List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(bankId);
            for (ReconBankMaster branch : branches) {
                if (!"BLOCKED".equals(branch.getStatus())) continue;
                String branchRestored = branch.getPreBlockStatus() != null ? branch.getPreBlockStatus() : CommonConstants.ACTIVE;
                branch.setStatus(branchRestored);
                branch.setPreBlockStatus(null);
                branch.setBlockReason(null);
                branch.setUpdatedAt(LocalDateTime.now());
                branch.setUpdatedBy(updatedBy);
                reconBankMasterRepository.save(branch);
                cascadeUnblockUsers(branch.getBankId(), updatedBy);
                try {
                    if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                        emailService.sendBranchBankBlockCancelled(branch.getEmail(), branch.getFullName(),
                                branch.getBankName(), branch.getBankCode(),
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Branch unblock email failed for {}: {}", branch.getBankCode(), e.getMessage());
                }
            }
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "UNBLOCK", "BLOCKED", restoreStatus, updatedBy, null, bankId,
                "Bank UNBLOCKED, status restored to: " + restoreStatus);
        logger.info("ReconBankMaster UNBLOCKED: bankId={} restored to {} by {}", bankId, restoreStatus, updatedBy);

        // Notify institution primary contact + actor
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendBlockCancelled(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), restoreStatus);
            }
        } catch (Exception e) {
            logger.warn("Unblock email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(updatedBy, "Unblocked", existing.getBankName(), existing.getBankCode(), null);

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank unblocked. Status restored to: " + restoreStatus, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleInactivate(Long bankId, LocalDateTime scheduledAt, String scheduledBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Scheduled time must be in the future.", null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus()) || "BLOCK_PENDING".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Cannot schedule inactivation — institution is " + existing.getStatus() + ".", null));
        }
        if (CommonConstants.INACTIVE.equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Institution is already INACTIVE.", null));
        }
        String oldStatus = existing.getStatus();
        existing.setStatus("INACTIVE_PENDING");
        existing.setInactivateScheduledAt(scheduledAt);
        existing.setInactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);

        // Cascade INACTIVE_PENDING to all active users of this institution (warning popup on their side)
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        for (ReconUser u : users) {
            if (CommonConstants.INACTIVE.equals(u.getStatus()) || "BLOCKED".equals(u.getStatus())
                    || "BLOCK_PENDING".equals(u.getStatus())) continue;
            u.setStatus("INACTIVE_PENDING");
            u.setInactivateScheduledAt(scheduledAt);
            u.setInactivateScheduledBy("CASCADE:" + scheduledBy);
            u.setUpdatedAt(LocalDateTime.now());
            u.setUpdatedBy(scheduledBy);
            reconUserRepository.save(u);
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "SCHEDULE", oldStatus, "INACTIVATE@" + scheduledAt, scheduledBy, null, bankId,
                "Inactivation scheduled at: " + scheduledAt);
        logger.info("ReconBankMaster INACTIVATE scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);

        // Warning email to institution primary contact + actor confirmation
        String atFormatted = scheduledAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendInactivatePendingWarning(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), atFormatted);
            }
        } catch (Exception e) {
            logger.warn("Inactivate warning email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(scheduledBy, "Inactivation Scheduled", existing.getBankName(), existing.getBankCode(), atFormatted);

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank inactivation scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleReactivate(Long bankId, LocalDateTime scheduledAt, String scheduledBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Scheduled time must be in the future.", null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus()) || "BLOCK_PENDING".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Cannot schedule reactivation — institution is " + existing.getStatus() + ".", null));
        }
        if (CommonConstants.ACTIVE.equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Institution is already ACTIVE.", null));
        }
        String oldStatus = existing.getStatus();
        existing.setStatus("ACTIVE_PENDING");
        existing.setReactivateScheduledAt(scheduledAt);
        existing.setReactivateScheduledBy(scheduledBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);

        // Cascade ACTIVE_PENDING to inactive users of this institution
        List<ReconUser> users = reconUserRepository.findByBankId(bankId);
        for (ReconUser u : users) {
            if (!CommonConstants.INACTIVE.equals(u.getStatus()) && !"INACTIVE_PENDING".equals(u.getStatus())) continue;
            u.setStatus("ACTIVE_PENDING");
            u.setReactivateScheduledAt(scheduledAt);
            u.setReactivateScheduledBy("CASCADE:" + scheduledBy);
            u.setInactivateScheduledAt(null);
            u.setInactivateScheduledBy(null);
            u.setUpdatedAt(LocalDateTime.now());
            u.setUpdatedBy(scheduledBy);
            reconUserRepository.save(u);
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "SCHEDULE", oldStatus, "REACTIVATE@" + scheduledAt, scheduledBy, null, bankId,
                "Reactivation scheduled at: " + scheduledAt);
        logger.info("ReconBankMaster REACTIVATE scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);

        // Notification email to institution primary contact + actor confirmation
        String atFormatted = scheduledAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendReactivatePendingNotification(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), atFormatted);
            }
        } catch (Exception e) {
            logger.warn("Reactivate pending email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(scheduledBy, "Reactivation Scheduled", existing.getBankName(), existing.getBankCode(), atFormatted);

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank reactivation scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, LocalDateTime scheduledAt, String scheduledBy, String reason) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Scheduled time must be in the future.", null));
        }
        ReconBankMaster existing = opt.get();
        if ("BLOCKED".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Institution is already permanently BLOCKED.", null));
        }
        if ("BLOCK_PENDING".equals(existing.getStatus())) {
            return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Block is already scheduled for this institution.", null));
        }
        String oldStatus = existing.getStatus();
        existing.setPreBlockStatus(oldStatus);
        existing.setStatus("BLOCK_PENDING");
        existing.setBlockScheduledAt(scheduledAt);
        existing.setBlockScheduledBy(scheduledBy);
        existing.setBlockReason(reason);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(scheduledBy);
        reconBankMasterRepository.save(existing);

        String atFormatted = scheduledAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // Cascade BLOCK_PENDING to all users of this institution + warning email each
        cascadeScheduleBlockUsers(bankId, scheduledAt, scheduledBy, reason,
                existing.getBankName(), existing.getBankCode(), atFormatted, existing.getEmail());

        // If top-level bank: cascade BLOCK_PENDING to all branches + their users
        if (existing.getParentBankId() == null) {
            List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(bankId);
            for (ReconBankMaster branch : branches) {
                if ("BLOCKED".equals(branch.getStatus()) || "BLOCK_PENDING".equals(branch.getStatus())) continue;
                branch.setPreBlockStatus(branch.getStatus());
                branch.setStatus("BLOCK_PENDING");
                branch.setBlockScheduledAt(scheduledAt);
                branch.setBlockScheduledBy("CASCADE:" + scheduledBy);
                branch.setBlockReason(reason);
                branch.setUpdatedAt(LocalDateTime.now());
                branch.setUpdatedBy(scheduledBy);
                reconBankMasterRepository.save(branch);
                cascadeScheduleBlockUsers(branch.getBankId(), scheduledAt, scheduledBy, reason,
                        branch.getBankName(), branch.getBankCode(), atFormatted, null);
                try {
                    if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                        emailService.sendBranchBankBlockWarning(branch.getEmail(), branch.getFullName(),
                                branch.getBankName(), branch.getBankCode(),
                                existing.getBankName(), existing.getBankCode(), atFormatted);
                    }
                } catch (Exception e) {
                    logger.warn("Branch block warning email failed for {}: {}", branch.getBankCode(), e.getMessage());
                }
            }
        }

        saveAuditLog("RECON_BANK_MASTER", bankId, "SCHEDULE", oldStatus, "BLOCK@" + scheduledAt, scheduledBy, null, bankId,
                "Block scheduled at: " + scheduledAt + ". Reason: " + reason);
        logger.info("ReconBankMaster BLOCK scheduled at {} for bankId={} by {}", scheduledAt, bankId, scheduledBy);

        // Warning email to institution primary contact + actor confirmation
        try {
            if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                emailService.sendBlockWarning(existing.getEmail(), existing.getFullName(),
                        existing.getBankName(), existing.getBankCode(), atFormatted);
            }
        } catch (Exception e) {
            logger.warn("Block warning email failed for {}: {}", existing.getBankCode(), e.getMessage());
        }
        notifyActor(scheduledBy, "Block Scheduled", existing.getBankName(), existing.getBankCode(), atFormatted);

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank block scheduled at: " + scheduledAt, null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> cancelSchedule(Long bankId, String scheduleType, String updatedBy) {
        Optional<ReconBankMaster> opt = reconBankMasterRepository.findById(bankId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        ReconBankMaster existing = opt.get();
        if (scheduleType == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "scheduleType is required (INACTIVATE / REACTIVATE / BLOCK).", null));
        }
        // Only the user who scheduled this institution-level action, or one of their ancestors, may
        // cancel it — a child can never override a parent's action, and an unrelated parent of the
        // affected users can never touch a pending action they didn't initiate.
        String bankActioner;
        switch (scheduleType.toUpperCase()) {
            case "REACTIVATE": bankActioner = existing.getReactivateScheduledBy(); break;
            case "BLOCK":      bankActioner = existing.getBlockScheduledBy();      break;
            default:           bankActioner = existing.getInactivateScheduledBy(); break; // INACTIVATE
        }
        if (bankActioner == null) {
            // No one currently scheduled this action — either it already finalized
            // (the scheduler clears *_SCHEDULED_BY on finalize) or it was never
            // scheduled/already cancelled. Either way "only the initiator can undo"
            // is misleading here since there is no pending action left to undo.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RestWithStatusList(CommonConstants.FAILURE,
                    "No pending " + scheduleType.toUpperCase() + " schedule found for this bank — it may have already finalized or been cancelled.", null));
        }
        ReconUser cancelCaller = hierarchyScope.caller(updatedBy).orElse(null);
        if (!hierarchyScope.canOverrideActionBy(cancelCaller, bankActioner)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new RestWithStatusList(CommonConstants.FAILURE,
                    "Only the user who initiated this action, or someone above them, can undo it.", null));
        }
        switch (scheduleType.toUpperCase()) {
            case "INACTIVATE":
                existing.setInactivateScheduledAt(null);
                existing.setInactivateScheduledBy(null);
                // Restore status + cascade users back to ACTIVE
                if ("INACTIVE_PENDING".equals(existing.getStatus())) {
                    existing.setStatus(CommonConstants.ACTIVE);
                    for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                        if (!"INACTIVE_PENDING".equals(u.getStatus())) continue;
                        u.setStatus(CommonConstants.ACTIVE);
                        u.setInactivateScheduledAt(null);
                        u.setInactivateScheduledBy(null);
                        u.setUpdatedAt(LocalDateTime.now());
                        u.setUpdatedBy(updatedBy);
                        reconUserRepository.save(u);
                        // Undoing the bank/branch-level inactivation goes through this
                        // path (not UserStatusServiceImpl.undoInactivate), so any pending
                        // replacement scheduled against this admin must be cancelled here too
                        // — otherwise it's left stuck at PENDING forever.
                        try {
                            replacementService.cancelPendingReplacement(u.getUserId());
                        } catch (Exception e) {
                            logger.warn("cancelPendingReplacement failed for userId {}: {}", u.getUserId(), e.getMessage());
                        }
                        try {
                            delegationService.cancelDelegation(u.getUserId(), updatedBy);
                        } catch (Exception e) {
                            logger.warn("cancelDelegation failed for userId {}: {}", u.getUserId(), e.getMessage());
                        }
                    }
                }
                try {
                    if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                        emailService.sendInactivateCancelled(existing.getEmail(), existing.getFullName(),
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Inactivate cancel email failed for {}: {}", existing.getBankCode(), e.getMessage());
                }
                break;
            case "REACTIVATE":
                existing.setReactivateScheduledAt(null);
                existing.setReactivateScheduledBy(null);
                // Restore status + cascade users back to INACTIVE
                if ("ACTIVE_PENDING".equals(existing.getStatus())) {
                    existing.setStatus(CommonConstants.INACTIVE);
                    for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                        if (!"ACTIVE_PENDING".equals(u.getStatus()) || u.getReactivateScheduledAt() == null) continue;
                        u.setStatus(CommonConstants.INACTIVE);
                        u.setReactivateScheduledAt(null);
                        u.setReactivateScheduledBy(null);
                        u.setUpdatedAt(LocalDateTime.now());
                        u.setUpdatedBy(updatedBy);
                        reconUserRepository.save(u);
                    }
                }
                try {
                    if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                        emailService.sendReactivateCancelled(existing.getEmail(), existing.getFullName(),
                                existing.getBankName(), existing.getBankCode());
                    }
                } catch (Exception e) {
                    logger.warn("Reactivate cancel email failed for {}: {}", existing.getBankCode(), e.getMessage());
                }
                break;
            case "BLOCK":
                existing.setBlockScheduledAt(null);
                existing.setBlockScheduledBy(null);
                existing.setBlockReason(null);
                // Restore status from preBlockStatus + cascade users and branches back
                if ("BLOCK_PENDING".equals(existing.getStatus())) {
                    String restored = existing.getPreBlockStatus() != null ? existing.getPreBlockStatus() : CommonConstants.ACTIVE;
                    existing.setStatus(restored);
                    existing.setPreBlockStatus(null);
                    cascadeCancelBlockUsers(bankId, updatedBy, existing.getBankName(), existing.getBankCode(), existing.getEmail());
                    if (existing.getParentBankId() == null) {
                        for (ReconBankMaster branch : reconBankMasterRepository.findByParentBankId(bankId)) {
                            if (!"BLOCK_PENDING".equals(branch.getStatus())) continue;
                            String branchRestored = branch.getPreBlockStatus() != null ? branch.getPreBlockStatus() : CommonConstants.ACTIVE;
                            branch.setStatus(branchRestored);
                            branch.setPreBlockStatus(null);
                            branch.setBlockScheduledAt(null);
                            branch.setBlockScheduledBy(null);
                            branch.setBlockReason(null);
                            branch.setUpdatedAt(LocalDateTime.now());
                            branch.setUpdatedBy(updatedBy);
                            reconBankMasterRepository.save(branch);
                            cascadeCancelBlockUsers(branch.getBankId(), updatedBy, branch.getBankName(), branch.getBankCode(), null);
                            try {
                                if (branch.getEmail() != null && !branch.getEmail().isEmpty()) {
                                    emailService.sendBranchBankBlockCancelled(branch.getEmail(), branch.getFullName(),
                                            branch.getBankName(), branch.getBankCode(),
                                            existing.getBankName(), existing.getBankCode());
                                }
                            } catch (Exception e) {
                                logger.warn("Branch block cancel email failed for {}: {}", branch.getBankCode(), e.getMessage());
                            }
                        }
                    }
                    try {
                        if (existing.getEmail() != null && !existing.getEmail().isEmpty()) {
                            emailService.sendBlockCancelled(existing.getEmail(), existing.getFullName(),
                                    existing.getBankName(), existing.getBankCode(), restored);
                        }
                    } catch (Exception e) {
                        logger.warn("Block cancel email failed for {}: {}", existing.getBankCode(), e.getMessage());
                    }
                }
                break;
            default:
                return ResponseEntity.badRequest()
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid scheduleType. Use INACTIVATE, REACTIVATE, or BLOCK.", null));
        }
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconBankMasterRepository.save(existing);
        saveAuditLog("RECON_BANK_MASTER", bankId, "CANCEL_SCHEDULE", scheduleType, null, updatedBy, null, bankId,
                scheduleType + " schedule cancelled");
        logger.info("ReconBankMaster {} schedule cancelled for bankId={} by {}", scheduleType, bankId, updatedBy);
        notifyActor(updatedBy, scheduleType + " Schedule Cancelled", existing.getBankName(), existing.getBankCode(), null);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, scheduleType + " schedule cancelled.", null));
    }

    // ── Helper: map transient address lines / phone parts → regAddress / regPhone ──
    private void mapAddressAndPhoneTransients(ReconBankMaster bank) {
        String combinedAddress = Stream.of(bank.getRegAddressLine1(), bank.getRegAddressLine2(), bank.getRegAddressLine3())
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.joining(", "));
        if (!combinedAddress.trim().isEmpty()) bank.setRegAddress(combinedAddress);

        String phoneCode = bank.getRegPhoneCode();
        String cityCode  = bank.getRegCityCode();
        String phoneNum  = bank.getRegPhone();
        if ((phoneCode != null && !phoneCode.trim().isEmpty()) || (cityCode != null && !cityCode.trim().isEmpty())) {
            String combined = Stream.of(phoneCode, cityCode, phoneNum)
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.joining("-"));
            bank.setRegPhone(combined);
        }
    }

    // ── Helper: string-field diff for update-notification sections ─────────────
    private void diffF(String label, String oldVal, String newVal, List<String> target) {
        if (newVal != null && !newVal.equals(oldVal)) {
            target.add(label + ": " + (oldVal == null || oldVal.isEmpty() ? "—" : oldVal) + " → " + newVal);
        }
    }

    // ── Helper: boolean-field diff (rendered as Enabled/Disabled) ──────────────
    private void diffB(String label, Boolean oldVal, Boolean newVal, List<String> target) {
        if (newVal != null && !newVal.equals(oldVal)) {
            String oldDisplay = oldVal == null ? "—" : (oldVal ? "Enabled" : "Disabled");
            target.add(label + ": " + oldDisplay + " → " + (newVal ? "Enabled" : "Disabled"));
        }
    }

    // ── Helper: product-subscription diff (added/removed) ──────────────────────
    private List<String> diffProducts(List<String> oldProducts, List<String> newProducts) {
        List<String> diffs = new ArrayList<>();
        if (newProducts == null || newProducts.isEmpty()) return diffs;
        for (String p : newProducts) {
            if (!oldProducts.contains(p)) diffs.add("Added: " + p);
        }
        for (String p : oldProducts) {
            if (!newProducts.contains(p)) diffs.add("Removed: " + p);
        }
        return diffs;
    }

    // ── Helper: build a ReconUser for primary/secondary admin ──────────────────
    private ReconUser buildAdminUser(Long bankId, String fullName, String email,
                                     String mobile, String username, String userType,
                                     String contactRank, String plainPassword,
                                     String status, String createdBy) {
        ReconUser u = new ReconUser();
        u.setBankId(bankId);
        u.setFullName(fullName != null && !fullName.trim().isEmpty() ? fullName : username);
        u.setEmail(email.trim().toLowerCase());
        u.setMobileNumber(mobile);
        u.setUsername(username);
        u.setUserType(userType);
        u.setContactRank(contactRank);
        u.setPasswordHash(passwordEncoder.encode(plainPassword));
        u.setPasswordSet(0);
        u.setStatus(status);
        u.setApprovedYn(CommonConstants.INACTIVE_N);
        u.setCreatedAt(LocalDateTime.now());
        u.setCreatedBy(createdBy);
        return u;
    }

    // ── Helper: save to RCN_RECON_PWD_MANAGER ──────────────────────────────────
    private void savePasswordHistory(ReconUser user, String createdBy) {
        ReconPasswordManager pwd = new ReconPasswordManager();
        pwd.setReconUser(user);
        pwd.setUserPassword(user.getPasswordHash());
        pwd.setCreatedAt(LocalDateTime.now());
        pwd.setCreatedBy(createdBy);
        pwd.setExpirationDate(LocalDateTime.now().plusDays(90));
        reconPasswordManagerRepository.save(pwd);
    }

    // ── Helper: save to AUDIT_LOG ───────────────────────────────────────────────
    private void saveAuditLog(String tableName, Long recordId, String operation,
                               String oldValue, String newValue,
                               String actorUsername, String actorType,
                               Long bankId, String actionLabel) {
        try {
            AuditLog log = new AuditLog();
            log.setTableName(tableName);
            log.setRecordId(recordId);
            log.setOperation(operation != null && operation.length() > 10 ? operation.substring(0, 10) : operation);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setActorUsername(actorUsername);
            log.setActorType(actorType);
            log.setBankId(bankId);
            log.setActionLabel(actionLabel);
            log.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("AuditLog save failed: {}", e.getMessage());
        }
    }

    // ── Status flow helpers ──────────────────────────────────────────────────────

    // Email to the actor (admin who performed the action) — resolved from RCN_RECON_USER
    private void notifyActor(String actorUsername, String action, String targetName, String targetCode, String scheduledAt) {
        try {
            if (actorUsername == null || "UNKNOWN".equals(actorUsername) || "SCHEDULER".equals(actorUsername)) return;
            Optional<ReconUser> actor = reconUserRepository.findByUsername(actorUsername);
            if (!actor.isPresent()) actor = reconUserRepository.findByEmail(actorUsername);
            if (actor.isPresent() && actor.get().getEmail() != null && !actor.get().getEmail().isEmpty()) {
                emailService.sendActorActionConfirmation(actor.get().getEmail(),
                        actor.get().getFullName() != null ? actor.get().getFullName() : actorUsername,
                        action, targetName, targetCode, scheduledAt);
            }
        } catch (Exception e) {
            logger.warn("Actor confirmation email failed for {}: {}", actorUsername, e.getMessage());
        }
    }

    // Direct status sync to this institution's admin users only (BANK_ADMIN / BRANCH_ADMIN)
    private void syncAdminStatus(Long bankId, String newStatus, String updatedBy) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if (!"BANK_ADMIN".equals(u.getUserType()) && !"BRANCH_ADMIN".equals(u.getUserType())) continue;
                if ("BLOCKED".equals(u.getStatus())) continue;
                u.setStatus(newStatus);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
                logger.info("Admin status synced: {} → {}", u.getUsername(), newStatus);
            }
        } catch (Exception e) {
            logger.warn("Admin status sync failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    /**
     * The users in this institution who are currently standing in for someone else. They are the
     * people taking OVER from the admins being blocked, so a block cascade must leave them alone.
     *
     * Snapshotted BEFORE the cascade runs, never re-checked inside the loop: blocking an original
     * calls onOriginalBlocked(), which flips their replacement record ACTIVE → FINALIZED. If we
     * asked "is this an active replacement?" per-user, the original would usually be visited first,
     * and by the time we reached the replacement they'd no longer look like one — so they'd be
     * blocked. (That's exactly the bug this snapshot fixes.)
     */
    private Set<Long> standingInUserIds(List<ReconUser> users) {
        Set<Long> ids = new HashSet<>();
        for (ReconUser u : users) {
            if (replacementService.isActiveReplacementUser(u.getUserId())) ids.add(u.getUserId());
        }
        return ids;
    }

    // Immediate BLOCKED cascade to all users of an institution
    private void cascadeBlockUsersNow(Long bankId, String reason, String updatedBy) {
        try {
            List<ReconUser> users = reconUserRepository.findByBankId(bankId);
            Set<Long> standingIn = standingInUserIds(users);
            for (ReconUser u : users) {
                if ("BLOCKED".equals(u.getStatus())) continue;
                // The replacement is taking over FROM the blocked original — don't block them too.
                if (standingIn.contains(u.getUserId())) continue;
                if (u.getPreBlockStatus() == null) u.setPreBlockStatus(u.getStatus());
                u.setStatus("BLOCKED");
                u.setBlockReason(reason);
                u.setBlockScheduledAt(null);
                u.setBlockScheduledBy(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
                // Mirrors StatusSchedulerService.cascadeBlockUsers() — this immediate-block cascade
                // is otherwise identical (same standingIn snapshot, same loop) but was missing these
                // two calls, so a bank/branch blocked via "Block Now" (not the scheduler) never
                // finalised the blocked original's ACTIVE replacement record, and never notified
                // that original's delegatee. Scheduled block cascades already did both.
                try { replacementService.onOriginalBlocked(u.getUserId()); }
                catch (Exception e) { logger.warn("cascade onOriginalBlocked failed for {}: {}", u.getUserId(), e.getMessage()); }
                try { delegationService.notifyDelegateeBlocked(u.getUserId()); }
                catch (Exception e) { logger.warn("cascade notifyDelegateeBlocked failed for {}: {}", u.getUserId(), e.getMessage()); }
                try {
                    if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                        emailService.sendBlockedNotification(u.getEmail(), u.getFullName());
                    }
                } catch (Exception e) {
                    logger.warn("User block email failed for {}: {}", u.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Block cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // Restore BLOCKED users to their pre-block status
    private void cascadeUnblockUsers(Long bankId, String updatedBy) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if (!"BLOCKED".equals(u.getStatus())) continue;
                String restored = u.getPreBlockStatus() != null ? u.getPreBlockStatus() : CommonConstants.ACTIVE;
                u.setStatus(restored);
                u.setPreBlockStatus(null);
                u.setBlockReason(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
                try {
                    delegationService.notifyDelegateeUnblocked(u.getUserId());
                } catch (Exception e) {
                    logger.warn("notifyDelegateeUnblocked failed for {}: {}", u.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Unblock cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // BLOCK_PENDING cascade to all users + per-user warning email
    // skipEmail: the institution's own primary contact — they already get a dedicated
    // sendBlockWarning call at the bank/branch level, so don't email them twice here
    // (the PRIMARY admin's ReconUser email is always identical to that contact email).
    private void cascadeScheduleBlockUsers(Long bankId, LocalDateTime scheduledAt, String scheduledBy,
                                           String reason, String entityName, String entityCode, String atFormatted,
                                           String skipEmail) {
        try {
            List<ReconUser> users = reconUserRepository.findByBankId(bankId);
            Set<Long> standingIn = standingInUserIds(users);
            for (ReconUser u : users) {
                if ("BLOCKED".equals(u.getStatus()) || "BLOCK_PENDING".equals(u.getStatus())) continue;
                // The replacement is taking over FROM the blocked original — don't block them too.
                if (standingIn.contains(u.getUserId())) continue;
                u.setPreBlockStatus(u.getStatus());
                u.setStatus("BLOCK_PENDING");
                u.setBlockScheduledAt(scheduledAt);
                u.setBlockScheduledBy("CASCADE:" + scheduledBy);
                u.setBlockReason(reason);
                u.setInactivateScheduledAt(null);
                u.setInactivateScheduledBy(null);
                u.setReactivateScheduledAt(null);
                u.setReactivateScheduledBy(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(scheduledBy);
                reconUserRepository.save(u);
                try {
                    boolean alreadyNotified = skipEmail != null && u.getEmail() != null
                            && skipEmail.equalsIgnoreCase(u.getEmail());
                    if (!alreadyNotified && u.getEmail() != null && !u.getEmail().isEmpty()) {
                        emailService.sendBlockWarning(u.getEmail(),
                                u.getFullName() != null ? u.getFullName() : u.getUsername(),
                                entityName, entityCode, atFormatted);
                    }
                } catch (Exception e) {
                    logger.warn("User block warning email failed for {}: {}", u.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Schedule block cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // Restore BLOCK_PENDING users to pre-block status + per-user cancellation email.
    // skipEmail: the institution's own primary contact — they already get a dedicated
    // sendBlockCancelled call at the bank/branch level, so don't email them twice here
    // (the PRIMARY admin's ReconUser email is always identical to that contact email).
    private void cascadeCancelBlockUsers(Long bankId, String updatedBy, String entityName, String entityCode, String skipEmail) {
        try {
            for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
                if (!"BLOCK_PENDING".equals(u.getStatus())) continue;
                String restored = u.getPreBlockStatus() != null ? u.getPreBlockStatus() : CommonConstants.ACTIVE;
                u.setStatus(restored);
                u.setPreBlockStatus(null);
                u.setBlockScheduledAt(null);
                u.setBlockScheduledBy(null);
                u.setBlockReason(null);
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(updatedBy);
                reconUserRepository.save(u);
                try {
                    boolean alreadyNotified = skipEmail != null && u.getEmail() != null
                            && skipEmail.equalsIgnoreCase(u.getEmail());
                    if (!alreadyNotified && u.getEmail() != null && !u.getEmail().isEmpty()) {
                        emailService.sendBlockCancelled(u.getEmail(),
                                u.getFullName() != null ? u.getFullName() : u.getUsername(),
                                entityName, entityCode, restored);
                    }
                } catch (Exception e) {
                    logger.warn("User block cancel email failed for {}: {}", u.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Cancel block cascade failed for bankId {}: {}", bankId, e.getMessage());
        }
    }

    // ── Product helpers ──────────────────────────────────────────────────────────

    private void saveProductMappings(Long bankId, ReconBankMaster bank, String actorBy) {
        List<String> products = bank.getSelectedProducts();
        if (products == null || products.isEmpty()) return;
        bankProductMapRepository.deleteByBankId(bankId);
        bankProductMapRepository.flush();
        Map<String, ReconBankMaster.ProductDateEntry> dates = bank.getProductDates();
        for (String productName : products) {
            Optional<ReconProductMaster> prodOpt = productMasterRepository.findByProductName(productName);
            if (!prodOpt.isPresent()) {
                ReconProductMaster newProd = new ReconProductMaster();
                newProd.setProductName(productName);
                newProd.setStatus(CommonConstants.ACTIVE);
                newProd.setCreatedAt(LocalDateTime.now());
                newProd.setCreatedBy(actorBy);
                prodOpt = Optional.of(productMasterRepository.save(newProd));
            }
            ReconBankMaster.ProductDateEntry entry = (dates != null) ? dates.get(productName) : null;
            CBankProductMap mapping = new CBankProductMap();
            mapping.setBankId(bankId);
            mapping.setProductId(prodOpt.get().getProductId());
            mapping.setValidFrom(entry != null ? entry.getValidFrom() : null);
            mapping.setValidTo(entry != null ? entry.getValidTo() : null);
            mapping.setStatus(CommonConstants.ACTIVE);
            mapping.setCreatedAt(LocalDateTime.now());
            mapping.setCreatedBy(actorBy);
            bankProductMapRepository.save(mapping);
        }
        logger.info("Product mappings saved for bankId={}: {}", bankId, products);
    }

    private void enrichWithProducts(ReconBankMaster bank) {
        List<CBankProductMap> mappings = bankProductMapRepository.findByBankIdAndStatus(bank.getBankId(), CommonConstants.ACTIVE);
        List<String> products = new ArrayList<>();
        Map<String, ReconBankMaster.ProductDateEntry> dates = new LinkedHashMap<>();
        for (CBankProductMap m : mappings) {
            productMasterRepository.findById(m.getProductId()).ifPresent(prod -> {
                products.add(prod.getProductName());
                dates.put(prod.getProductName(),
                        new ReconBankMaster.ProductDateEntry(m.getValidFrom(), m.getValidTo()));
            });
        }
        bank.setSelectedProducts(products);
        bank.setProductDates(dates);
    }

    private String generatePassword(int length) {
        int digits = 1000 + RNG.nextInt(9000);
        return "Recon@" + digits;
    }

    // Generates a unique 8-digit bank code from current timestamp (last 8 digits)
    private String generateBankCode() {
        String base = String.valueOf(System.currentTimeMillis());
        String code = base.substring(base.length() - 8);
        while (reconBankMasterRepository.existsByBankCodeAndContactRank(code, "PRIMARY")) {
            code = String.valueOf(System.currentTimeMillis()).substring(base.length() - 8);
        }
        return code;
    }

    // Generates a unique 8-digit branch code: first 4 of parent bank code + last 4 of timestamp
    private String generateBranchCode(Long parentBankId) {
        String prefix = "0000";
        if (parentBankId != null) {
            Optional<ReconBankMaster> parent = reconBankMasterRepository.findPrimaryById(parentBankId);
            if (parent.isPresent() && parent.get().getBankCode() != null
                    && parent.get().getBankCode().length() >= 4) {
                prefix = parent.get().getBankCode().substring(0, 4);
            }
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            String epochStr = String.valueOf(System.currentTimeMillis() + attempt);
            String suffix   = epochStr.substring(epochStr.length() - 4);
            String candidate = prefix + suffix;
            if (!reconBankMasterRepository.existsByBankCodeAndContactRank(candidate, "PRIMARY")) {
                return candidate;
            }
        }
        // Ultimate fallback: full timestamp
        return generateBankCode();
    }

    // Public helper used by the generate-branch-code endpoint
    public String generateBranchCodePublic(Long parentBankId) {
        return generateBranchCode(parentBankId);
    }

    // Public helper used by the generate-code endpoint (top-level bank preview code)
    public String generateBankCodePublic() {
        return generateBankCode();
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> uploadLogo(Long bankId, MultipartFile file, String uploadedBy) {
        Optional<ReconBankMaster> optional = reconBankMasterRepository.findPrimaryById(bankId);
        if (!optional.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank not found with ID: " + bankId, null));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "No file provided.", null));
        }
        if (!ALLOWED_LOGO_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid file type. Accepted formats: JPG, TIF.", null));
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "File size exceeds 2 MB limit.", null));
        }
        try {
            File uploadDir = new File(logoUploadDir);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            ReconBankMaster bank = optional.get();
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String savedFilename = bank.getBankCode() + "_logo" + extension;
            Path filePath = Paths.get(logoUploadDir + savedFilename);
            Files.write(filePath, file.getBytes());

            bank.setLogoPath(filePath.toString());
            bank.setUpdatedAt(LocalDateTime.now());
            bank.setUpdatedBy(uploadedBy);
            reconBankMasterRepository.save(bank);

            logger.info("Logo uploaded for bank {}: {}", bankId, filePath);
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Logo uploaded successfully.", null));
        } catch (IOException e) {
            logger.error("Logo upload failed for bank {}: {}", bankId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Logo upload failed. Please try again.", null));
        }
    }

    @Override
    public ResponseEntity<byte[]> getLogoImage(String bankCode) {
        Optional<ReconBankMaster> optional = reconBankMasterRepository.findByBankCodeAndContactRank(bankCode, "PRIMARY");
        if (!optional.isPresent()) return ResponseEntity.notFound().build();
        String logoPath = optional.get().getLogoPath();
        if (logoPath == null || logoPath.trim().isEmpty()) return ResponseEntity.notFound().build();
        try {
            String cleanPath = logoPath.trim().replaceAll("^\"|\"$", "");
            if (cleanPath.startsWith("'") && cleanPath.endsWith("'"))
                cleanPath = cleanPath.substring(1, cleanPath.length() - 1);
            cleanPath = cleanPath.replace('\\', '/');
            Path path = Paths.get(cleanPath);
            if (!Files.exists(path)) return ResponseEntity.notFound().build();
            byte[] imageBytes = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "image/jpeg";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(imageBytes);
        } catch (IOException e) {
            logger.error("Failed to serve logo for bank {}: {}", bankCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String deriveUsername(String email, String fullName) {
        String base;
        if (fullName != null && fullName.trim().length() > 0) {
            String[] parts = fullName.trim().toLowerCase().replaceAll("[^a-z ]", "").split("\\s+");
            base = parts.length > 1 ? parts[0] + "." + parts[parts.length - 1] : parts[0];
        } else {
            base = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9.]", "");
        }
        String candidate = base;
        int suffix = 1;
        while (reconUserRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private Long createDefaultAdminMenus(String bankCode, Long bankId, boolean isBranch, String createdBy) {
        try {

            // Role already exists? Skip — bankCode is unique, so the display name doubles
            // as the uniqueness key now that ROLE_CODE is a flat 4-digit sequence (same
            // generator as Add Role) rather than a bankCode-derived string.
            String roleDisplayName = (isBranch ? "Branch Admin - " : "Bank Admin - ") + bankCode;
            Optional<ReconRoleMaster> existingRole = reconRoleMasterRepository.findByRoleName(roleDisplayName);
            if (existingRole.isPresent()) {
                // Banks onboarded before a menu was added to the default set would never receive
                // it, since this method returns early for them. Backfill anything missing.
                ensureAdminMenus(existingRole.get().getRoleId(), bankId, isBranch ? "/branch-admin" : "/bank-admin", createdBy);
                return existingRole.get().getRoleId();
            }

            // Create role for this bank's admin
            ReconRoleMaster role = new ReconRoleMaster();
            role.setRoleCode(roleCodeGeneratorService.generateNextCode(roleDisplayName));
            role.setRoleName(roleDisplayName);
            // Self-documenting marker (was the generic "EXTERNAL") — ReconRoleMasterServiceImpl
            // reads this to know which menus in this role are the system-default set a Bank Admin
            // may add to but never remove, and to decide the role's Role List visibility.
            role.setRoleType(isBranch ? "BRANCH_ADMIN_DEFAULT" : "BANK_ADMIN_DEFAULT");
            role.setRoleDesc("Default " + (isBranch ? "Branch" : "Bank") + " Admin role for " + bankCode);
            role.setStatus(CommonConstants.ACTIVE);
            role.setCreatedBy(createdBy);
            role.setCreatedAt(java.time.LocalDateTime.now());
            ReconRoleMaster savedRole = reconRoleMasterRepository.save(role);
            Long roleId = savedRole.getRoleId();

            String prefix = isBranch ? "/branch-admin" : "/bank-admin";

            if (isBranch) {
                // Branch Admin: standalone Dashboard at top level
                saveMenu(null, "Master", "Dashboard", prefix + "/dashboard", roleId, bankId, createdBy);

                // My Organization — parentMenuCode must be the master's NAME (matches the
                // convention every other menu-creation path uses, e.g. AddMenu.jsx), not its
                // MENU_ID — using the ID here broke Main-menu lookups everywhere (Privileges
                // screen, Role/User view pages) that filter children by parentMenuCode === name.
                ReconMenuMaster myOrg = saveMenu(null, "Master", "My Organization", null, roleId, bankId, createdBy);
                String myOrgName = myOrg.getMenuName();
                for (String[] item : Arrays.asList(
                    new String[]{"Overview",     prefix + "/my-organization/overview"},
                    new String[]{"My Hierarchy", prefix + "/my-organization/hierarchy"},
                    new String[]{"User Status",  prefix + "/my-organization/admin-status"}
                )) {
                    saveMenu(myOrgName, "Main", item[0], item[1], roleId, bankId, createdBy);
                }
            } else {
                // Bank Admin: My Organization (no standalone Dashboard for Bank Admin)
                ReconMenuMaster myOrg = saveMenu(null, "Master", "My Organization", null, roleId, bankId, createdBy);
                String myOrgName = myOrg.getMenuName();
                for (String[] item : Arrays.asList(
                    new String[]{"Overview",           prefix + "/my-organization/overview"},
                    new String[]{"Branches",           prefix + "/my-organization/branches"},
                    new String[]{"Branch Onboarding",  prefix + "/branch-onboarding"},
                    new String[]{"My Hierarchy",       prefix + "/my-organization/hierarchy"},
                    new String[]{"User Status",        prefix + "/my-organization/user-status"},
                    new String[]{"Branch Admin Status",prefix + "/my-organization/admin-status"}
                )) {
                    saveMenu(myOrgName, "Main", item[0], item[1], roleId, bankId, createdBy);
                }
            }

            // Administration (same for both)
            ReconMenuMaster adminMenu = saveMenu(null, "Master", "Administration", null, roleId, bankId, createdBy);
            String adminMenuName = adminMenu.getMenuName();
            for (String[] item : Arrays.asList(
                new String[]{"Add User",  prefix + "/add-user"},
                new String[]{"Add Role",  prefix + "/admin/add-new-role"},
                new String[]{"Add Menu",  prefix + "/add-menu"},
                new String[]{"User List", prefix + "/user-list"},
                new String[]{"Role List", prefix + "/role-list"},
                new String[]{"Menu List", prefix + "/menu-list"},
                new String[]{"Handover & Delegation History", prefix + "/user-management"},
                new String[]{"Approval Request History", prefix + "/approval-history"},
                new String[]{"Checker Dashboard", prefix + "/checker-queue"}
                // Maker Dashboard is deliberately NOT granted by default — an Admin must never
                // hold it out of the box (it's a Maker-only privilege, and holding it alongside
                // Checker Dashboard here would already violate the Checker/Maker SOD rule that
                // savePrivileges enforces). The Admin can grant it explicitly via Privileges if a
                // role actually needs to act as a Maker.
            )) {
                saveMenu(adminMenuName, "Main", item[0], item[1], roleId, bankId, createdBy);
            }

            logger.info("Default {} menus created for: {}", isBranch ? "Branch Admin" : "Bank Admin", bankCode);
            return roleId;
        } catch (Exception e) {
            logger.error("Failed to create default menus for bank {}: {}", bankCode, e.getMessage());
            return null;
        }
    }

    /**
     * Adds any Administration menu this admin role is missing.
     *
     * createDefaultAdminMenus() returns early once the role exists, so a menu added to the
     * default set later would never reach a bank that was already onboarded. Each name is
     * checked against RECON_MENU_MASTER for this bank before inserting, which makes the method
     * idempotent and safe to call on every onboarding attempt.
     */
    private void ensureAdminMenus(Long roleId, Long bankId, String prefix, String createdBy) {
        if (roleId == null || bankId == null) return;
        ReconMenuMaster adminMaster = menuMasterRepository.findByMenuNameAndBankId("Administration", bankId);
        if (adminMaster == null) return; // no Administration tree for this bank — nothing to extend

        // Arrays.<String[]>asList, not Arrays.asList: with a single String[] the varargs form
        // would spread the array into a List<String> instead of wrapping it.
        for (String[] item : Arrays.asList(
            new String[]{"Handover & Delegation History", prefix + "/user-management"},
            new String[]{"Approval Request History", prefix + "/approval-history"}
            // Maker Dashboard intentionally excluded — see createDefaultAdminMenus() above.
        )) {
            if (menuMasterRepository.findByMenuNameAndBankId(item[0], bankId) == null) {
                saveMenu(adminMaster.getMenuName(), "Main", item[0], item[1], roleId, bankId, createdBy);
                logger.info("Backfilled Administration menu '{}' for bankId={}", item[0], bankId);
            }
        }
    }

    // roleId is only used for the C_ROLE_MENU_MAP grant below; bankId is the menu's owner.
    private ReconMenuMaster saveMenu(String parentMenuCode, String menuType, String menuName,
                                     String menuUrl, Long roleId, Long bankId, String createdBy) {
        ReconMenuMaster m = new ReconMenuMaster();
        // Permanent identity — display names may be renamed, this must not be derived from them.
        m.setSystemMenuCode(com.jpb.reconciliation.reconciliation.constants.SystemMenuCodes.of(menuName));
        m.setMenuType(menuType);
        m.setMenuName(menuName);
        m.setMenuUrl(menuUrl);
        m.setParentMenuCode(parentMenuCode);
        m.setSubMenu(CommonConstants.INACTIVE_N);
        m.setStatus(CommonConstants.ACTIVE_Y);
        m.setBankId(bankId);
        // Bootstrap menus are ordinary application menus, never system menus — set explicitly so the
        // generated INSERT lands 'N' rather than NULL (see sql/default_dashboard_system_menu.sql).
        m.setIsSystemMenu(CommonConstants.INACTIVE_N);
        m.setParentMenuId(resolveParentMenuId(menuType, parentMenuCode, bankId));
        m.setCreatedBy(createdBy);
        m.setCreatedDate(new Date());
        m.setInsertDate(new Date());
        ReconMenuMaster saved = menuMasterRepository.save(m);

        // Also attach via C_ROLE_MENU_MAP — the Sidebar reads privileges from there (not
        // RECON_MENU_MASTER.ROLE_ID directly) so this bootstrap menu actually shows up.
        // Both setId(...) AND setRole()/setMenu() must be set — ROLE_ID/MENU_ID are shared
        // columns between the @EmbeddedId and the @MapsId associations, and leaving the
        // associations null caused the insert to fail silently (see ReconRoleMasterServiceImpl
        // .savePrivileges() for the same pattern).
        ReconRoleMaster roleRef = reconRoleMasterRepository.findById(roleId).orElse(null);
        CRoleMenuMap map = new CRoleMenuMap();
        map.setId(new CRoleMenuMap.RoleMenuMapId(roleId, saved.getMenuId()));
        map.setRole(roleRef);
        map.setMenu(saved);
        map.setCreatedAt(LocalDateTime.now());
        map.setCreatedBy(createdBy);
        roleMenuMapRepository.save(map);

        return saved;
    }

    // Internal, additive mirror of parentMenuCode — see MenuMasterServiceImpl's identically-named
    // private helper (this class is in a different package, so it's duplicated per this
    // codebase's per-class-helper style rather than pulled into a shared utility). Resolves the
    // DIRECT parent's MENU_ID only; never throws — parentMenuCode remains authoritative. Every
    // call site here creates only Master/Main rows, so the Submenu branch is unreachable in
    // practice but kept for consistency with the other copies of this helper.
    // See sql/menu_parent_id_migration.sql.
    private Long resolveParentMenuId(String menuType, String parentMenuCode, Long bankId) {
        if ("Master".equals(menuType) || parentMenuCode == null) return null;
        String parentType = "Main".equals(menuType) ? "Master" : "Main";
        Optional<ReconMenuMaster> byName = menuMasterRepository
                .findAllByMenuNameAndMenuType(parentMenuCode, parentType).stream()
                .filter(p -> CommonConstants.ACTIVE_Y.equals(p.getStatus()))
                .filter(p -> Objects.equals(p.getBankId(), bankId))
                .filter(p -> bankId != null || !"CATALOG".equals(p.getCreatedBy()))
                .findFirst();
        if (byName.isPresent()) return byName.get().getMenuId();
        if ("Main".equals(menuType)) {
            try { return Long.parseLong(parentMenuCode); } catch (NumberFormatException ignored) {
                // parentMenuCode isn't numeric — not a legacy ID-based row, nothing to fall back to
            }
        }
        return null;
    }
}



