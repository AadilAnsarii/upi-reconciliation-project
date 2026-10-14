package com.jpb.reconciliation.reconciliation.dto.v2;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SftpServerResponseDTO {
    private Long    serverId;
    private String  serverName;
    private String  host;
    private Integer port;
    private String  defaultUsername;
    private String  protocol;
    private String  authType;
    private String  privateKeyPath; // SSH key file path — shown when authType=SSH_KEY
    // Note: password and passphrase intentionally excluded from response
    private String  remotePath;    // Remote Directory Path
    private String  filePattern;   // File Pattern e.g. *.csv
    private String  archivePath;   // Archive Path after pickup
    private String  isActive;
    private String  createdBy;
    private String  updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
