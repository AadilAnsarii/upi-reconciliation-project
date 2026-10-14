package com.jpb.reconciliation.reconciliation.service.v2;


import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconTemplateConfigRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ReconFileTemplateConfigService {

//    ResponseEntity<RestWithMapStatusList> addTemplate(ReconTemplateDetailsDto dto);

    ResponseEntity<RestWithMapStatusList> configureTemplateAndFieldData(ReconTemplateConfigRequest request);

    ResponseEntity<RestWithMapStatusList> updateTemplateById(Long templateId, ReconTemplateConfigRequest request);

    ResponseEntity<RestWithMapStatusList> viewTemplate(String action, int page, int size);

    /** @param actor caller's username — written to AUDIT_LOG and onto the flagged field rows */
    ResponseEntity<RestWithMapStatusList> deleteTemplate(Long templateId, String actor);

    ResponseEntity<RestWithMapStatusList> searchTemplate(String name, String type, int page, int size);

    ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId);

    /** @param headerLineCount 1-based row number of the real header (Excel only, optional) —
     *                         lets a report with leading metadata rows auto-detect correctly. */
    ResponseEntity<RestWithMapStatusList> autoDetectFields(MultipartFile file, Integer headerLineCount);

    /** Preview-only — returns what the next templateCode WOULD be, without reserving it. */
    ResponseEntity<RestWithMapStatusList> previewNextTemplateCode();
}
