package com.jpb.reconciliation.reconciliation.repository.merchantpayout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.merchantpayout.PartnerEntity;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

}
