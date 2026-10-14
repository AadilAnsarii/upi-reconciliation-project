package com.jpb.reconciliation.reconciliation.repository.v2;

import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconProductMasterRepository extends JpaRepository<ReconProductMaster, Long> {

    Optional<ReconProductMaster> findByProductName(String productName);

    boolean existsByProductName(String productName);

    List<ReconProductMaster> findByStatus(String status);
}
