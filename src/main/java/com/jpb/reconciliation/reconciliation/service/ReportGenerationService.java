package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;

import net.sf.jasperreports.engine.JRException;

@Service
public interface ReportGenerationService {

	ResponseEntity<String> generateReport(Long processId);

	ResponseEntity<ResponseDto> generateJasperReport(Long processId) throws JRException, IOException;

	ResponseEntity<RestWithStatusList> retriveReport(ReportDto reportDto);

	ResponseEntity<ResponseDto> generateReport(ReconFileDetailsMaster reconFileDetails, StringBuilder output,
			ReconBatchProcessEntity reconProcessManager, String dataCount, File file) throws JRException, IOException;

	void generateReconciliationReport(ReconProcessDefMaster reconProcessDefMaster, ReconBatchProcessEntity process) throws IOException;

	void generateExceptionReport(ReconProcessDefMaster reconProcessDefMaster, ReconBatchProcessEntity process);

	ResponseEntity<RestWithStatusList> viewExtrationDetails(ReportDto extractionRequest);

	void writeReversalReport(ReconFileDetailsMaster reconFileDetails, ReconBatchProcessEntity reconProcessManager);

	File generateManualFileForProcess(ReconFileDetailsMaster fileDetails);

}
