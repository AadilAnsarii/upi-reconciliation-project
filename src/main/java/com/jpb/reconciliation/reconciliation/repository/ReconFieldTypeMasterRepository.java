package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMaster;

@Repository
public interface ReconFieldTypeMasterRepository extends JpaRepository<ReconFieldTypeMaster, Long> {

	Optional<ReconFieldTypeMaster> findByFieldTypeDes(String fieldTypeDes);

//	List<ReconFieldTypeMaster> findByFieldTypeId(Long reconFieldType);

}
