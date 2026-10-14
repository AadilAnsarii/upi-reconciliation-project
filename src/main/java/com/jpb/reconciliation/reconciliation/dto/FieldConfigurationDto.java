package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class FieldConfigurationDto {
	private Long columnPosition;
	private String fieldName;
	private String fieldtype;
	private String fieldFormat;
	private Long fieldLength;
	private Long fromPosition;
	private Long toPosition;
	private String keyIdentity;
	private String columnOffset;
	private String qualifier;
}
