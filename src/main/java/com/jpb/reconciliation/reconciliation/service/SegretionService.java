package com.jpb.reconciliation.reconciliation.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;

@Service
public interface SegretionService {

	CompletableFuture<String> startSegretion(Long processId);

	Boolean startSegretion(ReconBatchProcessEntity reconProcessManager, ReconFileDetailsMaster reconFileDetails);

}
