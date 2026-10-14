package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.TTUMRefundQueryMasterEntity;

@Repository
public interface TTUMRefundQueryMasterRepository extends JpaRepository<TTUMRefundQueryMasterEntity, String> {

	List<TTUMRefundQueryMasterEntity> findByProcessId(Long ttumProcessId);

}
