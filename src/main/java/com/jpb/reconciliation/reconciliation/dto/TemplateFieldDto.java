package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import lombok.Data;

@Data
public class TemplateFieldDto {
	private String templateType;
	private String templateName;
	private Long columnCount;
	private String reversalIndicator;
	private String dataReference;
	private String onlineRefund;

	private List<FieldConfigurationDto> fieldDetails;
}
