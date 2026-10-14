package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleProductMap;

import java.util.List;

@Repository
public interface ReconRoleProductMapRepository extends JpaRepository<ReconRoleProductMap, Long> {

    @Query("SELECT m.productId FROM ReconRoleProductMap m WHERE m.roleId = :roleId")
    List<Long> findProductIdsByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("DELETE FROM ReconRoleProductMap m WHERE m.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
