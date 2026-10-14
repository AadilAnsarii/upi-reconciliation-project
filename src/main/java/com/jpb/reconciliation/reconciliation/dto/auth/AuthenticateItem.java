package com.jpb.reconciliation.reconciliation.dto.auth;

import lombok.Data;

@Data
public class AuthenticateItem {
	private String mode;
	private String value;
	private String action;
}
