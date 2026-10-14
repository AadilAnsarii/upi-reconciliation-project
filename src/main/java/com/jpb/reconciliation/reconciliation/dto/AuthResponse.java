package com.jpb.reconciliation.reconciliation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

	private String accessToken;
	private String refreshToken;
	private Long   userId;
	private Long   roleId;
	private String userType;
	private String fullName;
	private String username;
	private Long   bankId;

	public AuthResponse(String accessToken, String refreshToken) {
		this.accessToken  = accessToken;
		this.refreshToken = refreshToken;
	}

	public AuthResponse(String accessToken, String refreshToken,
						Long userId, Long roleId, String userType, String fullName, String username) {
		this.accessToken  = accessToken;
		this.refreshToken = refreshToken;
		this.userId       = userId;
		this.roleId       = roleId;
		this.userType     = userType;
		this.fullName     = fullName;
		this.username     = username;
	}

	public AuthResponse(String accessToken, String refreshToken,
						Long userId, Long roleId, String userType, String fullName, String username, Long bankId) {
		this(accessToken, refreshToken, userId, roleId, userType, fullName, username);
		this.bankId = bankId;
	}
}
