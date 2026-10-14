package com.jpb.reconciliation.reconciliation.dto.v2;

import java.time.LocalDateTime;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for saving / updating RECON_EXEC_SCHEDULE_CONFIG.
 *
 * Field alignment to DDL columns (exact): scheduleType → SCHEDULE_TYPE
 * VARCHAR2(20) CHECK: CRON|CYCLE|EVENT_BASED cronExpression → CRON_EXPRESSION
 * VARCHAR2(100) execWindowStart → EXEC_WINDOW_START DATE execWindowEnd →
 * EXEC_WINDOW_END DATE maxRetryCount → MAX_RETRY_COUNT NUMBER(3,0) DEFAULT 3
 * retryIntervalMins → RETRY_INTERVAL_MINS NUMBER(5,0) DEFAULT 15
 * dependencyTmpltIds → DEPENDENCY_TMPLT_IDS CLOB comma-separated string
 * timezone → TIMEZONE VARCHAR2(50) DEFAULT 'Asia/Kolkata' isActive → IS_ACTIVE
 * CHAR(1) Y|N
 *
 * Note: scheduleId, templateId, createdBy, createdAt, updatedBy, updatedAt are
 * all managed by the system — never accepted from the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConfigRequest {

	/**
	 * Schedule type — must be one of: CRON, CYCLE, EVENT_BASED Maps to DDL CHECK
	 * constraint CHK_RSCHED_TYPE.
	 */
	@Pattern(regexp = "CRON|CYCLE|EVENT_BASED", message = "scheduleType must be CRON, CYCLE, or EVENT_BASED")
	@Size(max = 20)
	private String scheduleType;

	/**
	 * Cron expression — only applicable when scheduleType = CRON. e.g. "0 30 2 * *
	 * ?" (Spring cron) or "0 2 * * *" (Unix cron)
	 */
	@Size(max = 100, message = "cronExpression must not exceed 100 characters")
	private String cronExpression;

	/** Execution window start timestamp — null = no restriction */
	private LocalDateTime execWindowStart;

	/** Execution window end timestamp — null = no restriction */
	private LocalDateTime execWindowEnd;

	/**
	 * Maximum retry attempts on failure. NUMBER(3,0) → max 999 retries. Default 3.
	 */
	@Min(value = 0, message = "maxRetryCount must be >= 0")
	@Max(value = 999, message = "maxRetryCount must be <= 999")
	@Builder.Default
	private Integer maxRetryCount = 3;

	/**
	 * Delay between retries in minutes. NUMBER(5,0) → max 99999 minutes. Default
	 * 15.
	 */
	@Min(value = 1, message = "retryIntervalMins must be >= 1")
	@Max(value = 99999, message = "retryIntervalMins must be <= 99999")
	@Builder.Default
	private Integer retryIntervalMins = 15;

	/**
	 * Comma-separated TEMPLATE_IDs that must complete before this schedule runs.
	 * Stored as CLOB in DDL. e.g. "1001,1002,1003" Send null or empty string if
	 * there are no dependencies.
	 */
	private String dependencyTmpltIds;

	/**
	 * Timezone used for cron evaluation. Defaults to "Asia/Kolkata" — must be a
	 * valid Java timezone ID.
	 */
	@Size(max = 50, message = "timezone must not exceed 50 characters")
	@Builder.Default
	private String timezone = "Asia/Kolkata";

	/**
	 * IS_ACTIVE flag — Y = active, N = inactive. Must match DDL CHECK constraint
	 * CHK_RSCHED_ACTIVE. Default is N (inactive until explicitly activated).
	 */
	@Pattern(regexp = "Y|N", message = "isActive must be 'Y' or 'N'")
	@Builder.Default
	private String isActive = "N";
}
