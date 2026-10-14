package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconProcessRequest {

	@NotBlank(message = "Process name is required")
	private String processName;

	private String tranChannel;

	@NotNull(message = "Retention period is required")
	private Long retentionPeriod;

	@NotNull(message = "Retention volume is required")
	private Long retentionVolume;

	@NotBlank(message = "Matching type is required")
	private String matchingType;

	@NotNull(message = "Input count is required")
	private Long inputCount;

	private String reconType;
	private String jpsRpsl;
	private String identicalMatching;

	// File Type mappings - should match inputCount
	@Valid
	private List<FileTypeMapping> fileTypeMappings;

	// Template mappings - should match inputCount
	@Valid
	private List<TemplateMapping> templateMappings;

	// Matching fields - should match inputCount
	@Valid
	private List<MatchingFieldMapping> matchingFields;

	// Additional fields
	private Long bankCode;
	private Long insUser;
	private Long inchgPosition;
	private Long interchangeId;
	private String issAcqFlag;
	private String manrecFlag;
	private Long mastTemp;
	private String matchingFlag;
	private String recMenuFlag;
	private Long tableType;
	private Long processMastId;
	private Long manrecCount;
	private String disputeFlag;
	private String emailSmsFlag;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class FileTypeMapping {
		@NotNull(message = "File type number is required")
		private Integer fileTypeNumber;

		@NotNull(message = "File type ID is required")
		private Long fileTypeId;

		private String fileTypeName;

		@NotBlank(message = "Data table name is required")
		private String dataTableName;

		@NotBlank(message = "Rec flag name is required")
		private String recFlagName;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TemplateMapping {
		@NotNull(message = "Template number is required")
		private Integer templateNumber;

		@NotNull(message = "Template ID is required")
		private Long templateId;

		private String templateName;
		private String stageTabName;
		private List<String> templateFields;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MatchingFieldMapping {
		@NotNull(message = "Field number is required")
		private Integer fieldNumber;

		@NotNull(message = "Selected fields are required")
		@Size(min = 1, message = "At least one matching field must be selected")
		private List<String> selectedFields;
	}

}