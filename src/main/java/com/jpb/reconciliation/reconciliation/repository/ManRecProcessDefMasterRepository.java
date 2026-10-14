package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ManRecProcessDefMaster;

@Repository
public interface ManRecProcessDefMasterRepository extends JpaRepository<ManRecProcessDefMaster, Long> {

	ManRecProcessDefMaster findByManRecProcessId(Long processId);

}
