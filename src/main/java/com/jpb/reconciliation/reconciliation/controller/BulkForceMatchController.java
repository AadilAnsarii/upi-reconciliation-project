package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.BulkForceReconService;

@RestController
@RequestMapping("/api/v1/bulkforce")
public class BulkForceMatchController {

	@Autowired
	BulkForceReconService bulkForceReconService;

	@GetMapping(path = "/get-bulkforce-list")
	ResponseEntity<RestWithStatusList> getReconBulkForceProcessList() {
		return bulkForceReconService.getBulkForceReconProcessList();
	}

	@GetMapping(path = "/process-bulkforce")
	ResponseEntity<RestWithStatusList> processReconBulkForceMatch(@RequestParam Long reconProcessId) {
		return bulkForceReconService.processReconBulkForceMatch(reconProcessId);
	}

	@GetMapping(path = "/action-bulkprocess", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> actionReconBulkProcess(@RequestParam Long processId) {
		if (processId != null) {
			return bulkForceReconService.actionForReconBulkProcess(processId);
		} else {
			RestWithStatusList restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Process id is required", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}
	}

}
