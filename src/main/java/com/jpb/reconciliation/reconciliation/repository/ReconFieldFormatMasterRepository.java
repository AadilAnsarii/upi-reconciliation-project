package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMaster;

@Repository
public interface ReconFieldFormatMasterRepository extends JpaRepository<ReconFieldFormatMaster, Long> {

	Optional<ReconFieldFormatMaster> findByReconFieldFormatDesc(String desc);

//	List<ReconFieldFormatMaster> findByReconFieldFormatId(Long reconFieldFormat);

}
