package com.jpb.reconciliation.reconciliation.entity.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RECON_AUTH_TOKEN")
public class ReconAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TOKEN_ID")
    private Long tokenId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "TOKEN_TYPE", length = 20, nullable = false)
    private String tokenType;

    @Column(name = "TOKEN_VALUE", length = 512, nullable = false)
    private String tokenValue;

    @Column(name = "STATUS", length = 10, nullable = false)
    private String status = CommonConstants.ACTIVE;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
