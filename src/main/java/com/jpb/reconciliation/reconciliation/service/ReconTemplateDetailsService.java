package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusListPagination;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;

@Service
public interface ReconTemplateDetailsService {

	ResponseEntity<?> addTemplate(ReconTemplateDetailsDto reconTemplateDetailsDto);

	ResponseEntity<RestWithStatusList> configureTemplateAndFieldData(TemplateFieldDto templateFieldrequest);

	ResponseEntity<RestWithStatusListPagination> viewTemplate(int page, int size);

	ResponseEntity<?> getTemplateById(Long templateId);

	ResponseEntity<RestWithStatusList> updateTemplate(Long templateId, TemplateFieldDto templateFieldRequest);

	ResponseEntity<RestWithStatusList> deleteTemplate(Long templateId);

	ResponseEntity<RestWithStatusListPagination> searchTemplate(String templateName, String templateType, int page,
			int size);

}
