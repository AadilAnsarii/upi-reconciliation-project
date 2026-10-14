package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;

import lombok.Data;

@Data
public class RoleMasterDto {
	private Long roleId;
	private String roleName;
	private String roleCode;
	private List<ReconMenuMaster> menu;
}
