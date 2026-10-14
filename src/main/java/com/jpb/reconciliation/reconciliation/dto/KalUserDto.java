package com.jpb.reconciliation.reconciliation.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class KalUserDto {

    @NotBlank(message = "Username is required")
    @Pattern(
        regexp = "^[a-z]+\\.[a-z]+$",
        message = "Username must be firstname.lastname (all lowercase, e.g. john.doe)"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(
        regexp = "^[a-zA-Z0-9._%+\\-]+@kalinfotech\\.com$",
        message = "Only @kalinfotech.com email addresses are allowed"
    )
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[6-9][0-9]{9}$",
        message = "Phone must be a valid 10-digit Indian mobile number"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=.*[0-9]).{8,}$",
        message = "Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character"
    )
    private String password;
}
