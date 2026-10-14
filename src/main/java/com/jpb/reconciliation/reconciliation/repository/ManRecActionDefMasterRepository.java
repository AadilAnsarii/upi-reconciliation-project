package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ManRecActionDefMaster;

@Repository
public interface ManRecActionDefMasterRepository extends JpaRepository<ManRecActionDefMaster, Long> {

	Optional<ManRecActionDefMaster> findByManRecActionId(Long manRecActionId);
	
}	
