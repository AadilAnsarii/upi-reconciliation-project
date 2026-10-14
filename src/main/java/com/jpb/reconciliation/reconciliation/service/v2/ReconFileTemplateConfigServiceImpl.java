package com.jpb.reconciliation.reconciliation.service.v2;


import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.v2.SftpConstants;
import com.jpb.reconciliation.reconciliation.constants.v2.TemplateConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconFieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconFileTemplateMastDto;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconTemplateConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldFormatMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldTypeMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconSftpServerMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.mapper.v2.ReconFieldDetailsMapper;
import com.jpb.reconciliation.reconciliation.mapper.v2.ReconFileTemplateMastMapper;
import com.jpb.reconciliation.reconciliation.mapper.v2.ReconSftpServerMapper;
import com.jpb.reconciliation.reconciliation.entity.ReconSourceSystemMast;
import com.jpb.reconciliation.reconciliation.repository.ReconSourceSystemMastRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFieldFormatMastRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFieldTypeMastRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFileTmpltMastRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconSftpServerMastRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconTmpltFieldDtlsRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.service.ReconFieldDtlMastService;
import com.jpb.reconciliation.reconciliation.util.CommonUtil;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
@Transactional(readOnly = true)
public class ReconFileTemplateConfigServiceImpl implements ReconFileTemplateConfigService {

    Logger logger = LoggerFactory.getLogger(ReconFileTemplateConfigServiceImpl.class);

    @Autowired ReconFileTmpltMastRepository      templateRepository;
    @Autowired ReconFieldFormatMastRepository    fieldFormatRepository;
    @Autowired ReconFieldTypeMastRepository      fieldTypeRepository;
    @Autowired ReconTmpltFieldDtlsRepository     fieldDetailsRepository;
    @Autowired ReconFieldDtlMastService          reconFieldDtlMastService;
    @Autowired ObjectMapper                      objectMapper;
    @Autowired ReconSourceSystemMastRepository   sourceSystemRepository;

    @Autowired
    ReconSftpServerService  reconSftpServerService;

    @Autowired
    ReconSftpServerMastRepository sftpServerRepo;

    @Autowired
    ReconSftpServerMapper reconSftpServerMapper;

    @Autowired
    ReconExecScheduleConfigService reconExecScheduleConfigService;

    @Autowired AuditLogService    auditLogService;
    @Autowired ReconUserRepository reconUserRepository;

    @Autowired @Lazy
    private ReconFileTemplateConfigServiceImpl self;

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    public ReconFileTemplateConfigServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // =========================================================================
    // CONFIGURE TEMPLATE + FIELDS (CREATE)
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> configureTemplateAndFieldData(ReconTemplateConfigRequest request) {

        // Normalise case
        if (null != request.getDeliverMode()) request.setDeliverMode(request.getDeliverMode().toUpperCase());
        if (null != request.getAction()) request.setAction(request.getAction().toUpperCase());
        if (null != request.getTemplateType()) request.setTemplateType(request.getTemplateType().toUpperCase());

        // templateId present → UPDATE via configure API; absent → CREATE
        if (null != request.getTemplateId()) {
            return updateTemplate(request);
        }

        boolean isDraft = TemplateConstants.ACTION_DRAFT.equalsIgnoreCase(request.getAction());
        if (!isDraft && (null == request.getFieldDetails() || request.getFieldDetails().isEmpty())) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("fieldDetails must not be null or empty"),
                    HttpStatus.BAD_REQUEST);
        }

        // ── Validation — DRAFT bypasses logical validations, SAVE runs full ──
        String valErr = isDraft ? validateDraftRequest(request) : validateRequest(request);
        if (null != valErr) {
            return new ResponseEntity<>(ResponseBuilder.failure(valErr), HttpStatus.BAD_REQUEST);
        }

        // Deleted templates no longer reserve their name — see findLiveByTemplateName.
        if (!templateRepository.findLiveByTemplateName(request.getTemplateName()).isEmpty()) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Template already configured."),
                    HttpStatus.BAD_REQUEST);
        }

        ReconFileTmpltMast savedTemplate;
        try {
            savedTemplate = self.saveTemplateAndFields(request);
            logger.info("Template saved. ID: {}, Code: {}",
                    savedTemplate.getTemplateId(), savedTemplate.getTemplateCode());

        } catch (IllegalArgumentException e) {
            logger.error("Validation error while saving template: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.failure(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity error saving template: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Template name already exists or a required field is missing."),
                    HttpStatus.CONFLICT);
        } catch (Exception e) {
            logger.error("Unexpected error saving template: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to save template: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        
      // DRAFT templates skip SP — stage table created only when SAVE (ACTIVE)
      if (!TemplateConstants.ACTION_DRAFT.equalsIgnoreCase(request.getAction())) {
          String spResult;
          try {
              spResult = self.callStageTableProcedure(savedTemplate);
              logger.info("SP result for [{}]: {}", savedTemplate.getTemplateCode(), spResult);
          } catch (Exception e) {
              logger.error("SP exception for [{}]. Rolling back.", savedTemplate.getTemplateCode(), e);
              self.rollbackTemplateAndFields(savedTemplate.getTemplateId());
              return new ResponseEntity<>(
                      ResponseBuilder.error(
                              "Stage table creation failed. Template rolled back. " + e.getMessage()),
                      HttpStatus.INTERNAL_SERVER_ERROR);
          }

          if (null == spResult || !spResult.equalsIgnoreCase("OK")) {
              logger.warn("SP returned [{}] for [{}]. Rolling back.",
                      spResult, savedTemplate.getTemplateCode());
              self.rollbackTemplateAndFields(savedTemplate.getTemplateId());
              return new ResponseEntity<>(
                      ResponseBuilder.error(
                              "Stage table creation failed. Template rolled back. Reason: " + spResult),
                      HttpStatus.INTERNAL_SERVER_ERROR);
          }
      } else {
          logger.info("DRAFT template [{}] — skipping SP_STAGE_TAB_CREATION.", savedTemplate.getTemplateCode());
      }

        // Return full DTO — JOIN FETCH so sourceSystem + fields are eagerly loaded
        List<ReconFileTmpltMast> found = templateRepository.findByIdWithDetails(savedTemplate.getTemplateId());
        ReconFileTmpltMast fullTemplate = found.isEmpty() ? savedTemplate : found.get(0);
        ReconFileTemplateMastDto dto = enrichWithSftp(ReconFileTemplateMastMapper.toDTO(fullTemplate), fullTemplate);
        Map<String, Object> row = ResponseBuilder.toMap(dto, objectMapper);

        long fieldCount = fieldDetailsRepository.findActiveFieldsByTemplateId(
                fullTemplate.getTemplateId()).size();
        auditTemplate(fullTemplate, "CREATE", request.getCreatedBy(),
                null,
                templateSnapshot(fullTemplate, fieldCount),
                isDraft ? "Template saved as draft" : "Template created",
                "Created via template-configure. Stage table: "
                        + (isDraft ? "not created (DRAFT)" : fullTemplate.getStageTabName()));

        return ResponseEntity.ok(
                ResponseBuilder.ok("Template configured successfully.", "template", Collections.singletonList(row)));
    }

    // =========================================================================
    // UPDATE TEMPLATE BY ID  (PUT endpoint entry point)
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> updateTemplateById(Long templateId,
                                                                     ReconTemplateConfigRequest request) {
        // If body also carries a templateId it must match the path variable
        if (request.getTemplateId() != null && !request.getTemplateId().equals(templateId)) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure(
                            "templateId in request body (" + request.getTemplateId()
                            + ") does not match path variable (" + templateId + ")."),
                    HttpStatus.BAD_REQUEST);
        }

        request.setTemplateId(templateId);   // path variable is authoritative
        return configureTemplateAndFieldData(request);
    }

    // =========================================================================
    // VIEW TEMPLATE — paginated
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> viewTemplate(String action, int page, int size) {
        try {
            if (page < 0) {
                return ResponseEntity.badRequest().body(
                        ResponseBuilder.failure("Page number cannot be negative."));
            }
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest().body(
                        ResponseBuilder.failure("Size must be between 1 and 100."));
            }

            // action=D → DRAFT, action=Y → ACTIVE, null/absent → all templates
            String statusFilter = null;
            if ("D".equalsIgnoreCase(action)) {
                statusFilter = TemplateConstants.STATUS_DRAFT;
            } else if (TemplateConstants.FLAG_YES.equalsIgnoreCase(action)) {
                statusFilter = TemplateConstants.STATUS_ACTIVE;
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<ReconFileTmpltMast> templatePage =
                    templateRepository.findAllWithFilters(statusFilter, null, null, pageable);

            if (templatePage.isEmpty()) {
                return ResponseEntity.ok(
                        ResponseBuilder.okPaged(
                                "No templates available.",
                                "templates",
                                Collections.emptyList(),
                                templatePage));
            }

            List<ReconFileTmpltMast> withDetails =
                    templateRepository.fetchTemplateDetails(templatePage.getContent());
            List<ReconFileTemplateMastDto> dtos = enrichListWithSftp(
                    ReconFileTemplateMastMapper.toDTOList(withDetails), withDetails);
            List<Map<String, Object>> rows = ResponseBuilder.toMapList(new ArrayList<>(dtos), objectMapper);

            return ResponseEntity.ok(
                    ResponseBuilder.okPaged(
                            "Templates retrieved successfully.",
                            "templates",
                            rows,
                            templatePage));

        } catch (Exception e) {
            logger.error("Error fetching templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.error("Error retrieving templates: " + e.getMessage()));
        }
    }

    // =========================================================================
    // DELETE TEMPLATE
    // =========================================================================

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> deleteTemplate(Long templateId, String actor) {
        Optional<ReconFileTmpltMast> opt = templateRepository.findById(templateId)
                .filter(t -> !TemplateConstants.STATUS_INACTIVE.equalsIgnoreCase(t.getStatus()));
        if (!opt.isPresent()) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Template not found: " + templateId),
                    HttpStatus.NOT_FOUND);
        }

        int fieldsDeleted;
        ReconFileTmpltMast template = opt.get();
        String beforeStatus = template.getStatus();
        try {
            // Fully soft delete — the template goes INACTIVE and its fields are flagged
            // IS_DELETED = 'Y' rather than removed, so the record of what the template
            // was made of survives the delete (previously the fields were hard-deleted,
            // which made the "soft" delete unrecoverable in practice).
            template.setStatus(TemplateConstants.STATUS_INACTIVE);
            template.setUpdatedBy(actor);
            templateRepository.save(template);
            fieldsDeleted = fieldDetailsRepository.softDeleteByTemplateId(templateId, actor);
            logger.info("Template soft-deleted. ID: {}, fields flagged: {}, by: {}",
                    templateId, fieldsDeleted, actor);

            auditTemplate(template, "DELETE", actor,
                    "status=" + beforeStatus + ", activeFields=" + fieldsDeleted,
                    "status=" + TemplateConstants.STATUS_INACTIVE + ", activeFields=0",
                    "Template deleted",
                    "Soft delete: template set INACTIVE, " + fieldsDeleted
                            + " field definition(s) flagged IS_DELETED='Y'. Stage table "
                            + template.getStageTabName() + " left intact.");

        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity error deleting template {}: {}", templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Cannot delete template: it is referenced by other records."),
                    HttpStatus.CONFLICT);
        } catch (Exception e) {
            logger.error("Unexpected error deleting template {}: {}", templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to delete template: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("deletedTemplateId", templateId);

        return ResponseEntity.ok(
                ResponseBuilder.ok("Template deleted successfully.", "deleted", Collections.singletonList(row)));
    }

    // =========================================================================
    // SEARCH TEMPLATE — paginated
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> searchTemplate(String name, String type,
                                                                 int page, int size) {
        if (page < 0) {
            return ResponseEntity.badRequest().body(
                    ResponseBuilder.failure("Page number cannot be negative."));
        }
        if (size <= 0 || size > 100) {
            return ResponseEntity.badRequest().body(
                    ResponseBuilder.failure("Size must be between 1 and 100."));
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ReconFileTmpltMast> results;

            if (null != name && null != type) {
                results = templateRepository
                        .findByTemplateNameContainingIgnoreCaseAndTemplateType(name, type, pageable);
            } else if (null != name) {
                results = templateRepository
                        .findByTemplateNameContainingIgnoreCase(name, pageable);
            } else if (null != type) {
                results = templateRepository.findByTemplateType(type, pageable);
            } else {
                results = templateRepository.findTemplates(pageable);
            }

            if (results.isEmpty()) {
                return ResponseEntity.ok(
                        ResponseBuilder.okPaged(
                                "No templates found matching the search criteria.",
                                "templates",
                                Collections.emptyList(),
                                results));
            }

            List<ReconFileTmpltMast> withDetails =
                    templateRepository.fetchTemplateDetails(results.getContent());
            List<ReconFileTemplateMastDto> dtos = enrichListWithSftp(
                    ReconFileTemplateMastMapper.toDTOList(withDetails), withDetails);
            List<Map<String, Object>> rows = ResponseBuilder.toMapList(new ArrayList<>(dtos), objectMapper);

            return ResponseEntity.ok(
                    ResponseBuilder.okPaged("Templates fetched successfully.", "templates", rows, results));

        } catch (Exception e) {
            logger.error("Error searching templates (name={}, type={}): {}", name, type, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.error("Error searching templates: " + e.getMessage()));
        }
    }

    // =========================================================================
    // GET TEMPLATE BY ID
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId) {
        try {
            List<ReconFileTmpltMast> results = templateRepository.findByIdWithDetails(templateId);
            ReconFileTmpltMast entity = results.isEmpty() ? null : results.get(0);
            if (null == entity || TemplateConstants.STATUS_INACTIVE.equalsIgnoreCase(entity.getStatus())) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("Template not found: " + templateId),
                        HttpStatus.NOT_FOUND);
            }

            ReconFileTemplateMastDto dto = enrichWithSftp(ReconFileTemplateMastMapper.toDTO(entity), entity);
            Map<String, Object> row = ResponseBuilder.toMap(dto, objectMapper);

            return ResponseEntity.ok(
                    ResponseBuilder.ok("Template fetched successfully.", "template", Collections.singletonList(row)));

        } catch (Exception e) {
            logger.error("Error fetching template {}: {}", templateId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.error("Error fetching template: " + e.getMessage()));
        }
    }
    
    
    @Override
    public ResponseEntity<RestWithMapStatusList> autoDetectFields(MultipartFile file, Integer headerLineCount) {

        try {

            // Validate uploaded file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseBuilder.failure("Please upload a valid CSV file."));
            }

            //// Validate supported file types
            // Validate supported file types
            String fileName = file.getOriginalFilename();

            if (fileName == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseBuilder.failure("Invalid file."));
            }

            fileName = fileName.toLowerCase();

            if (!(fileName.endsWith(".csv")
                    || fileName.endsWith(".xlsx")
                    || fileName.endsWith(".xls")
                    || fileName.endsWith(".xml")
                    || fileName.endsWith(".txt")
                    || fileName.endsWith(".dat")
                    )) {

                return ResponseEntity.badRequest()
                        .body(ResponseBuilder.failure(
                                "Unsupported file type. Allowed: CSV, Excel, XML, Fixed Width."));
            }

            // Parse CSV and build response
            List<ReconFieldConfigurationDto> fieldConfigurations;

            if (fileName.endsWith(".csv")) {

                fieldConfigurations = parseCsv(file);

            } else if (fileName.endsWith(".xlsx")
                    || fileName.endsWith(".xls")) {

                fieldConfigurations = parseExcel(file, headerLineCount);

            } else if (fileName.endsWith(".xml")) {

                fieldConfigurations = parseXml(file);

            } else if (fileName.endsWith(".txt")
                    || fileName.endsWith(".dat")) {

                fieldConfigurations = parseFixedWidth(file);

            } else {

                return ResponseEntity.badRequest()
                        .body(ResponseBuilder.failure(
                                "Unsupported file type. Allowed: CSV, Excel, XML, Fixed Width."));
            }

            List<Map<String, Object>> rows =
            		ResponseBuilder.toMapList(fieldConfigurations, objectMapper);

            return ResponseEntity.ok(
                    ResponseBuilder.ok(
                            "Field detection completed successfully.",
                            "fieldDetails",
                            rows));

        } catch (Exception e) {

            logger.error("Error while reading uploaded CSV file.", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error("Failed to process uploaded CSV file."));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithMapStatusList> previewNextTemplateCode() {
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("templateCode", generateTemplateCode());
            return ResponseEntity.ok(ResponseBuilder.ok(
                    "Preview only — not reserved.", "preview", Collections.singletonList(row)));
        } catch (Exception e) {
            logger.error("Error previewing next template code.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error("Failed to preview template code."));
        }
    }
    // =========================================================================
    // PARSE CSV FILE
    // =========================================================================
    private List<ReconFieldConfigurationDto> parseCsv(MultipartFile file) throws IOException {

        List<ReconFieldConfigurationDto> fieldConfigurations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

             CSVParser csvParser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            Map<String, Integer> headers = csvParser.getHeaderMap();
            List<CSVRecord> records = csvParser.getRecords();

            int sequence = 1;

            for (String header : headers.keySet()) {

                List<String> columnValues = new ArrayList<>();

                for (CSVRecord record : records) {
                    String value = record.get(header);
                    if (value != null && !value.trim().isEmpty()) {
                        columnValues.add(value.trim());
                    }
                }

                fieldConfigurations.add(
                        buildFieldConfiguration(
                                header,
                                sequence++,
                                columnValues));
            }
        }

        return fieldConfigurations;
    }
    // =========================================================================
    // PARSE EXCEL FILE
    // =========================================================================
    /**
     * @param headerLineCount Same field the template header carries into extraction
     *                        (ExcelToCsvConvertorService's headerLineCount) — the 1-based
     *                        row number of the real header. Previously auto-detect always
     *                        read row 0 as the header regardless of this, so a report with
     *                        leading metadata rows (e.g. NEFT-ISO exports: 11 "Login
     *                        Details / Search Criteria" rows before the real header on row
     *                        12) had its metadata row misread as columns — one field per
     *                        populated metadata cell, most of them unnamed ("FIELD").
     *                        Null or &lt;=0 keeps the old row-0 behaviour.
     */
    private List<ReconFieldConfigurationDto> parseExcel(MultipartFile file, Integer headerLineCount)
            throws IOException {

        List<ReconFieldConfigurationDto> fieldConfigurations = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return fieldConfigurations;
            }

            int headerRowIndex = (null != headerLineCount && headerLineCount > 0) ? headerLineCount - 1 : 0;
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                return fieldConfigurations;
            }

            int totalColumns = headerRow.getLastCellNum();

            int sequence = 1;

            for (int columnIndex = 0; columnIndex < totalColumns; columnIndex++) {

                Cell headerCell = headerRow.getCell(columnIndex);

                if (headerCell == null) {
                    continue;
                }

                String header = headerCell.toString().trim();

                List<String> columnValues = new ArrayList<>();

                for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                    Row row = sheet.getRow(rowIndex);

                    if (row == null) {
                        continue;
                    }

                    Cell cell = row.getCell(columnIndex);

                    columnValues.add(cell == null ? "" : cell.toString().trim());
                }

                fieldConfigurations.add(
                        buildFieldConfiguration(
                                header,
                                sequence++,
                                columnValues,
                                columnIndex));
            }
        }

        return fieldConfigurations;
    }
    // =========================================================================
    // PARSE XML FILE
    // =========================================================================
    private List<ReconFieldConfigurationDto> parseXml(MultipartFile file) throws Exception {

        List<ReconFieldConfigurationDto> fieldConfigurations = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(file.getInputStream());

        document.getDocumentElement().normalize();

        NodeList nodeList = document.getDocumentElement().getChildNodes();

        Map<String, List<String>> fieldMap = new LinkedHashMap<>();

        for (int i = 0; i < nodeList.getLength(); i++) {

            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) node;

            NodeList childNodes = element.getChildNodes();

            for (int j = 0; j < childNodes.getLength(); j++) {

                Node child = childNodes.item(j);

                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String fieldName = child.getNodeName();
                String value = child.getTextContent();

                fieldMap.computeIfAbsent(fieldName, key -> new ArrayList<>())
                        .add(value == null ? "" : value.trim());
            }
        }

        int sequence = 1;

        for (Map.Entry<String, List<String>> entry : fieldMap.entrySet()) {

            ReconFieldConfigurationDto dto = buildFieldConfiguration(
                    entry.getKey(),
                    sequence++,
                    entry.getValue());
            // XML templates require xmlXpath on every field at save time (see
            // validateRequest) — buildFieldConfiguration() never sets it (it's
            // shared with CSV/Excel, which don't use it), so every XML auto-detect
            // result used to fail "xmlXpath is required" on save. The tag name IS
            // the xpath for this single-level element structure.
            dto.setXmlXpath(entry.getKey());
            fieldConfigurations.add(dto);
        }

        return fieldConfigurations;
    }
    // =========================================================================
    // PARSE FIXED WIDTH FILE
    // =========================================================================
    // No universal delimiter to split on, and column boundaries can't be
    // reliably inferred from a raw fixed-width file without a record layout —
    // so this treats the file as pipe-delimited with a header row (same
    // simplified assumption as the original stub). Genuine space-padded
    // fixed-width files without a header/delimiter still need manual field
    // entry; this covers the common case where source systems export
    // "fixed width" as pipe-delimited with fixed column order.
    private List<ReconFieldConfigurationDto> parseFixedWidth(MultipartFile file) throws IOException {

        List<ReconFieldConfigurationDto> fieldConfigurations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String> lines = reader.lines().collect(Collectors.toList());

            if (lines.isEmpty()) {
                return fieldConfigurations;
            }

            // First line contains field names separated by '|'
            String[] headers = lines.get(0).split("\\|");

            int sequence = 1;
            int offset = 1; // positionFrom/positionTo required per field — derived here as
                             // contiguous ranges sized to each field's own detected max length,
                             // since a genuine byte layout can't be recovered from pipe-delimited input.

            for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {

                List<String> columnValues = new ArrayList<>();

                for (int rowIndex = 1; rowIndex < lines.size(); rowIndex++) {

                    String[] values = lines.get(rowIndex).split("\\|", -1);

                    if (columnIndex < values.length) {
                        columnValues.add(values[columnIndex].trim());
                    }
                }

                ReconFieldConfigurationDto dto = buildFieldConfiguration(
                        headers[columnIndex].trim(),
                        sequence++,
                        columnValues);
                int len = dto.getFieldLength() != null ? dto.getFieldLength() : 1;
                dto.setPositionFrom(offset);
                dto.setPositionTo(offset + len - 1);
                offset += len;
                fieldConfigurations.add(dto);
            }
        }

        return fieldConfigurations;
    }
    // =========================================================================
    // BUILD FIELD CONFIGURATION
    // =========================================================================
    private ReconFieldConfigurationDto buildFieldConfiguration(
            String header,
            Integer sequence,
            List<String> values) {
        return buildFieldConfiguration(header, sequence, values, null);
    }

    private static final Set<String> RESERVED_FIELDS = new HashSet<>(Arrays.asList(
            "REC_FLG", "PREMANRECREL_FLG", "MANRECREL_FLG", "REV_FLAG", "ACTION_ID",
            "REMARKS", "INST_CODE", "INS_USER", "INS_DATE", "LUPD_USER", "LUPD_DATE",
            "PROCESS_DATE", "FILE_NAME", "FILE_SEQ_NUM", "KNOCKOFF_FLG", "USER_PROCESS_ID",
            "USER_PROCESS_DATE", "USER_APPR_ID", "USER_APPR_DATE", "TTUM_ID", "UPD_FLG"
    ));

    private String renameIfReserved(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        String trimmed = fieldName.trim();
        if (RESERVED_FIELDS.contains(trimmed.toUpperCase())) {
            return "txn_" + trimmed;
        }
        return trimmed;
    }

    /**
     * Turns a raw source-file column header into a valid SQL*Loader bind-variable /
     * column identifier.
     *
     * Auto-detect only trimmed the header before this fix, so a header with a space —
     * "Sr No" from NEFT_CBS_25K.csv, e.g. — was carried verbatim into fieldName and then
     * shortName (ReconFieldDetailsMapper.setShortName == fieldName). The generated
     * control file then emitted a bind variable with a literal space, ":SR NO", which
     * SQL*Loader cannot parse ("SQL*Loader-350: Syntax error... Expecting keyword
     * PRESERVE or keyword NULLIF"). That aborted the whole load before a single row was
     * read, unlike a length overflow which just rejects the offending rows.
     *
     * Any character that is not a letter, digit or underscore becomes '_'; repeats
     * collapse to one; a leading digit is prefixed 'F_' (an identifier can't start with
     * a number, and Oracle column names can't start with '_' either).
     */
    private String sanitizeFieldName(String rawHeader) {
        if (null == rawHeader) {
            return null;
        }
        String cleaned = rawHeader.trim()
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (cleaned.isEmpty()) {
            cleaned = "FIELD";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "F_" + cleaned;
        }
        return cleaned;
    }

    private ReconFieldConfigurationDto buildFieldConfiguration(
            String header,
            Integer sequence,
            List<String> values,
            Integer excelColIndex) {

        ReconFieldConfigurationDto dto = new ReconFieldConfigurationDto();

        dto.setFieldName(renameIfReserved(sanitizeFieldName(header)));
        dto.setFieldSequence(sequence);

        dto.setFieldType(detectFieldType(values));
        dto.setFieldFormat(detectFieldFormat(values));
        dto.setFieldLength(detectFieldLength(values));
        dto.setFieldScale(detectFieldScale(values));

        dto.setIsMandatory(CommonConstants.INACTIVE_N);
        dto.setIsPrimaryKey(CommonConstants.INACTIVE_N);
        dto.setIsReconKey(CommonConstants.INACTIVE_N);
        dto.setTrimFlag(CommonConstants.ACTIVE_Y);

        dto.setExcelColIndex(excelColIndex);

        return dto;
    }
    private Integer detectFieldLength(List<String> values) {

        int maxLength = 0;

        for (String value : values) {

            if (value != null && value.length() > maxLength) {
                maxLength = value.length();
            }
        }

        // Guard against zero-length columns (Oracle ORA-01723) when the sampled
        // values are all null/blank — fall back to a safe generic default.
        return maxLength == 0 ? 255 : maxLength;
    }
    private Integer detectFieldScale(List<String> values) {

        int maxScale = 0;

        for (String value : values) {

            if (value != null && value.contains(".")) {

                String[] parts = value.split("\\.");

                if (parts.length == 2) {
                    maxScale = Math.max(maxScale, parts[1].length());
                }
            }
        }

        return maxScale == 0 ? null : maxScale;
    }
    // =========================================================================
    // DETECT FIELD TYPE
    // =========================================================================
    private String detectFieldType(List<String> values) {

        // Filter out null and empty values first
        List<String> nonEmpty = new ArrayList<>();
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                nonEmpty.add(v.trim());
            }
        }

        // If all values are empty — default to String
        if (nonEmpty.isEmpty()) {
            return "String";
        }

        boolean isNumber  = true;
        boolean isDecimal = true;
        boolean isBoolean = true;
        boolean isDate    = true;

        for (String value : nonEmpty) {

            // Number — only digits, no decimal point
            if (!value.matches("\\d+")) {
                isNumber = false;
            }

            // Decimal — digits with optional decimal point
            if (!value.matches("\\d+(\\.\\d+)?")) {
                isDecimal = false;
            }

            // Boolean — single-char flags only (Y/N/T/F/1/0). The stage table stores
            // Boolean as VARCHAR2(1) (see SP_STAGE_TAB_CREATION_V2), so multi-char
            // text like "true"/"false"/"yes"/"no" must fall through to String —
            // detecting it as Boolean here would truncate and reject every row.
            if (!(value.length() == 1 && (value.equalsIgnoreCase(CommonConstants.ACTIVE_Y)
                    || value.equalsIgnoreCase(CommonConstants.INACTIVE_N)
                    || value.equalsIgnoreCase("T")
                    || value.equalsIgnoreCase("F")
                    || value.equals("1")
                    || value.equals("0")))) {
                isBoolean = false;
            }

            // Date — dd-MM-yyyy / dd/MM/yyyy / yyyy-MM-dd
            if (!(value.matches("\\d{2}-\\d{2}-\\d{4}")
                    || value.matches("\\d{2}/\\d{2}/\\d{4}")
                    || value.matches("\\d{4}-\\d{2}-\\d{2}"))) {
                isDate = false;
            }
        }

        // Priority order matters:
        // Boolean before Number (1/0 matches both)
        // Date before String
        // Number before Decimal (123 matches both)
        if (isBoolean) return "Boolean";
        if (isDate)    return "Date";
        if (isNumber)  return "Number";
        if (isDecimal) return "Decimal";

        return "String";
    }
    // =========================================================================
    // DETECT FIELD FORMAT
    // =========================================================================
    private String detectFieldFormat(List<String> values) {

        String fieldType = detectFieldType(values);

        switch (fieldType) {

            case "Date":

                for (String value : values) {

                    if (value == null || value.trim().isEmpty()) {
                        continue;
                    }

                    if (value.matches("\\d{2}-\\d{2}-\\d{4}")) {
                        return "dd-MM-yyyy";
                    }

                    if (value.matches("\\d{2}/\\d{2}/\\d{4}")) {
                        return "dd/MM/yyyy";
                    }

                    if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return "yyyy-MM-dd";
                    }
                }

                return "Date";

            case "Decimal":
                return "##.##";

            case "Number":
                return "INTEGER";

            case "Boolean":
                return "TRUE/FALSE";

            default:
                return "N/A";
        }
    }
    // =========================================================================
    // INNER TRANSACTIONAL HELPERS (called via self-proxy)
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconFileTmpltMast saveTemplateAndFields(ReconTemplateConfigRequest request) {

        // ── deliverMode: SFTP → save/link SFTP server; MANUAL → skip SFTP entirely ──
        String deliverMode = request.getDeliverMode();
        if (!TemplateConstants.DELIVER_MODE_MANUAL.equalsIgnoreCase(deliverMode)
                && null != request.getSftpServerDetails()) {

            Long linkedSftpServerId;
            if (null != request.getSftpServerDetails().getServerId()) {
                // ── Case A: Existing server selected — just link it, no DB change ──
                linkedSftpServerId = request.getSftpServerDetails().getServerId();
                logger.info("Linking existing SFTP server [{}] to new template.", linkedSftpServerId);
            } else {
                // ── Case B: "+Add Custom SFTP" — nullify irrelevant auth fields then create and link ──
                SftpServerRequestDTO sftpReq = request.getSftpServerDetails();
                String sftpAuthType = null != sftpReq.getAuthType()
                        ? sftpReq.getAuthType().toUpperCase() : SftpConstants.AUTH_PASSWORD;
                if (SftpConstants.AUTH_SSH_KEY.equals(sftpAuthType)) {
                    // SSH_KEY: privateKeyPath required; password irrelevant → nullify
                    if (null == sftpReq.getPrivateKeyPath() || sftpReq.getPrivateKeyPath().trim().isEmpty())
                        throw new IllegalArgumentException(
                                "privateKeyPath is required when authType is SSH_KEY.");
                    sftpReq.setPassword(null);
                } else {
                    // PASSWORD: password required; privateKeyPath + passphrase irrelevant → nullify
                    if (null == sftpReq.getPassword() || sftpReq.getPassword().trim().isEmpty())
                        throw new IllegalArgumentException(
                                "password is required when authType is PASSWORD.");
                    sftpReq.setPrivateKeyPath(null);
                    sftpReq.setPassphrase(null);
                }
                ReconSftpServerMast reconSftpServerMast =
                        reconSftpServerMapper.toEntity(sftpReq);
                String actor = null != request.getCreatedBy()
                        ? request.getCreatedBy() : TemplateConstants.DEFAULT_ACTOR;
                reconSftpServerMast.setCreatedBy(actor);
                reconSftpServerMast.setUpdatedBy(actor);
                reconSftpServerMast = sftpServerRepo.save(reconSftpServerMast);
                linkedSftpServerId = reconSftpServerMast.getServerId();
                logger.info("Created new SFTP server [{}] for template.", linkedSftpServerId);
            }

            ReconFileTmpltMast template = ReconFileTemplateMastMapper
                    .mapTemplateDtoToFileTmpltMast(request, new ReconFileTmpltMast());
            template.setTemplateCode(generateTemplateCode());
            template.setStageTabName(generateStageTableName(template));
            // ── action: DRAFT → status=DRAFT; SAVE (or anything else) → ACTIVE ──
            template.setStatus(TemplateConstants.ACTION_DRAFT.equalsIgnoreCase(request.getAction())
                    ? TemplateConstants.STATUS_DRAFT : TemplateConstants.STATUS_ACTIVE);
            template.setSftpServerId(linkedSftpServerId);
            // sourceSystem FK lookup — set entity from sourceCode string
            if (null != request.getSourceSystem()) {
                ReconSourceSystemMast srcSys = sourceSystemRepository
                        .findBySourceCode(request.getSourceSystem().trim().toUpperCase())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Source system not found: " + request.getSourceSystem()
                                + ". Please add it in recon_source_system_mast table first."));
                template.setSourceSystem(srcSys);
            }
            // Null out fields irrelevant to this templateType + reversal condition
            nullifyIrrelevantTemplateFields(template);
            templateRepository.save(template);
            if (null != request.getFieldDetails() && !request.getFieldDetails().isEmpty()) {
                List<ReconTmpltFieldDtls> fields = buildFieldEntities(request.getFieldDetails(), template);
                fieldDetailsRepository.saveAll(fields);
                logger.info("Saved {} fields for Template [{}] (SFTP mode, action={})",
                        fields.size(), template.getTemplateCode(), request.getAction());
            } else {
                logger.info("No fields provided for Template [{}] (SFTP mode, action=DRAFT)",
                        template.getTemplateCode());
            }
            return template;
        }

        // MANUAL delivery mode — no SFTP server saved
        ReconFileTmpltMast template = ReconFileTemplateMastMapper
                .mapTemplateDtoToFileTmpltMast(request, new ReconFileTmpltMast());
        template.setTemplateCode(generateTemplateCode());
        template.setStageTabName(generateStageTableName(template));
        // ── action: DRAFT → status=DRAFT; SAVE (or anything else) → ACTIVE ──
        template.setStatus(TemplateConstants.ACTION_DRAFT.equalsIgnoreCase(request.getAction())
                ? TemplateConstants.STATUS_DRAFT : TemplateConstants.STATUS_ACTIVE);
        // sourceSystem FK lookup — set entity from sourceCode string
        if (null != request.getSourceSystem()) {
            ReconSourceSystemMast srcSys = sourceSystemRepository
                    .findBySourceCode(request.getSourceSystem().trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source system not found: " + request.getSourceSystem()
                            + ". Please add it in recon_source_system_mast table first."));
            template.setSourceSystem(srcSys);
        }
        // Null out fields irrelevant to this templateType + reversal condition
        nullifyIrrelevantTemplateFields(template);
        templateRepository.save(template);
        if (null != request.getFieldDetails() && !request.getFieldDetails().isEmpty()) {
            List<ReconTmpltFieldDtls> fields = buildFieldEntities(request.getFieldDetails(), template);
            fieldDetailsRepository.saveAll(fields);
            logger.info("Saved {} fields for Template [{}] (MANUAL mode, action={})",
                    fields.size(), template.getTemplateCode(), request.getAction());
        } else {
            logger.info("No fields provided for Template [{}] (MANUAL mode, action=DRAFT)",
                    template.getTemplateCode());
        }

        // Scheduler config suppressed — will be handled via separate dedicated API
        // reconExecScheduleConfigService.saveScheduleConfig(...) removed intentionally

        return template;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconFileTmpltMast updateTemplateAndFields(Long templateId, ReconTemplateConfigRequest request) {
        ReconFileTmpltMast template = templateRepository.findById(templateId)
                .filter(t -> !TemplateConstants.STATUS_INACTIVE.equalsIgnoreCase(t.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        // Duplicate name check — only if name is being changed to something different
        if (null != request.getTemplateName()
                && !request.getTemplateName().equalsIgnoreCase(template.getTemplateName())
                && templateRepository.existsLiveByTemplateNameAndTenantId(
                        request.getTemplateName(), template.getTenantId())) {
            throw new IllegalArgumentException(
                    "Template name '" + request.getTemplateName() + "' already exists for this tenant.");
        }

        if (null != request.getTemplateName()) template.setTemplateName(request.getTemplateName());
        if (null != request.getTemplateType()) template.setTemplateType(request.getTemplateType());
        if (null != request.getColumnCount()) template.setColumnCount(Long.valueOf(request.getColumnCount()));
        if (null != request.getReversalIndicator()) template.setReversalIndicator(request.getReversalIndicator());
        if (null != request.getDataReference()) template.setDataReferenceFlag(request.getDataReference());
        if (null != request.getOnlineRefund()) template.setOnlRefundFlag(request.getOnlineRefund());
        if (null != request.getDelimiter()) template.setDelimiter(request.getDelimiter());
        if (null != request.getTextQualifier()) template.setTextQualifier(request.getTextQualifier());
        if (null != request.getFileEncoding()) template.setFileEncoding(request.getFileEncoding());
        if (null != request.getDateFormat()) template.setDateFormat(request.getDateFormat());
        if (null != request.getAmountFormat()) template.setAmountFormat(request.getAmountFormat());
        if (null != request.getDupCheckFlag()) template.setDupCheckFlag(request.getDupCheckFlag());
        if (null != request.getReversalHandling()) template.setReversalHandling(request.getReversalHandling());
        if (null != request.getRecordLength()) template.setRecordLength(request.getRecordLength());
        if (null != request.getPaddingChar()) template.setPaddingChar(request.getPaddingChar());
        if (null != request.getXmlRootTag()) template.setXmlRootTag(request.getXmlRootTag());
        if (null != request.getXmlRowTag()) template.setXmlRowTag(request.getXmlRowTag());
        if (null != request.getXmlNamespace()) template.setXmlNamespace(request.getXmlNamespace());
        if (null != request.getFilePath()) template.setFilePath(request.getFilePath());
        if (null != request.getDescription()) template.setDescription(request.getDescription());
        // sourceSystem FK lookup — update only if new code provided
        if (null != request.getSourceSystem()) {
            ReconSourceSystemMast srcSys = sourceSystemRepository
                    .findBySourceCode(request.getSourceSystem().trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source system not found: " + request.getSourceSystem()
                            + ". Please add it in recon_source_system_mast table first."));
            template.setSourceSystem(srcSys);
        }
        if (null != request.getHasHeader()) template.setHasHeader(request.getHasHeader());
        if (null != request.getHasTrailer()) template.setHasTrailer(request.getHasTrailer());
        if (null != request.getHeaderLineCount()) template.setHeaderLineCount(request.getHeaderLineCount());
        if (null != request.getTrailerLineCount()) template.setTrailerLineCount(request.getTrailerLineCount());
        if (null != request.getFrequencyType()) template.setFrequencyType(request.getFrequencyType());
        if (null != request.getFileNamePattern()) template.setFileNamePattern(request.getFileNamePattern());
        if (null != request.getReversalFieldName()) template.setReversalFieldName(request.getReversalFieldName());
        if (null != request.getReversalValue()) template.setReversalValue(request.getReversalValue());
        if (null != request.getReversalAmtHandling()) template.setReversalAmtHandling(request.getReversalAmtHandling());

        // SAVE promotes DRAFT → ACTIVE; DRAFT keeps existing status (SAVE→DRAFT blocked in configureAsUpdate)
        if (TemplateConstants.ACTION_SAVE.equalsIgnoreCase(request.getAction())) {
            template.setStatus(TemplateConstants.STATUS_ACTIVE);
        }

        template.setUpdatedBy(request.getUpdatedBy());
        template.setUpdatedAt(java.time.LocalDateTime.now());

        // ── SFTP / deliverMode handling on update ────────────────────────────
        // Case 2: MANUAL → SFTP  (add new SFTP server)
        // Case 3: SFTP  → SFTP  (update existing server details)
        // Case 4: SFTP  → MANUAL (remove SFTP association)
        String deliverMode = request.getDeliverMode();
        if (TemplateConstants.DELIVER_MODE_SFTP.equalsIgnoreCase(deliverMode)) {
            if (null == request.getSftpServerDetails()) {
                throw new IllegalArgumentException(
                        "sftpServerDetails is required when deliverMode is SFTP.");
            }
            ReconSftpServerMast sftpServer;
            if (null != request.getSftpServerDetails().getServerId()) {
                // Case 3: existing server — fetch first to preserve createdBy/createdAt, then update
                sftpServer = sftpServerRepo.findById(request.getSftpServerDetails().getServerId())
                        .orElse(reconSftpServerMapper.toEntity(request.getSftpServerDetails()));
                reconSftpServerMapper.updateEntityFromRequest(request.getSftpServerDetails(), sftpServer);
                // Nullify irrelevant auth fields directly on entity AFTER mapper runs
                // (mapper skips null DTO values to preserve existing credentials —
                //  so we must explicitly null the irrelevant field on the entity itself)
                String sftpAuthType3 = null != request.getSftpServerDetails().getAuthType()
                        ? request.getSftpServerDetails().getAuthType().toUpperCase() : SftpConstants.AUTH_PASSWORD;
                if (SftpConstants.AUTH_SSH_KEY.equals(sftpAuthType3)) {
                    sftpServer.setPassword(null);
                } else {
                    sftpServer.setPrivateKeyPath(null);
                    sftpServer.setPassphrase(null);
                }
                sftpServer.setUpdatedBy(null != request.getUpdatedBy()
                        ? request.getUpdatedBy() : TemplateConstants.DEFAULT_ACTOR);
            } else {
                // Case 2: new SFTP server being added during update
                // Nullify irrelevant auth fields before mapping to entity
                SftpServerRequestDTO sftpReq = request.getSftpServerDetails();
                String sftpAuthType = null != sftpReq.getAuthType()
                        ? sftpReq.getAuthType().toUpperCase() : SftpConstants.AUTH_PASSWORD;
                if (SftpConstants.AUTH_SSH_KEY.equals(sftpAuthType)) {
                    sftpReq.setPassword(null);
                } else {
                    sftpReq.setPrivateKeyPath(null);
                    sftpReq.setPassphrase(null);
                }
                sftpServer = reconSftpServerMapper.toEntity(sftpReq);
                String actor = null != request.getUpdatedBy()
                        ? request.getUpdatedBy() : TemplateConstants.DEFAULT_ACTOR;
                sftpServer.setCreatedBy(actor);
                sftpServer.setUpdatedBy(actor);
            }
            // Case 2 & 3: save (insert or update) SFTP server
            sftpServer = sftpServerRepo.save(sftpServer);
            template.setSftpServerId(sftpServer.getServerId());
        } else if (TemplateConstants.DELIVER_MODE_MANUAL.equalsIgnoreCase(deliverMode)) {
            // Case 4: SFTP → MANUAL — unlink only; SFTP server record is preserved
            Long oldSftpServerId = template.getSftpServerId();
            template.setSftpServerId(null);
            if (null != oldSftpServerId) {
                logger.info("Unlinked SFTP server [{}] from template — record preserved.", oldSftpServerId);
            }
        } else if (null != deliverMode) {
            // Unknown deliverMode value — reject
            throw new IllegalArgumentException(
                    "Invalid deliverMode: " + deliverMode + ". Must be SFTP or MANUAL.");
        }
        // deliverMode null = frontend didn't send it = no change (defensive only — HTML marks it required)

        // Null out fields irrelevant to this templateType + reversal condition
        nullifyIrrelevantTemplateFields(template);
        templateRepository.save(template);
        // Hard delete old fields then re-insert updated ones
        fieldDetailsRepository.deleteByTemplateId(template.getTemplateId());
        if (null != request.getFieldDetails() && !request.getFieldDetails().isEmpty()) {
            fieldDetailsRepository.saveAll(buildFieldEntities(request.getFieldDetails(), template));
        }
        return template;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String callStageTableProcedure(ReconFileTmpltMast template) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_STAGE_TAB_CREATION")
                .declareParameters(
                        new SqlParameter("Prm_tmplt_Id", Types.NUMERIC),
                        new SqlOutParameter("Prm_Error", Types.VARCHAR));
        Map<String, Object> params = new HashMap<>();
        params.put("Prm_tmplt_Id", template.getTemplateId());
        Map<String, Object> result = call.execute(params);
        return (String) result.get("Prm_Error");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackTemplateAndFields(Long templateId) {
        try {
            fieldDetailsRepository.deleteByTemplateId(templateId);
            templateRepository.deleteById(templateId);
            logger.info("Rolled back template {}", templateId);
        } catch (Exception e) {
            logger.error("Rollback failed for template {}: {}", templateId, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreTemplateAndFields(ReconFileTmpltMast snapshot,
                                          List<ReconTmpltFieldDtls> prevFields) {
        try {
            templateRepository.save(snapshot);
            fieldDetailsRepository.deleteByTemplateId(snapshot.getTemplateId());
            if (null != prevFields && !prevFields.isEmpty()) {
                prevFields.forEach(f -> f.setFieldId(null));
                fieldDetailsRepository.saveAll(prevFields);
            }
            logger.info("Restored template {}", snapshot.getTemplateId());
        } catch (Exception e) {
            logger.error("Restore failed for template {}: {}", snapshot.getTemplateId(), e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * UPDATE path for both the configure API (POST) and the dedicated update endpoint (PUT).
     * Invoked by configureTemplateAndFieldData() when request.templateId is non-null,
     * and also directly by the PUT /update-template/{id} controller (which sets templateId
     * from the path variable into the request before calling configureTemplateAndFieldData).
     *
     * Rules:
     *   SAVE → DRAFT demotion is rejected — an ACTIVE template cannot go back to DRAFT.
     *   DRAFT → SAVE, DRAFT → DRAFT, and SAVE → SAVE are all permitted.
     */
    private ResponseEntity<RestWithMapStatusList> updateTemplate(ReconTemplateConfigRequest request) {

        Long templateId = request.getTemplateId();

        // ── Load existing template ────────────────────────────────────────────
        ReconFileTmpltMast existing = templateRepository.findById(templateId)
                .filter(t -> !TemplateConstants.STATUS_INACTIVE.equalsIgnoreCase(t.getStatus()))
                .orElse(null);
        if (null == existing) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Template not found: " + templateId),
                    HttpStatus.NOT_FOUND);
        }

        // ── SAVE → DRAFT restriction ──────────────────────────────────────────
        // An ACTIVE (saved) template cannot be demoted back to DRAFT.
        // Only DRAFT → SAVE promotion is permitted, not the reverse.
        if (TemplateConstants.STATUS_ACTIVE.equalsIgnoreCase(existing.getStatus())
                && TemplateConstants.ACTION_DRAFT.equalsIgnoreCase(request.getAction())) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure(
                            "Cannot switch to DRAFT — template is already SAVED (ACTIVE). "
                            + "DRAFT → SAVE is allowed but SAVE → DRAFT is not."),
                    HttpStatus.BAD_REQUEST);
        }

        boolean isDraft = TemplateConstants.ACTION_DRAFT.equalsIgnoreCase(request.getAction());

        // ── fieldDetails guard ────────────────────────────────────────────────
        if (!isDraft && (null == request.getFieldDetails() || request.getFieldDetails().isEmpty())) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("fieldDetails must not be null or empty"),
                    HttpStatus.BAD_REQUEST);
        }

        // ── Partial-update fill — merge missing request fields from DB ────────
        // Ensures validation runs against the full merged state, not just
        // what the caller happened to send.
        if (!isDraft) {
            fillMissingFromExisting(request, existing);
        }

        // ── Validation ────────────────────────────────────────────────────────
        String valErr = isDraft ? validateDraftRequest(request) : validateRequest(request);
        if (null != valErr) {
            return new ResponseEntity<>(ResponseBuilder.failure(valErr), HttpStatus.BAD_REQUEST);
        }

        // ── Snapshot — needed to restore previous state if SP fails ──────────
        List<ReconTmpltFieldDtls> prevFields =
                fieldDetailsRepository.findActiveFieldsByTemplateId(existing.getTemplateId());
        ReconFileTmpltMast prevSnapshot = copyTemplateSnapshot(existing);
        // Audit "before" is taken off the full loaded entity, not off prevSnapshot —
        // that copy only carries the columns the SP-failure rollback needs, so using it
        // logged source/frequency/filePath as null. Rendered to a String right here
        // because `existing` is managed and will hold the new values after the update.
        final String beforeSnapshot = templateSnapshot(existing, prevFields.size());

        // ── Persist update ────────────────────────────────────────────────────
        ReconFileTmpltMast updatedTemplate;
        try {
            updatedTemplate = self.updateTemplateAndFields(templateId, request);
            logger.info("Template updated. ID: {}", updatedTemplate.getTemplateId());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error — updateTemplate templateId={}: {}",
                    templateId, e.getMessage(), e);
            return new ResponseEntity<>(ResponseBuilder.failure(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity error — updateTemplate templateId={}: {}",
                    templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error(
                            "Data conflict during template update. Check for duplicate template name."),
                    HttpStatus.CONFLICT);
        } catch (Exception e) {
            logger.error("Unexpected error — updateTemplate templateId={}: {}",
                    templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to update template: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // ── Stage table — DRAFT templates skip SP ─────────────────────────────
        if (!isDraft) {
            String spResult;
            try {
                spResult = self.callStageTableProcedure(updatedTemplate);
            } catch (Exception e) {
                self.restoreTemplateAndFields(prevSnapshot, prevFields);
                return new ResponseEntity<>(
                        ResponseBuilder.error(
                                "Stage table update failed. Previous state restored. " + e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (null == spResult || !spResult.equalsIgnoreCase("OK")) {
                self.restoreTemplateAndFields(prevSnapshot, prevFields);
                return new ResponseEntity<>(
                        ResponseBuilder.error(
                                "Stage table update failed. Previous state restored. Reason: " + spResult),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            logger.info("DRAFT template [{}] — skipping SP_STAGE_TAB_CREATION on update.",
                    updatedTemplate.getTemplateCode());
        }

        // ── Build and return full response ────────────────────────────────────
        entityManager.clear();
        List<ReconFileTmpltMast> foundUpd =
                templateRepository.findByIdWithDetails(updatedTemplate.getTemplateId());
        ReconFileTmpltMast fullTemplate = foundUpd.isEmpty() ? updatedTemplate : foundUpd.get(0);
        ReconFileTemplateMastDto updatedDto =
                enrichWithSftp(ReconFileTemplateMastMapper.toDTO(fullTemplate), fullTemplate);
        Map<String, Object> updatedRow = ResponseBuilder.toMap(updatedDto, objectMapper);

        long newFieldCount = fieldDetailsRepository.findActiveFieldsByTemplateId(
                fullTemplate.getTemplateId()).size();
        auditTemplate(fullTemplate, "UPDATE", request.getUpdatedBy(),
                beforeSnapshot,
                templateSnapshot(fullTemplate, newFieldCount),
                "Template updated",
                "Updated via template-configure"
                        + (isDraft ? " (DRAFT — stage table untouched)" : ""));

        return ResponseEntity.ok(
                ResponseBuilder.ok("Template updated successfully.", "template",
                        Collections.singletonList(updatedRow)));
    }

    private List<ReconTmpltFieldDtls> buildFieldEntities(List<ReconFieldConfigurationDto> dtos,
                                                           ReconFileTmpltMast template) {
        List<ReconTmpltFieldDtls> fields = new ArrayList<>();
        for (ReconFieldConfigurationDto dto : dtos) {
            // fieldType  → always uppercase (DB stores STRING, DATE, NUMBER, DECIMAL, BOOLEAN)
            // fieldFormat → keep as-is  (DB stores mixed-case: "dd-MM-yyyy", "yyyy-MM-dd" etc.)
            String fieldTypeInput   = null != dto.getFieldType() ? dto.getFieldType().trim().toUpperCase() : null;
            String fieldFormatInput = null != dto.getFieldFormat() ? dto.getFieldFormat().trim()             : null;

            ReconFieldTypeMast type = fieldTypeRepository
                    .findByFieldTypeDes(fieldTypeInput)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Type: " + dto.getFieldType()
                            + ". Valid values: STRING, NUMBER, DATE, DECIMAL, BOOLEAN"));
            ReconFieldFormatMast format = fieldFormatRepository
                    .findByReconFieldFormatDesc(fieldFormatInput)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Format: " + dto.getFieldFormat()
                            + ". Use GET /api/v2/field/field-formats to see valid values."));
            ReconTmpltFieldDtls field =
                    ReconFieldDetailsMapper.mapFieldDtoToEntity(dto, template, type, format);
            // Null out field columns irrelevant to this templateType
            String templateType = null != template.getTemplateType()
                    ? template.getTemplateType().trim().toUpperCase() : "";
            nullifyIrrelevantFieldColumns(field, templateType);
            fields.add(field);
        }
        return fields;
    }

    /**
     * Nullifies template-level fields that do not belong to the given templateType.
     * Called after building / updating a ReconFileTmpltMast entity so that DB always
     * stores NULL for fields irrelevant to the current type — regardless of what
     * the frontend sent in the payload.
     *
     * Also nullifies reversal fields when reversalIndicator is not CommonConstants.ACTIVE_Y.
     */
    private void nullifyIrrelevantTemplateFields(ReconFileTmpltMast template) {
        String type = null != template.getTemplateType()
                ? template.getTemplateType().trim().toUpperCase() : "";

        switch (type) {
            case TemplateConstants.TYPE_CSV:
                // CSV uses: delimiter, textQualifier
                // Null out Fixed Width, XML specific fields
                template.setRecordLength(null);
                template.setPaddingChar(null);
                template.setXmlRootTag(null);
                template.setXmlRowTag(null);
                template.setXmlNamespace(null);
                break;

            case TemplateConstants.TYPE_FIXED_WIDTH:
            case TemplateConstants.TYPE_FIXED_WIDTH2:
                // Fixed Width uses: recordLength, paddingChar
                // Null out CSV, XML specific fields
                template.setDelimiter(null);
                template.setTextQualifier(null);
                template.setXmlRootTag(null);
                template.setXmlRowTag(null);
                template.setXmlNamespace(null);
                break;

            case TemplateConstants.TYPE_XML:
                // XML uses: xmlRootTag, xmlRowTag, xmlNamespace
                // Null out CSV, Fixed Width specific fields
                template.setDelimiter(null);
                template.setTextQualifier(null);
                template.setRecordLength(null);
                template.setPaddingChar(null);
                break;

            case TemplateConstants.TYPE_EXCEL:
                // Excel has no template-level type-specific fields
                // Null out all type-specific fields
                template.setDelimiter(null);
                template.setTextQualifier(null);
                template.setRecordLength(null);
                template.setPaddingChar(null);
                template.setXmlRootTag(null);
                template.setXmlRowTag(null);
                template.setXmlNamespace(null);
                break;

            default:
                // Unknown type — leave as-is (will be caught by validation)
                break;
        }

        // Reversal conditional: if reversalIndicator is not CommonConstants.ACTIVE_Y, null out reversal fields
        if (!TemplateConstants.FLAG_YES.equalsIgnoreCase(template.getReversalIndicator())) {
            template.setReversalFieldName(null);
            template.setReversalValue(null);
            template.setReversalAmtHandling(null);
        }
    }

    /**
     * Nullifies field-level columns that do not belong to the given templateType.
     * Called after building each ReconTmpltFieldDtls entity so that DB always
     * stores NULL for columns irrelevant to the current template type.
     *
     * Fixed Width: positionFrom / positionTo kept; xmlXpath + excelColIndex → NULL
     * XML:         xmlXpath kept; positionFrom / positionTo + excelColIndex → NULL
     * Excel:       excelColIndex kept; positionFrom / positionTo + xmlXpath → NULL
     * CSV:         no type-specific field columns; all set to NULL
     */
    private void nullifyIrrelevantFieldColumns(ReconTmpltFieldDtls field, String type) {
        switch (type) {
            case TemplateConstants.TYPE_CSV:
                field.setPositionFrom(null);
                field.setPositionTo(null);
                field.setFromPosition(null);   // legacy VARCHAR column
                field.setToPosition(null);     // legacy VARCHAR column
                field.setXmlXpath(null);
                field.setExcelColIndex(null);
                break;

            case TemplateConstants.TYPE_FIXED_WIDTH:
            case TemplateConstants.TYPE_FIXED_WIDTH2:
                // positionFrom / positionTo kept
                field.setXmlXpath(null);
                field.setExcelColIndex(null);
                break;

            case TemplateConstants.TYPE_XML:
                // xmlXpath kept
                field.setPositionFrom(null);
                field.setPositionTo(null);
                field.setFromPosition(null);
                field.setToPosition(null);
                field.setExcelColIndex(null);
                break;

            case TemplateConstants.TYPE_EXCEL:
                // excelColIndex kept
                field.setPositionFrom(null);
                field.setPositionTo(null);
                field.setFromPosition(null);
                field.setToPosition(null);
                field.setXmlXpath(null);
                break;

            default:
                break;
        }
    }

    private ReconFileTmpltMast copyTemplateSnapshot(ReconFileTmpltMast src) {
        ReconFileTmpltMast snap = new ReconFileTmpltMast();
        snap.setTemplateId(src.getTemplateId());
        snap.setTemplateName(src.getTemplateName());
        snap.setTemplateType(src.getTemplateType());
        snap.setTemplateCode(src.getTemplateCode());
        snap.setColumnCount(src.getColumnCount());
        snap.setReversalIndicator(src.getReversalIndicator());
        snap.setDataReferenceFlag(src.getDataReferenceFlag());
        snap.setOnlRefundFlag(src.getOnlRefundFlag());
        snap.setStageTabName(src.getStageTabName());
        snap.setSubTemplateId(src.getSubTemplateId());
        snap.setStatus(src.getStatus());
        snap.setTenantId(src.getTenantId());
        return snap;
    }

    /**
     * DRAFT validation — only bare minimum checked.
     * Bypasses all logical/conditional validations (sourceSystem, filePath,
     * frequencyType, dateFormat, amountFormat, deliverMode, fieldDetails etc.)
     * Only templateName + templateType + tenantId are required for a draft save.
     */
    private String validateDraftRequest(ReconTemplateConfigRequest request) {
        if (isEmpty(request.getTemplateName())) return "templateName is required even for Draft.";
        if (isEmpty(request.getTemplateType())) return "templateType is required even for Draft.";
        if (null == request.getTenantId())      return "tenantId is required even for Draft.";
        // templateType value must still be a known type
        String type = request.getTemplateType().trim().toUpperCase();
        if (!type.equals(TemplateConstants.TYPE_CSV)
                && !type.equals(TemplateConstants.TYPE_FIXED_WIDTH)
                && !type.equals(TemplateConstants.TYPE_FIXED_WIDTH2)
                && !type.equals(TemplateConstants.TYPE_XML)
                && !type.equals(TemplateConstants.TYPE_EXCEL)) {
            return "Invalid templateType: " + request.getTemplateType()
                    + ". Allowed: CSV, Fixed Width, XML, Excel.";
        }
        return null;
    }

    /**
     * Enriches a ReconFileTemplateMastDto with SFTP server details.
     * Called after every mapper.toDTO() so all responses include sftpServerDetails.
     */
    private ReconFileTemplateMastDto enrichWithSftp(ReconFileTemplateMastDto dto,
                                                     ReconFileTmpltMast entity) {
        if (null != entity.getSftpServerId()) {
            sftpServerRepo.findById(entity.getSftpServerId()).ifPresent(sftp ->
                    dto.setSftpServerDetails(reconSftpServerMapper.toResponseDTO(sftp)));
        }
        return dto;
    }

    /**
     * Enriches a list of DTOs with SFTP server details.
     * Uses a templateId→entity map so order-independence is guaranteed
     * (fetchTemplateDetails uses DISTINCT which may reorder results).
     */
    private List<ReconFileTemplateMastDto> enrichListWithSftp(List<ReconFileTemplateMastDto> dtos,
                                                               List<ReconFileTmpltMast> entities) {
        Map<Long, ReconFileTmpltMast> entityMap = new HashMap<>();
        for (ReconFileTmpltMast e : entities) {
            entityMap.put(e.getTemplateId(), e);
        }
        for (ReconFileTemplateMastDto dto : dtos) {
            ReconFileTmpltMast entity = entityMap.get(dto.getTemplateId());
            if (null != entity) enrichWithSftp(dto, entity);
        }
        return dtos;
    }

    /**
     * PUT partial-update helper — fills null request fields from the existing
     * template so validation can run against the full merged state.
     * Only fills if the request field is null/blank (user didn't send it).
     * Fields the user DID send are left untouched — those are their intended changes.
     */
    private void fillMissingFromExisting(ReconTemplateConfigRequest request,
                                          ReconFileTmpltMast existing) {
        if (isEmpty(request.getTemplateName()))    request.setTemplateName(existing.getTemplateName());
        if (isEmpty(request.getTemplateType()))    request.setTemplateType(existing.getTemplateType());
        if (null == request.getTenantId())         request.setTenantId(existing.getTenantId());
        // sourceSystem — extract code string from FK entity (null-safe)
        if (isEmpty(request.getSourceSystem()) && null != existing.getSourceSystem())
            request.setSourceSystem(existing.getSourceSystem().getSourceCode());
        if (isEmpty(request.getFilePath()))        request.setFilePath(existing.getFilePath());
        if (isEmpty(request.getFrequencyType()))   request.setFrequencyType(existing.getFrequencyType());
        if (isEmpty(request.getDateFormat()))      request.setDateFormat(existing.getDateFormat());
        if (isEmpty(request.getAmountFormat()))    request.setAmountFormat(existing.getAmountFormat());
        if (isEmpty(request.getHasHeader()))       request.setHasHeader(existing.getHasHeader());
        // Type-specific fields — only fill the relevant ones
        if (isEmpty(request.getDelimiter()))       request.setDelimiter(existing.getDelimiter());
        if (null == request.getRecordLength())     request.setRecordLength(existing.getRecordLength());
        if (isEmpty(request.getPaddingChar()))      request.setPaddingChar(existing.getPaddingChar());
        if (isEmpty(request.getXmlRootTag()))      request.setXmlRootTag(existing.getXmlRootTag());
        if (isEmpty(request.getXmlRowTag()))       request.setXmlRowTag(existing.getXmlRowTag());
        if (isEmpty(request.getXmlNamespace()))    request.setXmlNamespace(existing.getXmlNamespace());
        // deliverMode — infer from existing sftpServerId if not provided
        if (isEmpty(request.getDeliverMode()))
            request.setDeliverMode(null != existing.getSftpServerId()
                    ? TemplateConstants.DELIVER_MODE_SFTP : TemplateConstants.DELIVER_MODE_MANUAL);
        // reversalFieldName — fill only if reversalIndicator is Y and fieldName missing
        if (isEmpty(request.getReversalIndicator()))
            request.setReversalIndicator(existing.getReversalIndicator());
        if (TemplateConstants.FLAG_YES.equalsIgnoreCase(request.getReversalIndicator())
                && isEmpty(request.getReversalFieldName()))
            request.setReversalFieldName(existing.getReversalFieldName());
    }

    /**
     * Full HTML-aligned validation — covers ALL sections.
     * Returns first error message found, or null if valid.
     *
     * Section 1 Base     → templateType, templateName, sourceSystem, filePath,
     *                       frequencyType, dateFormat, amountFormat, hasHeader required
     * Section 1 Reversal → reversalFieldName required when reversalIndicator = Y
     * Section 1 Type     → CSV/Fixed Width/XML/Excel conditional rules
     * Section 2 Fields   → fieldName required per row; type-specific field rules
     * Section 4 Delivery → deliverMode required; SFTP rules when deliverMode=SFTP
     */
    private String validateRequest(ReconTemplateConfigRequest request) {

        // ── Section 1: Base required fields ──────────────────────────────────
        if (isEmpty(request.getTemplateName()))  return "templateName is required.";
        if (isEmpty(request.getTemplateType()))  return "templateType is required.";
        if (null == request.getTenantId())       return "tenantId is required.";
        if (isEmpty(request.getSourceSystem()))  return "sourceSystem is required.";
        if (isEmpty(request.getFilePath()))      return "filePath is required.";
        if (isEmpty(request.getFrequencyType())) return "frequencyType is required.";
        if (isEmpty(request.getDateFormat()))    return "dateFormat is required.";
        if (isEmpty(request.getAmountFormat()))  return "amountFormat is required.";
        if (isEmpty(request.getHasHeader()))     return "hasHeader is required (Y/N).";

        // ── Section 1: Reversal conditional ──────────────────────────────────
        if (TemplateConstants.FLAG_YES.equalsIgnoreCase(request.getReversalIndicator())) {
            if (isEmpty(request.getReversalFieldName())) {
                return "reversalFieldName is required when reversalIndicator is Y.";
            }
        }

        // ── Section 1: Template-type conditional ─────────────────────────────
        String type = request.getTemplateType().trim().toUpperCase();
        switch (type) {

            case TemplateConstants.TYPE_CSV:
                // Only required field check — irrelevant fields nullified on save
                if (isEmpty(request.getDelimiter()))
                    return "delimiter is required for CSV template.";
                break;

            case TemplateConstants.TYPE_FIXED_WIDTH:
            case TemplateConstants.TYPE_FIXED_WIDTH2:
                // Only required field checks — irrelevant fields nullified on save
                if (null == request.getRecordLength())
                    return "recordLength is required for Fixed Width template.";
                // paddingChar: intentionally NOT using isEmpty() — a single space " " is a valid padding char
                if (null == request.getPaddingChar() || request.getPaddingChar().isEmpty())
                    return "paddingChar is required for Fixed Width template.";
                break;

            case TemplateConstants.TYPE_XML:
                // Only required field checks — irrelevant fields nullified on save.
                // xmlNamespace is NOT required: XmlToCsvConvertorService never reads it
                // (most source XML has no namespace at all), so mandating it here only
                // blocked ordinary namespace-less files with no actual downstream use.
                if (isEmpty(request.getXmlRootTag()))
                    return "xmlRootTag is required for XML template.";
                if (isEmpty(request.getXmlRowTag()))
                    return "xmlRowTag is required for XML template.";
                break;

            case TemplateConstants.TYPE_EXCEL:
                // No template-level required fields — irrelevant fields nullified on save
                break;

            default:
                return "Invalid templateType: " + request.getTemplateType()
                        + ". Allowed values: CSV, Fixed Width, XML, Excel.";
        }

        // ── Section 2: Field-level validation ────────────────────────────────
        if (null != request.getFieldDetails()) {
            for (ReconFieldConfigurationDto f : request.getFieldDetails()) {

                // fieldName required for every row
                if (isEmpty(f.getFieldName()))
                    return "fieldName is required for every field row.";

                // Fixed Width → positionFrom + positionTo required per field
                if (TemplateConstants.TYPE_FIXED_WIDTH.equals(type)
                        || TemplateConstants.TYPE_FIXED_WIDTH2.equals(type)) {
                    if (null == f.getPositionFrom() || null == f.getPositionTo())
                        return "positionFrom and positionTo are required for every field in Fixed Width template."
                                + " Missing in field: " + f.getFieldName();
                }

                // Excel → excelColIndex required per field
                if (TemplateConstants.TYPE_EXCEL.equals(type)) {
                    if (null == f.getExcelColIndex())
                        return "excelColIndex is required for every field in Excel template."
                                + " Missing in field: " + f.getFieldName();
                }

                // XML → xmlXpath required per field
                if (TemplateConstants.TYPE_XML.equals(type)) {
                    if (isEmpty(f.getXmlXpath()))
                        return "xmlXpath is required for every field in XML template."
                                + " Missing in field: " + f.getFieldName();
                }
            }
        }

        // ── Section 4: Delivery mode required ────────────────────────────────
        if (isEmpty(request.getDeliverMode()))
            return "deliverMode is required (SFTP or MANUAL).";

        if (TemplateConstants.DELIVER_MODE_SFTP.equalsIgnoreCase(request.getDeliverMode())) {
            // sftpServerDetails object required
            if (null == request.getSftpServerDetails())
                return "sftpServerDetails is required when deliverMode is SFTP.";

            // remotePath required
            if (isEmpty(request.getSftpServerDetails().getRemotePath()))
                return "sftpServerDetails.remotePath is required when deliverMode is SFTP.";

            // New custom SFTP (no serverId) → host, username, password required
            if (null == request.getSftpServerDetails().getServerId()) {
                if (isEmpty(request.getSftpServerDetails().getHost()))
                    return "sftpServerDetails.host is required for a new SFTP server.";
                if (isEmpty(request.getSftpServerDetails().getDefaultUsername()))
                    return "sftpServerDetails.defaultUsername is required for a new SFTP server.";
                // password required only when authType = PASSWORD (default)
                String authType = request.getSftpServerDetails().getAuthType();
                if (null == authType || SftpConstants.AUTH_PASSWORD.equalsIgnoreCase(authType)) {
                    if (isEmpty(request.getSftpServerDetails().getPassword()))
                        return "sftpServerDetails.password is required when authType is PASSWORD.";
                }
            }
        } else if (!TemplateConstants.DELIVER_MODE_MANUAL.equalsIgnoreCase(request.getDeliverMode())) {
            return "Invalid deliverMode: " + request.getDeliverMode() + ". Allowed: SFTP, MANUAL.";
        }

        return null; // all valid
    }

    /** Java 8 compatible isEmpty — null or blank string check */
    private boolean isEmpty(String value) {
        return null == value || value.trim().isEmpty();
    }

    /**
     * Race-condition-safe: the numeric suffix comes from SEQ_FILE_TMPLT_MAST.NEXTVAL —
     * the same Oracle sequence that generates templateId — so two concurrent saves
     * can never be handed the same number. (Reusing the templateId sequence for this
     * is fine: nothing requires the code's number to equal the row's actual templateId,
     * only that each NEXTVAL call is atomic and unique, which Oracle guarantees.)
     * Previously this queried MAX(existing codes)+1, which raced under concurrent
     * saves and could also reissue a deleted template's number.
     */
    private String generateTemplateCode() {
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        Long nextVal = jdbcTemplate.queryForObject(
                "SELECT seq_file_tmplt_mast.NEXTVAL FROM DUAL", Long.class);
        // template_code is VARCHAR2(20) — "TMPLT-" (6) + date (8) + "-" (1) leaves
        // exactly 5 digits of headroom; %06d (tried first) overflowed it (ORA-12899).
        return String.format("TMPLT-%s-%05d", date, nextVal);
    }

    // =========================================================================
    // AUDIT
    // =========================================================================

    /**
     * Writes one AUDIT_LOG row per template lifecycle event (CREATE / UPDATE / DELETE).
     * Deliberately fail-safe: AuditLogService.log already swallows its own errors and
     * runs REQUIRES_NEW, so an audit problem can never roll back or fail the operation
     * it is recording. Actor identity is resolved from the username the controller took
     * off the JWT; if that lookup misses, the username is still recorded.
     */
    private void auditTemplate(ReconFileTmpltMast template, String operation, String actor,
                               String oldValue, String newValue, String label, String remarks) {
        Long   actorUserId = null;
        String actorType   = null;
        Long   bankId      = null;
        try {
            if (null != actor) {
                Optional<ReconUser> u = reconUserRepository.findByUsername(actor);
                if (u.isPresent()) {
                    actorUserId = u.get().getUserId();
                    actorType   = u.get().getUserType();
                    bankId      = u.get().getBankId();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not resolve audit actor '{}': {}", actor, e.getMessage());
        }

        auditLogService.log(
                "RECON_FILE_TMPLT_MAST",
                template.getTemplateId(),
                operation,
                actorUserId,
                actor,
                actorType,
                "TEMPLATE",
                bankId,
                oldValue,
                newValue,
                label,
                null,
                remarks);
    }

    /** Compact one-line snapshot of a template, used as the audit old/new value. */
    private String templateSnapshot(ReconFileTmpltMast t, long fieldCount) {
        return "code=" + t.getTemplateCode()
                + ", name=" + t.getTemplateName()
                + ", type=" + t.getTemplateType()
                // sourceSystem is an entity — take its code, not its toString()
                + ", source=" + (null != t.getSourceSystem() ? t.getSourceSystem().getSourceCode() : null)
                + ", frequency=" + t.getFrequencyType()
                + ", filePath=" + t.getFilePath()
                + ", status=" + t.getStatus()
                + ", stageTable=" + t.getStageTabName()
                + ", fields=" + fieldCount;
    }

    private String generateStageTableName(ReconFileTmpltMast t) {
        return "RECON_" + CommonUtil.removeAllWhitespace(
                t.getTemplateCode()).toUpperCase().replace("-", "_") + "_STAGE_T";
    }
}