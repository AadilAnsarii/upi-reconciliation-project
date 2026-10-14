package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.LoadMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconKeyIdentifyMaster;

@Repository
public interface LoadMasterRepository extends JpaRepository<LoadMasterEntity, Long> {
	
	LoadMasterEntity findByRlmFileId(Long rlmFileId);
	

}
