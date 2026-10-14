package com.jpb.reconciliation.reconciliation.repository.merchantpayout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.merchantpayout.MerchantEntity;

@Repository
public interface MerchantRepository extends JpaRepository<MerchantEntity, Long> {
	

}
