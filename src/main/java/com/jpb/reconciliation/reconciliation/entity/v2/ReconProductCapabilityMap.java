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
@Table(name = "RECON_PRODUCT_CAPABILITY_MAP")
public class ReconProductCapabilityMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CAPABILITY_ID")
    private Long capabilityId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "CAPABILITY_TYPE", length = 10, nullable = false)
    private String capabilityType;   // MAKER / CHECKER

    @Column(name = "ACTION_TYPE", length = 10, nullable = false)
    private String actionType;       // ROLE / MENU / USER

    @Column(name = "CAN_DELEGATE", length = 1, nullable = false)
    private String canDelegate = "N";

    @Column(name = "GRANTED_BY", nullable = false)
    private Long grantedBy;

    @Column(name = "GRANTED_AT", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "STATUS", length = 10, nullable = false)
    private String status = CommonConstants.ACTIVE;

    @Column(name = "REVOKED_BY")
    private Long revokedBy;

    @Column(name = "REVOKED_AT")
    private LocalDateTime revokedAt;

    @Column(name = "REVOKE_REASON", length = 30)
    private String revokeReason;     // MANUAL / USER_BLOCKED / PRODUCT_DEACTIVATED

    @Column(name = "SUSPENDED_AT")
    private LocalDateTime suspendedAt;   // set when STATUS becomes SUSPENDED (product expired, grace period running)
}
