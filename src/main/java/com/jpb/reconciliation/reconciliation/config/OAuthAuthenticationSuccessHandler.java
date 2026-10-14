package com.jpb.reconciliation.reconciliation.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.service.CustomUserDetailService;

@Component
public class OAuthAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	Logger logger = LoggerFactory.getLogger(OAuthAuthenticationSuccessHandler.class);

	@Autowired
	CustomUserDetailService customUserDetailService;

	@Autowired
	JwtHelper helper;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
		UserDetails userDetails = customUserDetailService.loadUserByUsername(user.getName());
		System.out.println("LOGIN DETAILS WITH GOOGLE ::::::::::" + userDetails);
		String jwttoken = helper.generateToken(userDetails);
		response.sendRedirect("http://localhost:5173/?token=" + jwttoken);
		logger.info("USER DETAILS FOR GOOGLE :::::::::::::::::" + user);
	}

}
