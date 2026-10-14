package com.jpb.reconciliation.reconciliation.service;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

@Service
public class CustomUserDetailService implements UserDetailsService {

    Logger logger = LoggerFactory.getLogger(CustomUserDetailService.class);

    @Autowired
    private ReconUserRepository reconUserRepository;

    /**
     * Called by JwtAuthenticationFilter to validate every request's Bearer token.
     *
     * Strategy:
     * 1. Try RECON_USER by username
     * 2. Try RECON_USER by email (some tokens use email as subject)
     *
     * If none found → UsernameNotFoundException → 401
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // ── Step 1: ReconUser — by username ──
        Optional<ReconUser> byUsername = reconUserRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            ReconUser user = byUsername.get();
            logger.debug("loadUserByUsername — ReconUser found by username: {}", username);
            return buildUserDetails(healLegacyUserType(user));
        }

        // ── Step 2: ReconUser — by email (some tokens store email as subject) ──
        try {
            Optional<ReconUser> byEmail = reconUserRepository.findByEmail(username);
            if (byEmail.isPresent()) {
                ReconUser user = byEmail.get();
                logger.debug("loadUserByUsername — ReconUser found by email: {}", username);
                return buildUserDetails(healLegacyUserType(user));
            }
        } catch (Exception ignored) {
            // safe to ignore if email lookup fails
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }

    // "Super User" was renamed to "Bank Admin" everywhere in the app, but accounts created
    // before that rename still carry the literal old USER_TYPE value in the DB — every check
    // in the codebase now only recognizes "BANK_ADMIN", so those accounts silently stopped
    // being treated as Admin (e.g. their own user-creations wrongly went to Checker Queue).
    // Self-heals here, on every authenticated request, instead of a one-off manual UPDATE.
    private ReconUser healLegacyUserType(ReconUser user) {
        if ("SUPER_USER".equalsIgnoreCase(user.getUserType())) {
            user.setUserType("BANK_ADMIN");
            reconUserRepository.save(user);
            logger.info("Auto-migrated legacy USER_TYPE 'SUPER_USER' -> 'BANK_ADMIN' for username: {}", user.getUsername());
        }
        return user;
    }

    public UserDetails loadUserByUserEmail(String email) throws UsernameNotFoundException {
        ReconUser user = reconUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with given email: " + email));
        return buildUserDetails(user);
    }

    // ── Build a UserDetails from ReconUser ──
    private UserDetails buildUserDetails(ReconUser user) {
        String role = resolveRole(user.getUserType());
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "";
        return User.builder()
                .username(user.getUsername())
                .password(password)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }

    // ── Map userType to Spring Security role ──
    private String resolveRole(String userType) {
        if (userType == null) {
            return "ROLE_USER";
        }
        switch (userType.toUpperCase()) {
            case "KAL_ADMIN":
                return "ROLE_KAL_ADMIN";
            case "TEST_INSTITUTION":
            case "MAIN_BANK":
            case "BANK_ADMIN":
                return "ROLE_BANK_ADMIN";
            case "BRANCH":
            case "BRANCH_ADMIN":
                return "ROLE_BRANCH_ADMIN";
            default:
                return "ROLE_USER";
        }
    }
}
