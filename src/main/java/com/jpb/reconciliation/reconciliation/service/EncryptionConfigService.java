package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.EncryptionConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.EncryptionConfigResponse;

public interface EncryptionConfigService {

    /**
     * Upsert — creates a new encryption config row if none exists for this template,
     * or updates the existing row if one already exists.
     * Passing encryptionType=NONE effectively disables encryption.
     * Writes a SAVE_ENCRYPTION_CONFIG audit log entry.
     *
     * @param templateId  target template
     * @param request     encryptionRequired (Boolean), encryptionType (NONE|PGP|AES|GPG),
     *                    keyReference (vault path), decryptionCommand,
     *                    preProcessingSteps (String), postProcessingSteps (String)
     */
    EncryptionConfigResponse saveEncryptionConfig(Long templateId,
                                                   EncryptionConfigRequest request);

    /**
     * Fetch the encryption config for the given template.
     * Throws TemplateException if no config row exists.
     */
    EncryptionConfigResponse getEncryptionConfig(Long templateId);
}
