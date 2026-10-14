package com.jpb.reconciliation.reconciliation.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * Skip JWT filter for public endpoints.
     *
     * WHY THIS IS NEEDED:
     * SecurityConfig.permitAll() only skips the authorization check.
     * It does NOT skip custom filters like this one.
     * JwtAuthenticationEntryPoint returns 401 when no valid token is found
     * — even on public routes — unless we explicitly skip here.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
            throws ServletException {

        String path = request.getServletPath();

        return path.startsWith("/api/kalinfotech/")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/admin-login")
                || path.startsWith("/auth/google")
                || path.startsWith("/auth/refresh-token")
                || path.startsWith("/auth/forgot-password")
                || path.startsWith("/auth/reset-password")
                || path.startsWith("/authentication/app")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/h2-console")

                // User APIs
                //
                // NOT /api/v2/user/create: it is an authenticated admin/maker action. Skipping the
                // filter there left SecurityContext empty, so the controller's `Authentication`
                // argument was null and every user was created by "UNKNOWN" — no parentUserId, no
                // maker-checker audit row, and an Admin's own user landed in PENDING_APPROVAL
                // instead of REQUEST. It also let anyone create a user with no token at all.
                || path.startsWith("/api/v2/user/auth/")

                // Bank APIs
                || path.startsWith("/test/api/v1/bank/verify-credentials")
                || path.startsWith("/test/api/v1/bank/set-password")
                || path.startsWith("/test/api/v1/bank/login")
                || path.startsWith("/test/api/v1/bank/verify-email")
                || path.startsWith("/test/api/v1/bank/check-user-status")
                || path.startsWith("/test/api/v1/bank/forgot-password")
                || path.startsWith("/test/api/v1/bank/reset-password")
                || path.startsWith("/test/api/v1/bank/check-email")
                || path.startsWith("/test/api/v1/bank/check-name")

                // Logo images — public (brand logos, no sensitive data)
                || path.startsWith("/test/api/v1/bank/logo/")

                // Bank APIs (additional public endpoints)
                || path.startsWith("/test/api/v1/bank/direct-login")
                || path.startsWith("/test/api/v1/bank/verify-forgot-otp")

                // Branch Bank APIs
                || path.startsWith("/test/api/v1/branch/verify-email")
                || path.startsWith("/test/api/v1/branch/verify-credentials")
                || path.startsWith("/test/api/v1/branch/set-password")
                || path.startsWith("/test/api/v1/branch/login")
                || path.startsWith("/test/api/v1/branch/direct-login")
                || path.startsWith("/test/api/v1/branch/forgot-password")
                || path.startsWith("/test/api/v1/branch/verify-forgot-otp")
                || path.startsWith("/test/api/v1/branch/reset-password")
                || path.startsWith("/test/api/v1/branch/check-user-status")
                || path.startsWith("/test/api/v1/branch/activate")
                || path.startsWith("/test/api/v1/branch/check-email")
                || path.startsWith("/test/api/v1/branch/check-name")
                || path.startsWith("/test/api/v1/branch/logo/")

                // OTP APIs
                || path.startsWith("/api/otp/verify");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        logger.debug("FILTER RUNNING FOR: {}", requestURI);

        String requestHeader = request.getHeader("Authorization");

        String username = null;
        String token    = null;
        String jti      = null;
        String role     = null;

        if (requestHeader != null && requestHeader.startsWith("Bearer ")) {

            token = requestHeader.substring(7);

            try {

                username = jwtHelper.getUsernameFromToken(token);
                jti      = jwtHelper.getJtiFromToken(token);
                role     = jwtHelper.getRoleFromToken(token);

            } catch (ExpiredJwtException e) {
                logger.warn("JWT expired for request [{}]: {}", requestURI, e.getMessage());
            } catch (MalformedJwtException e) {
                logger.warn("Malformed JWT for request [{}]: {}", requestURI, e.getMessage());
            } catch (SignatureException e) {
                logger.warn("Invalid JWT signature for request [{}]: {}", requestURI, e.getMessage());
            } catch (UnsupportedJwtException e) {
                logger.warn("Unsupported JWT for request [{}]: {}", requestURI, e.getMessage());
            } catch (IllegalArgumentException e) {
                logger.warn("JWT claims empty for request [{}]: {}", requestURI, e.getMessage());
            } catch (Exception e) {
                logger.warn("JWT parse error for request [{}]: {}", requestURI, e.getMessage());
            }

        } else {
            logger.debug("No Bearer token found in request [{}]", requestURI);
        }

        if (username != null
                && jti != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {

                UserDetails userDetails         = userDetailsService.loadUserByUsername(username);
                boolean     isTokenBlacklisted  = tokenBlacklistService.isTokenBlacklisted(jti);
                boolean     validateToken       = jwtHelper.validateToken(token, username, isTokenBlacklisted);

                if (validateToken) {

                    List<GrantedAuthority> authorities = (role != null)
                            ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                            : (userDetails.getAuthorities() != null
                                    ? Collections.unmodifiableList(
                                            new java.util.ArrayList<>(userDetails.getAuthorities()))
                                    : Collections.emptyList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, authorities);

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.debug("JWT authenticated user [{}] for request [{}]", username, requestURI);

                } else {
                    logger.warn("JWT validation failed for user [{}]", username);
                }

            } catch (Exception e) {
                logger.warn("Could not authenticate user [{}] for request [{}]: {}",
                        username, requestURI, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
