package com.jpb.reconciliation.reconciliation.secondary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.secondary.entity.ReconOfflineRefundElmsEntity;

@Repository
public interface ReconOfflineRefundElmsRepository extends JpaRepository<ReconOfflineRefundElmsEntity, Long> {

}
