package com.jpb.reconciliation.reconciliation.secondary.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.constants.FileProcessStatus;
import com.jpb.reconciliation.reconciliation.secondary.entity.FileProcessStatusEntity;

@Repository
public interface FileProcessStatusRepository extends JpaRepository<FileProcessStatusEntity, Long> {
 
	FileProcessStatusEntity findByFileIdAndUploadedBy(Long fileId, String uploadedBy);

	List<FileProcessStatusEntity> findByStatus(FileProcessStatus pending);

	List<FileProcessStatusEntity> findByStatusAndUploadDataStatus(FileProcessStatus pending,
			FileProcessStatus processedSuccess);

	List<FileProcessStatusEntity> findByFileName(String originalFilename);

	List<FileProcessStatusEntity> findByFileNameAndStatus(String fileName, FileProcessStatus approved);
}
