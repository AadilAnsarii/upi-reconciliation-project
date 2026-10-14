package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReconPasswordManagerServiceImpl implements ReconPasswordManagerService {

    private static final Logger logger = LoggerFactory.getLogger(ReconPasswordManagerServiceImpl.class);

    // Password expiry: 90 days
    private static final int PASSWORD_EXPIRY_DAYS = 90;

    @Autowired
    private ReconPasswordManagerRepository reconPasswordManagerRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> setPassword(Long userId, String newPassword, String updatedBy) {
        Optional<ReconUser> userOpt = reconUserRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found: " + userId, null));
        }
        ReconUser user = userOpt.get();

        String hashed = passwordEncoder.encode(newPassword);

        // Upsert: update existing record or create new
        Optional<ReconPasswordManager> existing = reconPasswordManagerRepository.findByReconUser_UserId(userId);
        ReconPasswordManager pm = existing.isPresent() ? existing.get() : new ReconPasswordManager();

        pm.setReconUser(user);
        pm.setUserPassword(hashed);
        pm.setExpirationDate(LocalDateTime.now().plusDays(PASSWORD_EXPIRY_DAYS));
        pm.setToken(null);

        if (!existing.isPresent()) {
            pm.setCreatedAt(LocalDateTime.now());
            pm.setCreatedBy(updatedBy);
        }
        pm.setUpdatedAt(LocalDateTime.now());
        pm.setUpdatedBy(updatedBy);

        reconPasswordManagerRepository.save(pm);

        // Also update password_hash on ReconUser
        user.setPasswordHash(hashed);
        user.setPasswordSet(1);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        reconUserRepository.save(user);

        logger.info("Password set for userId={} by {}", userId, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Password set successfully.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> changePassword(Long userId, String oldPassword, String newPassword, String updatedBy) {
        Optional<ReconUser> userOpt = reconUserRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found: " + userId, null));
        }
        ReconUser user = userOpt.get();

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Current password is incorrect.", null));
        }

        return setPassword(userId, newPassword, updatedBy);
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> generateResetToken(Long userId, String createdBy) {
        Optional<ReconUser> userOpt = reconUserRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found: " + userId, null));
        }
        ReconUser user = userOpt.get();

        String resetToken = UUID.randomUUID().toString();

        Optional<ReconPasswordManager> existing = reconPasswordManagerRepository.findByReconUser_UserId(userId);
        ReconPasswordManager pm = existing.isPresent() ? existing.get() : new ReconPasswordManager();

        pm.setReconUser(user);
        pm.setToken(resetToken);
        // Token expiry: 1 hour
        pm.setExpirationDate(LocalDateTime.now().plusHours(1));

        if (!existing.isPresent()) {
            pm.setCreatedAt(LocalDateTime.now());
            pm.setCreatedBy(createdBy);
        }
        pm.setUpdatedAt(LocalDateTime.now());
        pm.setUpdatedBy(createdBy);

        reconPasswordManagerRepository.save(pm);

        logger.info("Reset token generated for userId={}", userId);
        return ResponseEntity.ok(
                new RestWithStatusList(CommonConstants.SUCCESS, "Reset token generated.", Collections.singletonList(resetToken)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> resetPasswordByToken(String token, String newPassword) {
        Optional<ReconPasswordManager> pmOpt = reconPasswordManagerRepository.findByToken(token);
        if (!pmOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid or expired reset token.", null));
        }
        ReconPasswordManager pm = pmOpt.get();

        if (pm.getExpirationDate() != null && LocalDateTime.now().isAfter(pm.getExpirationDate())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Reset token has expired.", null));
        }

        Long userId = pm.getReconUser().getUserId();
        return setPassword(userId, newPassword, "SELF");
    }

    @Override
    public ResponseEntity<RestWithStatusList> getByUserId(Long userId) {
        Optional<ReconPasswordManager> pmOpt = reconPasswordManagerRepository.findByReconUser_UserId(userId);
        if (!pmOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "No password record found for userId: " + userId, null));
        }
        return ResponseEntity.ok(
                new RestWithStatusList(CommonConstants.SUCCESS, "Password record found.", Collections.singletonList(pmOpt.get())));
    }
}
