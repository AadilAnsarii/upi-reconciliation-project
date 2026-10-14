package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RefreshRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import net.sf.jasperreports.engine.JRException;

@Service
public interface ExtractionService {
    
	CompletableFuture<String> startExtraction(ReconFileDetailsMaster reconFileDetails, List<ReconBatchProcessEntity> extractionProcessList, List<File> fileList, ReconUser userData) throws IOException, InterruptedException, JRException;

	ResponseEntity<RestWithStatusList> refreshProcessData(Long processId);

	List<ReconBatchProcessEntity> extractionRunningStatus(List<File> fileList,
			ReconFileDetailsMaster reconFileDetails, ReconUser userData);

	ResponseEntity<RestWithStatusList> refreshExtraction(List<RefreshRequestDto.ProcessManager> requestProcess);
    
}
