package com.jpb.reconciliation.reconciliation.mapper.v2;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigResponse;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconExecScheduleConfig;

/**
 * Mapper for RECON_EXEC_SCHEDULE_CONFIG.
 *
 * Responsibilities: 1. toEntity(templateId, request) → build a brand new entity
 * from a request DTO 2. updateEntity(entity, request) → apply non-null request
 * fields onto an existing entity (upsert) 3. toResponse(entity) → convert a
 * saved entity to the response DTO
 *
 * All fields are set explicitly one by one — no reflection, no MapStruct, no
 * builder magic in the service layer.
 */
@Component
public class ReconExecScheduleConfigMapper {

	// =========================================================================
	// 1. toEntity — for INSERT (new record)
	// Sets templateId from the path variable + all DTO fields.
	// Audit fields (createdBy, createdAt, updatedBy, updatedAt) are managed
	// by @CreatedBy / @CreatedDate / @LastModifiedBy / @LastModifiedDate —
	// do NOT set them here.
	// =========================================================================

	/**
	 * Builds a new entity from scratch. Use this when no existing row is found for
	 * the templateId.
	 *
	 * @param templateId FK → RECON_FILE_TMPLT_MAST.TEMPLATE_ID (from path variable)
	 * @param request    client-supplied schedule configuration fields
	 * @return a fully populated, unsaved entity ready for scheduleRepo.save()
	 */
	public ReconExecScheduleConfig toEntity(Long templateId, ScheduleConfigRequest request) {

		ReconExecScheduleConfig entity = new ReconExecScheduleConfig();

		// FK — always set from path variable, never from DTO
		entity.setTemplateId(templateId);

		// SCHEDULE_TYPE — VARCHAR2(20) CHECK: CRON|CYCLE|EVENT_BASED
		entity.setScheduleType(request.getScheduleType());

		// CRON_EXPRESSION — VARCHAR2(100)
		entity.setCronExpression(request.getCronExpression());

		// EXEC_WINDOW_START — DATE → LocalDateTime
		entity.setExecWindowStart(request.getExecWindowStart());

		// EXEC_WINDOW_END — DATE → LocalDateTime
		entity.setExecWindowEnd(request.getExecWindowEnd());

		// MAX_RETRY_COUNT — NUMBER(3,0) DEFAULT 3
		// Use request value if provided; fall back to entity default (3)
		if (null != request.getMaxRetryCount()) {
			entity.setMaxRetryCount(request.getMaxRetryCount());
		} else {
			entity.setMaxRetryCount(3);
		}

		// RETRY_INTERVAL_MINS — NUMBER(5,0) DEFAULT 15
		if (null != request.getRetryIntervalMins()) {
			entity.setRetryIntervalMins(request.getRetryIntervalMins());
		} else {
			entity.setRetryIntervalMins(15);
		}

		// DEPENDENCY_TMPLT_IDS — CLOB (comma-separated template IDs)
		entity.setDependencyTmpltIds(request.getDependencyTmpltIds());

		// TIMEZONE — VARCHAR2(50) DEFAULT 'Asia/Kolkata'
		if (null != request.getTimezone()) {
			entity.setTimezone(request.getTimezone());
		} else {
			entity.setTimezone("Asia/Kolkata");
		}

		// IS_ACTIVE — CHAR(1) DEFAULT 'N' CHECK: Y|N
		if (null != request.getIsActive()) {
			entity.setIsActive(request.getIsActive());
		} else {
			entity.setIsActive(CommonConstants.INACTIVE_N);
		}
		
		entity.setCreatedAt(LocalDateTime.now());

		return entity;
	}

	// =========================================================================
	// 2. updateEntity — for UPDATE (existing record)
	// Only overrides fields that are non-null in the request.
	// Existing entity values are preserved when the request field is null.
	// templateId and audit fields are never touched here.
	// =========================================================================

	/**
	 * Applies client-supplied fields from the request DTO onto an existing entity.
	 * Use this when an existing row is found for the templateId (upsert update
	 * path). Null-safe: if a request field is null, the existing entity value is
	 * kept as-is.
	 *
	 * @param entity  the loaded entity (from scheduleRepo.findByTemplateId)
	 * @param request client-supplied fields to apply
	 */
	public void updateEntity(ReconExecScheduleConfig entity, ScheduleConfigRequest request) {

		// SCHEDULE_TYPE
		if (null != request.getScheduleType()) {
			entity.setScheduleType(request.getScheduleType());
		}

		// CRON_EXPRESSION
		if (null != request.getCronExpression()) {
			entity.setCronExpression(request.getCronExpression());
		}

		// EXEC_WINDOW_START
		if (null != request.getExecWindowStart()) {
			entity.setExecWindowStart(request.getExecWindowStart());
		}

		// EXEC_WINDOW_END
		if (null != request.getExecWindowEnd()) {
			entity.setExecWindowEnd(request.getExecWindowEnd());
		}

		// MAX_RETRY_COUNT
		if (null != request.getMaxRetryCount()) {
			entity.setMaxRetryCount(request.getMaxRetryCount());
		}

		// RETRY_INTERVAL_MINS
		if (null != request.getRetryIntervalMins()) {
			entity.setRetryIntervalMins(request.getRetryIntervalMins());
		}

		// DEPENDENCY_TMPLT_IDS
		if (null != request.getDependencyTmpltIds()) {
			entity.setDependencyTmpltIds(request.getDependencyTmpltIds());
		}

		// TIMEZONE
		if (null != request.getTimezone()) {
			entity.setTimezone(request.getTimezone());
		}

		// IS_ACTIVE
		if (null != request.getIsActive()) {
			entity.setIsActive(request.getIsActive());
		}
	}

	// =========================================================================
	// 3. toResponse — entity → response DTO
	// Maps every DDL column to its response field one by one.
	// All 15 columns surfaced so the client has complete visibility.
	// =========================================================================

	/**
	 * Converts a saved entity to the response DTO. Every field is set explicitly —
	 * no builder shortcuts.
	 *
	 * @param entity saved / loaded entity from the database
	 * @return fully populated ScheduleConfigResponse
	 */
	public ScheduleConfigResponse toResponse(ReconExecScheduleConfig entity) {

		ScheduleConfigResponse response = new ScheduleConfigResponse();

		// SCHEDULE_ID — auto-generated PK
		response.setScheduleId(entity.getScheduleId());

		// TEMPLATE_ID — FK to RECON_FILE_TMPLT_MAST
		response.setTemplateId(entity.getTemplateId());

		// SCHEDULE_TYPE — CRON | CYCLE | EVENT_BASED
		response.setScheduleType(entity.getScheduleType());

		// CRON_EXPRESSION
		response.setCronExpression(entity.getCronExpression());

		// EXEC_WINDOW_START
		response.setExecWindowStart(entity.getExecWindowStart());

		// EXEC_WINDOW_END
		response.setExecWindowEnd(entity.getExecWindowEnd());

		// MAX_RETRY_COUNT
		response.setMaxRetryCount(entity.getMaxRetryCount());

		// RETRY_INTERVAL_MINS
		response.setRetryIntervalMins(entity.getRetryIntervalMins());

		// DEPENDENCY_TMPLT_IDS
		response.setDependencyTmpltIds(entity.getDependencyTmpltIds());

		// TIMEZONE
		response.setTimezone(entity.getTimezone());

		// IS_ACTIVE — Y | N
		response.setIsActive(entity.getIsActive());

		// CREATED_BY
		response.setCreatedBy(entity.getCreatedBy());

		// CREATED_AT
		response.setCreatedAt(entity.getCreatedAt());

		// UPDATED_BY
		response.setUpdatedBy(entity.getUpdatedBy());

		// UPDATED_AT
		response.setUpdatedAt(entity.getUpdatedAt());

		return response;
	}
}
