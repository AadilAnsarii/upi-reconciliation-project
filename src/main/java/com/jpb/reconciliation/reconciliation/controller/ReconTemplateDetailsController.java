package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusListPagination;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;
import com.jpb.reconciliation.reconciliation.service.ReconTemplateDetailsService;
import com.jpb.reconciliation.reconciliation.service.SegretionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path = "/api/v1/template/")
@Tag(name = "Reconciliation Template", description = "APIs for managing reconciliation templates")
public class ReconTemplateDetailsController {

	private final SegretionService segretionService;

	@Autowired
	ReconTemplateDetailsService reconTemplateDetailsService;

	ReconTemplateDetailsController(SegretionService segretionService) {
		this.segretionService = segretionService;
	}

	@PostMapping(value = "add-template", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> addTemplate(@RequestBody ReconTemplateDetailsDto reconTemplateDetailsDto) {
		return reconTemplateDetailsService.addTemplate(reconTemplateDetailsDto);
	}

	@PostMapping(value = "template-configure", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> configureTemplateWithField(@RequestBody TemplateFieldDto templateFieldrequest) {
		RestWithStatusList restWithStatusList = null;

		if (templateFieldrequest.getFieldDetails() == null || templateFieldrequest.getFieldDetails().isEmpty()) {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "fieldDetails must not be null or empty", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}
		return reconTemplateDetailsService.configureTemplateAndFieldData(templateFieldrequest);

	}

	@GetMapping(value = "/view-template", produces = CommonConstants.APPLICATION_JSON)
	@Operation(summary = "View all templates with pagination", description = "Retrieves all reconciliation templates with field details using pagination")
	public ResponseEntity<RestWithStatusListPagination> viewTemplate(
			@Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,

			@Parameter(description = "Number of records per page", example = "10") @RequestParam(defaultValue = "10") int size) {
		return reconTemplateDetailsService.viewTemplate(page, size);
	}

//	@PutMapping(value = "/update-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
//	@Operation(summary = "Update an existing template with its field details")
//	public ResponseEntity<RestWithStatusList> updateTemplate(@PathVariable Long templateId,
//			@RequestBody TemplateFieldDto templateFieldRequest) {
//		if (templateFieldRequest.getFieldDetails() == null || templateFieldRequest.getFieldDetails().isEmpty()) {
//			RestWithStatusList response = new RestWithStatusList(CommonConstants.FAILURE, "fieldDetails must not be null or empty",
//					null);
//			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//		}
//		return reconTemplateDetailsService.updateTemplate(templateId, templateFieldRequest);
//	}
	
	
	@PutMapping(value = "/update-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
	@Operation(summary = "Update template fields and format only")
	public ResponseEntity<RestWithStatusList> updateTemplate(@PathVariable Long templateId,
	        @RequestBody TemplateFieldDto templateFieldRequest) {
	    if (templateFieldRequest.getFieldDetails() == null || templateFieldRequest.getFieldDetails().isEmpty()) {
	        return new ResponseEntity<>(
	                new RestWithStatusList(CommonConstants.FAILURE, "fieldDetails must not be null or empty", null),
	                HttpStatus.BAD_REQUEST);
	    }
	    return reconTemplateDetailsService.updateTemplate(templateId, templateFieldRequest);
	}

	@DeleteMapping(value = "/delete-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
	@Operation(summary = "Delete a template by ID")
	public ResponseEntity<RestWithStatusList> deleteTemplate(@PathVariable Long templateId) {
		return reconTemplateDetailsService.deleteTemplate(templateId);
	}

	@GetMapping(value = "/search-template", produces = CommonConstants.APPLICATION_JSON)
	@Operation(summary = "Search templates by name or type")
	public ResponseEntity<RestWithStatusListPagination> searchTemplate(
			@RequestParam(required = false) String templateName, @RequestParam(required = false) String templateType,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return reconTemplateDetailsService.searchTemplate(templateName, templateType, page, size);
	}

	@GetMapping(value = "/get-template/{templateId}", produces = CommonConstants.APPLICATION_JSON)
	@Operation(summary = "Get template by ID")
	public ResponseEntity<?> getTemplateById(@PathVariable Long templateId) {
		return reconTemplateDetailsService.getTemplateById(templateId);
	}


}
