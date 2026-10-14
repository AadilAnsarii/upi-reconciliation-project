package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconKeyIdentifyMaster;

@Repository
public interface ReconKeyIdentifyMasterRepository extends JpaRepository<ReconKeyIdentifyMaster, Long> {

	List<ReconKeyIdentifyMaster> findByKeyIdentityId(Long reconKeyIdentifier);

}
