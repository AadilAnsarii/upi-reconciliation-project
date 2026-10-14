package com.jpb.reconciliation.reconciliation.controller;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ForgotPasswordResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResetPasswordRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.ForgotPasswordService;

@RestController
@RequestMapping("/auth")
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    @PostMapping(value = "/forgot-password", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(
            @RequestBody ForgotPasswordRequestDto request) {

        return forgotPasswordService.forgotPassword(request);
    }

    @PostMapping(value = "/reset-password", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<ForgotPasswordResponseDto> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        return forgotPasswordService.resetPassword(request);
    }
}