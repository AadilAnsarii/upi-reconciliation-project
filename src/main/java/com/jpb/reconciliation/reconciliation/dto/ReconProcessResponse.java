package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconProcessResponse {

	// Basic Process Information
	private Long processId;
	private String processName;
	private String tranChannel;
	private Long retentionPeriod;
	private Long retentionVolume;
	private String matchingType;
	private Long inputCount;
	private String reconType;
	private String jpsRpsl;
	private String identicalMatching;
	private String reconDisputeFlag;
	// File Type Information
	private List<FileTypeInfo> fileTypes;

	// Template Information
	private List<TemplateInfo> templates;

	// Matching Fields
	private List<MatchingFieldInfo> matchingFields;

	// Audit Fields
	private Long bnkCode;
	private LocalDateTime insDate;
	private Long insUser;
	private LocalDateTime lupdDate;
	private Long lupdUser;

	// Additional Configuration Fields
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

	/**
	 * File Type Information with nested file details
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class FileTypeInfo {
		private Integer fileTypeNumber; // 1, 2, 3, or 4
		private Long fileTypeId; // File ID
		private String fileTypeName; // Short name of file
		private String dataTableName; // Data table name
		private String recFlagName; // Reconciliation flag name
		private FileDetailInfo fileDetails; // Nested file details
	}

	/**
	 * Detailed file information
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class FileDetailInfo {
		private Long fileId;
		private String fileName;
		private String shortName;
		private String fileType;
		private String fileDescription;
		private String fileLocation;
		private String fileDelimiter;
		private String hdrAvlFlag;
		private String ftrAvailFlag;
		private Long templateId;
		private String templateName;
	}

	/**
	 * Template Information
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class TemplateInfo {
		private Integer templateNumber; // 1, 2, 3, or 4
		private Long templateId; // Template ID
		private String templateName; // Template name
		private String stagingTableName; // Staging table name
		private Long columnCount; // Number of columns
		private String templateType; // Template type
		private List<String> templateFields; // Template fields
	}

	/**
	 * Matching Field Information
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class MatchingFieldInfo {
		private Integer fieldNumber; // 1, 2, 3, or 4
		private String matchingField; // Comma-separated field string
		private List<String> selectedFields; // List of selected fields
	}
}
