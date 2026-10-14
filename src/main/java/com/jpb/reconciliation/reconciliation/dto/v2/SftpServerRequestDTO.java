package com.jpb.reconciliation.reconciliation.dto.v2;

import lombok.Data;

@Data
public class SftpServerRequestDTO {
    private Long   serverId;
    private String serverName;
    private String host;
    private Integer port = 22;
    private String defaultUsername;

    /** Required when authType = PASSWORD */
    private String password;

    /** Required when authType = SSH_KEY — absolute path to private key file on server */
    private String privateKeyPath;

    /** Optional — passphrase for encrypted SSH private key (authType = SSH_KEY) */
    private String passphrase;

    private String protocol;

    /**
     * PASSWORD  → authenticate with username + password
     * SSH_KEY   → authenticate with username + private key file
     */
    private String authType = "PASSWORD";

    // HTML Section 4 — SFTP panel fields
    private String remotePath;    // Remote Directory Path — e.g. /data/recon/inward/
    private String filePattern;   // File Pattern         — e.g. *.csv or TXN_*.txt
    private String archivePath;   // Archive Path         — e.g. /data/recon/archive/
}
