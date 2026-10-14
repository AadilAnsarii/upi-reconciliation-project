// ══════════════════════════════════════════════════════════════════
// FILE PATH:
//   src/main/java/com/jpb/reconciliation/reconciliation/dto/ForgotPasswordRequest.java
//
// ⚠️  CRITICAL — YEH STEP ZAROOR KARO:
//   1. Apne project mein dhundho:
//      src/main/java/.../service/ForgotPasswordRequest.java
//   2. Us file ko DELETE karo — woh galat jagah hai
//   3. Yeh file dto package mein rakho
//   4. mvn clean compile ya IntelliJ → Build → Rebuild Project
//
// Root cause: Java/IDE service package wali file pick kar raha tha
// (bina @Data ke ya wrong package ke), isliye getters undefined tha
// ══════════════════════════════════════════════════════════════════
 
package com.jpb.reconciliation.reconciliation.dto;
 
import lombok.Data;
 
@Data
public class ForgotPasswordRequestDto {
 
    // User email se dhundh sakta hai
    private String email;

    // Ya username + bankCode se dhundh sakta hai
    private String username;

    private String bankCode;

    // OTP — Step B verify ke liye
    private String otp;
}
 