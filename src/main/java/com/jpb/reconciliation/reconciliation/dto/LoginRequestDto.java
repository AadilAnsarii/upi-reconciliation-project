package com.jpb.reconciliation.reconciliation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
	
	private String userName;
	private String userPassword;
	private String emailId;

}

