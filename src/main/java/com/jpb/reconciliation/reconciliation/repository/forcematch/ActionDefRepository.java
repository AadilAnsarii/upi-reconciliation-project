package com.jpb.reconciliation.reconciliation.repository.forcematch;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.forcematch.ActionDef;

@Repository
public interface ActionDefRepository extends JpaRepository<ActionDef, Long> {

    @Query("SELECT a FROM ActionDef a ORDER BY a.rmtActionId ASC")
    List<ActionDef> findAllOrdered();

    List<ActionDef> findByRmtActDataTbl(String tableName);

    List<ActionDef> findByRmtRuleId(Long ruleId);
}
