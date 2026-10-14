package com.jpb.reconciliation.reconciliation.service.v2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.config.SchedulerConfig;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.LoginRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UserPasswordChangeRequest;
import com.jpb.reconciliation.reconciliation.entity.PasswordManager;
import com.jpb.reconciliation.reconciliation.entity.Role;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.mapper.ReconMenuMasterMapper;
import com.jpb.reconciliation.reconciliation.repository.PasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleManageRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;

import io.jsonwebtoken.JwtException;

@Service
public class ReconUserServiceImpl implements ReconUserService {

	@Override
	public ResponseEntity<RestWithStatusList> createUser(ReconUserDto reconUserDto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUserByUserId(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> getUserData(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<ResponseDto> removeUser(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<ResponseDto> changePassword(UserPasswordChangeRequest changePasswordRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestWithStatusList> updateUser(ReconUserDto userUpdateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestWithStatusList> getApprovedUSers(String approvedYN) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestWithStatusList> approveOrRejectUser(ReconUserDto approveUserRequest,
			UserDetails userDetails) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestWithStatusList> getAllUsers() {
		// TODO Auto-generated method stub
		return null;
	}

//	private final ProcessMasterRepository processMasterRepository;
//
//	private final SchedulerConfig schedulerConfig;
//
//	@Autowired
//	ReconUserRepository reconUserRepository;
//
//	@Autowired
//	RoleRepository roleRepository;
//
//	@Autowired
//	PasswordEncoder passwordEncoder;
//
//	@Autowired
//	CustomUserDetailService customUserDetailService;
//
//	@Autowired
//	AuditLogManagerService auditLogManagerService;
//
//	@Autowired
//	JwtHelper helper;
//
//	@Autowired
//	PasswordManagerRepository passwordManagerRepository;
//
//	@Autowired
//	RoleManageRepository roleManageRepository;
//
//	@Autowired
//	private AuthenticationManager manager;
//
//	private Logger logger = LoggerFactory.getLogger(ReconUserServiceImpl.class);
//
//	ReconUserServiceImpl(SchedulerConfig schedulerConfig, ProcessMasterRepository processMasterRepository) {
//		this.schedulerConfig = schedulerConfig;
//		this.processMasterRepository = processMasterRepository;
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> createUser(ReconUserDto reconUserDto) {
//		RestWithStatusList restWithStatusList = null;
//
//		Boolean existsUser = reconUserRepository.existsByUsername(reconUserDto.getUserName());
//		logger.info("Check user present into records :::::::::" + existsUser);
//		if (existsUser) {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "User Already Exists", null);
//			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
//		} else {
//			Role userRole = roleManageRepository.findByRoleId(reconUserDto.getRoleId());
////			ReconUser reconUser = ReconUser.mapToReconUser(reconUserDto, new ReconUser());
////			Role role = saveUserRole(reconUserDto.getRoleName());
////			roleRepository.save(role);
////			reconUser.setRole(userRole);
//			PasswordManager passwordManager = saveUserPasswordData(reconUserDto.getUserPassword());
////			passwordManager.setReconUser(reconUser);
////			reconUser.setPasswordManager(passwordManager);
////			logger.info("User data :::::::::" + reconUser.toString());
////			if (reconUser != null) {
////				reconUserRepository.save(reconUser);
////				reconUserRepository.flush();
////				restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "User Created Succussfully", null);
////			}
//		}
//		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
//	}
//
////	private Role saveUserRole(String roleName) {
////		Role role = new Role();
////		role.setRoleName(roleName);
////		if (roleName.equalsIgnoreCase("JPB")) {
////			role.setRoleCode("ROLE_JPB");
////		} else if (roleName.equalsIgnoreCase("bankadmin")) {
////			role.setRoleCode("ROLE_ADMIN");
////		} else if (roleName.equalsIgnoreCase("VIEW")) {
////			role.setRoleCode("ROLE_VIEW");
////		} else if (roleName.equalsIgnoreCase("AppSupport")) {
////			role.setRoleCode("ROLE_APPSUPPORT");
////		} else if (roleName.equalsIgnoreCase("RPSL")) {
////			role.setRoleCode("ROLE_RPSL");
////		}
////		role.setCreatedAt(LocalDateTime.now());
////		return role;
////	}
//
//	private PasswordManager saveUserPasswordData(String userPassword) {
//		PasswordManager passwordManager = new PasswordManager();
//		passwordManager.setUserPassword(passwordEncoder.encode(userPassword));
//		passwordManager.setExpirationDate(LocalDateTime.now());
//		passwordManager.setCreatedAt(LocalDateTime.now());
//		return passwordManager;
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> getUserByUserId(Long userId) {
//		RestWithStatusList restWithStatusList;
//		List<Object> addReconUser = new ArrayList<>();
//
//		ReconUser reconUser = reconUserRepository.findById(userId)
//				.orElseThrow(() -> new ResourceNotFoundException("USER NOT FOUND"));
//
////		ReconUserResponseDto reconUserResponseDto = ReconUserMapper.mapToReconUserResponseDto(reconUser,
////				new ReconUserResponseDto());
//		logger.info("User found in given user id :::" + userId + reconUser);
//		if (reconUser != null) {
//			addReconUser.add(reconUserResponseDto);
//			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "User found", addReconUser);
//			return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
//		}
//		restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "User Not Found", null);
//		return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
//	}
//
//	@Override
//	public ResponseEntity<?> login(LoginRequestDto request, HttpServletResponse response) {
//		RestWithStatusList restWithStatusList;
//		this.doAuthenticate(request.getUserName(), request.getUserPassword());
//		UserDetails userDetails = customUserDetailService.loadUserByUsername(request.getUserName());
//		System.out.println("Login User Details ::::::::::" + userDetails);
//
//		String token = this.helper.generateToken(userDetails);
//		Optional<ReconUser> reconUser = reconUserRepository.findByUserName(userDetails.getUsername());
//		System.out.println("User Details ::::::::::" + reconUser);
//		ReconUser user = reconUser.get();
//
//		if (CommonConstants.ACTIVE_Y.equals(user.getApprovedYn()) && user.getUserStatus().equalsIgnoreCase(CommonConstants.ACTIVE)) {
//			PasswordManager passwordManager = user.getPasswordManager();
//			passwordManager.setToken(token);
//			user.setPasswordManager(passwordManager);
//			reconUserRepository.save(user);
//
//			try {
//				String userName = helper.getUsernameFromToken(token);
//				String refreshToken = helper.generateTokenForRefresh(userName);
//				Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
//				refreshCookie.setHttpOnly(true);
//				refreshCookie.setMaxAge((int) (JwtHelper.JWT_TOKEN_REFRESH / 1000));
//				refreshCookie.setPath("/");
//				response.addCookie(refreshCookie);
//				auditLogManagerService.loginAudit(user, token, refreshToken);
//				return ResponseEntity.ok(new AuthResponse(token, refreshToken));
//			} catch (JwtException e) {
//				return ResponseEntity.status(403).body("Invalid or expired refresh token");
//			}
//		} else {
////			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "User is not approved or inactive", null);
//			ResponseDto responseDto = new ResponseDto(CommonConstants.STATUS_400, CommonConstants.MESSAGE_400);
//			return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
//		}
//	}
//
//	private void doAuthenticate(String userName, String userPassword) {
//
//		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userName,
//				userPassword);
//		logger.info("UsernamePasswordAuthenticationToken Response is ::::::::::::::::::::::::::::::" + authentication);
//		try {
//			Authentication authData = manager.authenticate(authentication);
//			logger.info("AUTHENTICATION DATA :::::::::::::::::" + authData);
//		} catch (BadCredentialsException e) {
//			throw new BadCredentialsException("Invalid Username or Password !");
//		} catch (DisabledException e) {
//			throw new DisabledException("User is not approved.");
//		}
//	}
//
//	@Override
//	public ResponseEntity<?> getUserData(String username) {
//		RestWithStatusList restWithStatusList;
//		List<Object> addReconUser = new ArrayList<>();
//
//		ReconUser reconUser = reconUserRepository.findByUserName(username)
//				.orElseThrow(() -> new ResourceNotFoundException("USER NOT FOUND :" + username));
//
//		ReconUserResponseDto reconUserResponseDto = ReconUserMapper.mapToReconUserResponseDto(reconUser,
//				new ReconUserResponseDto());
//
//		logger.info("User found in given user id :::" + username + reconUser);
//		addReconUser.add(reconUserResponseDto);
//		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "USER FOUND SUCCESSFULLY", addReconUser);
//		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
//	}
//
//	@Override
//	public ResponseEntity<ResponseDto> removeUser(Long userId) {
//		ReconUser reconUser = reconUserRepository.findById(userId)
//				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
//		reconUserRepository.deleteById(reconUser.getUserId());
//		roleRepository.deleteById(userId);
//		return new ResponseEntity<>(new ResponseDto(UserConstants.STATUS_200, "User Successfully Deleted"),
//				HttpStatus.OK);
//	}
//
//	@Override
//	public ResponseEntity<ResponseDto> changePassword(UserPasswordChangeRequest changePasswordRequest) {
//
//		Optional<ReconUser> findUser = reconUserRepository.findById(changePasswordRequest.getUserId());
//		logger.info("USER BY USER ID :::::::::::::::::::::::::::::::" + findUser.get());
//		ReconUser UserData = findUser.get();
//
//		PasswordManager password = UserData.getPasswordManager();
//
//		if (password != null) {
//			logger.info("USER PASSWORD :::::::::" + password.getUserPassword());
//			logger.info("OLD PASSWORD ::::::::::::::" + changePasswordRequest.getOldPassword());
//			logger.info("NEW PASSWORD :::::::::" + changePasswordRequest.getNewPassword());
//			logger.info("NEW CONFIRM PASSWORD :::::::::::::" + changePasswordRequest.getConfirmNewPassword());
//
//			boolean checkPassword = passwordEncoder.matches(changePasswordRequest.getOldPassword(),
//					password.getUserPassword());
//			logger.info("Checking password :::::::::::" + checkPassword);
//			if (checkPassword) {
//				String newEncodedPassword = passwordEncoder.encode(changePasswordRequest.getNewPassword());
//				password.setUserPassword(newEncodedPassword);
//				password.setExpirationDate(LocalDateTime.now());
//				password.setCreatedAt(LocalDateTime.now());
//				UserData.setPasswordManager(password);
//				reconUserRepository.save(UserData);
//			} else {
//				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//						.body(new ResponseDto("400", "Please enter valid old password!!"));
//			}
//		}
//		return ResponseEntity.status(HttpStatus.OK).body(new ResponseDto(CommonConstants.STATUS_200, "New Password Changed Successfully"));
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> updateUser(ReconUserDto userUpdateRequest) {
//		RestWithStatusList restWithStatusList = null;
//		Optional<ReconUser> userDetails = reconUserRepository.findById(userUpdateRequest.getUserId());
//
//		ReconUser user = userDetails.get();
//		if (user != null) {
//			ReconUser updatedUserDetails = ReconUserMapper.mapToReconUserUpdate(userUpdateRequest, user);
//			Optional<Role> getRole = roleRepository.findById(userUpdateRequest.getRoleId());
//			Role getRoleData = getRole.get();
//			updatedUserDetails.setRole(getRoleData);
//			reconUserRepository.save(updatedUserDetails);
//			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "User Update Successfully", null);
//		} else {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Please enter valid user details", null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
//		}
//		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> getApprovedUSers(String approvedYN) {
//		RestWithStatusList restWithStatusList = null;
//		List<Object> userDetailsList = new ArrayList<>();
//		List<ReconUser> allUsersIsPresent = reconUserRepository.findByApprovedYn(approvedYN);
//		logger.info("USer Details ::::::::" + allUsersIsPresent);
//		if (allUsersIsPresent.isEmpty()) {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "User Details Is Not Found", userDetailsList);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
//		} else {
//			List<ReconUserResponseDto> userDetailsListData = ReconUserMapper
//					.mapToReconUsersResponseDto(allUsersIsPresent);
//			userDetailsList.addAll(userDetailsListData);
//			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "User Update Successfully", userDetailsList);
//		}
//		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> approveOrRejectUser(ReconUserDto approveUserRequest,UserDetails userDetails) {
//		RestWithStatusList restWithStatusList = null;
//		if (approveUserRequest == null) {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Please select user to approve", null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//		} else {
//			Optional<ReconUser> user = reconUserRepository.findById(approveUserRequest.getUserId());
//			ReconUser getUser = user.get();
//			approveUserRequest.setApprovedBy(userDetails.getUsername());
//			ReconUser approvedOrRejectUser = ReconUserMapper.mapToApproveRejectReconUser(approveUserRequest, getUser);
//			logger.info("User approved or reject user :::::::::::" + approvedOrRejectUser);
//			reconUserRepository.save(approvedOrRejectUser);
//			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "User Approved Successfully", null);
//		}
//		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//	}
//
//	@Override
//	public ResponseEntity<RestWithStatusList> getAllUsers() {
//		RestWithStatusList restWithStatusList;
//		List<Object> userList = new ArrayList<>();
//
//		List<ReconUser> allUserExists = reconUserRepository.findAll();
//		List<ReconUserResponseDto> users = ReconUserMapper.mapToReconUsersResponseDto(allUserExists);
//		if (!allUserExists.isEmpty()) {
//			userList.addAll(users);
//			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Users found in the system", userList);
//		} else {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "No users found in the system", userList);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
//		}
//		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
//	}

}
