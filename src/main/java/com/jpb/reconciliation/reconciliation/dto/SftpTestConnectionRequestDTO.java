package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class SftpTestConnectionRequestDTO {
    private String host;
    private Integer port;
    private String username;

    /** Required when authType = PASSWORD */
    private String password;

    /**
     * PASSWORD (default) or SSH_KEY
     */
    private String authType = "PASSWORD";

    /** Required when authType = SSH_KEY — absolute path to private key file on server */
    private String privateKeyPath;

    /** Optional passphrase for encrypted private key */
    private String passphrase;

    private String remotePath;
}