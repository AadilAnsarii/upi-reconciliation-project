package com.jpb.reconciliation.reconciliation.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigDTO;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.TemplateDTO;

public interface FileConfigService {

	List<TemplateDTO> getAllTemplates();

	TemplateDTO getTemplateById(Long templateId);

	Page<FileConfigDTO> getAllFileConfigs(int page, int size, Long templateId, String fileName);

	FileConfigDTO getFileConfigById(Long fileId);

	ResponseEntity<RestWithStatusList> createFileConfig(FileConfigRequest request, Long userId);

	FileConfigDTO updateFileConfig(Long fileId, FileConfigRequest request, Long userId);

	ResponseEntity<RestWithStatusList> deleteFileConfig(Long fileId);
}
