package com.jpb.reconciliation.reconciliation.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TokenRefreshRequest;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.google.GoogleRequest;
import com.jpb.reconciliation.reconciliation.google.GoogleService;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.security.TokenBlacklistService;
import com.jpb.reconciliation.reconciliation.service.CustomUserDetailService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    GoogleService googleService;

    @Autowired
    CustomUserDetailService customUserDetailService;
    
    @Autowired
    ReconUserRepository reconUserRepository;

    @Autowired
    JwtHelper jwtHelper;

    @Autowired
    TokenBlacklistService tokenBlacklistService;

    private Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping(value = "/refresh-token", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response,
            @RequestBody TokenRefreshRequest tokenRefreshRequest) {
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
            logger.warn("Refresh token is null or empty in the cookie.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found.");
        }
        String userName = null;
        String refreshTokenJti = null;

        try {
            userName = jwtHelper.getUsernameFromToken(refreshToken);
            refreshTokenJti = jwtHelper.getJtiFromToken(refreshToken);

            if (tokenBlacklistService.isTokenBlacklisted(refreshTokenJti)) {
                clearRefreshCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or blacklisted refresh token. Please log in again.");
            }

            UserDetails userDetails = customUserDetailService.loadUserByUsername(userName);

            if (!jwtHelper.validateToken(refreshToken, userName, false)) {
                clearRefreshCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired refresh token. Please log in again.");
            }

            String newAccessToken = jwtHelper.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));
        } catch (UsernameNotFoundException e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired. Please log in again.");
        } catch (SignatureException | MalformedJwtException e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token. Please log in again.");
        } catch (IllegalArgumentException e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token format.");
        } catch (Exception e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
        }
    }

    @PostMapping(value = "/revoke-token", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<?> logout(@RequestBody TokenRefreshRequest request, HttpServletResponse response) {
        String refreshToken = request.getRefreshToken();
        String userNameFromToken = jwtHelper.getUsernameFromToken(refreshToken);
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token is missing.");
        }
        Optional<ReconUser> user = reconUserRepository.findByUsername(userNameFromToken);
        if (user.isPresent()) {
                ReconUser getUser = user.get();
                getUser.setLastLogin(LocalDateTime.now());
                reconUserRepository.save(getUser);
        } else {
                logger.info("User is not found");
        }
        String jti = null;
        try {
            jti = jwtHelper.getJtiFromToken(refreshToken);
            tokenBlacklistService.blacklistToken(jti);
            clearRefreshCookie(response);
        } catch (ExpiredJwtException e) {
            clearRefreshCookie(response);
            return ResponseEntity.ok("Token already expired, cleared from client.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error revoking token.");
        }
        return ResponseEntity.ok("Token revoked and cookie cleared");
    }

    @PostMapping(value = "/google", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> loginWithGoogle(@RequestBody GoogleRequest googleRequest,
            HttpServletResponse response) {
        return googleService.authenticateWithGoogle(googleRequest, response);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
