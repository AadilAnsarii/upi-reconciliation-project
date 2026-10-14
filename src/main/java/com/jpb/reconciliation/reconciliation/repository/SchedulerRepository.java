package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.SchedulerJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulerRepository extends JpaRepository<SchedulerJob, Long> {
    List<SchedulerJob> findByDays(String today);

    List<SchedulerJob> findAll();
}
