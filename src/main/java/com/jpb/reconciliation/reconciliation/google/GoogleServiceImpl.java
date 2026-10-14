package com.jpb.reconciliation.reconciliation.google;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.jpb.reconciliation.reconciliation.config.AppInitializer;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.AuditLogManagerService;
import com.jpb.reconciliation.reconciliation.service.CustomUserDetailService;

@Service
public class GoogleServiceImpl implements GoogleService {

	private final AppInitializer appInitializer;

	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String GOOGLE_CLIENT_ID;

	@Autowired
	CustomUserDetailService customUserDetailService;

	@Autowired
	AuditLogManagerService auditLogManagerService;

	@Autowired
	JwtHelper helper;

	@Autowired
	ReconUserRepository reconUserRepository;

	Logger logger = LoggerFactory.getLogger(GoogleServiceImpl.class);

	GoogleServiceImpl(AppInitializer appInitializer) {
		this.appInitializer = appInitializer;
	}

	@Override
	public ResponseEntity<RestWithStatusList> authenticateWithGoogle(GoogleRequest googleRequest,
			HttpServletResponse response) {
		String googleToken = googleRequest.getGoogleToken();
		List<Object> userData = new ArrayList<>();
		RestWithStatusList restWithStatusList;

		if (googleToken == null || googleToken.trim().isEmpty()) {
			logger.warn("Google token is null or empty in the request.");
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Google token is missing", userData);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}

		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
				.setAudience(Collections.singletonList(GOOGLE_CLIENT_ID)).build();
		logger.info("GoogleIdTokenVerifier :::::::::::::::" + verifier);
		try {
			GoogleIdToken idToken = verifier.verify(googleToken);
			logger.info("GoogleIdToken respomse :::::::::::::::" + idToken);
			if (idToken == null) {
				logger.warn("Google ID Token verification failed for token: {}", googleToken);
				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Invalid Google token", userData);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.UNAUTHORIZED);
			}

			GoogleIdToken.Payload tokenPayload = idToken.getPayload();
			logger.info("GoogleIdToken Payload :::::::::::::::" + tokenPayload);
			String email = tokenPayload.getEmail();
			String name = (String) tokenPayload.get("name");
			logger.info("email :::" + email + "name :::" + name);
			UserDetails userDetails = null;
			try {
//				userDetails = customUserDetailService.loadUserByUsername(name);
				userDetails = customUserDetailService.loadUserByUserEmail(email);
				logger.info("User Details For :::::::::::::::::" + name + " {}" + userDetails);
			} catch (UsernameNotFoundException e) {
				logger.warn("User not found in system for Google email/name: {}", email != null ? email : name);
				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "User not registered in our system", userData);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
			}

			String jwtToken = this.helper.generateToken(userDetails);
			String username = userDetails.getUsername();
			logger.info("jwtToken :::::::" + jwtToken);
			logger.info("username :::::::" + username);
			Optional<ReconUser> reconUserOptional = reconUserRepository.findByEmail(email);
			logger.info("ReconUser by email :::::::" + reconUserOptional);
			if (!reconUserOptional.isPresent()) {
				logger.error(
						"User details found in Spring Security, but ReconUser not found in repository for email: {}",
						email);
				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "User Not Found, Please Try Registered User",
						userData);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			ReconUser user = reconUserOptional.get();

			if (CommonConstants.ACTIVE_Y.equals(user.getApprovedYn())) {
				user.setLastLogin(LocalDateTime.now());
				reconUserRepository.save(user);

				String refreshToken = helper.generateTokenForRefresh(username);
				Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
				refreshCookie.setHttpOnly(true);
				refreshCookie.setMaxAge(60 * 60 * 24);
				refreshCookie.setPath("/");
				refreshCookie.setSecure(true);
				response.addCookie(refreshCookie);

				AuthResponse authResponse = new AuthResponse(jwtToken, refreshToken);
				userData.add(authResponse);
				restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "User logged in successfully", userData);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
			} else {
				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "User is not approved.", userData);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
			}
		} catch (GeneralSecurityException e) {
			logger.error("Google ID Token verification failed due to security issues (e.g., malformed token): {}",
					e.getMessage(), e);
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Google token security error", userData);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.UNAUTHORIZED);
		} catch (IOException e) {
			logger.error("Network or I/O error during Google ID Token verification: {}", e.getMessage(), e);
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Network error during authentication", userData);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			logger.error("An unexpected error occurred during Google authentication: {}", e.getMessage(), e);
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Internal server error during authentication",
					userData);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
