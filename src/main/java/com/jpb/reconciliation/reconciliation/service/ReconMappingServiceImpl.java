//package com.jpb.reconciliation.reconciliation.service;
//
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.dto.ReconMappingRequest;
//import com.jpb.reconciliation.reconciliation.dto.ReconMappingResponse;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.bytebuddy.dynamic.DynamicType.Builder.FieldDefinition;
//
///**
// * Implementation of ReconMappingService.
// *
// * Repositories used  (all exist in /repository):
// *   ReconMappingRepository       – findByTemplateMaster_TemplateId,
// *                                   deleteByTemplateMaster_TemplateId,
// *                                   saveAll, findById, delete
// *   TemplateMasterRepository     – findByTemplateIdAndIsDeletedFalse
// *   FieldDefinitionRepository    – findByFieldIdAndIsDeletedFalse,
// *                                   findByTemplateMaster_TemplateIdAndIsDeletedFalseOrderBySequenceOrderAsc
// *
// * Mapper methods used  (all exist in their respective mapper classes):
// *   templateMapper.toReconMappingResponse(ReconMapping) → ReconMappingResponse
// *   fieldMapper.toResponse(FieldDefinition)             → FieldDefinitionResponse
// *
// * Audit service:
// *   auditService.log(templateId, action, entityType, entityId, oldValue, newValue)
// */
//@Service
//@Transactional
//@RequiredArgsConstructor
//@Slf4j
//public class ReconMappingServiceImpl implements ReconMappingService {
//
//    private final ReconMappingRepository    mappingRepo;     // ReconMappingRepository
//    private final TemplateMasterRepository  templateRepo;    // TemplateMasterRepository
//    private final FieldDefinitionRepository fieldRepo;       // FieldDefinitionRepository
//    private final TemplateAuditService      auditService;    // TemplateAuditService
//    private final TemplateMapper            templateMapper;  // TemplateMapper
//    private final FieldDefinitionMapper     fieldMapper;     // FieldDefinitionMapper
//
//    // =========================================================================
//    // SAVE MAPPINGS  (atomic replace-all)
//    // =========================================================================
//
//    @Override
//    public List<ReconMappingResponse> saveMappings(Long templateId,
//                                                    List<ReconMappingRequest> requests) {
//        TemplateMaster template = findTemplateOrThrow(templateId);
//
//        // Step 1: delete all existing mappings for this template
//        mappingRepo.deleteByTemplateMaster_TemplateId(templateId);
//
//        // Step 2: build and persist new mappings
//        List<ReconMapping> mappings = requests.stream().map(req -> {
//            FieldDefinition field = fieldRepo
//                    .findByFieldIdAndIsDeletedFalse(req.getTemplateFieldId())
//                    .orElseThrow(() -> new TemplateException(
//                            "Field not found or deleted: " + req.getTemplateFieldId()));
//
//            return ReconMapping.builder()
//                    .templateMaster(template)
//                    .systemField(req.getSystemField())
//                    .fieldDefinition(field)
//                    .transformationRule(req.getTransformationRule())
//                    .isMandatoryMapping(req.getIsMandatoryMapping() != null
//                            ? req.getIsMandatoryMapping() : false)
//                    .build();
//        }).collect(Collectors.toList());
//
//        List<ReconMapping> saved = mappingRepo.saveAll(mappings);
//        log.info("Saved {} recon mappings for templateId={}", saved.size(), templateId);
//
//        auditService.log(templateId, "SAVE_RECON_MAPPINGS", "RECON_MAPPING",
//                templateId, null, saved.size() + " mappings saved");
//
//        return saved.stream()
//                .map(templateMapper::toReconMappingResponse)
//                .collect(Collectors.toList());
//    }
//
//    // =========================================================================
//    // GET MAPPINGS
//    // =========================================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ReconMappingResponse> getMappings(Long templateId) {
//        return mappingRepo.findByTemplateMaster_TemplateId(templateId)
//                .stream()
//                .map(templateMapper::toReconMappingResponse)
//                .collect(Collectors.toList());
//    }
//
//    // =========================================================================
//    // GET AVAILABLE FIELDS  (for UI dropdown)
//    // =========================================================================
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<FieldDefinitionResponse> getAvailableFields(Long templateId) {
//        return fieldRepo
//                .findByTemplateMaster_TemplateIdAndIsDeletedFalseOrderBySequenceOrderAsc(
//                        templateId)
//                .stream()
//                .map(fieldMapper::toResponse)
//                .collect(Collectors.toList());
//    }
//
//    // =========================================================================
//    // DELETE SINGLE MAPPING
//    // =========================================================================
//
//    @Override
//    public void deleteMapping(Long templateId, Long mappingId) {
//        ReconMapping mapping = mappingRepo.findById(mappingId)
//                .orElseThrow(() -> new TemplateException(
//                        "Recon mapping not found: " + mappingId));
//
//        // Guard: the mapping must belong to the given templateId
//        if (!mapping.getTemplateMaster().getTemplateId().equals(templateId)) {
//            throw new TemplateException("Mapping " + mappingId
//                    + " does not belong to template " + templateId);
//        }
//
//        mappingRepo.delete(mapping);
//        log.info("Deleted recon mapping id={} for templateId={}", mappingId, templateId);
//
//        auditService.log(templateId, "DELETE_MAPPING", "RECON_MAPPING",
//                mappingId, mapping, null);
//    }
//
//    // =========================================================================
//    // DELETE ALL MAPPINGS FOR A TEMPLATE
//    // =========================================================================
//
//    @Override
//    public void deleteAllMappings(Long templateId) {
//        // Confirm the template exists before wiping its mappings
//        findTemplateOrThrow(templateId);
//
//        mappingRepo.deleteByTemplateMaster_TemplateId(templateId);
//        log.info("Deleted all recon mappings for templateId={}", templateId);
//
//        auditService.log(templateId, "DELETE_ALL_MAPPINGS", "RECON_MAPPING",
//                templateId, null, "All mappings removed");
//    }
//
//    // =========================================================================
//    // PRIVATE HELPERS
//    // =========================================================================
//
//    private TemplateMaster findTemplateOrThrow(Long templateId) {
//        return templateRepo.findByTemplateIdAndIsDeletedFalse(templateId)
//                .orElseThrow(() -> new TemplateException(
//                        "Template not found: " + templateId));
//    }
//}
