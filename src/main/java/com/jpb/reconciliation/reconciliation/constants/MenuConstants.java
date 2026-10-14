package com.jpb.reconciliation.reconciliation.constants;

public class MenuConstants {
	
	public static final String STATUS_201 = "201";
	public static final String MESSAGE_201 = "Menu created successfully";
	public static final String STATUS_200 =CommonConstants.STATUS_200;
	public static final String MESSAGE_200 ="Request execute successfully";
	public static final String STATUS_417 = "417";
	public static final String MESSAGE_417="Update operation failed";

	// Menu URL uniqueness — scoped per owner (Bank or Branch), never system-wide. See
	// MenuMasterServiceImpl.validateMenuUrlUniqueForOwner.
	public static final String MESSAGE_MENU_URL_ALREADY_EXISTS =
			"This URL is already used by another menu in your Bank/Branch. Please choose a different URL.";
}
