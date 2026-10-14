package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "RECON_OTP_MANAGER")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpManager {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OTP")
    @SequenceGenerator(name = "SEQ_OTP", sequenceName = "SEQ_OTP", allocationSize = 1)
    @Column(name = "otp_id")
    private Long otpId;

    @Column(name = "email_id", nullable = false)
    private String emailId;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    // Y = already used, N = still valid
    @Column(name = "is_used")
    private String isUsed = CommonConstants.INACTIVE_N;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}