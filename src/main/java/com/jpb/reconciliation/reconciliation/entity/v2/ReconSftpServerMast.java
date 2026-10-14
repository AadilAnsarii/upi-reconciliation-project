package com.jpb.reconciliation.reconciliation.entity.v2;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "RECON_SFTP_SERVER_MAST")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconSftpServerMast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SERVER_ID")
    private Long serverId;

    @Column(name = "SERVER_NAME", nullable = false, length = 100)
    private String serverName;

    @Column(name = "HOST", nullable = false, length = 200)
    private String host;

    @Column(name = "PORT", nullable = false)
    @Builder.Default
    private Integer port = 22;

    @Column(name = "DEFAULT_USERNAME", length = 100)
    private String defaultUsername;

    @Column(name = "PASSWORD", length = 100)
    private String password;

    /** SSH private key file path — used when authType = SSH_KEY */
    @Column(name = "PRIVATE_KEY_PATH", length = 500)
    private String privateKeyPath;

    /** Optional passphrase for encrypted SSH private key */
    @Column(name = "PASSPHRASE", length = 200)
    private String passphrase;

    @Column(name = "PROTOCOL", length = 100)
    private String protocol;

    @Column(name = "AUTH_TYPE", nullable = false, length = 20)
    @Builder.Default
    private String authType = "PASSWORD";

    // HTML Section 4 — SFTP panel fields (were missing)
    @Column(name = "REMOTE_PATH", length = 500)
    private String remotePath;    // Remote Directory Path — e.g. /data/recon/inward/

    @Column(name = "FILE_PATTERN", length = 200)
    private String filePattern;   // File Pattern — e.g. *.csv or TXN_*.txt

    @Column(name = "ARCHIVE_PATH", length = 500)
    private String archivePath;   // Archive Path — e.g. /data/recon/archive/

    @Column(name = "IS_ACTIVE", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    @Builder.Default
    private String createdBy = "SYSTEM";

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}