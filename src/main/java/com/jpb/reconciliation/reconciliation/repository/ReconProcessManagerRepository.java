package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.REProcessManager;

@Repository
public interface ReconProcessManagerRepository extends JpaRepository<REProcessManager, Long> {

	List<REProcessManager> findByProcessId(Long processId);

	REProcessManager findByProcessIdAndSequenceId(Long processId, Long sequenceId);

}
