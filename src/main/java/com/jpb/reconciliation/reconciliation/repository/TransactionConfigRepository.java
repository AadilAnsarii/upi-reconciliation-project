package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.TransactionConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionConfigRepository extends JpaRepository<TransactionConfigEntity, Long> {

    TransactionConfigEntity findByTemplateId(Long tempId);
}
