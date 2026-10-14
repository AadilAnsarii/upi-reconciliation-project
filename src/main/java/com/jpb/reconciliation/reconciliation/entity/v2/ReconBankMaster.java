package com.jpb.reconciliation.reconciliation.entity.v2;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RECON_BANK_MASTER")
public class ReconBankMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BANK_ID")
    private Long bankId;

    // unique constraint removed — same BANK_CODE appears in PRIMARY + SECONDARY rows
    @JsonAlias("branchCode")
    @Column(name = "BANK_CODE", length = 20, nullable = false)
    private String bankCode;

    // BANK = top-level bank, BRANCH = branch of a bank
    @Column(name = "BANK_LEVEL", length = 10)
    private String bankLevel;

    @Column(name = "BANK_TYPE", length = 100, nullable = false)
    private String bankType;

    @Column(name = "PARENT_BANK_ID")
    private Long parentBankId;

    @JsonProperty("bankNameFull")
    @JsonAlias({"bankName", "branchNameFull"})
    @Column(name = "BANK_NAME", length = 150, nullable = false)
    private String bankName;

    @JsonAlias("branchNameShort")
    @Column(name = "BANK_NAME_SHORT", length = 50)
    private String bankNameShort;

    @Column(name = "BANK_CATEGORY", length = 100)
    private String bankCategory;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "REQUEST";

    @Column(name = "PRE_BLOCK_STATUS", length = 20)
    private String preBlockStatus;

    // When the bank most recently became INACTIVE — drives the reactivation
    // cooldown in updateStatus() (can't go ACTIVE again within the window).
    @Column(name = "INACTIVATED_AT")
    private LocalDateTime inactivatedAt;

    @Column(name = "INACTIVATE_SCHEDULED_AT")
    private LocalDateTime inactivateScheduledAt;

    @Column(name = "INACTIVATE_SCHEDULED_BY", length = 100)
    private String inactivateScheduledBy;

    @Column(name = "REACTIVATE_SCHEDULED_AT")
    private LocalDateTime reactivateScheduledAt;

    @Column(name = "REACTIVATE_SCHEDULED_BY", length = 100)
    private String reactivateScheduledBy;

    @Column(name = "BLOCK_SCHEDULED_AT")
    private LocalDateTime blockScheduledAt;

    @Column(name = "BLOCK_SCHEDULED_BY", length = 100)
    private String blockScheduledBy;

    @Column(name = "BLOCK_REASON", length = 500)
    private String blockReason;

    @Column(name = "REG_ADDRESS", length = 500)
    private String regAddress;

    @Column(name = "REG_CITY", length = 100)
    private String regCity;

    @Column(name = "REG_STATE", length = 100)
    private String regState;

    @Column(name = "REG_COUNTRY", length = 100)
    private String regCountry;

    @Column(name = "REG_PHONE", length = 20)
    private String regPhone;

    @Column(name = "ENABLE_MFA")
    private Boolean enableMfa;

    @Column(name = "ENABLE_OTP")
    private Boolean enableOtp;

    @Column(name = "ENABLE_HRMS")
    private Boolean enableHrms;

    // ── Contact person fields (PRIMARY row = main contact, SECONDARY row = backup) ──
    @Column(name = "CONTACT_RANK", length = 20)
    private String contactRank = "PRIMARY";

    @JsonAlias("primaryFullName")
    @Column(name = "FULL_NAME", length = 200)
    private String fullName;

    @JsonAlias("primaryEmail")
    @Column(name = "EMAIL", length = 150)
    private String email;

    @JsonAlias({"primaryMobile", "primaryMobileNumber"})
    @Column(name = "MOBILE_NUMBER", length = 20)
    private String mobileNumber;

    @JsonAlias({"primaryAltMobile", "primaryAltMobileNumber"})
    @Column(name = "ALT_MOBILE_NUMBER", length = 20)
    private String altMobileNumber;

    // ── Transient: secondary contact received from request, saved as a separate row ──
    @Transient private String secondaryFullName;
    @Transient private String secondaryEmail;
    @JsonAlias("secondaryMobile")
    @Transient private String secondaryMobileNumber;
    @Transient private String secondaryAltMobile;

    // ── Transient: address lines from frontend → combined into regAddress on save ──
    @Transient private String regAddressLine1;
    @Transient private String regAddressLine2;
    @Transient private String regAddressLine3;

    // ── Transient: phone parts from frontend → combined into regPhone on save ──
    @Transient private String regPhoneCode;
    @Transient private String regCityCode;

    @Column(name = "BANK_ADMIN_USERNAME", length = 100)
    private String bankAdminUsername;

    // BCrypt-hashed default password — cleared after admin sets their own password.
    // Never displayed back to the frontend; @JsonIgnore prevents API leakage.
    @JsonIgnore
    @Column(name = "DEFAULT_PASSWORD", length = 255)
    private String defaultPassword;

    @Column(name = "LOGO_PATH", length = 500)
    private String logoPath;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    // Set when every one of this bank's C_BANK_PRODUCT_MAP rows has expired (VALID_TO
    // in the past) — anchors the bank-wide grace-period countdown. Cleared as soon as
    // any product becomes valid again (renewal).
    @Column(name = "ALL_PRODUCTS_EXPIRED_AT")
    private LocalDateTime allProductsExpiredAt;

    // Transient — not persisted; populated on read / consumed on write via C_BANK_PRODUCT_MAP
    @Transient
    private List<String> selectedProducts;

    @Transient
    private Map<String, ProductDateEntry> productDates;

    // ── Transient: replacement awareness for "View"/list screens ────────────────
    // Read-only, populated per-request by ReconBankMasterServiceImpl.enrichWithReplacement() —
    // never persisted. While this bank's admin has an ACTIVE (not yet permanent) replacement,
    // fullName/email/mobileNumber/bankAdminUsername above are swapped in-memory to the
    // replacement's own contact info, and these three fields are set so the frontend can label
    // the row "(Replaced)" — matching the shape ReconBankMasterController.getAllBankAdmins()
    // already returns per-admin-row, but here per bank/branch row (see get-all/get/{id}/
    // branch-banks). Always false/null once the replacement is FINALIZED — at that point
    // RECON_BANK_MASTER's own stored columns already reflect the replacement permanently.
    @Transient private boolean replacementAdminRow;
    @Transient private String replacementStatus;
    @Transient private String replacedByUsername;

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProductDateEntry {
        private LocalDate validFrom;
        private LocalDate validTo;
    }
}
