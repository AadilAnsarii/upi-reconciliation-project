package com.jpb.reconciliation.reconciliation.dto.auth;

import java.util.List;

import lombok.Data;

@Data
public class AppAuthRequest {
	private Application application;
	private String scope;
	private List<AuthenticateData> authenticateList;
	private Secure secure;
	private int purpose;
	private String extraInfo;
}
