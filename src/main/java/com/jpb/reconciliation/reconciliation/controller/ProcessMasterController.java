package com.jpb.reconciliation.reconciliation.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFileTmpltMastRepository;
import com.jpb.reconciliation.reconciliation.service.ProcessMasterService;


@RestController
@RequestMapping("/api/v1")
public class ProcessMasterController {

	@Autowired
	ProcessMasterService processMasterService;

	@Autowired
	ReconFileDetailsMasterRepository fileDetailsMasterRepository;

	@Autowired
	ReconProcessDefMasterRepository processDefMasterRepository;

	@Autowired
	ReconFileTmpltMastRepository reconFileTmpltMastRepository;

	@GetMapping(value = "get-process", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllProcessData(@RequestParam String menuFlag){
		return processMasterService.getAllProcessData(menuFlag);
	}

	// Add Menu's Extraction/Reconciliation Submenu step: select the file/process directly by its
	// own real PK (RFD_FILE_ID / RPM_PROCESS_ID) — no PROCESS_MAST_ID grouping step at all. The
	// Template Name is just that same row's own name, returned alongside so the frontend can
	// auto-populate a read-only field once the id is picked. menuProcessId is always this id.
	@GetMapping(value = "get-extraction-files", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getExtractionFiles() {
		List<Object> files = fileDetailsMasterRepository.findAll().stream()
				.map(f -> {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("reconFileId", f.getReconFileId());
					m.put("reconFileName", f.getReconFileName());
					return (Object) m;
				})
				.collect(Collectors.toList());
		return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Files found successfully", files), HttpStatus.OK);
	}

	// V2 counterpart of getExtractionFiles — every new Extraction Submenu is V2-by-default (see
	// ReconMenuMaster.version), so Add Menu's Template picker sources from RECON_FILE_TMPLT_MAST
	// (TEMPLATE_ID) instead of RCN_FILE_DTL_MAST (RFD_FILE_ID) here.
	@GetMapping(value = "get-extraction-templates", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getExtractionTemplatesForMenu() {
		List<Object> templates = reconFileTmpltMastRepository.findAllTemplates().stream()
				.map(t -> {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("templateId", t.getTemplateId());
					m.put("templateName", t.getTemplateName());
					return (Object) m;
				})
				.collect(Collectors.toList());
		return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Templates found successfully", templates), HttpStatus.OK);
	}

	@GetMapping(value = "get-reconciliation-process-defs", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getReconciliationProcessDefs() {
		List<Object> defs = processDefMasterRepository.findAll().stream()
				.map(p -> {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("reconProcessId", p.getReconProcessId());
					m.put("reconProcessName", p.getReconProcessName());
					return (Object) m;
				})
				.collect(Collectors.toList());
		return new ResponseEntity<>(new RestWithStatusList(CommonConstants.SUCCESS, "Processes found successfully", defs), HttpStatus.OK);
	}
}
