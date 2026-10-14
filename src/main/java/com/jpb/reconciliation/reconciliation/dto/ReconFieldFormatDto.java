package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReconFieldFormatDto {

	private Long reconFieldFormatId;
	private Long reconFieldTypeId;
	private String reconFieldFormatDesc;
	private Long reconInsertCode;
	private Long reconInsertUser;
	private Date reconInsertDate;
	private Long reconLastUpdatedUser;
	private Date reconLastUpdatedDate;
}