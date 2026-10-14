package com.jpb.reconciliation.reconciliation.controller.v2;
//package com.jpb.reconciliation.reconciliation.controller;
//
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import javax.validation.Valid;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.jpb.reconciliation.reconciliation.dto.ReconMappingRequest;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.enums.SystemField;
//import com.jpb.reconciliation.reconciliation.service.ReconMappingService;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.var;
//
///**
// * Recon field mapping per template.
// *
// * URL base  : /api/v1/templates/{templateId}/recon-mapping
// * Entity    : ReconMapping  (tbl_recon_mapping)
// * Service   : ReconMappingService  (impl: ReconMappingServiceImpl)
// * Response  : RestWithStatusList – project-wide standard wrapper
// *
// * Request DTO  – ReconMappingRequest  (one item in the list):
// *   systemField        SystemField enum – TXN_DATE | AMOUNT | TXN_ID | UTR
// *                                         DR_CR | RRN | ACCOUNT_NO | CHANNEL | STATUS
// *   templateFieldId    Long  – FK to tbl_field_definition.field_id
// *   transformationRule String (optional)  e.g. "DIVIDE:100", "MAP:CR=CREDIT,DR=DEBIT"
// *   isMandatoryMapping Boolean (default false)
// *
// * Response DTO – ReconMappingResponse:
// *   mappingId, templateId, systemField, templateFieldId, templateFieldName,
// *   transformationRule, isMandatoryMapping, createdBy, createdAt
// *
// * Validation rules enforced by the entity:
// *   – unique constraint on (template_id, system_field)
// *   – each systemField value must match the SystemField enum exactly
// *
// * Available-fields DTO – FieldDefinitionResponse:
// *   fieldId, templateId, fieldName, fieldLabel, dataType, sequenceOrder, ...
// */
//@RestController
//@RequestMapping("/api/v1/templates/{templateId}/recon-mapping")
//@RequiredArgsConstructor
//@Tag(name = "Recon Mapping",
//        description = "Map template fields to reconciliation engine system fields")
//public class ReconMappingController {
//
//    // Injected via @RequiredArgsConstructor – interface only, no impl reference
//    private final ReconMappingService reconMappingService;
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // POST  /api/v1/templates/{templateId}/recon-mapping
//    // Atomically replaces ALL existing mappings for this template.
//    // Send the full desired mapping list; any system fields not present
//    // in the new list will be removed.
//    // ─────────────────────────────────────────────────────────────────────────
//    @PostMapping
//    @Operation(summary = "Save recon field mappings — atomically replaces all existing mappings")
//    public ResponseEntity<RestWithStatusList> saveMappings(
//            @PathVariable Long templateId,
//            @Valid @RequestBody List<ReconMappingRequest> requests) {
//
//        if (requests == null || requests.isEmpty()) {
//            return ResponseEntity.badRequest().body(RestWithStatusList.builder()
//                    .status("VALIDATION_FAILED")
//                    .statusMsg("Mapping list must not be empty.")
//                    .data(List.of())
//                    .build());
//        }
//
//        return ok("Mappings saved successfully.",
//                reconMappingService.saveMappings(templateId, requests));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET   /api/v1/templates/{templateId}/recon-mapping
//    // ─────────────────────────────────────────────────────────────────────────
//    @GetMapping
//    @Operation(summary = "Get all recon mappings for a template")
//    public ResponseEntity<RestWithStatusList> getMappings(
//            @PathVariable Long templateId) {
//
//        var mappings = reconMappingService.getMappings(templateId);
//        String msg = mappings.isEmpty()
//                ? "No recon mappings configured for this template."
//                : "Recon mappings fetched successfully.";
//        return ok(msg, mappings);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET   /api/v1/templates/{templateId}/recon-mapping/available-fields
//    // Returns the active field definitions of the template so the UI can
//    // present a dropdown of valid templateFieldId options.
//    // ─────────────────────────────────────────────────────────────────────────
//    @GetMapping("/available-fields")
//    @Operation(summary = "Get template fields eligible for recon mapping (dropdown source)")
//    public ResponseEntity<RestWithStatusList> getAvailableFields(
//            @PathVariable Long templateId) {
//
//        return ok("Available fields fetched.",
//                reconMappingService.getAvailableFields(templateId));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET   /api/v1/templates/{templateId}/recon-mapping/system-fields
//    // Returns all valid SystemField enum values for UI dropdowns.
//    // No service call needed — enum is static.
//    // ─────────────────────────────────────────────────────────────────────────
//    @GetMapping("/system-fields")
//    @Operation(summary = "Get all valid system field values "
//            + "(TXN_DATE, AMOUNT, TXN_ID, UTR, DR_CR, RRN, ACCOUNT_NO, CHANNEL, STATUS)")
//    public ResponseEntity<RestWithStatusList> getSystemFields(
//            @PathVariable Long templateId) {
//
//        List<String> fields = Arrays.stream(SystemField.values())
//                .map(Enum::name)
//                .collect(Collectors.toList());
//
//        return ok("System fields fetched.", fields);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // DELETE /api/v1/templates/{templateId}/recon-mapping/{mappingId}
//    // Deletes a single mapping row by its primary key.
//    // Validates that the mapping belongs to the given templateId.
//    // ─────────────────────────────────────────────────────────────────────────
//    @DeleteMapping("/{mappingId}")
//    @Operation(summary = "Delete a specific recon mapping by mapping ID")
//    public ResponseEntity<RestWithStatusList> deleteMapping(
//            @PathVariable Long templateId,
//            @PathVariable Long mappingId) {
//
//        reconMappingService.deleteMapping(templateId, mappingId);
//        return ok("Mapping deleted successfully.", null);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // DELETE /api/v1/templates/{templateId}/recon-mapping
//    // Deletes ALL mappings for the template in one shot.
//    // Useful when reconfiguring the template from scratch.
//    // ─────────────────────────────────────────────────────────────────────────
//    @DeleteMapping
//    @Operation(summary = "Delete all recon mappings for a template")
//    public ResponseEntity<RestWithStatusList> deleteAllMappings(
//            @PathVariable Long templateId) {
//
//        reconMappingService.deleteAllMappings(templateId);
//        return ok("All recon mappings deleted for template " + templateId + ".", null);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // private helper
//    // ─────────────────────────────────────────────────────────────────────────
//    private ResponseEntity<RestWithStatusList> ok(String msg, Object data) {
//        return ResponseEntity.ok(RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg(msg)
//                .data(data != null ? List.of(data) : List.of())
//                .build());
//    }
//}
