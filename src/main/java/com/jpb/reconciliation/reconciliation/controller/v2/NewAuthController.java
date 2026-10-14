package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.LoginRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconAuthToken;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconAuthTokenRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.security.TokenBlacklistService;
import com.jpb.reconciliation.reconciliation.service.CustomUserDetailService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/auth")
@CrossOrigin(origins = "*")
public class NewAuthController {

    private static final Logger logger = LoggerFactory.getLogger(NewAuthController.class);

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ReconAuthTokenRepository reconAuthTokenRepository;

    /**
     * V2 Login — authenticates against RCN_RECON_USER table.
     * Accepts userName or emailId + userPassword.
     */
    @PostMapping(value = "/login", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request, HttpServletResponse response) {
        String identifier = (request.getUserName() != null && !request.getUserName().trim().isEmpty())
                ? request.getUserName().trim().toLowerCase()
                : (request.getEmailId() != null ? request.getEmailId().trim().toLowerCase() : null);

        if (identifier == null || request.getUserPassword() == null || request.getUserPassword().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Username/email and password are required.", null));
        }

        // Load user from RCN_RECON_USER
        UserDetails userDetails;
        try {
            userDetails = customUserDetailService.loadUserByUsername(identifier);
        } catch (UsernameNotFoundException e) {
            logger.warn("V2 login failed — user not found: {}", identifier);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }

        // Verify password against BCrypt hash in RCN_RECON_USER
        if (!passwordEncoder.matches(request.getUserPassword(), userDetails.getPassword())) {
            logger.warn("V2 login failed — wrong password for user: {}", identifier);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid credentials.", null));
        }

        // Check user status
        Optional<ReconUser> userOpt = reconUserRepository.findByUsername(identifier);
        if (!userOpt.isPresent()) {
            userOpt = reconUserRepository.findByEmail(identifier);
        }
        if (userOpt.isPresent()) {
            ReconUser user = userOpt.get();
            if ("BLOCKED".equals(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Account is BLOCKED. Reason: " + (user.getBlockReason() != null ? user.getBlockReason() : "Contact administrator."), null));
            }
            if (CommonConstants.INACTIVE.equals(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Account is INACTIVE. Please contact administrator.", null));
            }
            if ("PENDING_APPROVAL".equals(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Account is pending approval. Please wait for admin approval.", null));
            }
            if (!CommonConstants.ACTIVE_Y.equals(user.getApprovedYn())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "Account is not yet approved.", null));
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            reconUserRepository.save(user);
        }

        // Generate tokens
        String accessToken  = jwtHelper.generateToken(userDetails);
        String refreshToken = jwtHelper.generateTokenForRefresh(userDetails.getUsername());

        // Set refresh token as HttpOnly cookie
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(8 * 60 * 60); // 8 hours
        response.addCookie(refreshCookie);

        // Save tokens to RECON_AUTH_TOKEN (same pattern as BankAuthServiceImpl)
        if (userOpt.isPresent()) {
            Long userId = userOpt.get().getUserId();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            reconAuthTokenRepository.findByUserIdAndStatus(userId, CommonConstants.ACTIVE)
                    .forEach(t -> { t.setStatus("EXPIRED"); reconAuthTokenRepository.save(t); });

            ReconAuthToken accessEntry = new ReconAuthToken();
            accessEntry.setUserId(userId);
            accessEntry.setTokenType("ACCESS");
            accessEntry.setTokenValue(accessToken);
            accessEntry.setStatus(CommonConstants.ACTIVE);
            accessEntry.setExpiresAt(now.plusHours(1));
            accessEntry.setCreatedAt(now);
            reconAuthTokenRepository.save(accessEntry);

            ReconAuthToken refreshEntry = new ReconAuthToken();
            refreshEntry.setUserId(userId);
            refreshEntry.setTokenType("REFRESH");
            refreshEntry.setTokenValue(refreshToken);
            refreshEntry.setStatus(CommonConstants.ACTIVE);
            refreshEntry.setExpiresAt(now.plusHours(8));
            refreshEntry.setCreatedAt(now);
            reconAuthTokenRepository.save(refreshEntry);
        }

        logger.info("V2 login successful for user: {}", identifier);

        // Build enriched response so frontend can skip a /me call
        AuthResponse authResponse;
        if (userOpt.isPresent()) {
            ReconUser u = userOpt.get();
            authResponse = new AuthResponse(
                    accessToken, refreshToken,
                    u.getUserId(), u.getRoleId(), u.getUserType(), u.getFullName(), u.getUsername(), u.getBankId()
            );
        } else {
            authResponse = new AuthResponse(accessToken, refreshToken);
        }
        return ResponseEntity.ok(authResponse);
    }

    /**
     * V2 Refresh Token — same as existing refresh-token but v2-namespaced.
     */
    @PostMapping(value = "/refresh-token", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found.");
        }

        String userName = null;
        try {
            userName = jwtHelper.getUsernameFromToken(refreshToken);
            String jti = jwtHelper.getJtiFromToken(refreshToken);

            if (tokenBlacklistService.isTokenBlacklisted(jti)) {
                clearRefreshCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Blacklisted refresh token.");
            }

            UserDetails userDetails = customUserDetailService.loadUserByUsername(userName);
            if (!jwtHelper.validateToken(refreshToken, userName, false)) {
                clearRefreshCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token.");
            }

            String newAccessToken = jwtHelper.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));

        } catch (ExpiredJwtException e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired.");
        } catch (MalformedJwtException e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token.");
        } catch (Exception e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Token refresh error.");
        }
    }

    /**
     * V2 Logout — blacklists the refresh token and updates ReconUser.lastLogin.
     */
    @PostMapping(value = "/logout", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<?> logout(@RequestBody(required = false) java.util.Map<String, String> body, HttpServletResponse response) {
        String refreshToken = body != null ? body.get("refreshToken") : null;
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            clearRefreshCookie(response);
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Logged out.", null));
        }
        try {
            String jti = jwtHelper.getJtiFromToken(refreshToken);
            tokenBlacklistService.blacklistToken(jti);

            String userName = jwtHelper.getUsernameFromToken(refreshToken);
            Optional<ReconUser> userOpt = reconUserRepository.findByUsername(userName);
            if (!userOpt.isPresent()) userOpt = reconUserRepository.findByEmail(userName);
            userOpt.ifPresent(u -> {
                u.setLastLogin(LocalDateTime.now());
                reconUserRepository.save(u);
            });

            clearRefreshCookie(response);
            logger.info("V2 logout successful for user: {}", userName);
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Logged out successfully.", null));
        } catch (ExpiredJwtException e) {
            clearRefreshCookie(response);
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Token already expired. Logged out.", null));
        } catch (Exception e) {
            logger.error("V2 logout error: {}", e.getMessage());
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Logout error.", null));
        }
    }

    /**
     * V2 — Get current logged-in user info from RCN_RECON_USER.
     */
    @GetMapping(value = "/me", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "No token provided.", null));
        }
        try {
            String token = authHeader.substring(7);
            String username = jwtHelper.getUsernameFromToken(token);
            Optional<ReconUser> userOpt = reconUserRepository.findByUsername(username);
            if (!userOpt.isPresent()) userOpt = reconUserRepository.findByEmail(username);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new RestWithStatusList(CommonConstants.FAILURE, "User not found.", null));
            }
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User fetched.", Collections.singletonList(userOpt.get())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Invalid token.", null));
        }
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
