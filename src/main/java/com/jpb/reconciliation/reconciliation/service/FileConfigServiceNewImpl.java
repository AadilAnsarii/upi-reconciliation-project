package com.jpb.reconciliation.reconciliation.service;



import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigRequest;
import com.jpb.reconciliation.reconciliation.entity.ProcessMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileIngestConfig;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileIngestConfigRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFileTmpltMastRepository;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileConfigServiceNewImpl implements FileConfigServiceNew {

    private final ReconFileIngestConfigRepository  fileConfigRepository;
    private final ReconFileTmpltMastRepository     templateRepository;
    private final ProcessMasterRepository          processRepository;

    // =========================================================================
    // GET ALL TEMPLATES  (for dropdown / selection)
    // data key: "templates"
    // No pagination — returns full active list for dropdown use
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithMapStatusList> getAllTemplates() {
        List<ReconFileTmpltMast> templates = templateRepository.findAllTemplates();

        List<Map<String, Object>> rows = templates.stream()
                .map(t -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("templateId",   t.getTemplateId());
                    row.put("templateCode", t.getTemplateCode());
                    row.put("templateName", t.getTemplateName());
                    row.put("templateType", t.getTemplateType());
                    row.put("status",       t.getStatus());
                    return row;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ResponseBuilder.ok("Templates fetched successfully.", "templates", rows));
    }

    // =========================================================================
    // GET TEMPLATE BY ID
    // data key: "template"
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId) {
        ReconFileTmpltMast t = templateRepository.findById(templateId)
                .filter(tmpl -> !CommonConstants.INACTIVE.equalsIgnoreCase(tmpl.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template not found: " + templateId));

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("templateId",   t.getTemplateId());
        row.put("templateCode", t.getTemplateCode());
        row.put("templateName", t.getTemplateName());
        row.put("templateType", t.getTemplateType());
        row.put("status",       t.getStatus());
        row.put("fileEncoding", t.getFileEncoding());
        row.put("delimiter",    t.getDelimiter());
        row.put("hasHeader",    t.getHasHeader());
        row.put("hasTrailer",   t.getHasTrailer());

        // FIX 1: List.of() → Collections.singletonList() (Java 8 compatible)
        // FIX 2: Removed extra closing parenthesis ')' that caused compile error
        return ResponseEntity.ok(
                ResponseBuilder.ok("Template fetched successfully.", "template", Collections.singletonList(row)));
    }

    // =========================================================================
    // GET ALL FILE CONFIGS  (paginated + filtered)
    // data keys: "fileConfigs" + "pagination"
    //
    // FIX 1: Added input validation (was missing — viewTemplate had it but this didn't)
    // FIX 2: Empty result now returns "pagination" key via ResponseBuilder.okPaged()
    // FIX 3: Removed duplicate buildPaginationMap — uses ResponseBuilder.okPaged()
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithMapStatusList> getAllFileConfigs(int page, int size,
                                                                   Long templateId, String fileName) {
        // FIX: input validation (consistent with viewTemplate / searchTemplate)
        if (page < 0) {
            return ResponseEntity.badRequest().body(
                    ResponseBuilder.failure("Page number cannot be negative."));
        }
        if (size <= 0 || size > 100) {
            return ResponseEntity.badRequest().body(
                    ResponseBuilder.failure("Size must be between 1 and 100."));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ReconFileIngestConfig> result =
                fileConfigRepository.findByFilters(templateId, fileName, pageable);

        // FIX: empty result returns pagination metadata too
        if (result.isEmpty()) {
            return ResponseEntity.ok(
                    ResponseBuilder.okPaged(
                            "No file configurations found.",
                            "fileConfigs",
                            Collections.emptyList(),
                            result));
        }

        List<Map<String, Object>> rows = result.getContent().stream()
                .map(this::toRowMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ResponseBuilder.okPaged(
                        "File configurations fetched successfully.",
                        "fileConfigs",
                        rows,
                        result));
    }

    // =========================================================================
    // GET FILE CONFIG BY ID
    // data key: "fileConfig"
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithMapStatusList> getFileConfigById(Long fileId) {
        ReconFileIngestConfig config = fileConfigRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File configuration not found: " + fileId));

        // FIX: List.of() → Collections.singletonList() (Java 8 compatible)
        return ResponseEntity.ok(
                ResponseBuilder.ok("File configuration fetched successfully.",
                        "fileConfig", Collections.singletonList(toRowMap(config))));
    }

    // =========================================================================
    // CREATE FILE CONFIG
    // data key: "created"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> createFileConfig(FileConfigRequest request,
                                                                   Long userId) {
        ReconFileTmpltMast template = templateRepository.findById(request.getRtdTemplateId())
                .filter(t -> !CommonConstants.INACTIVE.equalsIgnoreCase(t.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template not found: " + request.getRtdTemplateId()));

        ProcessMasterEntity process = processRepository.findById(request.getProcessMastId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Process not found: " + request.getProcessMastId()));

        ReconFileIngestConfig config = new ReconFileIngestConfig();
        mapRequestToEntity(config, request, template, process);
        config.setCreatedBy(userId);
        config.setCreatedAt(LocalDateTime.now());

        ReconFileIngestConfig saved = fileConfigRepository.save(config);
        log.info("File ingest config created. ID: {}", saved.getIngestConfigId());

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ingestConfigId", saved.getIngestConfigId());
        row.put("fileName",       saved.getFileName());
        row.put("templateId",     saved.getTemplate().getTemplateId());
        row.put("templateName",   saved.getTemplate().getTemplateName());

        // FIX: List.of() → Collections.singletonList() (Java 8 compatible)
        return new ResponseEntity<>(
                ResponseBuilder.ok("File configuration created successfully.",
                        "created", Collections.singletonList(row)),
                HttpStatus.CREATED);
    }

    // =========================================================================
    // UPDATE FILE CONFIG
    // data key: "updated"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> updateFileConfig(Long fileId,
                                                                   FileConfigRequest request,
                                                                   Long userId) {
        ReconFileIngestConfig config = fileConfigRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File configuration not found: " + fileId));

        ReconFileTmpltMast template = templateRepository.findById(request.getRtdTemplateId())
                .filter(t -> !CommonConstants.INACTIVE.equalsIgnoreCase(t.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template not found: " + request.getRtdTemplateId()));

        ProcessMasterEntity process = processRepository.findById(request.getProcessMastId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Process not found: " + request.getProcessMastId()));

        mapRequestToEntity(config, request, template, process);
        config.setUpdatedBy(userId);
        config.setUpdatedAt(LocalDateTime.now());

        ReconFileIngestConfig saved = fileConfigRepository.save(config);
        log.info("File ingest config updated. ID: {}", saved.getIngestConfigId());

        // FIX: List.of() → Collections.singletonList() (Java 8 compatible)
        return ResponseEntity.ok(
                ResponseBuilder.ok("File configuration updated successfully.",
                        "updated", Collections.singletonList(toRowMap(saved))));
    }

    // =========================================================================
    // DELETE FILE CONFIG
    // data key: "deleted"
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> deleteFileConfig(Long fileId) {
        ReconFileIngestConfig config = fileConfigRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File configuration not found: " + fileId));

        fileConfigRepository.delete(config);
        log.info("File ingest config deleted. ID: {}", fileId);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("deletedFileConfigId", fileId);

        // FIX: List.of() → Collections.singletonList() (Java 8 compatible)
        return ResponseEntity.ok(
                ResponseBuilder.ok("File configuration deleted successfully.",
                        "deleted", Collections.singletonList(row)));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void mapRequestToEntity(ReconFileIngestConfig e, FileConfigRequest r,
                                     ReconFileTmpltMast template, ProcessMasterEntity process) {
        e.setFileName(r.getRfdFileName());
        e.setShortName(r.getRfdShortName());
        e.setFileDescription(r.getRfdFileDescription());
        e.setFileType(r.getRfdFileType());
        e.setFileLocation(r.getRfdFileLocation());
        e.setFileDelimiter(r.getRfdFileDelimiter());
        e.setDestPath(r.getRfdFileDestPath());
        e.setDupCheckFlag(r.getRfdFileDupChkFlag());
        e.setFileDefineConst(r.getRfdFileDefineConst());
        e.setFilenameLength(r.getRfdFilenameLength());
        e.setNameConvFormat(r.getRfdNameConvFormat());
        e.setDependentFileId(r.getRfdDependentFileId());
        e.setFileUpdateFlag(r.getFileUpdateFlag());
        e.setHdrAvailFlag(r.getRfdHdrAvlFlag());
        e.setHdrBlockSize(r.getRfdHdrBlockSize());
        e.setHdrId(r.getRfdHdrId());
        e.setHdrKeyCount(r.getRfdHdrKeyCount());
        e.setHdrWithDr(r.getRfdHdrWithDr());
        e.setFtrAvailFlag(r.getRfdFtrAvailFlag());
        e.setFtrBeginConstVal(r.getRfdFtrBeginConstVal());
        e.setFtrLength(r.getRfdFtrLength());
        e.setFtrType(r.getRfdFtrType());
        e.setFtrCtrlTagCnt(r.getRfdFtrCtrlTagCnt());
        e.setDrBlockSize(r.getRfdDrBlockSize());
        e.setDrBlockSizeFlag(r.getRfdDrBlockSizeFlag());
        e.setDrFormat(r.getRfdDrFormat());
        e.setMultiDrCheck(r.getRfdMultiDrCheck());
        e.setMultiDrCount(r.getRfdMultiDrCount());
        e.setSftpFilePath(r.getRfdFtpFilePath());
        e.setSftpServerNameLegacy(r.getRfdFtpServerName());
        e.setEmailSmsFlag(r.getRfdEmailSmsFlag());
        e.setExitMenuFlag(r.getRfdExtMenuFlag());
        e.setExitMenuName(r.getRfdExtMenuName());
        e.setGlFlag(r.getRfdGlFlag());
        e.setTranFileFlag(r.getRfdTranFileFlag());
        e.setSettleFlag(r.getRfdSettleFlg());
        e.setJpslRpsl(r.getRfdJpslRpsl());
        e.setXsdName(r.getRfdXsdName());
        e.setInstCode(r.getRfdInstCode());
        e.setTemplate(template);
        e.setProcessMastId(process.getProcessMastId());
    }

    private Map<String, Object> toRowMap(ReconFileIngestConfig e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ingestConfigId",  e.getIngestConfigId());
        m.put("fileName",        e.getFileName());
        m.put("shortName",       e.getShortName());
        m.put("fileDescription", e.getFileDescription());
        m.put("fileType",        e.getFileType());
        m.put("fileLocation",    e.getFileLocation());
        m.put("fileDelimiter",   e.getFileDelimiter());
        m.put("destPath",        e.getDestPath());
        m.put("dupCheckFlag",    e.getDupCheckFlag());
        m.put("hdrAvailFlag",    e.getHdrAvailFlag());
        m.put("ftrAvailFlag",    e.getFtrAvailFlag());
        m.put("drFormat",        e.getDrFormat());
        m.put("sftpFilePath",    e.getSftpFilePath());
        m.put("tranFileFlag",    e.getTranFileFlag());
        m.put("glFlag",          e.getGlFlag());
        m.put("settleFlag",      e.getSettleFlag());
        m.put("createdBy",       e.getCreatedBy());
        m.put("createdAt",       e.getCreatedAt());
        m.put("updatedBy",       e.getUpdatedBy());
        m.put("updatedAt",       e.getUpdatedAt());
        if (e.getTemplate() != null) {
            m.put("templateId",   e.getTemplate().getTemplateId());
            m.put("templateName", e.getTemplate().getTemplateName());
        }
        m.put("processMastId",   e.getProcessMastId());
        return m;
    }
}