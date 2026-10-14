package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import lombok.Data;

@Data
public class RoleCreateRequest {
	 private Long roleId;
	 private String roleCode;
     private String roleName;
     private List<Long> menuIds;
}
