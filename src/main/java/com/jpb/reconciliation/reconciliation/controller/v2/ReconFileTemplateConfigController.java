package com.jpb.reconciliation.reconciliation.controller.v2;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconTemplateConfigRequest;
import com.jpb.reconciliation.reconciliation.service.v2.ReconFileTemplateConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/v2/template/")
@RequiredArgsConstructor
@Tag(name = "Reconciliation Template v2", description = "APIs for managing reconciliation file templates and field definitions")
public class ReconFileTemplateConfigController {

    private final ReconFileTemplateConfigService templateConfigService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v2/template/template-configure
    // Dual-mode: templateId absent = CREATE, templateId present = UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(value = "template-configure", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Configure or update a template. "
            + "templateId absent → creates new template. "
            + "templateId present → updates existing template. "
            + "action=SAVE promotes DRAFT to ACTIVE; SAVE→DRAFT switch is rejected.")
    public ResponseEntity<RestWithMapStatusList> configureTemplate(
            @RequestBody ReconTemplateConfigRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        request.setCreatedBy(userDetails.getUsername());
        request.setUpdatedBy(userDetails.getUsername());
        return templateConfigService.configureTemplateAndFieldData(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v2/template/update-template/{templateId}
    // Dedicated update endpoint — routes through the same configure flow.
    // templateId is taken from the path variable and set on the request,
    // so configureTemplateAndFieldData() treats it as an UPDATE automatically.
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping(value = "/update-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Update an existing template and its field definitions by templateId. "
            + "Routes through the configure flow — SAVE→DRAFT demotion is rejected.")
    public ResponseEntity<RestWithMapStatusList> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody ReconTemplateConfigRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        request.setCreatedBy(userDetails.getUsername());
        request.setUpdatedBy(userDetails.getUsername());
        return templateConfigService.updateTemplateById(templateId, request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v2/template/view-template?action=&page=0&size=10
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/view-template", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "List all templates with pagination. action: D=DRAFT, Y=ACTIVE, absent=all")
    public ResponseEntity<RestWithMapStatusList> viewTemplateList(
            @Parameter(description = "Status filter: D=DRAFT, Y=ACTIVE, omit for all")
            @RequestParam(required = false) String action,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Records per page (max 100)", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return templateConfigService.viewTemplate(action, page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v2/template/get-template/{templateId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/get-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Get full template details including all field definitions by templateId")
    public ResponseEntity<RestWithMapStatusList> getTemplateById(
            @PathVariable Long templateId) {
        return templateConfigService.getTemplateById(templateId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v2/template/search-template?templateName=&templateType=
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/search-template", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Search templates by name or type with pagination")
    public ResponseEntity<RestWithMapStatusList> searchTemplate(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String templateType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return templateConfigService.searchTemplate(templateName, templateType, page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v2/template/delete-template/{templateId}
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping(value = "/delete-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Soft-delete a template by templateId (status = INACTIVE, its fields "
            + "flagged IS_DELETED = 'Y'). Recorded in AUDIT_LOG.")
    public ResponseEntity<RestWithMapStatusList> deleteTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // This endpoint previously took no principal, so a delete left no trace of who did it.
        return templateConfigService.deleteTemplate(
                templateId, null != userDetails ? userDetails.getUsername() : null);
    }
    
    
    @PostMapping(
    	    value = "/auto-detect-fields",
    	    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    	    produces = CommonConstants.APPLICATION_JSON
    	)
    @Operation(summary = "Read uploaded file and auto detect field configuration. "
            + "headerLineCount (Excel only, optional): 1-based row number of the real "
            + "header, for reports with metadata rows above the header (e.g. NEFT-ISO exports).")
    public ResponseEntity<RestWithMapStatusList> autoDetectFields(
    	    @RequestParam("file") MultipartFile file,
    	    @RequestParam(value = "headerLineCount", required = false) Integer headerLineCount,
    	    @AuthenticationPrincipal UserDetails userDetails) {
        return templateConfigService.autoDetectFields(file, headerLineCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v2/template/next-template-code
    // Preview only — shows what the code WOULD be; nothing is reserved until save.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping(value = "/next-template-code", produces = CommonConstants.APPLICATION_JSON)
    @Operation(summary = "Preview the next auto-generated template code, without reserving it")
    public ResponseEntity<RestWithMapStatusList> previewNextTemplateCode() {
        return templateConfigService.previewNextTemplateCode();
    }
}
