package com.jpb.reconciliation.reconciliation.service.v2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.v2.SftpConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.SftpTestConnectionRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpDownloadRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconSftpServerMast;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconSftpServerMastRepository;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReconSftpServerServiceImpl implements ReconSftpServerService {

    private static final Logger logger = LoggerFactory.getLogger(ReconSftpServerServiceImpl.class);

    private final ReconSftpServerMastRepository sftpServerRepo;
    
    private static final int CONNECT_TIMEOUT_MS = 10000;

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> createServer(SftpServerRequestDTO request, String createdBy) {

        if (sftpServerRepo.existsByServerName(request.getServerName())) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server name already exists: " + request.getServerName()),
                    HttpStatus.BAD_REQUEST);
        }

        // ── Auth type validation ──────────────────────────────────────────────
        String authType = null != request.getAuthType()
                ? request.getAuthType().toUpperCase() : SftpConstants.AUTH_PASSWORD;
        if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
            if (null == request.getPrivateKeyPath() || request.getPrivateKeyPath().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("privateKeyPath is required when authType is SSH_KEY. Do not send password for SSH_KEY auth."),
                        HttpStatus.BAD_REQUEST);
            }
            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("password must not be sent when authType is SSH_KEY. Use privateKeyPath instead."),
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            // PASSWORD (default)
            if (null == request.getPassword() || request.getPassword().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("password is required when authType is PASSWORD. Do not send privateKeyPath for PASSWORD auth."),
                        HttpStatus.BAD_REQUEST);
            }
            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("privateKeyPath must not be sent when authType is PASSWORD. Use password instead."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        ReconSftpServerMast saved;
        try {
            ReconSftpServerMast entity = ReconSftpServerMast.builder()
                    .serverName(request.getServerName())
                    .host(request.getHost())
                    .port(null != request.getPort() ? request.getPort() : 22)
                    .defaultUsername(request.getDefaultUsername())
                    .password(request.getPassword())
                    .privateKeyPath(request.getPrivateKeyPath())
                    .passphrase(request.getPassphrase())
                    .protocol(request.getProtocol())
                    .authType(authType)
                    .remotePath(request.getRemotePath())
                    .filePattern(request.getFilePattern())
                    .archivePath(request.getArchivePath())
                    .isActive(SftpConstants.ACTIVE)
                    .createdBy(null != createdBy ? createdBy : SftpConstants.DEFAULT_ACTOR)
                    .updatedBy(null != createdBy ? createdBy : SftpConstants.DEFAULT_ACTOR)
                    .build();

            saved = sftpServerRepo.save(entity);
            logger.info("SFTP Server created. ID: {}, Name: {}",
                    saved.getServerId(), saved.getServerName());

        } catch (IllegalArgumentException e) {
            logger.error("Validation error while creating SFTP server: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.failure(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error creating SFTP server: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to create SFTP server: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = buildServerRow(saved);
        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server created successfully.", "sftpServer", Collections.singletonList(row)));
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> updateServer(Long serverId, SftpServerRequestDTO request, String updatedBy) {

        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, SftpConstants.ACTIVE)
                .orElse(null);

        if (null == entity) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
                    HttpStatus.BAD_REQUEST);
        }

        if (sftpServerRepo.existsByServerNameAndServerIdNot(request.getServerName(), serverId)) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Server name already in use: " + request.getServerName()),
                    HttpStatus.BAD_REQUEST);
        }

        // ── Auth type switch validation ───────────────────────────────────────
        String authType = null != request.getAuthType()
                ? request.getAuthType().toUpperCase() : entity.getAuthType();
        if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
            // privateKeyPath must exist (new in request OR already in DB)
            boolean hasKeyPath = (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty())
                    || (null != entity.getPrivateKeyPath() && !entity.getPrivateKeyPath().trim().isEmpty());
            if (!hasKeyPath) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("privateKeyPath is required when authType is SSH_KEY."),
                        HttpStatus.BAD_REQUEST);
            }
            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("password must not be sent when authType is SSH_KEY. Use privateKeyPath instead."),
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            // PASSWORD — must have password (new in request OR already in DB)
            boolean hasPassword = (null != request.getPassword() && !request.getPassword().trim().isEmpty())
                    || (null != entity.getPassword() && !entity.getPassword().trim().isEmpty());
            if (!hasPassword) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("password is required when authType is PASSWORD."),
                        HttpStatus.BAD_REQUEST);
            }
            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("privateKeyPath must not be sent when authType is PASSWORD. Use password instead."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        ReconSftpServerMast saved;
        try {
            entity.setServerName(request.getServerName());
            entity.setHost(request.getHost());
            entity.setPort(null != request.getPort() ? request.getPort() : 22);
            entity.setDefaultUsername(request.getDefaultUsername());
            entity.setAuthType(authType);
            entity.setProtocol(request.getProtocol());
            // Preserve existing credential if new one not sent
            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
                entity.setPassword(request.getPassword());
            }
            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
                entity.setPrivateKeyPath(request.getPrivateKeyPath());
            }
            if (null != request.getPassphrase() && !request.getPassphrase().trim().isEmpty()) {
                entity.setPassphrase(request.getPassphrase());
            }
            entity.setUpdatedBy(updatedBy);

            saved = sftpServerRepo.save(entity);
            logger.info("SFTP Server updated. ID: {}, Name: {}", saved.getServerId(), saved.getServerName());

        } catch (Exception e) {
            logger.error("Error updating SFTP server ID {}: {}", serverId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to update SFTP server: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = buildServerRow(saved);
        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server updated successfully.", "sftpServer", Collections.singletonList(row)));
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithMapStatusList> getServerById(Long serverId) {

        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, SftpConstants.ACTIVE)
                .orElse(null);

        if (null == entity) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
                    HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> row = buildServerRow(entity);
        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server fetched successfully.", "sftpServer", Collections.singletonList(row)));
    }

    // ─── GET ALL ──────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithStatusList> getAllServers(boolean activeOnly) {

        List<ReconSftpServerMast> list = activeOnly
                ? sftpServerRepo.findByIsActive(SftpConstants.ACTIVE)
                : sftpServerRepo.findAll();

        logger.info("Fetched {} SFTP server(s). activeOnly={}", list.size(), activeOnly);

        // Map to safe rows — password intentionally excluded (same as buildServerRow)
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ReconSftpServerMast s : list) {
            rows.add(buildServerRow(s));
        }

        return ResponseEntity.ok(
                ResponseBuilder.okList("SFTP Servers fetched successfully.", rows));
    }

    // ─── SOFT DELETE ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> deleteServer(Long serverId, String updatedBy) {

        ReconSftpServerMast entity = sftpServerRepo.findByServerIdAndIsActive(serverId, SftpConstants.ACTIVE)
                .orElse(null);

        if (null == entity) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("SFTP Server not found for ID: " + serverId),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            entity.setIsActive(SftpConstants.INACTIVE);
            entity.setUpdatedBy(updatedBy);
            sftpServerRepo.save(entity);
            logger.info("SFTP Server soft-deleted. ID: {}", serverId);

        } catch (Exception e) {
            logger.error("Error deleting SFTP server ID {}: {}", serverId, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to delete SFTP server: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("serverId",   entity.getServerId());
        row.put("serverName", entity.getServerName());
        row.put("isActive",   entity.getIsActive());

        return ResponseEntity.ok(
                ResponseBuilder.ok("SFTP Server deleted successfully.", "sftpServer", Collections.singletonList(row)));
    }

    // ─── TEST CONNECTION ──────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithStatusList> testConnection(SftpTestConnectionRequestDTO request) {

        Session session = null;
        ChannelSftp channel = null;

        // ── Validate auth fields before attempting connection ─────────────────
        String authType = null != request.getAuthType()
                ? request.getAuthType().toUpperCase() : SftpConstants.AUTH_PASSWORD;
        if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
            if (null == request.getPrivateKeyPath() || request.getPrivateKeyPath().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.errorList("privateKeyPath is required when authType is SSH_KEY."),
                        HttpStatus.BAD_REQUEST);
            }
            if (null != request.getPassword() && !request.getPassword().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.errorList("password must not be sent when authType is SSH_KEY. Use privateKeyPath instead."),
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            if (null == request.getPassword() || request.getPassword().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.errorList("password is required when authType is PASSWORD."),
                        HttpStatus.BAD_REQUEST);
            }
            if (null != request.getPrivateKeyPath() && !request.getPrivateKeyPath().trim().isEmpty()) {
                return new ResponseEntity<>(
                        ResponseBuilder.errorList("privateKeyPath must not be sent when authType is PASSWORD. Use password instead."),
                        HttpStatus.BAD_REQUEST);
            }
        }

        try {
            JSch jsch = new JSch();

            if (SftpConstants.AUTH_SSH_KEY.equals(authType)) {
                String passphrase = request.getPassphrase();
                if (null != passphrase && !passphrase.trim().isEmpty()) {
                    jsch.addIdentity(request.getPrivateKeyPath(), passphrase);
                } else {
                    jsch.addIdentity(request.getPrivateKeyPath());
                }
            }

            session = jsch.getSession(request.getUsername(), request.getHost(), request.getPort());

            if (!SftpConstants.AUTH_SSH_KEY.equals(authType)) {
                session.setPassword(request.getPassword());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(10000);
            session.connect();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            if (null != request.getRemotePath() && !request.getRemotePath().trim().isEmpty()) {
                channel.ls(request.getRemotePath());
            }

            logger.info("SFTP Test Connection SUCCESS -> {}:{}", request.getHost(), request.getPort());

            return ResponseEntity.ok(
                    ResponseBuilder.okEmpty("Connection successful! Remote path is accessible."));

        } catch (Exception e) {
            logger.warn("SFTP Test Connection FAILED -> {}:{} | {}", request.getHost(), request.getPort(), e.getMessage());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("host",       request.getHost());
            row.put("port",       request.getPort());
            row.put("remotePath", request.getRemotePath());
            row.put("status",     "FAILED");
            row.put("reason",     e.getMessage());

            return new ResponseEntity<>(
                    ResponseBuilder.errorList("Connection failed: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } finally {
            if (null != channel && channel.isConnected()) channel.disconnect();
            if (null != session && session.isConnected()) session.disconnect();
        }
    }

    // ─── PRIVATE MAPPER ───────────────────────────────────────────────────────

    private Map<String, Object> buildServerRow(ReconSftpServerMast e) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("serverId",        e.getServerId());
        row.put("serverName",      e.getServerName());
        row.put("host",            e.getHost());
        row.put("port",            e.getPort());
        row.put("defaultUsername", e.getDefaultUsername());
        row.put("authType",        e.getAuthType());
        row.put("privateKeyPath",  e.getPrivateKeyPath());  // shown only when SSH_KEY
        row.put("protocol",        e.getProtocol());
        row.put("remotePath",      e.getRemotePath());
        row.put("filePattern",     e.getFilePattern());
        row.put("archivePath",     e.getArchivePath());
        row.put("isActive",        e.getIsActive());
        row.put("createdBy",       e.getCreatedBy());
        row.put("createdAt",       e.getCreatedAt());
        row.put("updatedBy",       e.getUpdatedBy());
        row.put("updatedAt",       e.getUpdatedAt());
        // password + passphrase intentionally excluded from response
        return row;
    }
    
    @Override
    public ResponseEntity<RestWithMapStatusList> downloadFiles(SftpDownloadRequestDTO request, String actor) {
 
        // 1) Resolve effective connection details (serverId lookup + overrides)
        EffectiveSftpConfig config;
        try {
            config = resolveConfig(request);
        } catch (IllegalArgumentException e) {
            logger.warn("File pickup validation failed: {}", e.getMessage());
            return new ResponseEntity<>(ResponseBuilder.failure(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
 
        // 2) Ensure local directory exists before we even attempt connection
        try {
            ensureLocalDirectory(config.localPath);
        } catch (Exception e) {
            logger.error("Unable to create local directory {}: {}", config.localPath, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Unable to create local directory: " + config.localPath + " -> " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
 
        Session session = null;
        ChannelSftp channel = null;
 
        List<String> downloaded = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
 
        try {
            // 3) Connect + authenticate
            session = openSession(config);
            channel = openChannel(session);
 
            // 4) Navigate to remote path
            try {
                channel.cd(config.remotePath);
            } catch (Exception e) {
                logger.error("Remote directory not found or inaccessible: {} -> {}", config.remotePath, e.getMessage());
                return new ResponseEntity<>(
                        ResponseBuilder.error("Remote directory not found or not accessible: " + config.remotePath),
                        HttpStatus.BAD_REQUEST);
            }
 
            // 5) List + filter files
            Vector<LsEntry> entries = channel.ls(".");
            Pattern filterPattern = wildcardToPattern(config.filePattern);
 
            for (LsEntry entry : entries) {
 
                String fileName = entry.getFilename();
 
                if (isDirectoryEntry(entry)) {
                    continue; // ignore "." ".." and sub-directories
                }
 
                if (!filterPattern.matcher(fileName).matches()) {
                    skipped.add(fileName);
                    continue;
                }
 
                String remoteFile = config.remotePath + "/" + fileName;
                String localFile = config.localPath + File.separator + fileName;
 
                try {
                    // 6) Download
                    channel.get(remoteFile, localFile);
                    downloaded.add(fileName);
                    logger.info("Downloaded file: {} -> {}", remoteFile, localFile);
 
                    // 7) Archive only after successful download
                    try {
                        moveToArchive(channel, config.archivePath, config.remotePath, fileName);
                        logger.info("Archived file: {} -> {}/{}", fileName, config.archivePath, fileName);
                    } catch (Exception archiveEx) {
                        // Download succeeded but archive failed — do NOT mark as failed download,
                        // but do surface it so ops can manually move/re-run archive.
                        logger.error("Archive failed for {} -> {}: {}", fileName, config.archivePath, archiveEx.getMessage(), archiveEx);
                        failed.add(fileName + " (downloaded but archive failed: " + archiveEx.getMessage() + ")");
                    }
 
                } catch (Exception downloadEx) {
                    logger.error("Download failed for {}: {}", fileName, downloadEx.getMessage(), downloadEx);
                    failed.add(fileName + " (download failed: " + downloadEx.getMessage() + ")");
                    // Do NOT archive if download failed
                }
            }
 
            logger.info("File pickup complete. downloaded={}, skipped={}, failed={}, actor={}",
                    downloaded.size(), skipped.size(), failed.size(), actor);
 
            return ResponseEntity.ok(buildSuccessResponse(downloaded, skipped, failed));
 
        } catch (AuthFailedException e) {
            logger.warn("SFTP authentication failed for host {}: {}", config.host, e.getMessage());
            return new ResponseEntity<>(
                    ResponseBuilder.error("Authentication failed: " + e.getMessage()),
                    HttpStatus.UNAUTHORIZED);
 
        } catch (ConnectionTimeoutException e) {
            logger.warn("SFTP connection timed out for host {}: {}", config.host, e.getMessage());
            return new ResponseEntity<>(
                    ResponseBuilder.error("Connection timed out: " + e.getMessage()),
                    HttpStatus.REQUEST_TIMEOUT);
 
        } catch (Exception e) {
            logger.error("Unexpected error during file pickup for host {}: {}", config.host, e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("File pickup failed: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
 
        } finally {
            // 8) Always disconnect
            if (null != channel && channel.isConnected()) {
                channel.disconnect();
            }
            if (null != session && session.isConnected()) {
                session.disconnect();
            }
        }
    }
 
    // ─── CONFIG RESOLUTION ──────────────────────────────────────────────────
 
    private EffectiveSftpConfig resolveConfig(SftpDownloadRequestDTO request) {
 
        EffectiveSftpConfig config = new EffectiveSftpConfig();
 
        if (null != request.getServerId()) {
            ReconSftpServerMast server = sftpServerRepo
                    .findByServerIdAndIsActive(request.getServerId(), SftpConstants.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "SFTP Server not found for ID: " + request.getServerId()));
 
            config.host = server.getHost();
            config.port = server.getPort();
            config.username = server.getDefaultUsername();
            config.password = server.getPassword();
            config.privateKeyPath = server.getPrivateKeyPath();
            config.passphrase = server.getPassphrase();
            config.authType = server.getAuthType();
            config.remotePath = server.getRemotePath();
            config.archivePath = server.getArchivePath();
            config.filePattern = server.getFilePattern();
        }
 
        // Individually supplied fields override server-loaded ones
        if (notBlank(request.getHost())) config.host = request.getHost();
        if (null != request.getPort()) config.port = request.getPort();
        if (notBlank(request.getUsername())) config.username = request.getUsername();
        if (notBlank(request.getPassword())) config.password = request.getPassword();
        if (notBlank(request.getPrivateKeyPath())) config.privateKeyPath = request.getPrivateKeyPath();
        if (notBlank(request.getPassphrase())) config.passphrase = request.getPassphrase();
        if (notBlank(request.getAuthType())) config.authType = request.getAuthType();
        if (notBlank(request.getRemotePath())) config.remotePath = request.getRemotePath();
        if (notBlank(request.getArchivePath())) config.archivePath = request.getArchivePath();
        if (notBlank(request.getFilePattern())) config.filePattern = request.getFilePattern();
 
        // localPath always comes from the request (Template Header value), never from server master
        config.localPath = request.getLocalPath();
 
        config.authType = notBlank(config.authType) ? config.authType.toUpperCase() : SftpConstants.AUTH_PASSWORD;
        if (null == config.port) {
            config.port = 22;
        }
        if (!notBlank(config.filePattern)) {
            config.filePattern = "*"; // default: match everything
        }
 
        validateConfig(config);
        return config;
    }
 
    private void validateConfig(EffectiveSftpConfig config) {
        if (!notBlank(config.host)) {
            throw new IllegalArgumentException("host is required.");
        }
        if (!notBlank(config.username)) {
            throw new IllegalArgumentException("username is required.");
        }
        if (!notBlank(config.remotePath)) {
            throw new IllegalArgumentException("remotePath is required.");
        }
        if (!notBlank(config.localPath)) {
            throw new IllegalArgumentException("localPath is required.");
        }
        if (!notBlank(config.archivePath)) {
            throw new IllegalArgumentException("archivePath is required.");
        }
 
        if (SftpConstants.AUTH_SSH_KEY.equals(config.authType)) {
            if (!notBlank(config.privateKeyPath)) {
                throw new IllegalArgumentException("privateKeyPath is required when authType is SSH_KEY.");
            }
        } else {
            if (!notBlank(config.password)) {
                throw new IllegalArgumentException("password is required when authType is PASSWORD.");
            }
        }
    }
 
    // ─── CONNECTION HELPERS ─────────────────────────────────────────────────
 
    private Session openSession(EffectiveSftpConfig config) throws Exception {
        try {
            JSch jsch = new JSch();
 
            if (SftpConstants.AUTH_SSH_KEY.equals(config.authType)) {
                if (notBlank(config.passphrase)) {
                    jsch.addIdentity(config.privateKeyPath, config.passphrase);
                } else {
                    jsch.addIdentity(config.privateKeyPath);
                }
            }
 
            Session session = jsch.getSession(config.username, config.host, config.port);
 
            if (!SftpConstants.AUTH_SSH_KEY.equals(config.authType)) {
                session.setPassword(config.password);
            }
 
            Properties props = new Properties();
            props.put("StrictHostKeyChecking", "no");
            session.setConfig(props);
            session.setTimeout(CONNECT_TIMEOUT_MS);
            session.connect();
 
            return session;
 
        } catch (com.jcraft.jsch.JSchException e) {
            String msg = null != e.getMessage() ? e.getMessage().toLowerCase() : "";
            if (msg.contains("auth fail") || msg.contains("authentication")) {
                throw new AuthFailedException(e.getMessage());
            }
            if (msg.contains("timeout") || msg.contains("timed out")) {
                throw new ConnectionTimeoutException(e.getMessage());
            }
            throw e;
        }
    }
 
    private ChannelSftp openChannel(Session session) throws Exception {
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        return channel;
    }
 
    // ─── FILE HELPERS ───────────────────────────────────────────────────────
 
    private boolean isDirectoryEntry(LsEntry entry) {
        return entry.getAttrs().isDir()
                || ".".equals(entry.getFilename())
                || "..".equals(entry.getFilename());
    }
 
    /** Converts a simple wildcard pattern (*.csv, TXN_*.txt) into a regex Pattern. */
    private Pattern wildcardToPattern(String wildcard) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : wildcard.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append(".");
            } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                regex.append("\\").append(c);
            } else {
                regex.append(c);
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }
 
    private void ensureLocalDirectory(String localPath) throws Exception {
        Path path = Paths.get(localPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Created local directory: {}", localPath);
        }
    }
 
    private void moveToArchive(ChannelSftp channel, String archivePath, String remotePath, String fileName) throws Exception {
        // Ensure remote archive directory exists; create it if missing.
        try {
            channel.cd(archivePath);
            channel.cd(remotePath); // move back to remotePath since cd changes working dir
        } catch (Exception e) {
            try {
                channel.mkdir(archivePath);
            } catch (Exception mkdirEx) {
                throw new IllegalStateException("Archive directory does not exist and could not be created: " + archivePath, mkdirEx);
            }
        }
 
        String source = remotePath + "/" + fileName;
        String destination = archivePath + "/" + fileName;
        channel.rename(source, destination);
    }
 
    private boolean notBlank(String value) {
        return null != value && !value.trim().isEmpty();
    }
 
    // ─── RESPONSE BUILDING ──────────────────────────────────────────────────
 
    private RestWithMapStatusList buildSuccessResponse(List<String> downloaded, List<String> skipped, List<String> failed) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("downloadedFiles", downloaded);
        row.put("downloadedCount", downloaded.size());
        row.put("skippedFiles", skipped);
        row.put("skippedCount", skipped.size());
        row.put("failedFiles", failed);
        row.put("failedCount", failed.size());
        row.put("status", failed.isEmpty() ? CommonConstants.SUCCESS : "PARTIAL_SUCCESS");
 
        String message = failed.isEmpty()
                ? "File pickup completed successfully."
                : "File pickup completed with some failures.";
 
        return ResponseBuilder.ok(message, "filePickup", Collections.singletonList(row));
    }
 
    // ─── INTERNAL VALUE HOLDER ──────────────────────────────────────────────
 
    /** Holds the fully resolved connection + path configuration for a single pickup run. */
    private static class EffectiveSftpConfig {
        String host;
        Integer port;
        String username;
        String password;
        String privateKeyPath;
        String passphrase;
        String authType;
        String remotePath;
        String localPath;
        String archivePath;
        String filePattern;
    }
 
    // ─── INTERNAL EXCEPTIONS (for clean HTTP status mapping) ────────────────
 
    private static class AuthFailedException extends Exception {
        AuthFailedException(String message) { super(message); }
    }
 
    private static class ConnectionTimeoutException extends Exception {
        ConnectionTimeoutException(String message) { super(message); }
    }
}