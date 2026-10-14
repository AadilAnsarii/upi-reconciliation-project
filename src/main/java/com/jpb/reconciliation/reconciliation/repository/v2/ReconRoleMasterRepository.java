package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconRoleMasterRepository extends JpaRepository<ReconRoleMaster, Long> {

    boolean existsByRoleCode(String roleCode);

    /**
     * Atomically claims a Checker decision: the status only flips if it still matches what the
     * caller last read. Two Checkers racing on the same request will not both succeed — whichever
     * transaction commits first wins the WHERE clause, and the loser gets 0 rows affected (not a
     * silent overwrite). Callers must treat rowsUpdated == 0 as "someone else already decided this."
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReconRoleMaster r SET r.status = :newStatus WHERE r.roleId = :roleId AND r.status = :expectedStatus")
    int compareAndSetStatus(@Param("roleId") Long roleId, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus);

    boolean existsByRoleName(String roleName);

    Optional<ReconRoleMaster> findByRoleCode(String roleCode);

    Optional<ReconRoleMaster> findByRoleName(String roleName);

    List<ReconRoleMaster> findByStatus(String status);

    List<ReconRoleMaster> findByRoleType(String roleType);

    List<ReconRoleMaster> findByRoleIdIn(List<Long> roleIds);

    List<ReconRoleMaster> findByCreatedByIn(List<String> usernames);
}
