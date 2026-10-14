package com.jpb.reconciliation.reconciliation.service.v2;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import net.sf.jasperreports.engine.JRException;

@Service
public interface SqlLoaderAiService {

	String startLoading(String generateControlFile, String generateLogFile, String generateBadFile,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconBatchProcessEntity reconBatchProcessEntity,
			ReconUser userData, File file) throws JRException, IOException;

}
