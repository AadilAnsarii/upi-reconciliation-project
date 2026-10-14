package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpb.reconciliation.reconciliation.entity.ProcessMasterEntity;

public interface ProcessMasterRepository extends JpaRepository<ProcessMasterEntity, Long> {

}
