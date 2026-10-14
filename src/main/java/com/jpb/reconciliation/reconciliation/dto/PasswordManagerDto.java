package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PasswordManagerDto {
	
	private Long pwdId;
	private String userPassword;
	private Date expirationDate;

}
