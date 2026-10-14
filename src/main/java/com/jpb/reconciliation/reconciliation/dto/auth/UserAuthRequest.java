package com.jpb.reconciliation.reconciliation.dto.auth;

import java.util.List;

public class UserAuthRequest {
	
	private User user;
	private String scope;
	private List<AuthenticateItem> authenticateList;
	private Secure secure;
	private int purpose; 
	
}
