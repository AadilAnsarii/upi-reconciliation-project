package com.jpb.reconciliation.reconciliation.dto.v2;

import lombok.Data;

@Data
public class ResetPasswordDto {

    private String emailId;

    private String otp;

    private String newPassword;

    private String confirmPassword;
}