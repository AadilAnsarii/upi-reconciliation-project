package com.jpb.reconciliation.reconciliation.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconProcessRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconProcessResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.ReconProcessService;
import com.jpb.reconciliation.reconciliation.util.ReconProcessRequestValidator;

@RestController
@RequestMapping("/api/recon/process")
@CrossOrigin(origins = "*")
public class ReconProcessController {

	@Autowired
	private ReconProcessService reconProcessService;

	@Autowired
	private ReconProcessRequestValidator reconProcessRequestValidator;

	@InitBinder("reconProcessRequest")
	protected void initBinder(WebDataBinder binder) {
		binder.addValidators(reconProcessRequestValidator);
	}

	/**
	 * Add new recon process with validation POST /api/recon/process/add
	 */
	@PostMapping("/add")
	public ResponseEntity<?> addReconProcess(@Valid @RequestBody ReconProcessRequest request,
			BindingResult bindingResult) {

		// Check for validation errors
		if (bindingResult.hasErrors()) {
			Map<String, String> errors = new HashMap<>();
			bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
			return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
		}

		try {
			ReconProcessResponse response = reconProcessService.createReconProcess(request);
			return new ResponseEntity<>(response, HttpStatus.CREATED);
		} catch (Exception e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage());
			return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * View recon process by ID GET /api/recon/process/view/{processId}
	 */
	@GetMapping("/view/{processId}")
	public ResponseEntity<ReconProcessResponse> viewReconProcess(@PathVariable Long processId) {
		try {
			ReconProcessResponse response = reconProcessService.getReconProcessById(processId);
			if (response != null) {
				return new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * View all recon processes GET /api/recon/process/view/all
	 */
	@GetMapping("/view/all")
	public ResponseEntity<List<ReconProcessResponse>> viewAllReconProcesses() {
		try {
			List<ReconProcessResponse> processes;
			processes = reconProcessService.getAllReconProcesses();
			return new ResponseEntity<>(processes, HttpStatus.OK);
		} catch (

		Exception e) {
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Update recon process PUT /api/recon/process/update/{processId}
	 */
	@PutMapping("/update/{processId}")
	public ResponseEntity<ReconProcessResponse> updateReconProcess(@PathVariable Long processId,
			@Valid @RequestBody ReconProcessRequest request) {
		try {
			ReconProcessResponse response = reconProcessService.updateReconProcess(processId, request);
			if (response != null) {
				return new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Delete recon process DELETE /api/recon/process/delete/{processId}
	 */
	@DeleteMapping("/delete/{processId}")
	public ResponseEntity<RestWithStatusList> deleteReconProcess(@PathVariable Long processId) {
		try {
			RestWithStatusList restWithStatusList = null;
			reconProcessService.deleteReconProcess(processId);
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Recon Process Succussfully Deleted.", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get recon process by name GET /api/recon/process/view/name/{processName}
	 */
	@GetMapping("/view/name/{processName}")
	public ResponseEntity<ReconProcessResponse> getReconProcessByName(@PathVariable String processName) {
		try {
			ReconProcessResponse response = reconProcessService.getReconProcessByName(processName);
			if (response != null) {
				return new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}