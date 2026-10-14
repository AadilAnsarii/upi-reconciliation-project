package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigResponse;

/**
 * Service contract for RECON_EXEC_SCHEDULE_CONFIG operations.
 *
 * All methods are scoped by templateId (UQ_RSCHED_TMPLT enforces one row per
 * template).
 */
public interface ReconExecScheduleConfigService {

	/**
	 * Upsert schedule config for a template.
	 *
	 * Behaviour: - If no config exists for this templateId → INSERT a new row. - If
	 * a config already exists for this templateId → UPDATE the existing row.
	 *
	 * Validates: - scheduleType must be CRON, CYCLE, or EVENT_BASED. - If
	 * scheduleType = CRON, cronExpression must not be blank. - isActive must be Y
	 * or N.
	 *
	 * @param templateId FK → RECON_FILE_TMPLT_MAST.TEMPLATE_ID
	 * @param request    schedule configuration fields from the client
	 * @return persisted ScheduleConfigResponse
	 */
	ScheduleConfigResponse saveScheduleConfig(Long templateId, ScheduleConfigRequest request);

	/**
	 * Fetch the schedule config for a given template.
	 *
	 * @param templateId FK → RECON_FILE_TMPLT_MAST.TEMPLATE_ID
	 * @return ScheduleConfigResponse
	 * @throws RuntimeException if no config exists for the templateId
	 */
	ScheduleConfigResponse getScheduleConfig(Long templateId);

	/**
	 * Activate the schedule — sets IS_ACTIVE = 'Y'.
	 *
	 * @param templateId FK → RECON_FILE_TMPLT_MAST.TEMPLATE_ID
	 */
	void activateSchedule(Long templateId);

	/**
	 * Deactivate the schedule — sets IS_ACTIVE = 'N'.
	 *
	 * @param templateId FK → RECON_FILE_TMPLT_MAST.TEMPLATE_ID
	 */
	void deactivateSchedule(Long templateId);

	/**
	 * Delete the schedule config for a template. DB-level ON DELETE CASCADE on
	 * FK_RSCHED_TMPLT handles deletion when the parent template is deleted from the
	 * DB directly, but this method allows explicit deletion from the Java layer.
	 *
	 * @param templateId FK → RECON_FILE_TMPLT_MAST.TEMPLATE_ID
	 */
	void deleteScheduleConfig(Long templateId);
}
