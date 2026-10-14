package com.jpb.reconciliation.reconciliation.service;


import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigRequest;

public interface FileConfigServiceNew {

    ResponseEntity<RestWithMapStatusList> getAllTemplates();

    ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId);

    ResponseEntity<RestWithMapStatusList> getAllFileConfigs(int page, int size,
                                                           Long templateId, String fileName);

    ResponseEntity<RestWithMapStatusList> getFileConfigById(Long fileId);

    ResponseEntity<RestWithMapStatusList> createFileConfig(FileConfigRequest request, Long userId);

    ResponseEntity<RestWithMapStatusList> updateFileConfig(Long fileId,
                                                           FileConfigRequest request, Long userId);

    ResponseEntity<RestWithMapStatusList> deleteFileConfig(Long fileId);
}
