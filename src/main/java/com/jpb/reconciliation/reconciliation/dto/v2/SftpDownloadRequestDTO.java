package com.jpb.reconciliation.reconciliation.dto.v2;

import lombok.Data;

@Data
public class SftpDownloadRequestDTO {

    /** Optional. If present, connection details are loaded from RECON_SFTP_SERVER_MAST. */
    private Long serverId;

    private String host;

    private Integer port;

    private String username;

    /** Required when authType = PASSWORD */
    private String password;

    /** Required when authType = SSH_KEY */
    private String privateKeyPath;

    /** Optional passphrase for encrypted SSH private key */
    private String passphrase;

    /**
     * PASSWORD (default) or SSH_KEY.
     * Only used when serverId is NOT supplied (i.e. ad-hoc mode),
     * otherwise the authType stored against the server record is used.
     */
    private String authType;

    /** Remote directory where the bank/NPCI places files. e.g. /opt/recon */
    private String remotePath;

    /**
     * Local directory where downloaded files must be saved.
     * Typically comes from Template Header "File Path" field.
     * e.g. D:/Recon/Input  or  /home/recon/inbound
     */
    private String localPath;

    /** Remote directory where successfully downloaded files are moved to. e.g. /opt/recon/archive */
    private String archivePath;

    /** Wildcard file filter. e.g. *.csv, TXN_*.txt, *.xml */
    private String filePattern;
}