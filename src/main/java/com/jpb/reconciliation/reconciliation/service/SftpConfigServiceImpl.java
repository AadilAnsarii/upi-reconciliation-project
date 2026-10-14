//
//import java.time.LocalDateTime;
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * Implementation of SftpConfigService.
// *
// * Repositories used  (all exist in /repository):
// *   SftpConfigRepository        – findByTemplateMaster_TemplateId, save, delete
// *   TemplateMasterRepository    – findByTemplateIdAndIsDeletedFalse
// *
// * Mapper methods used  (all exist in TemplateMapper):
// *   mapper.updateSftpEntity(SftpConfig, SftpConfigRequest)
// *   mapper.toSftpResponse(SftpConfig) → SftpConfigResponse
// *
// * Audit service used (TemplateAuditService / TemplateAuditServiceImpl):
// *   auditService.log(templateId, action, entityType, entityId, oldValue, newValue)
// */
//@Service
//@Transactional
//@RequiredArgsConstructor
//@Slf4j
//public class SftpConfigServiceImpl implements SftpConfigService {
//
//    private final SftpConfigRepository     sftpRepo;        // SftpConfigRepository
//    private final TemplateMasterRepository  templateRepo;    // TemplateMasterRepository
//    private final TemplateAuditService      auditService;    // TemplateAuditService
//    private final TemplateMapper            mapper;          // TemplateMapper
//
//    // =========================================================================
//    // SAVE / UPDATE  (upsert)
//    // =========================================================================
//
//    @Override
//    public SftpConfigResponse saveSftpConfig(Long templateId, SftpConfigRequest request) {
//        TemplateMaster template = findTemplateOrThrow(templateId);
//
//        // Fetch existing row or create a new entity with only the FK set
//        SftpConfig config = sftpRepo.findByTemplateMaster_TemplateId(templateId)
//                .orElseGet(() -> SftpConfig.builder()
//                        .templateMaster(template)
//                        .build());
//
//        // TemplateMapper.updateSftpEntity — null-safe, only overwrites non-null fields
//        mapper.updateSftpEntity(config, request);
//
//        SftpConfig saved = sftpRepo.save(config);
//        log.info("SFTP config saved for templateId={}", templateId);
//
//        auditService.log(templateId, "SAVE_SFTP_CONFIG", "SFTP_CONFIG",
//                saved.getSftpConfigId(), null, saved);
//
//        return mapper.toSftpResponse(saved);
//    }
//
//    // =========================================================================
//    // GET
//    // =========================================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public SftpConfigResponse getSftpConfig(Long templateId) {
//        SftpConfig config = sftpRepo.findByTemplateMaster_TemplateId(templateId)
//                .orElseThrow(() -> new TemplateException(
//                        "SFTP config not found for template: " + templateId));
//        return mapper.toSftpResponse(config);
//    }
//
//    // =========================================================================
//    // TEST CONNECTION
//    // =========================================================================
//
//    @Override
//    public SftpTestResult testConnection(Long templateId) {
//        SftpConfig config = sftpRepo.findByTemplateMaster_TemplateId(templateId)
//                .orElseThrow(() -> new TemplateException(
//                        "SFTP config not found for template: " + templateId));
//
//        SftpTestResult result = performConnectionTest(config);
//
//        // Persist test outcome regardless of pass / fail
//        config.setLastTestedAt(LocalDateTime.now());
//        config.setLastTestStatus(result.isSuccess() ? CommonConstants.SUCCESS : "FAILED");
//        sftpRepo.save(config);
//
//        auditService.log(templateId, "TEST_SFTP", "SFTP_CONFIG",
//                config.getSftpConfigId(), null,
//                "Test result: " + (result.isSuccess() ? CommonConstants.SUCCESS : "FAILED")
//                        + " — " + result.getMessage());
//
//        log.info("SFTP test for templateId={}: {}", templateId,
//                result.isSuccess() ? CommonConstants.SUCCESS : "FAILED");
//        return result;
//    }
//
//    // =========================================================================
//    // DELETE
//    // =========================================================================
//
//    @Override
//    public void deleteSftpConfig(Long templateId) {
//        SftpConfig config = sftpRepo.findByTemplateMaster_TemplateId(templateId)
//                .orElseThrow(() -> new TemplateException(
//                        "SFTP config not found for template: " + templateId));
//
//        Long configId = config.getSftpConfigId();
//        sftpRepo.delete(config);
//        log.info("SFTP config deleted for templateId={}", templateId);
//
//        auditService.log(templateId, "DELETE_SFTP_CONFIG", "SFTP_CONFIG",
//                configId, config, null);
//    }
//
//    // =========================================================================
//    // PRIVATE HELPERS
//    // =========================================================================
//
//    /**
//     * Live SFTP connection test.
//     *
//     * Replace the placeholder body with real JSch logic:
//     *
//     *   JSch jsch = new JSch();
//     *   if ("KEY_BASED".equals(config.getAuthType())) {
//     *       String key = credentialResolver.resolve(config.getCredentialRef());
//     *       jsch.addIdentity("key", key.getBytes(), null, null);
//     *   }
//     *   Session session = jsch.getSession(
//     *           config.getUsername(), config.getHost(), config.getPort());
//     *   if ("PASSWORD".equals(config.getAuthType())) {
//     *       session.setPassword(credentialResolver.resolve(config.getCredentialRef()));
//     *   }
//     *   session.setConfig("StrictHostKeyChecking", "no");
//     *   session.connect(5_000);
//     *   session.disconnect();
//     */
//    private SftpTestResult performConnectionTest(SftpConfig config) {
//        try {
//            log.info("Testing SFTP connection to {}:{} (user: {})",
//                    config.getHost(), config.getPort(), config.getUsername());
//
//            // ── Placeholder — wire in JSch / Apache Commons VFS here ──────────
//            return SftpTestResult.builder()
//                    .success(true)
//                    .message("Connection successful (placeholder — integrate JSch)")
//                    .testedAt(LocalDateTime.now())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("SFTP connection test failed for {}:{} — {}",
//                    config.getHost(), config.getPort(), e.getMessage());
//            return SftpTestResult.builder()
//                    .success(false)
//                    .message("Connection failed: " + e.getMessage())
//                    .testedAt(LocalDateTime.now())
//                    .build();
//        }
//    }
//
//    private TemplateMaster findTemplateOrThrow(Long templateId) {
//        return templateRepo.findByTemplateIdAndIsDeletedFalse(templateId)
//                .orElseThrow(() -> new TemplateException(
//                        "Template not found: " + templateId));
//    }
//}
