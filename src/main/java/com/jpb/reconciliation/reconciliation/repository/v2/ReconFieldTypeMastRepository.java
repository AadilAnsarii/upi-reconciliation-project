package com.jpb.reconciliation.reconciliation.repository.v2;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldTypeMast;

@Repository
public interface ReconFieldTypeMastRepository extends JpaRepository<ReconFieldTypeMast, Long> {

    Optional<ReconFieldTypeMast> findByFieldTypeCode(String fieldTypeCode);

    // Legacy alias — old code calls findByFieldTypeDes()
    default Optional<ReconFieldTypeMast> findByFieldTypeDes(String desc) {
        return findByFieldTypeDesc(desc);
    }

    Optional<ReconFieldTypeMast> findByFieldTypeDesc(String fieldTypeDesc);
}
