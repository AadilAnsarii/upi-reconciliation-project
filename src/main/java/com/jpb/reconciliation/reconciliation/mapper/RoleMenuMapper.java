package com.jpb.reconciliation.reconciliation.mapper;

import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.RoleMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.Role;

public class RoleMenuMapper {

	public static RoleMasterDto mapRoleWithMenu(Role role, List<ReconMenuMaster> menuWithRoleList, RoleMasterDto roleMasterDto) {
		
		roleMasterDto.setRoleId(role.getRoleId());
		roleMasterDto.setRoleName(role.getRoleName());
		roleMasterDto.setRoleCode(role.getRoleCode());
		roleMasterDto.setMenu(menuWithRoleList);
		return roleMasterDto;
	}

}
