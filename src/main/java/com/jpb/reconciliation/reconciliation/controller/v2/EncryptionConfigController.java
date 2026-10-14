package com.jpb.reconciliation.reconciliation.controller.v2;
//package com.jpb.reconciliation.reconciliation.controller;
//
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.jpb.reconciliation.reconciliation.dto.EncryptionConfigRequest;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.enums.EncryptionType;
//import com.jpb.reconciliation.reconciliation.service.EncryptionConfigService;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//
///**
// * Encryption config per template.
// *
// * URL base  : /api/v1/templates/{templateId}/encryption-config
// * Entity    : EncryptionConfig  (tbl_encryption_config)
// * Service   : EncryptionConfigService  (impl: EncryptionConfigServiceImpl)
// * Response  : RestWithStatusList – project-wide standard wrapper
// *
// * Request DTO  – EncryptionConfigRequest:
// *   encryptionRequired   Boolean       true / false   (NOT CommonConstants.ACTIVE_Y/CommonConstants.INACTIVE_N)
// *   encryptionType       EncryptionType enum – NONE | PGP | AES | GPG
// *   keyReference         String        vault/KMS path – never store a plain key
// *   decryptionCommand    String        shell command used to decrypt the file
// *   preProcessingSteps   String        plain String stored as TEXT column
// *   postProcessingSteps  String        plain String stored as TEXT column
// *
// * Response DTO – EncryptionConfigResponse:
// *   encryptionConfigId, templateId, encryptionRequired, encryptionType,
// *   keyReference, decryptionCommand, preProcessingSteps, postProcessingSteps, createdAt
// *
// * Notes:
// *   – POST is an upsert: creates the row if absent, updates it if already present.
// *   – To disable encryption, POST with encryptionRequired=false, encryptionType=NONE.
// *   – DELETE performs the same disable operation to preserve the audit trail.
// */
//@RestController
//@RequestMapping("/api/v1/templates/{templateId}/encryption-config")
//@RequiredArgsConstructor
//@Tag(name = "Encryption Configuration",
//        description = "PGP / AES / GPG encryption config per template")
//public class EncryptionConfigController {
//
//    // Injected via @RequiredArgsConstructor – interface only, no impl reference
//    private final EncryptionConfigService encryptionConfigService;
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // POST  /api/v1/templates/{templateId}/encryption-config
//    // Upsert: creates the row if absent, updates it if already exists.
//    // To disable, send encryptionRequired=false, encryptionType=NONE.
//    // ─────────────────────────────────────────────────────────────────────────
//    @PostMapping
//    @Operation(summary = "Save or update encryption config for a template")
//    public ResponseEntity<RestWithStatusList> save(
//            @PathVariable Long templateId,
//            @RequestBody EncryptionConfigRequest request) {
//
//        return ok("Encryption config saved successfully.",
//                encryptionConfigService.saveEncryptionConfig(templateId, request));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET   /api/v1/templates/{templateId}/encryption-config
//    // ─────────────────────────────────────────────────────────────────────────
//    @GetMapping
//    @Operation(summary = "Get encryption config for a template")
//    public ResponseEntity<RestWithStatusList> get(
//            @PathVariable Long templateId) {
//
//        return ok("Encryption config fetched.",
//                encryptionConfigService.getEncryptionConfig(templateId));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // DELETE /api/v1/templates/{templateId}/encryption-config
//    // Soft-disable: resets to encryptionType=NONE, encryptionRequired=false.
//    // Preserves the row so the audit trail is intact.
//    // ─────────────────────────────────────────────────────────────────────────
//    @DeleteMapping
//    @Operation(summary = "Disable encryption — resets config to encryptionType=NONE")
//    public ResponseEntity<RestWithStatusList> delete(
//            @PathVariable Long templateId) {
//
//        EncryptionConfigRequest disableRequest = EncryptionConfigRequest.builder()
//                .encryptionRequired(false)
//                .encryptionType(EncryptionType.NONE)
//                .keyReference(null)
//                .decryptionCommand(null)
//                .preProcessingSteps(null)
//                .postProcessingSteps(null)
//                .build();
//
//        encryptionConfigService.saveEncryptionConfig(templateId, disableRequest);
//        return ok("Encryption config disabled (reset to NONE) successfully.", null);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET   /api/v1/templates/{templateId}/encryption-config/encryption-types
//    // Returns all valid EncryptionType enum values for UI dropdowns.
//    // No service call needed — enum is static.
//    // ─────────────────────────────────────────────────────────────────────────
//    @GetMapping("/encryption-types")
//    @Operation(summary = "Get all valid encryption types (NONE, PGP, AES, GPG)")
//    public ResponseEntity<RestWithStatusList> getEncryptionTypes(
//            @PathVariable Long templateId) {
//
//        List<String> types = Arrays.stream(EncryptionType.values())
//                .map(Enum::name)
//                .collect(Collectors.toList());
//
//        return ok("Encryption types fetched.", types);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // private helper
//    // ─────────────────────────────────────────────────────────────────────────
//    private ResponseEntity<RestWithStatusList> ok(String msg, Object data) {
//        return ResponseEntity.ok(RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg(msg)
//                .data(data != null ? List.of(data) : List.of())
//                .build());
//    }
//}
