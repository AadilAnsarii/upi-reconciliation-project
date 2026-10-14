package com.jpb.reconciliation.reconciliation.entity.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "RCN_RECON_PWD_MANAGER")
public class ReconPasswordManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PWD_ID")
    private Long pwdId;

    @Column(name = "USER_PASSWORD", length = 255)
    private String userPassword;

    @Column(name = "EXPIRATION_DATE")
    private LocalDateTime expirationDate;

    @Column(name = "TOKEN", length = 500)
    private String token;

    @OneToOne
    @JoinColumn(name = "USER_ID")
    @JsonIgnore
    private ReconUser reconUser;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", updatable = false, length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT", insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", insertable = false, length = 100)
    private String updatedBy;
}
