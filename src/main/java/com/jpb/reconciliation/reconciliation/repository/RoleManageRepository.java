package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.Role;

@Repository
public interface RoleManageRepository extends JpaRepository<Role, Long> {

	Role findByRoleId(Long roleId);

	boolean existsByRoleCode(String roleCode);

	boolean existsByRoleCodeAndRoleIdNot(String newCode, Long roleId);

}
