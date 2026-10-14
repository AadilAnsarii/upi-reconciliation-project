//package com.jpb.reconciliation.reconciliation.mapper;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
//import com.jpb.reconciliation.reconciliation.dto.ReconUserResponseDto;
//import com.jpb.reconciliation.reconciliation.dto.RoleDto;
//import com.jpb.reconciliation.reconciliation.entity.ReconUser;
//import com.jpb.reconciliation.reconciliation.entity.Role;
//
//public class ReconUserMapper {
//
//	public static ReconUser mapToReconUser(ReconUserDto reconUserDto, ReconUser reconUser) {
//		reconUser.setUserName(reconUserDto.getUserName());
//		reconUser.setUserStatus(CommonConstants.INACTIVE);
//		reconUser.setDesignation(reconUserDto.getDesignation());
//		reconUser.setEmailId(reconUserDto.getEmailId());
//		reconUser.setInstitution(reconUserDto.getInstitution());
//		reconUser.setMobileNumber(reconUserDto.getMobileNumber());
//		reconUser.setType(reconUserDto.getType());
//		reconUser.setCreatedAt(LocalDateTime.now());
//		reconUser.setCreatedBy(reconUserDto.getCreatedBy());
//		reconUser.setUpdatedAt(LocalDateTime.now());
//		reconUser.setUpdatedBy(reconUserDto.getUpdatedBy());
//		return reconUser;
//	}
//
//	public static ReconUserResponseDto mapToReconUserResponseDto(ReconUser reconUser,
//			ReconUserResponseDto reconUserResponseDto) {
//		reconUserResponseDto.setUserId(reconUser.getUserId());
//		reconUserResponseDto.setUserName(reconUser.getUserName());
//		reconUserResponseDto.setUserStatus(reconUser.getUserStatus());
//		reconUserResponseDto.setDesignation(reconUser.getDesignation());
//		reconUserResponseDto.setEmailId(reconUser.getEmailId());
//		reconUserResponseDto.setInstitution(reconUser.getInstitution());
//		reconUserResponseDto.setMobileNumber(reconUser.getMobileNumber());
//		reconUserResponseDto.setType(reconUser.getType());
//		reconUserResponseDto.setCreatedAt(LocalDateTime.now());
//		reconUserResponseDto.setCreatedBy(reconUser.getUserName());
//
//		RoleDto roleDto = mapToRole(reconUser.getRole());
//		reconUserResponseDto.setRole(roleDto);
//		return reconUserResponseDto;
//	}
//
//	private static RoleDto mapToRole(Role role) {
//		if (role == null) {
//			return null;
//		}
//
//		RoleDto roleDto = new RoleDto();
//		roleDto.setRoleName(role.getRoleName());
//		roleDto.setRoleId(role.getRoleId());
//		roleDto.setRoleCode(role.getRoleCode());
//		roleDto.setCreatedAt(LocalDateTime.now());
//		roleDto.setCreatedBy(role.getRoleName());
//		return roleDto;
//	}
//
//	public static List<ReconUserResponseDto> mapToReconUsersResponseDto(List<ReconUser> allUsersIsPresent) {
//		List<ReconUserResponseDto> userDataList = new ArrayList<>();
//		for (ReconUser user : allUsersIsPresent) {
//			ReconUserResponseDto mapUser = new ReconUserResponseDto();
//
//			mapUser.setUserId(user.getUserId());
//			mapUser.setUserName(user.getUserName());
//			mapUser.setUserStatus(user.getUserStatus());
//			mapUser.setDesignation(user.getDesignation());
//			mapUser.setEmailId(user.getEmailId());
//			mapUser.setInstitution(user.getInstitution());
//			mapUser.setMobileNumber(user.getMobileNumber());
//			mapUser.setType(user.getType());
//			mapUser.setCreatedAt(user.getCreatedAt());
//			mapUser.setCreatedBy(user.getCreatedBy());
//			mapUser.setUpdatedAt(user.getUpdatedAt());
//			mapUser.setUpdatedBy(user.getUpdatedBy());
//
//			RoleDto roleDto = mapToRole(user.getRole());
//			mapUser.setRole(roleDto);
//			userDataList.add(mapUser);
//		}
//		return userDataList;
//	}
//
//	public static ReconUser mapToApproveRejectReconUser(ReconUserDto reconUserDto, ReconUser reconUser) {
//		reconUser.setApprovedYn(reconUserDto.getApprovedYn());
//		reconUser.setApprovedBy(reconUserDto.getApprovedBy());
//		reconUser.setUserStatus(CommonConstants.ACTIVE);
//		return reconUser;
//	}
//
//	public static ReconUser mapToReconUserUpdate(ReconUserDto reconUserDto, ReconUser reconUser) {
////		reconUser.setUserId(reconUserDto.getUserId());
//		reconUser.setUserName(reconUserDto.getUserName());
//		reconUser.setUserStatus(reconUserDto.getUserStatus());
//		reconUser.setDesignation(reconUserDto.getDesignation());
//		reconUser.setEmailId(reconUserDto.getEmailId());
//		reconUser.setInstitution(reconUserDto.getInstitution());
//		reconUser.setMobileNumber(reconUserDto.getMobileNumber());
//		reconUser.setType(reconUserDto.getType());
//		reconUser.setCreatedAt(LocalDateTime.now());
//		reconUser.setCreatedBy(reconUserDto.getCreatedBy());
//		reconUser.setUpdatedAt(LocalDateTime.now());
//		reconUser.setUpdatedBy(reconUserDto.getUpdatedBy());
//		return reconUser;
//	}
//
//}
