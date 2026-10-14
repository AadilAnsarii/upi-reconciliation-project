package com.jpb.reconciliation.reconciliation.service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

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

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.MenuConstants;
import com.jpb.reconciliation.reconciliation.dto.FieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.dto.PageMetadata;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplatesDetailsDTO;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusListPagination;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.mapper.ReconFieldDetailsMapper;
import com.jpb.reconciliation.reconciliation.mapper.ReconTemplateDetailsMapper;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldFormatMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldTypeMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;
import com.jpb.reconciliation.reconciliation.util.CommonUtil;

@Service
@Transactional(readOnly = true)
public class ReconTemplateDetailsServiceImpl implements ReconTemplateDetailsService {

    Logger logger = LoggerFactory.getLogger(ReconTemplateDetailsServiceImpl.class);

    @Autowired
    ReconTemplateDetailsRepository reconTemplateDetailsRepository;

    @Autowired
    ReconFieldFormatMasterRepository reconFieldFormatRepository;

    @Autowired
    ReconFieldTypeMasterRepository reconFieldTypeRepository;

    @Autowired
    ReconFieldDetailsMasterRepository reconFieldDetailsRepository;

    @Autowired
    private ReconFieldDtlMastService reconFieldDtlMastService;

    // Self-injection via @Lazy — required so Spring proxy intercepts
    // @Transactional(REQUIRES_NEW) on inner methods called from within this bean
    @Autowired
    @Lazy
    private ReconTemplateDetailsServiceImpl self;

    private final JdbcTemplate jdbcTemplate;

    public ReconTemplateDetailsServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // =========================================================================
    // ADD TEMPLATE
    // =========================================================================

    @Override
    public ResponseEntity<?> addTemplate(ReconTemplateDetailsDto reconTemplateDetailsDto) {
        ReconTemplateDetails templateDetails = ReconTemplateDetailsMapper
                .mapToReconTemplateDetails(reconTemplateDetailsDto, new ReconTemplateDetails());

        if (templateDetails != null) {
            reconTemplateDetailsRepository.save(templateDetails);
            return new ResponseEntity<>(
                    new ResponseDto(MenuConstants.STATUS_201, "Template Successfully Configured."),
                    HttpStatus.CREATED);
        }

        return new ResponseEntity<>(
                new ResponseDto(MenuConstants.STATUS_417, "Template not Configured."),
                HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // CONFIGURE TEMPLATE + FIELDS (CREATE)
    //
    // FLOW:
    //   Step 1 [REQUIRES_NEW] -> saveTemplateAndFields()   -> commits immediately
    //   Step 2 [REQUIRES_NEW] -> callStageTableProcedure() -> SP sees committed data
    //   Step 3 (if SP fails)  -> rollbackTemplateAndFields() -> compensating delete
    // =========================================================================

    @Override
    // NOT @Transactional — orchestrates two separate committed inner transactions
    public ResponseEntity<RestWithStatusList> configureTemplateAndFieldData(TemplateFieldDto request) {

        // Duplicate check
        ReconTemplateDetails templateExists = reconTemplateDetailsRepository
                .findByTemplateName(request.getTemplateName());
        if (templateExists != null) {
            return new ResponseEntity<>(
                    new RestWithStatusList(CommonConstants.FAILURE, "Template already configured.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // Step 1 — save and commit
        ReconTemplateDetails savedTemplate;
        try {
            savedTemplate = self.saveTemplateAndFields(request);
            logger.info("Template and fields saved. Template ID: {}", savedTemplate.getReconTemplateId());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error while saving template/fields: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList(CommonConstants.FAILURE, e.getMessage(), null),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error while saving template/fields: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR", "Failed to save template data: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Step 2 — call SP (data is already committed)
        String procedureResult;
        try {
            procedureResult = self.callStageTableProcedure(savedTemplate);
            logger.info("SP_STAGE_TAB_CREATION result for Template ID {}: {}",
                    savedTemplate.getReconTemplateId(), procedureResult);
        } catch (Exception e) {
            logger.error("Exception during SP_STAGE_TAB_CREATION for Template ID {}. Rolling back. Error: {}",
                    savedTemplate.getReconTemplateId(), e.getMessage(), e);
            self.rollbackTemplateAndFields(savedTemplate.getReconTemplateId());
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR",
                            "Stage table creation failed (exception). Template data rolled back. Error: " + e.getMessage(),
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Step 3 — SP returned non-OK -> compensating rollback
        if (procedureResult == null || !procedureResult.equalsIgnoreCase("OK")) {
            logger.warn("SP_STAGE_TAB_CREATION returned failure for Template ID {}. Reason: {}. Rolling back.",
                    savedTemplate.getReconTemplateId(), procedureResult);
            self.rollbackTemplateAndFields(savedTemplate.getReconTemplateId());
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR",
                            "Stage table creation failed. Template data rolled back. Reason: " + procedureResult,
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.info("Template configured successfully. Template ID: {}", savedTemplate.getReconTemplateId());
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Template configured successfully", null));
    }

    // =========================================================================
    // UPDATE TEMPLATE + FIELDS
    //
    // ROOT CAUSE: The old @Transactional on this method held delete + saveAll
    // uncommitted when callStageTableProcedure() ran in REQUIRES_NEW, so the
    // SP queried the DB and found stale/missing data — same bug as CREATE.
    //
    // FIX: Remove @Transactional from orchestrator. Split into:
    //   Step 1 [REQUIRES_NEW] -> updateTemplateAndFields()  -> commits immediately
    //   Step 2 [REQUIRES_NEW] -> callStageTableProcedure()  -> SP sees committed data
    //   Step 3 (if SP fails)  -> restoreTemplateAndFields() -> restore previous state
    // =========================================================================

    @Override
    // NOT @Transactional — orchestrates two separate committed inner transactions
    public ResponseEntity<RestWithStatusList> updateTemplate(Long templateId, TemplateFieldDto templateFieldRequest) {

        // Load existing template BEFORE any write transaction
        ReconTemplateDetails existingTemplate = reconTemplateDetailsRepository.findById(templateId).orElse(null);
        if (existingTemplate == null) {
            return new ResponseEntity<>(
                    new RestWithStatusList(CommonConstants.FAILURE, "Template not found with ID: " + templateId, null),
                    HttpStatus.NOT_FOUND);
        }

        // Take snapshot of current state BEFORE making changes,
        // so we can restore if the SP fails after we've already committed updates
        List<ReconFieldDetailsMaster> previousFields =
                reconFieldDetailsRepository.findFullFieldDetailsByTemplateId(existingTemplate.getReconTemplateId());
        ReconTemplateDetails previousTemplateSnapshot = copyTemplateSnapshot(existingTemplate);

        // Step 1 — apply changes and commit
        ReconTemplateDetails updatedTemplate;
        try {
            updatedTemplate = self.updateTemplateAndFields(templateId, templateFieldRequest);
            logger.info("Template and fields updated. Template ID: {}", updatedTemplate.getReconTemplateId());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error while updating template/fields: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList(CommonConstants.FAILURE, e.getMessage(), null),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error while updating template/fields for ID {}: {}", templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR",
                            "Failed to update template data: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Step 2 — call SP (updated data is now committed, SP will find it)
        String procedureResult;
        try {
            procedureResult = self.callStageTableProcedure(updatedTemplate);
            logger.info("SP_STAGE_TAB_CREATION result for Template ID {}: {}",
                    updatedTemplate.getReconTemplateId(), procedureResult);
        } catch (Exception e) {
            logger.error("Exception during SP_STAGE_TAB_CREATION for Template ID {}. Restoring previous state. Error: {}",
                    updatedTemplate.getReconTemplateId(), e.getMessage(), e);
            self.restoreTemplateAndFields(previousTemplateSnapshot, previousFields);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR",
                            "Stage table update failed (exception). Previous data restored. Error: " + e.getMessage(),
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Step 3 — SP returned non-OK -> restore previous committed state
        if (procedureResult == null || !procedureResult.equalsIgnoreCase("OK")) {
            logger.warn("SP_STAGE_TAB_CREATION returned failure for Template ID {}. Reason: {}. Restoring previous state.",
                    updatedTemplate.getReconTemplateId(), procedureResult);
            self.restoreTemplateAndFields(previousTemplateSnapshot, previousFields);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR",
                            "Stage table update failed. Previous data restored. Reason: " + procedureResult,
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.info("Template updated successfully. Template ID: {}", updatedTemplate.getReconTemplateId());
        return new ResponseEntity<>(
                new RestWithStatusList(CommonConstants.SUCCESS, "Template fields updated successfully", null),
                HttpStatus.OK);
    }

    // =========================================================================
    // HELPER — Step 1 for CREATE
    //          Saves template + fields and commits immediately via REQUIRES_NEW
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconTemplateDetails saveTemplateAndFields(TemplateFieldDto request) {

        ReconTemplateDetails template = ReconTemplateDetailsMapper
                .mapTemplateDtoToTemplate(request, new ReconTemplateDetails());
        template.setStageTabName(generateStagetableName(template));
        reconTemplateDetailsRepository.save(template);
        logger.info("Template saved with ID: {}", template.getReconTemplateId());

        List<ReconFieldDetailsMaster> fieldEntities = buildFieldEntities(request.getFieldDetails(), template);
        reconFieldDetailsRepository.saveAll(fieldEntities);
        logger.info("Saved {} field(s) for Template ID: {}", fieldEntities.size(), template.getReconTemplateId());

        // REQUIRES_NEW commits here — data is visible to the DB before method returns
        return template;
    }

    // =========================================================================
    // HELPER — Step 1 for UPDATE
    //          Deletes old fields, saves new ones, commits immediately via REQUIRES_NEW
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconTemplateDetails updateTemplateAndFields(Long templateId, TemplateFieldDto request) {

        ReconTemplateDetails existingTemplate = reconTemplateDetailsRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));

        // Apply header-level updates
        if (request.getTemplateName() != null)
            existingTemplate.setTemplateName(request.getTemplateName());
        if (request.getTemplateType() != null)
            existingTemplate.setTemplateType(request.getTemplateType());
        if (request.getColumnCount() != null)
            existingTemplate.setColumnCount(request.getColumnCount());
        if (request.getReversalIndicator() != null)
            existingTemplate.setReversalIndicator(request.getReversalIndicator());
        if (request.getDataReference() != null)
            existingTemplate.setDataReferenceFlag(request.getDataReference());
        if (request.getOnlineRefund() != null)
            existingTemplate.setOnlRefundFlag(request.getOnlineRefund());

        reconTemplateDetailsRepository.save(existingTemplate);

        // Delete existing fields and insert new ones
        reconFieldDtlMastService.deleteFieldsByTemplateId(existingTemplate.getReconTemplateId());
        List<ReconFieldDetailsMaster> newFields = buildFieldEntities(request.getFieldDetails(), existingTemplate);
        reconFieldDetailsRepository.saveAll(newFields);
        logger.info("Updated {} field(s) for Template ID: {}", newFields.size(), existingTemplate.getReconTemplateId());

        // REQUIRES_NEW commits here — data is visible to the DB before method returns
        return existingTemplate;
    }

    // =========================================================================
    // HELPER — Step 2 (shared by CREATE and UPDATE)
    //          Calls the stored procedure in its own REQUIRES_NEW transaction
    //          so it always reads from the committed DB state
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String callStageTableProcedure(ReconTemplateDetails template) {

        Long templateId = template.getReconTemplateId();

        // Local variable — SimpleJdbcCall must NOT be a shared field (not thread-safe)
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_STAGE_TAB_CREATION")
                .declareParameters(
                        new SqlParameter("Prm_tmplt_Id", Types.NUMERIC),
                        new SqlOutParameter("Prm_Error", Types.VARCHAR));

        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("Prm_tmplt_Id", templateId);
        logger.info("Calling SP_STAGE_TAB_CREATION with Template ID: {}", templateId);

        Map<String, Object> result = jdbcCall.execute(inputParams);
        String procedureMsg = (String) result.get("Prm_Error");
        logger.info("SP_STAGE_TAB_CREATION returned: {} for Template ID: {}", procedureMsg, templateId);

        return procedureMsg;
    }

    // =========================================================================
    // HELPER — Compensating rollback for CREATE
    //          Deletes what was just committed if SP fails
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackTemplateAndFields(Long templateId) {
        try {
            reconFieldDetailsRepository.deleteByTemplateId(templateId);
            logger.info("Rolled back field details for Template ID: {}", templateId);
            reconTemplateDetailsRepository.deleteById(templateId);
            logger.info("Rolled back template for Template ID: {}", templateId);
        } catch (Exception e) {
            // Best-effort — log and continue, do not rethrow
            logger.error("Failed to rollback template/fields for Template ID {}: {}", templateId, e.getMessage(), e);
        }
    }

    // =========================================================================
    // HELPER — Compensating restore for UPDATE
    //          Re-inserts the pre-update snapshot if SP fails after commit
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreTemplateAndFields(ReconTemplateDetails previousSnapshot,
                                         List<ReconFieldDetailsMaster> previousFields) {
        try {
            Long templateId = previousSnapshot.getReconTemplateId();

            // Restore template header to its previous values
            reconTemplateDetailsRepository.save(previousSnapshot);
            logger.info("Restored template header for Template ID: {}", templateId);

            // Delete the newly committed fields and re-insert the previous ones
            reconFieldDetailsRepository.deleteByTemplateId(templateId);
            if (previousFields != null && !previousFields.isEmpty()) {
                // Clear primary keys so JPA inserts fresh rows bnkead of trying to merge
                previousFields.forEach(f -> f.setReconFieldId(null));
                reconFieldDetailsRepository.saveAll(previousFields);
            }
            logger.info("Restored {} previous field(s) for Template ID: {}",
                    previousFields != null ? previousFields.size() : 0, templateId);

        } catch (Exception e) {
            // Best-effort — log and continue, do not rethrow
            logger.error("Failed to restore template/fields for Template ID {}: {}",
                    previousSnapshot.getReconTemplateId(), e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE — Build field entity list from DTOs (reused by create + update)
    // =========================================================================

    private List<ReconFieldDetailsMaster> buildFieldEntities(
            List<FieldConfigurationDto> fieldDtos, ReconTemplateDetails template) {

        List<ReconFieldDetailsMaster> fieldEntities = new ArrayList<>();
        for (FieldConfigurationDto fieldDto : fieldDtos) {

            ReconFieldTypeMaster fieldType = reconFieldTypeRepository
                    .findByFieldTypeDes(fieldDto.getFieldtype())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Type: " + fieldDto.getFieldtype()));

            ReconFieldFormatMaster fieldFormat = reconFieldFormatRepository
                    .findByReconFieldFormatDesc(fieldDto.getFieldFormat())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Format: " + fieldDto.getFieldFormat()));

            fieldEntities.add(ReconFieldDetailsMapper.mapFieldDtoToEntity(
                    fieldDto, template, fieldType, fieldFormat));
        }
        return fieldEntities;
    }

    // =========================================================================
    // PRIVATE — Shallow copy of ReconTemplateDetails for pre-update snapshot
    //
    // !! Add any additional fields from your entity that updateTemplate may modify !!
    // =========================================================================

    private ReconTemplateDetails copyTemplateSnapshot(ReconTemplateDetails source) {
        ReconTemplateDetails snapshot = new ReconTemplateDetails();
        snapshot.setReconTemplateId(source.getReconTemplateId());
        snapshot.setTemplateName(source.getTemplateName());
        snapshot.setTemplateType(source.getTemplateType());
        snapshot.setColumnCount(source.getColumnCount());
        snapshot.setReversalIndicator(source.getReversalIndicator());
        snapshot.setDataReferenceFlag(source.getDataReferenceFlag());
        snapshot.setOnlRefundFlag(source.getOnlRefundFlag());
        snapshot.setStageTabName(source.getStageTabName());
        snapshot.setSubTemplateId(source.getSubTemplateId());
        // Add any other fields present on ReconTemplateDetails here
        return snapshot;
    }

    // =========================================================================
    // PRIVATE — Stage table name generator
    // =========================================================================

    private String generateStagetableName(ReconTemplateDetails template) {
        return "REC_" + CommonUtil.removeAllWhitespace(template.getTemplateName()).toUpperCase() + "_STAGE_T";
    }

    // =========================================================================
    // VIEW TEMPLATE
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusListPagination> viewTemplate(int page, int size) {
        try {
            logger.info("Fetching templates - Page: {}, Size: {}", page, size);

            if (page < 0) {
                return ResponseEntity.badRequest().body(
                        RestWithStatusListPagination.builder()
                                .status("ERROR").statusMsg("Page number cannot be negative")
                                .data(Collections.emptyList()).build());
            }
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest().body(
                        RestWithStatusListPagination.builder()
                                .status("ERROR").statusMsg("Size must be between 1 and 100")
                                .data(Collections.emptyList()).build());
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<ReconTemplateDetails> templatesPage = reconTemplateDetailsRepository.findTemplates(pageable);

            if (templatesPage.isEmpty()) {
                logger.warn("No templates found for page: {}", page);
                return ResponseEntity.ok(
                        RestWithStatusListPagination.builder()
                                .status(CommonConstants.SUCCESS).statusMsg("No templates available")
                                .data(Collections.emptyList())
                                .pageMetadata(PageMetadata.builder()
                                        .currentPage(page).pageSize(size)
                                        .totalElements(0L).totalPages(0)
                                        .isFirst(true).isLast(true)
                                        .hasNext(false).hasPrevious(false).build())
                                .build());
            }

            List<ReconTemplateDetails> templatesWithDetails =
                    reconTemplateDetailsRepository.fetchTemplateDetails(templatesPage.getContent());
            List<ReconTemplatesDetailsDTO> templateDTOs =
                    ReconTemplateDetailsMapper.toDTOList(templatesWithDetails);

            PageMetadata pageMetadata = PageMetadata.builder()
                    .currentPage(templatesPage.getNumber()).pageSize(templatesPage.getSize())
                    .totalElements(templatesPage.getTotalElements()).totalPages(templatesPage.getTotalPages())
                    .isFirst(templatesPage.isFirst()).isLast(templatesPage.isLast())
                    .hasNext(templatesPage.hasNext()).hasPrevious(templatesPage.hasPrevious()).build();

            logger.info("Successfully retrieved {} templates out of {} total on page {}",
                    templateDTOs.size(), templatesPage.getTotalElements(), page);

            return ResponseEntity.ok(
                    RestWithStatusListPagination.builder()
                            .status(CommonConstants.SUCCESS).statusMsg("Templates retrieved successfully")
                            .data(new ArrayList<>(templateDTOs)).pageMetadata(pageMetadata).build());

        } catch (Exception e) {
            logger.error("Error fetching templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestWithStatusListPagination.builder()
                            .status("ERROR").statusMsg("Error retrieving templates: " + e.getMessage())
                            .data(Collections.emptyList()).build());
        }
    }

    // =========================================================================
    // DELETE TEMPLATE
    // =========================================================================

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteTemplate(Long templateId) {
        Optional<ReconTemplateDetails> templateOpt = reconTemplateDetailsRepository.findById(templateId);
        if (!templateOpt.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList(CommonConstants.FAILURE, "Template not found with ID: " + templateId, null),
                    HttpStatus.NOT_FOUND);
        }
        reconFieldDetailsRepository.deleteByTemplateId(templateOpt.get().getReconTemplateId());
        reconTemplateDetailsRepository.deleteById(templateId);

        return new ResponseEntity<>(
                new RestWithStatusList(CommonConstants.SUCCESS, "Template deleted successfully", null),
                HttpStatus.OK);
    }

    // =========================================================================
    // SEARCH TEMPLATE
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusListPagination> searchTemplate(
            String templateName, String templateType, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ReconTemplateDetails> results;

        if (templateName != null && templateType != null) {
            results = reconTemplateDetailsRepository
                    .findByTemplateNameContainingIgnoreCaseAndTemplateType(templateName, templateType, pageable);
        } else if (templateName != null) {
            results = reconTemplateDetailsRepository
                    .findByTemplateNameContainingIgnoreCase(templateName, pageable);
        } else if (templateType != null) {
            results = reconTemplateDetailsRepository.findByTemplateType(templateType, pageable);
        } else {
            results = reconTemplateDetailsRepository.findAll(pageable);
        }

        List<ReconTemplatesDetailsDTO> templateDTOs = ReconTemplateDetailsMapper.toDTOList(results.getContent());

        PageMetadata pageMetadata = PageMetadata.builder()
                .currentPage(results.getNumber()).pageSize(results.getSize())
                .totalElements(results.getTotalElements()).totalPages(results.getTotalPages())
                .isFirst(results.isFirst()).isLast(results.isLast())
                .hasNext(results.hasNext()).hasPrevious(results.hasPrevious()).build();

        return ResponseEntity.ok(
                RestWithStatusListPagination.builder()
                        .status(CommonConstants.SUCCESS).statusMsg("Templates fetched successfully")
                        .data(new ArrayList<>(templateDTOs)).pageMetadata(pageMetadata).build());
    }

    // =========================================================================
    // GET TEMPLATE BY ID
    // =========================================================================

    @Override
    public ResponseEntity<?> getTemplateById(Long templateId) {
        try {
            Optional<ReconTemplateDetails> templateOpt = reconTemplateDetailsRepository.findById(templateId);
            if (!templateOpt.isPresent()) {
                return new ResponseEntity<>(
                        new RestWithStatusList(CommonConstants.FAILURE, "Template not found with ID: " + templateId, null),
                        HttpStatus.NOT_FOUND);
            }

            ReconTemplatesDetailsDTO templateDTO = ReconTemplateDetailsMapper.toDTO(templateOpt.get());
            List<Object> responseData = new ArrayList<>();
            responseData.add(templateDTO);

            return new ResponseEntity<>(
                    new RestWithStatusList(CommonConstants.SUCCESS, "Template fetched successfully", responseData),
                    HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error fetching template by ID {}: {}", templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR", "Error fetching template: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}