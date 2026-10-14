package com.jpb.reconciliation.reconciliation.controller;
import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigDTO;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.TemplateDTO;
import com.jpb.reconciliation.reconciliation.service.FileConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/file/")
@RequiredArgsConstructor
@Slf4j
public class FileConfigController {

    private final FileConfigService fileConfigService;

    /**
     * API 1: Get all configured templates for dropdown/selection
     * GET /api/templates
     */
    @GetMapping("/templates")
    public ResponseEntity<List<TemplateDTO>> getAllTemplates() {
        log.info("REST request to get all templates");
        List<TemplateDTO> templates = fileConfigService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * API 2: Get template details by ID
     * GET /api/templates/{templateId}
     */
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<TemplateDTO> getTemplateById(@PathVariable Long templateId) {
        log.info("REST request to get template by ID: {}", templateId);
        TemplateDTO template = fileConfigService.getTemplateById(templateId);
        return ResponseEntity.ok(template);
    }

    /**
     * API 3: Get all file configurations with pagination and filters
     * GET /api/file-configurations?page=0&size=10&templateId=1&fileName=test
     */
    @GetMapping("/file-configurations")
    public ResponseEntity<Page<FileConfigDTO>> getAllFileConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String fileName) {
        
        log.info("REST request to get file configurations - page: {}, size: {}, templateId: {}, fileName: {}", 
                page, size, templateId, fileName);
        
        Page<FileConfigDTO> fileConfigs = fileConfigService.getAllFileConfigs(
                page, size, templateId, fileName);
        
        return ResponseEntity.ok(fileConfigs);
    }

    /**
     * API 4: Get file configuration by ID
     * GET /api/file-configurations/{fileId}
     */
    @GetMapping("/file-configurations/{fileId}")
    public ResponseEntity<FileConfigDTO> getFileConfigById(@PathVariable Long fileId) {
        log.info("REST request to get file configuration by ID: {}", fileId);
        FileConfigDTO fileConfig = fileConfigService.getFileConfigById(fileId);
        return ResponseEntity.ok(fileConfig);
    }

    /**
     * API 5: Create new file configuration
     * POST /api/file-configurations
     */
    @PostMapping("/file-configurations")
    public ResponseEntity<RestWithStatusList> createFileConfig(
            @Valid @RequestBody FileConfigRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        
        log.info("REST request to create file configuration: {}", request.getRfdFileName());
        return fileConfigService.createFileConfig(request, userId);
    }

    /**
     * API 6: Update existing file configuration
     * PUT /api/file-configurations/{fileId}
     */
    @PutMapping("/file-configurations/{fileId}")
    public ResponseEntity<FileConfigDTO> updateFileConfig(
            @PathVariable Long fileId,
            @Valid @RequestBody FileConfigRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        
        log.info("REST request to update file configuration with ID: {}", fileId);
        FileConfigDTO updatedConfig = fileConfigService.updateFileConfig(fileId, request, userId);
        return ResponseEntity.ok(updatedConfig);
    }

    /**
     * API 7: Delete file configuration
     * DELETE /api/file-configurations/{fileId}
     */
    @DeleteMapping("/file-configurations/{fileId}")
    public ResponseEntity<RestWithStatusList> deleteFileConfig(@PathVariable Long fileId) {
        log.info("REST request to delete file configuration with ID: {}", fileId);
        fileConfigService.deleteFileConfig(fileId);
        return ResponseEntity.noContent().build();
    }
}