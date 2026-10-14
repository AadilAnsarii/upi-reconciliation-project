package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import net.sf.jasperreports.engine.JRException;

@Service
public interface SqlLoaderService {

	String startLoading(String generateControlFile, String generateLogFile, String generateBadFile,
			ReconFileDetailsMaster reconFileDetails, ReconBatchProcessEntity reconProcessManager, ReconUser userDetails, File file)
			throws JRException, IOException;

}
