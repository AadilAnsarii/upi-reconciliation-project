package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;

public interface ForgotPasswordService {

    ResponseEntity<ForgotPasswordResponseDto> forgotPassword(ForgotPasswordRequestDto request);

    ResponseEntity<ForgotPasswordResponseDto> resetPassword(ResetPasswordRequest request);
}