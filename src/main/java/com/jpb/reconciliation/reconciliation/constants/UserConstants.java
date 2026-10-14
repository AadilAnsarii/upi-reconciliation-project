package com.jpb.reconciliation.reconciliation.constants;

public class UserConstants {
	
	public static final String STATUS_201="201";
	public static final String MESSAGE_201="User created successfully";
	public static final String STATUS_200=CommonConstants.STATUS_200;
	public static final String MESSAGE_200="Request execute successfully";
	
	// For Master user
	public static final String STATUS_302="302";
	public static final String MESSAGE_302="User already exists with given user name";

	// Admin-tier USER_TYPE values — an Admin acting as Maker needs no Checker (no
	// "super" above them within their own org), so their own Role/Menu creations
	// go straight to ACTIVE instead of through the DRAFT->PENDING->ACTIVE flow.
	public static boolean isAdminUserType(String userType) {
		return "KAL_ADMIN".equals(userType) || "BANK_ADMIN".equals(userType) || "BRANCH_ADMIN".equals(userType);
	}

}
