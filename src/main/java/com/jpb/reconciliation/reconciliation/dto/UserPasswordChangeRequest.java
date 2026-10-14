package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class UserPasswordChangeRequest {

	private Long userId;
	private String oldPassword;
	private String newPassword;
	private String confirmNewPassword;

}
