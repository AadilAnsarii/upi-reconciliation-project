package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.TTUMConfigMasterEntity;

@Repository
public interface TTUMConfigMasterRepository extends JpaRepository<TTUMConfigMasterEntity, Long> {

}
