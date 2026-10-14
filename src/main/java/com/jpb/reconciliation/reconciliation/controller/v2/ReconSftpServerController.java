package com.jpb.reconciliation.reconciliation.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.SftpTestConnectionRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpDownloadRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.service.v2.ReconSftpServerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/recon/sftp-servers")
@RequiredArgsConstructor
@Tag(name = "SFTP Server v2", description = "APIs for managing SFTP server configurations used in template file delivery")
public class ReconSftpServerController {

    private final ReconSftpServerService sftpServerService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v2/recon/sftp-servers
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Register a new SFTP server configuration")
    public ResponseEntity<RestWithMapStatusList> createSftpServer(
            @RequestBody SftpServerRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return sftpServerService.createServer(request, userDetails.getUsername());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v2/recon/sftp-servers/{serverId}
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{serverId}")
    @Operation(summary = "Update an existing SFTP server configuration by serverId")
    public ResponseEntity<RestWithMapStatusList> updateSftpServer(
            @PathVariable Long serverId,
            @RequestBody SftpServerRequestDTO request,
            @RequestHeader(value = "X-User-Id", defaultValue = CommonConstants.SYSTEM) String updatedBy) {
        return sftpServerService.updateServer(serverId, request, updatedBy);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v2/recon/sftp-servers/{serverId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{serverId}")
    @Operation(summary = "Get a single SFTP server configuration by serverId")
    public ResponseEntity<RestWithMapStatusList> getSftpServerById(
            @PathVariable Long serverId) {
        return sftpServerService.getServerById(serverId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v2/recon/sftp-servers?activeOnly=true
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "List all SFTP server configurations. activeOnly=true returns only active servers")
    public ResponseEntity<RestWithStatusList> getAllSftpServers(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return sftpServerService.getAllServers(activeOnly);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v2/recon/sftp-servers/{serverId}
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{serverId}")
    @Operation(summary = "Soft-delete an SFTP server configuration by serverId (sets isActive = N)")
    public ResponseEntity<RestWithMapStatusList> deleteSftpServer(
            @PathVariable Long serverId,
            @RequestHeader(value = "X-User-Id", defaultValue = CommonConstants.SYSTEM) String deletedBy) {
        return sftpServerService.deleteServer(serverId, deletedBy);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v2/recon/sftp-servers/test-connection
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/test-connection")
    @Operation(summary = "Test connectivity to an SFTP server using the provided credentials")
    public ResponseEntity<RestWithStatusList> testSftpConnection(
            @RequestBody SftpTestConnectionRequestDTO request) {
        return sftpServerService.testConnection(request);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v2/recon/sftp-servers/download-files
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/download-files")
    @Operation(summary = "Connect to an SFTP server, download files matching filePattern from remotePath into localPath, "
            + "then move successfully downloaded files to archivePath")
    public ResponseEntity<RestWithMapStatusList> downloadFiles(
            @RequestBody SftpDownloadRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
 
        String actor = null != userDetails ? userDetails.getUsername() : CommonConstants.SYSTEM;
        return sftpServerService.downloadFiles(request, actor);
    }
    
}
