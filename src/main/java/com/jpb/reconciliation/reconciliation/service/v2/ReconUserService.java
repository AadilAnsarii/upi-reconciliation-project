package com.jpb.reconciliation.reconciliation.service.v2;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.LoginRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UserPasswordChangeRequest;

@Service
public interface ReconUserService {

	ResponseEntity<RestWithStatusList> createUser(ReconUserDto reconUserDto);

	ResponseEntity<RestWithStatusList> getUserByUserId(Long userId);

	ResponseEntity<?> login(LoginRequestDto loginRequestDto, HttpServletResponse response);

	ResponseEntity<?> getUserData(String username);

	ResponseEntity<ResponseDto> removeUser(Long userId);

	ResponseEntity<ResponseDto> changePassword(UserPasswordChangeRequest changePasswordRequest);

	ResponseEntity<RestWithStatusList> updateUser(ReconUserDto userUpdateRequest);

	ResponseEntity<RestWithStatusList> getApprovedUSers(String approvedYN);

	ResponseEntity<RestWithStatusList> approveOrRejectUser(ReconUserDto approveUserRequest, UserDetails userDetails);

	ResponseEntity<RestWithStatusList> getAllUsers();
    
}
