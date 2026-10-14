package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {

    private String bankCode;

    private String username;

    // ✅ Also support email-based reset (for forgot password via email input)
    private String email;

    private String otp;

    private String newPassword;

    private String confirmNewPassword;
}