package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RoleCreateRequest;
import com.jpb.reconciliation.reconciliation.dto.RoleMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.Role;
import com.jpb.reconciliation.reconciliation.mapper.RoleMenuMapper;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleManageRepository;

@Service
public class RoleManageServiceImpl implements RoleManageService {

	Logger logger = LoggerFactory.getLogger(RoleManageServiceImpl.class);

	@Autowired
	RoleManageRepository roleManageRepository;

	@Autowired
	MenuMasterRepository menuMasterRepository;

	@Autowired
	com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository roleMenuMapRepository;

	@Override
	public ResponseEntity<RestWithStatusList> getAllRoleDetails() {
		RestWithStatusList restWithStatusList;

		List<Object> roleWithMenuList = new ArrayList<>();
		List<Role> getAllRole = roleManageRepository.findAll();

		if (!getAllRole.isEmpty()) {
//			getAllRole.stream().map(role -> (Object) role).forEach(roleList::add);
			for (Role role : getAllRole) {
				// Menus a role can see now come from C_ROLE_MENU_MAP. RECON_MENU_MASTER.ROLE_ID
				// held ownership, not grants, and no longer exists.
				List<Long> menuIds = roleMenuMapRepository.findMenuIdsByRoleId(role.getRoleId());
				List<ReconMenuMaster> menuWithRoleList = menuIds.isEmpty()
						? new ArrayList<>()
						: menuMasterRepository.findAllById(menuIds);
				RoleMasterDto roleMenu = RoleMenuMapper.mapRoleWithMenu(role, menuWithRoleList, new RoleMasterDto());
				roleWithMenuList.add(roleMenu);
			}

			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Role Found Successfully", roleWithMenuList);

		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Role Not Found ", roleWithMenuList);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusList> getRoleByUserLogin(Long verifiedRoleId) {
		RestWithStatusList restWithStatusList = null;
		List<Object> roleData = new ArrayList<>();
		Role getLoginRole = roleManageRepository.findByRoleId(verifiedRoleId);
		roleData.add(getLoginRole);

		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Login role user found.", roleData);
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> createRole(String roleCode, String roleName, List<Long> menuIds,
			String createdBy) {

		logger.info("Creating role: code={}, name={}, menus={}", roleCode, roleName, menuIds);

		// 1. Validate: duplicate role code check
		if (roleManageRepository.existsByRoleCode(roleCode)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RestWithStatusList(CommonConstants.FAILURE,
					"Role code '" + roleCode + "' already exists. Please use a unique code.", null));
		}

		// 2. Create and save role
		Role newRole = new Role();
		newRole.setRoleCode(roleCode.toUpperCase().trim());
		newRole.setRoleName(roleName.trim());
		newRole.setCreatedAt(LocalDateTime.now());
		newRole.setCreatedBy(createdBy);
		Role savedRole = roleManageRepository.save(newRole);

		logger.info("Role saved with ID: {}", savedRole.getRoleId());
//         
//		// 3. Assign menus if provided
//		int menusAssigned = 0;
//		if (menuIds != null && !menuIds.isEmpty()) {
//			List<ReconMenuMaster> menus = menuMasterRepository.findAllById(menuIds);
//			if (menus.size() != menuIds.size()) {
//				logger.warn("Some menuIds were not found. Requested: {}, Found: {}", menuIds.size(), menus.size());
//			}
//			menusAssigned = menuMasterRepository.assignMenusToRole(savedRole.getRoleId(), menuIds);
//			logger.info("Assigned {} menus to role {}", menusAssigned, savedRole.getRoleId());
//		}
//
//		List<Object> result = new ArrayList<>();
//		result.add(buildRoleResponse(savedRole));
//
//		String message = menusAssigned > 0 ? "Role created successfully with " + menusAssigned + " menus assigned"
//				: "Role created successfully (no menus assigned)";

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new RestWithStatusList(CommonConstants.SUCCESS, "Role created successfully", null));
	}

	@Override
	public ResponseEntity<RestWithStatusList> updateRoleDetails(RoleCreateRequest request, String username) {
		Role existingRole = roleManageRepository.findById(request.getRoleId()).orElse(null);
		if (existingRole == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					new RestWithStatusList(CommonConstants.FAILURE, "Role with ID '" + request.getRoleId() + "' not found.", null));
		}

		if (request.getRoleCode() != null) {
			String newCode = request.getRoleCode().toUpperCase().trim();
			if (roleManageRepository.existsByRoleCodeAndRoleIdNot(newCode, existingRole.getRoleId())) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RestWithStatusList(CommonConstants.FAILURE,
						"Role code '" + newCode + "' is already used by another role.", null));
			}
			existingRole.setRoleCode(newCode);
		}

		if (request.getRoleName() != null) {
			existingRole.setRoleName(request.getRoleName().trim());
		}

		existingRole.setUpdatedAt(LocalDateTime.now());
		existingRole.setUpdatedBy(username);
		roleManageRepository.save(existingRole);

		logger.info("Role updated successfully: roleId={}", existingRole.getRoleId());

		return ResponseEntity.status(HttpStatus.OK)
				.body(new RestWithStatusList(CommonConstants.SUCCESS, "Role updated successfully", null));
	}

}
