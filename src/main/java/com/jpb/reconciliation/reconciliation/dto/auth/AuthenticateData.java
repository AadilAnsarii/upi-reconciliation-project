package com.jpb.reconciliation.reconciliation.dto.auth;

import lombok.Data;

@Data
public class AuthenticateData {
	private String mode;
	private String value;
}
