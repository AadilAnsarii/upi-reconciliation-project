package com.jpb.reconciliation.reconciliation.secondary.repository;

import com.jpb.reconciliation.reconciliation.secondary.entity.FileProcessStatusEntity;

public interface SecondaryProcedureRepository {

	Boolean fileProcessingData(FileProcessStatusEntity newFileEntry);

	Boolean approvalProcess(FileProcessStatusEntity fileEntryForProcess, FileProcessStatusEntity fileInputByUser);

}
