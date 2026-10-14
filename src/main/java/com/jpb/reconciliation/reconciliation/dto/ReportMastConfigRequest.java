package com.jpb.reconciliation.reconciliation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request payload for creating/updating RCN_REPORT_MAST_CONFIG")
public class ReportMastConfigRequest {

	@Schema(description = "Unique report identifier", example = "10")
	@JsonProperty("report_id")
	private Long reportId;

	@Schema(description = "Name of the report", example = "SWITCH_RECON")
	@JsonProperty("file_name")
	private String fileName; // FILE_NAME (report name)

	@Schema(description = "Process ID linked to the report", example = "815065384329")
	@JsonProperty("process_id")
	private Long processId;

	@Schema(description = "Report date (DD-MM-RR)", example = "26-08-25")
	@JsonProperty("report_date")
	private LocalDate reportDate;

	@Schema(description = "Report header pipe-separated", example = "PROCESS_DATE~TRAN_AMOUNT~TRAN_DATE")
	@JsonProperty("report_header")
	private String reportHeader;

	@Schema(description = "Report key identifier", example = "SWITCH_RECON_KEY")
	@JsonProperty("report_key")
	private String reportKey;

	@Schema(description = "Full SQL query for the report")
	@JsonProperty("report_query")
	private String reportQuery;

	// ── UI-level fields (used to build report_query dynamically) ──

	@Schema(description = "Process type: EXTRACTION or RECONCILIATION", example = "EXTRACTION")
	@JsonProperty("process_type")
	private String processType; // EXTRACTION | RECONCILIATION

	@Schema(description = "Template ID selected by user", example = "5")
	@JsonProperty("template_id")
	private Long templateId;

	@Schema(description = "Selected file name from template file details", example = "aeps_transaction_file.csv")
	@JsonProperty("selected_file")
	private String selectedFile;

	@Schema(description = "List of selected columns for the report")
	@JsonProperty("selected_columns")
	private List<String> selectedColumns;

	@Schema(description = "Report output format: CSV or EXCEL", example = "CSV")
	@JsonProperty("report_type")
	private String reportType; // CSV | EXCEL

	@Schema(description = "WHERE clause conditions for the report")
	@JsonProperty("where_conditions")
	private List<WhereCondition> whereConditions;

	@Data
	@Schema(description = "A single WHERE condition")
	public static class WhereCondition {
		@Schema(description = "Column name", example = "REC_FLG")
		private String column;

		@Schema(description = "Operator", example = "=")
		private String operator; // =, !=, >, <, LIKE, IN, BETWEEN

		@Schema(description = "Value for the condition", example = "1")
		private String value;

		@Schema(description = "Logical join with next condition", example = "AND")
		private String logicalOp; // AND | OR
	}
}