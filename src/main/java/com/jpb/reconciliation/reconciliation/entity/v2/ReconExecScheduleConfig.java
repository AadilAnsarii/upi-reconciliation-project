package com.jpb.reconciliation.reconciliation.entity.v2;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity : RECON_EXEC_SCHEDULE_CONFIG (schema: JPB_RECON)
 *
 * DDL → Java field mapping (exact column names preserved):
 *
 * SCHEDULE_ID NUMBER GENERATED ALWAYS AS IDENTITY → scheduleId (PK, auto)
 * TEMPLATE_ID NUMBER NOT NULL → templateId (raw FK Long) SCHEDULE_TYPE
 * VARCHAR2(20) CHECK: CRON|CYCLE|EVENT_BASED → scheduleType CRON_EXPRESSION
 * VARCHAR2(100) → cronExpression EXEC_WINDOW_START DATE → execWindowStart
 * (LocalDateTime) EXEC_WINDOW_END DATE → execWindowEnd (LocalDateTime)
 * MAX_RETRY_COUNT NUMBER(3,0) DEFAULT 3 → maxRetryCount RETRY_INTERVAL_MINS
 * NUMBER(5,0) DEFAULT 15 → retryIntervalMins DEPENDENCY_TMPLT_IDS CLOB →
 * dependencyTmpltIds (comma-sep String) TIMEZONE VARCHAR2(50) DEFAULT
 * 'Asia/Kolkata' → timezone IS_ACTIVE CHAR(1) DEFAULT 'N' Y|N → isActive
 * CREATED_BY VARCHAR2(50) DEFAULT 'SYSTEM' → createdBy CREATED_AT TIMESTAMP(6)
 * DEFAULT SYSTIMESTAMP → createdAt UPDATED_BY VARCHAR2(50) → updatedBy
 * UPDATED_AT TIMESTAMP(6) → updatedAt
 *
 * Constraints: PK_REXEC_SCHED → PRIMARY KEY (SCHEDULE_ID) UQ_RSCHED_TMPLT →
 * UNIQUE (TEMPLATE_ID) — one config per template FK_RSCHED_TMPLT → FK
 * (TEMPLATE_ID) → RECON_FILE_TMPLT_MAST.TEMPLATE_ID ON DELETE CASCADE
 * CHK_RSCHED_TYPE → schedule_type IN ('CRON','CYCLE','EVENT_BASED')
 * CHK_RSCHED_ACTIVE→ is_active IN ('Y','N')
 */
@Entity
@Table(name = "RECON_EXEC_SCHEDULE_CONFIG", uniqueConstraints = @UniqueConstraint(name = "UQ_RSCHED_TMPLT", columnNames = "TEMPLATE_ID"))
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconExecScheduleConfig {

	/** PK — NUMBER GENERATED ALWAYS AS IDENTITY */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SCHEDULE_ID", nullable = false, updatable = false)
	private Long scheduleId;

	/**
	 * FK → JPB_RECON.RECON_FILE_TMPLT_MAST.TEMPLATE_ID (ON DELETE CASCADE). Stored
	 * as raw Long to avoid coupling to TemplateMaster entity. Unique constraint
	 * UQ_RSCHED_TMPLT enforces one config per template.
	 */
	@Column(name = "TEMPLATE_ID", nullable = false, unique = true, updatable = false)
	private Long templateId;

	/**
	 * CHECK: CRON | CYCLE | EVENT_BASED Stored as plain String to match the DDL
	 * VARCHAR2 column. Validate in service: scheduleType must be one of the three
	 * values.
	 */
	@Column(name = "SCHEDULE_TYPE", length = 20)
	private String scheduleType;

	/**
	 * Standard cron expression e.g. "0 30 2 * * ?" — only meaningful when
	 * scheduleType = CRON
	 */
	@Column(name = "CRON_EXPRESSION", length = 100)
	private String cronExpression;

	/** Daily execution window start — DATE in DDL, mapped to LocalDateTime */
	@Column(name = "EXEC_WINDOW_START")
	private LocalDateTime execWindowStart;

	/** Daily execution window end — DATE in DDL, mapped to LocalDateTime */
	@Column(name = "EXEC_WINDOW_END")
	private LocalDateTime execWindowEnd;

	/** Maximum retry attempts on failure — NUMBER(3,0) DEFAULT 3 */
	@Column(name = "MAX_RETRY_COUNT", nullable = false)
	@Builder.Default
	private Integer maxRetryCount = 3;

	/** Delay between retries in minutes — NUMBER(5,0) DEFAULT 15 */
	@Column(name = "RETRY_INTERVAL_MINS", nullable = false)
	@Builder.Default
	private Integer retryIntervalMins = 15;

	/**
	 * Comma-separated list of TEMPLATE_IDs that must complete before this schedule
	 * runs. Stored as CLOB in DDL. e.g. "1001,1002,1003"
	 */
	@Column(name = "DEPENDENCY_TMPLT_IDS", columnDefinition = "CLOB")
	private String dependencyTmpltIds;

	/** Timezone for cron evaluation — VARCHAR2(50) DEFAULT 'Asia/Kolkata' */
	@Column(name = "TIMEZONE", length = 50, nullable = false)
	@Builder.Default
	private String timezone = "Asia/Kolkata";

	/**
	 * IS_ACTIVE — CHAR(1) DEFAULT 'N' CHECK: Y = active (schedule runs), N =
	 * inactive (schedule paused)
	 */
	@Column(name = "IS_ACTIVE", columnDefinition = "CHAR(1)", nullable = false)
	@Builder.Default
	private String isActive = "N";

	@CreatedBy
	@Column(name = "CREATED_BY", length = 50, nullable = false, updatable = false)
	@Builder.Default
	private String createdBy = "SYSTEM";

	@CreatedDate
	@Column(name = "CREATED_AT", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedBy
	@Column(name = "UPDATED_BY", length = 50)
	private String updatedBy;

	@LastModifiedDate
	@Column(name = "UPDATED_AT")
	private LocalDateTime updatedAt;
}
