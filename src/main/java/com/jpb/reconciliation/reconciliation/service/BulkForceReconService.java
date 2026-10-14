package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface BulkForceReconService {

	ResponseEntity<RestWithStatusList> getBulkForceReconProcessList();

	ResponseEntity<RestWithStatusList> processReconBulkForceMatch(Long reconProcessId);

	ResponseEntity<RestWithStatusList> actionForReconBulkProcess(Long processId);

}
