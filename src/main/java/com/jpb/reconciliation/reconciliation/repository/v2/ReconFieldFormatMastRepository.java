package com.jpb.reconciliation.reconciliation.repository.v2;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldFormatMast;

@Repository
public interface ReconFieldFormatMastRepository extends JpaRepository<ReconFieldFormatMast, Long> {

    Optional<ReconFieldFormatMast> findByFieldFormatCode(String fieldFormatCode);

    // Legacy alias — old code calls findByReconFieldFormatDesc()
    default Optional<ReconFieldFormatMast> findByReconFieldFormatDesc(String desc) {
        return findByFieldFormatDesc(desc);
    }

    Optional<ReconFieldFormatMast> findByFieldFormatDesc(String fieldFormatDesc);
}
