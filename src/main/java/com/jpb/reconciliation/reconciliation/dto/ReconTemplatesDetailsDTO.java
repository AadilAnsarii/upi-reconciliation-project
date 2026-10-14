package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconTemplatesDetailsDTO {

	private Long reconTemplateId;
	private Long subTemplateId;
	private Long typeId;
	private String templateType;
	private String templateName;
	private String stageTabName;
	private Long columnCount;
	private String existFlag;
	private String reversalIndicator;
	private String dataReferenceFlag;
	private String onlRefundFlag;
	private String issacqFlag;
	private String dataTableInd;
	private String masterFlag;
	private String masterTemplateId;
	private String settlementFlag;
	private String productType;
	private Long insertCode;
	private Long insertUser;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
	private Date insertDate;

	private Long lastUpdatedUser;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
	private Date reconLastUpdatedDate;

	// ONLY field details (file details removed)
	private Set<ReconFieldDetailsMasterDTO> fieldDetails;
}