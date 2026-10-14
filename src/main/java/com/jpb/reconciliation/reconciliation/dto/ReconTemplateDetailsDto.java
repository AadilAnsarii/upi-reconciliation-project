package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconTemplateDetailsDto {
	private Long reconTypeId;
	private Long reconTemplateId;
	private Long subTemplateId;
	private String reconTemplateName;
	private String reconStageTabName;
	private Long reconColumnCnt;
	private String reconExistFlag;
	private String reconReversalInd;
	private String reconRefFlag;
	private String reconOnlRefFlag;
	private String reconIssacqFlag;
	private String reconDataTableInd;
	private String reconMasterFlag;
	private String reconMasterTemplateId;
	private Long InsertCode;
	private Long InsertUser;
	private Date reconInsertDate;
	private Long reconLastUpdatedUser;
	private Date reconLastUpdatedDate;
}
