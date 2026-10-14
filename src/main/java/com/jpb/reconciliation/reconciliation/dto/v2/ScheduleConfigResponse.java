package com.jpb.reconciliation.reconciliation.dto.v2;


import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for RECON_EXEC_SCHEDULE_CONFIG.
 *
 * All DDL columns are surfaced here so the client has complete visibility into
 * the stored schedule config.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConfigResponse {

	/** SCHEDULE_ID — auto-generated PK */
	private Long scheduleId;

	/** TEMPLATE_ID — FK to RECON_FILE_TMPLT_MAST */
	private Long templateId;

	/** SCHEDULE_TYPE — CRON | CYCLE | EVENT_BASED */
	private String scheduleType;

	/** CRON_EXPRESSION — e.g. "0 30 2 * * ?" */
	private String cronExpression;

	/** EXEC_WINDOW_START */
	private LocalDateTime execWindowStart;

	/** EXEC_WINDOW_END */
	private LocalDateTime execWindowEnd;

	/** MAX_RETRY_COUNT — default 3 */
	private Integer maxRetryCount;

	/** RETRY_INTERVAL_MINS — default 15 */
	private Integer retryIntervalMins;

	/** DEPENDENCY_TMPLT_IDS — comma-separated template IDs */
	private String dependencyTmpltIds;

	/** TIMEZONE — default Asia/Kolkata */
	private String timezone;

	/** IS_ACTIVE — Y | N */
	private String isActive;

	/** CREATED_BY */
	private String createdBy;

	/** CREATED_AT */
	private LocalDateTime createdAt;

	/** UPDATED_BY */
	private String updatedBy;

	/** UPDATED_AT */
	private LocalDateTime updatedAt;
}
