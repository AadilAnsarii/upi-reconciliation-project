//package com.jpb.reconciliation.reconciliation.service;
//
//
//
//public interface SftpConfigService {
//
//    /**
//     * Upsert — creates a new SftpConfig row if none exists for this template,
//     * or updates the existing one.
//     * Uses SftpConfigRepository.findByTemplateMaster_TemplateId and save().
//     * Writes a SAVE_SFTP_CONFIG audit entry via TemplateAuditService.
//     */
//    SftpConfigResponse saveSftpConfig(Long templateId, SftpConfigRequest request);
//
//    /**
//     * Fetches the SftpConfig row for the template.
//     * Throws TemplateException if no row exists.
//     */
//    SftpConfigResponse getSftpConfig(Long templateId);
//
//    /**
//     * Performs a live SFTP connection test.
//     * Persists lastTestedAt and lastTestStatus on the config row.
//     * Writes a TEST_SFTP audit entry.
//     */
//    SftpTestResult testConnection(Long templateId);
//
//    /**
//     * Hard-deletes the SftpConfig row for the template.
//     * Throws TemplateException if no row exists.
//     * Writes a DELETE_SFTP_CONFIG audit entry.
//     */
//    void deleteSftpConfig(Long templateId);
//}
