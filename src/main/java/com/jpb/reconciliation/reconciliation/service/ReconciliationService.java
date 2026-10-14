package com.jpb.reconciliation.reconciliation.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

@Service
public interface ReconciliationService{

	List<ReconBatchProcessEntity> runReconciliation(Long processId, ReconProcessDefMaster reconProcessDefMaster, ReconUser userData);

    ResponseEntity<RestWithStatusList> refreshReconciliation(List<RefreshRequestDto.ProcessManager> requestProcess);

    CompletableFuture<String> startReconciliation(Long processId, List<ReconBatchProcessEntity> reconciliationStatus, ReconProcessDefMaster reconProcessDefMaster, ReconUser userData) throws IOException;
}
