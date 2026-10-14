//package com.jpb.reconciliation.reconciliation.service;
//
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.dto.EncryptionConfigRequest;
//import com.jpb.reconciliation.reconciliation.dto.EncryptionConfigResponse;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * Implementation of EncryptionConfigService.
// *
// * Repositories used  (all exist in /repository):
// *   EncryptionConfigRepository   – findByTemplateMaster_TemplateId, save
// *   TemplateMasterRepository     – findByTemplateIdAndIsDeletedFalse
// *
// * Mapper methods used  (all exist in TemplateMapper):
// *   mapper.updateEncryptionEntity(EncryptionConfig, EncryptionConfigRequest)
// *     — null-safe field-by-field update:
// *       encryptionRequired, encryptionType, keyReference,
// *       decryptionCommand, preProcessingSteps, postProcessingSteps
// *   mapper.toEncryptionResponse(EncryptionConfig) → EncryptionConfigResponse
// *
// * Audit service:
// *   auditService.log(templateId, action, entityType, entityId, oldValue, newValue)
// *
// * Key behaviour:
// *   saveEncryptionConfig is an upsert — one row per template enforced by the
// *   UNIQUE constraint on tbl_encryption_config.template_id.
// *   Passing encryptionType=NONE, encryptionRequired=false effectively disables
// *   encryption without removing the row, keeping the audit trail intact.
// */
//@Service
//@Transactional
//@RequiredArgsConstructor
//@Slf4j
//public class EncryptionConfigServiceImpl implements EncryptionConfigService {
//
//    private final EncryptionConfigRepository encryptionRepo; // EncryptionConfigRepository
//    private final TemplateMasterRepository   templateRepo;   // TemplateMasterRepository
//    private final TemplateAuditService       auditService;   // TemplateAuditService
//    private final TemplateMapper             mapper;         // TemplateMapper
//
//    // =========================================================================
//    // SAVE / UPDATE  (upsert)
//    // =========================================================================
//
//    @Override
//    public EncryptionConfigResponse saveEncryptionConfig(Long templateId,
//                                                          EncryptionConfigRequest request) {
//        TemplateMaster template = findTemplateOrThrow(templateId);
//
//        // Fetch existing row or create a new entity with only the FK set
//        EncryptionConfig config = encryptionRepo
//                .findByTemplateMaster_TemplateId(templateId)
//                .orElseGet(() -> EncryptionConfig.builder()
//                        .templateMaster(template)
//                        .build());
//
//        // TemplateMapper.updateEncryptionEntity — null-safe field update
//        mapper.updateEncryptionEntity(config, request);
//
//        EncryptionConfig saved = encryptionRepo.save(config);
//        log.info("Encryption config saved for templateId={} — type={}",
//                templateId, saved.getEncryptionType());
//
//        auditService.log(templateId, "SAVE_ENCRYPTION_CONFIG", "ENCRYPTION_CONFIG",
//                saved.getEncryptionConfigId(), null,
//                "encryptionType=" + saved.getEncryptionType()
//                        + " | required=" + saved.getEncryptionRequired());
//
//        return mapper.toEncryptionResponse(saved);
//    }
//
//    // =========================================================================
//    // GET
//    // =========================================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public EncryptionConfigResponse getEncryptionConfig(Long templateId) {
//        EncryptionConfig config = encryptionRepo
//                .findByTemplateMaster_TemplateId(templateId)
//                .orElseThrow(() -> new TemplateException(
//                        "Encryption config not found for template: " + templateId));
//        return mapper.toEncryptionResponse(config);
//    }
//
//    // =========================================================================
//    // PRIVATE HELPERS
//    // =========================================================================
//
//    private TemplateMaster findTemplateOrThrow(Long templateId) {
//        return templateRepo.findByTemplateIdAndIsDeletedFalse(templateId)
//                .orElseThrow(() -> new TemplateException(
//                        "Template not found: " + templateId));
//    }
//}
