package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReconUserResponseDto {
	private Long userId;
	private String bank;
	private String designation;
	private String emailId;
	private String type;
	private String userStatus;
	private String userName;
	private Long mobileNumber;
	private LocalDateTime createdAt;
	private String createdBy;
	private LocalDateTime updatedAt;
	private String updatedBy;
	private Long roleId;
}
