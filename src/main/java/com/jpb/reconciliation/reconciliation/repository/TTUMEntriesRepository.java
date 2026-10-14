package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.TTUMEntriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TTUMEntriesRepository  extends JpaRepository<TTUMEntriesEntity, Long> {

	TTUMEntriesEntity findByRteProcessId(Long ttumProcessId);
}
