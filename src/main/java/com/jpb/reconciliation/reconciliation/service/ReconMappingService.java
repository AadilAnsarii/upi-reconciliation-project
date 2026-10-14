//package com.jpb.reconciliation.reconciliation.service;
//
//
//import java.util.List;
//
//import com.jpb.reconciliation.reconciliation.dto.ReconMappingRequest;
//import com.jpb.reconciliation.reconciliation.dto.ReconMappingResponse;
//import com.kalinfotech.reconciliation.dto.response.FieldDefinitionResponse;
//
//public interface ReconMappingService {
//
//    /**
//     * Atomically replaces ALL recon mappings for a template.
//     * Calls ReconMappingRepository.deleteByTemplateMaster_TemplateId first,
//     * then saveAll() with the new list.
//     * Each request item's templateFieldId must resolve via
//     * FieldDefinitionRepository.findByFieldIdAndIsDeletedFalse.
//     * Writes a SAVE_RECON_MAPPINGS audit entry.
//     */
//    List<ReconMappingResponse> saveMappings(Long templateId,
//                                             List<ReconMappingRequest> requests);
//
//    /**
//     * Returns all ReconMapping rows for the template.
//     * Returns an empty list (not an exception) when none exist.
//     */
//    List<ReconMappingResponse> getMappings(Long templateId);
//
//    /**
//     * Returns active FieldDefinition rows for the template ordered by sequenceOrder.
//     * Used by the UI to populate the templateFieldId dropdown.
//     * Delegates to FieldDefinitionRepository
//     *   .findByTemplateMaster_TemplateIdAndIsDeletedFalseOrderBySequenceOrderAsc.
//     */
//    List<FieldDefinitionResponse> getAvailableFields(Long templateId);
//
//    /**
//     * Deletes a single ReconMapping by its primary key.
//     * Validates the mapping belongs to templateId before deleting.
//     * Writes a DELETE_MAPPING audit entry.
//     */
//    void deleteMapping(Long templateId, Long mappingId);
//
//    /**
//     * Deletes ALL ReconMapping rows for a template in one batch.
//     * Delegates to ReconMappingRepository.deleteByTemplateMaster_TemplateId.
//     * Writes a DELETE_ALL_MAPPINGS audit entry.
//     */
//    void deleteAllMappings(Long templateId);
//}
