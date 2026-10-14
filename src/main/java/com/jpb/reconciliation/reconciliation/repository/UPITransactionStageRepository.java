package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.UPITransactionStageEntity;

@Repository
public interface UPITransactionStageRepository extends JpaRepository<UPITransactionStageEntity, String> {

	@Query(value = "SELECT u.* FROM upi_p2m_txn_stg_tbl u", nativeQuery = true)
	List<UPITransactionStageEntity> searchTransactions(@Param("postingBatchId") String postingBatchId,
			@Param("referenceNumber") String referenceNumber,
			@Param("payerAccountNumber") String payerAccountNumber,
			@Param("payeeAccountNumber") String payeeAccountNumber);
	
}
