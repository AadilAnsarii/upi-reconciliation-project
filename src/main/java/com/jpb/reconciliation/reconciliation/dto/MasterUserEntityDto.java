package com.jpb.reconciliation.reconciliation.dto;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MasterUserEntityDto {

	private Long userId;

	private String userName;

	private String userPassword;

	private String emailId;

	private Timestamp loginTimestamp;

	private String loginYn;

	private String token;
	
	private LocalDateTime createdAt;

	private String createdBy;

	private LocalDateTime updatedAt;

	private String updatedBy;

}
