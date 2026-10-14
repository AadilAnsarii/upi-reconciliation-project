package com.jpb.reconciliation.reconciliation.mapper.v2;

import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpServerResponseDTO;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconSftpServerMast;

@Component
public class ReconSftpServerMapper {

    /**
     * Convert RequestDTO → Entity (for CREATE)
     */
    public ReconSftpServerMast toEntity(SftpServerRequestDTO dto) {
        if (null == dto) return null;
        return ReconSftpServerMast.builder()
                .serverName(dto.getServerName())
                .host(dto.getHost())
                .port(null != dto.getPort() ? dto.getPort() : 22)
                .defaultUsername(dto.getDefaultUsername())
                .password(dto.getPassword())
                .privateKeyPath(dto.getPrivateKeyPath())
                .passphrase(dto.getPassphrase())
                .protocol(dto.getProtocol())
                .authType(null != dto.getAuthType() ? dto.getAuthType().toUpperCase() : "PASSWORD")
                .remotePath(dto.getRemotePath())
                .filePattern(dto.getFilePattern())
                .archivePath(dto.getArchivePath())
                .isActive(CommonConstants.ACTIVE_Y)
                // createdBy and createdAt set in service
                .build();
    }

    /**
     * Convert Entity → ResponseDTO
     * password, passphrase intentionally excluded from response
     */
    public SftpServerResponseDTO toResponseDTO(ReconSftpServerMast entity) {
        if (null == entity) return null;
        return SftpServerResponseDTO.builder()
                .serverId(entity.getServerId())
                .serverName(entity.getServerName())
                .host(entity.getHost())
                .port(entity.getPort())
                .defaultUsername(entity.getDefaultUsername())
                .protocol(entity.getProtocol())
                .authType(entity.getAuthType())
                .privateKeyPath(entity.getPrivateKeyPath())
                // passphrase intentionally excluded from response
                .remotePath(entity.getRemotePath())
                .filePattern(entity.getFilePattern())
                .archivePath(entity.getArchivePath())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                // password intentionally excluded from response
                .build();
    }

    /**
     * Update existing Entity from RequestDTO (for UPDATE / PUT)
     * Skips: serverId, isActive, createdBy, createdAt — must not change on update
     * Skips password/privateKeyPath if blank — preserves existing credential
     */
    public void updateEntityFromRequest(SftpServerRequestDTO dto, ReconSftpServerMast entity) {
        if (null == dto || null == entity) return;
        entity.setServerName(dto.getServerName());
        entity.setHost(dto.getHost());
        entity.setPort(null != dto.getPort() ? dto.getPort() : 22);
        entity.setDefaultUsername(dto.getDefaultUsername());
        if (null != dto.getAuthType()) entity.setAuthType(dto.getAuthType().toUpperCase());
        entity.setProtocol(dto.getProtocol());
        if (null != dto.getRemotePath()) entity.setRemotePath(dto.getRemotePath());
        if (null != dto.getFilePattern()) entity.setFilePattern(dto.getFilePattern());
        if (null != dto.getArchivePath()) entity.setArchivePath(dto.getArchivePath());
        // Preserve existing credential if new one not sent
        if (null != dto.getPassword() && !dto.getPassword().trim().isEmpty()) {
            entity.setPassword(dto.getPassword());
        }
        if (null != dto.getPrivateKeyPath() && !dto.getPrivateKeyPath().trim().isEmpty()) {
            entity.setPrivateKeyPath(dto.getPrivateKeyPath());
        }
        if (null != dto.getPassphrase() && !dto.getPassphrase().trim().isEmpty()) {
            entity.setPassphrase(dto.getPassphrase());
        }
        // updatedBy set in service via request header
        // updatedAt handled by @UpdateTimestamp
    }
}
