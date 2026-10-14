package com.jpb.reconciliation.reconciliation.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.ReconciliationService;

@RestController
@RequestMapping(path = "/api/v1/recon")
public class ReconciliationController {

	@Autowired
	ReconciliationService reconciliationService;

	@Autowired
	ReconUserRepository reconUserRepository;

	@Autowired
	ReconProcessDefMasterRepository reconProcessDefMasterRepository;
	
	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	Logger logger = LoggerFactory.getLogger(ReconciliationController.class);

	@GetMapping(value = "/start-reconciliation", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> startRecon(@RequestParam Long processId,
			@AuthenticationPrincipal UserDetails userDetails) throws IOException {
		RestWithStatusList restWithStatusList;
		List<Object> reconciliationData = new ArrayList<>();
		ReconProcessDefMaster reconProcessDefMaster = reconProcessDefMasterRepository.findByReconProcessId(processId);
		logger.info("Recon Process Def Master Data ::::::::::" + reconProcessDefMaster);
		ReconUser userData = reconUserRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
		
		List<ReconBatchProcessEntity> checkProcessIsRunning = reconBatchProcessEntityRepository
				.findByProcessIdAndStatus(processId, "Running");
		if (!checkProcessIsRunning.isEmpty()) {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Reconciliation process is running", reconciliationData);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		
		if (reconProcessDefMaster != null) {
			List<ReconBatchProcessEntity> reconciliationStatus = reconciliationService.runReconciliation(processId,
					reconProcessDefMaster, userData);
			for (ReconBatchProcessEntity process : reconciliationStatus) {
				reconciliationData.add(process);
				logger.info("RUNNING RECONCILIATION EACH PROCESS ::::::::::::::" + process);
			}

			if (!reconciliationStatus.isEmpty()) {
				CompletableFuture<String> reconStatus = reconciliationService.startReconciliation(processId,
						reconciliationStatus, reconProcessDefMaster, userData);
				logger.info("RECONCILIATION STATUS ::::::::::::::" + reconStatus);
			} else {
				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Reconciliation not started", null);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
			}
		}
		logger.info("reconciliationData ::::::::::::::" + reconciliationData);
		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Reconciliation has started", reconciliationData);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@PostMapping(value = "/refresh-reconciliation", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> refreshReconciliation(
			@RequestBody List<RefreshRequestDto.ProcessManager> requestProcess) {
		return reconciliationService.refreshReconciliation(requestProcess);
	}
}
