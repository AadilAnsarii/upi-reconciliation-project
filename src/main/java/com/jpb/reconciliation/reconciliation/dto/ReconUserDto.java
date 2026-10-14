package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReconUserDto {

	private Long userId;
	private String bank;
	private String designation;
	private String emailId;
	private String type;
	private String userStatus;
	private String roleName;
	private Long roleId;
	private String userName;
	private String userPassword;
	private Long mobileNumber;
	private String createdBy;
	private LocalDateTime updatedAt;
	private String updatedBy;
	private String approvedYn;
	private String approvedBy;
}
