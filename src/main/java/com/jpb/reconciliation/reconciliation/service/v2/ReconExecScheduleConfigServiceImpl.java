package com.jpb.reconciliation.reconciliation.service.v2;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.v2.ScheduleConfigResponse;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconExecScheduleConfig;
import com.jpb.reconciliation.reconciliation.mapper.v2.ReconExecScheduleConfigMapper;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconExecScheduleConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ReconExecScheduleConfigService.
 *
 * Dependencies: - ReconExecScheduleConfigRepository → DB operations -
 * ReconExecScheduleConfigMapper → DTO ↔ Entity conversion
 *
 * Mapper responsibilities (no conversion logic lives here):
 * mapper.toEntity(templateId, request) → new entity for INSERT
 * mapper.updateEntity(entity, request) → patch existing entity for UPDATE
 * mapper.toResponse(entity) → entity → response DTO
 *
 * Every public method wraps its body in a try-catch so that unexpected runtime
 * failures are caught, logged, and re-thrown with a clean message.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReconExecScheduleConfigServiceImpl implements ReconExecScheduleConfigService {

	private final ReconExecScheduleConfigRepository scheduleRepo;
	private final ReconExecScheduleConfigMapper scheduleMapper;

	// =========================================================================
	// SAVE (upsert)
	// =========================================================================

	@Override
	public ScheduleConfigResponse saveScheduleConfig(Long templateId, ScheduleConfigRequest request) {
		try {
			log.info("Saving schedule config for templateId={}", templateId);

			// Business rule: cronExpression is required when scheduleType = CRON
			if ("CRON".equalsIgnoreCase(request.getScheduleType())
					&& !StringUtils.hasText(request.getCronExpression())) {
				throw new IllegalArgumentException("cronExpression is required when scheduleType is CRON");
			}

			ReconExecScheduleConfig config;

			if (scheduleRepo.existsByTemplateId(templateId)) {
				// UPDATE path — load existing row and patch only non-null fields
				config = scheduleRepo.findByTemplateId(templateId).orElseThrow(
						() -> new RuntimeException("Schedule config not found for template: " + templateId));
				scheduleMapper.updateEntity(config, request);
			} else {
				// INSERT path — build a brand new entity from the request
				config = scheduleMapper.toEntity(templateId, request);
			}

			ReconExecScheduleConfig saved = scheduleRepo.save(config);
			log.info("Schedule config saved — scheduleId={}, templateId={}", saved.getScheduleId(), templateId);

			return scheduleMapper.toResponse(saved);

		} catch (IllegalArgumentException e) {
			log.warn("Validation error saving schedule config for templateId={}: {}", templateId, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Error saving schedule config for templateId={}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Failed to save schedule config for template: " + templateId, e);
		}
	}

	// =========================================================================
	// GET BY TEMPLATE ID
	// =========================================================================

	@Override
	@Transactional(readOnly = true)
	public ScheduleConfigResponse getScheduleConfig(Long templateId) {
		try {
			log.info("Fetching schedule config for templateId={}", templateId);

			ReconExecScheduleConfig config = scheduleRepo.findByTemplateId(templateId)
					.orElseThrow(() -> new RuntimeException("Schedule config not found for template: " + templateId));

			return scheduleMapper.toResponse(config);

		} catch (RuntimeException e) {
			log.warn("Schedule config not found for templateId={}: {}", templateId, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Error fetching schedule config for templateId={}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Failed to fetch schedule config for template: " + templateId, e);
		}
	}

	// =========================================================================
	// ACTIVATE
	// =========================================================================

	@Override
	public void activateSchedule(Long templateId) {
		try {
			log.info("Activating schedule for templateId={}", templateId);

			int updated = scheduleRepo.activateByTemplateId(templateId);
			if (updated == 0) {
				throw new RuntimeException("No schedule config found to activate for template: " + templateId);
			}

			log.info("Schedule activated for templateId={}", templateId);

		} catch (RuntimeException e) {
			log.warn("Activate schedule failed for templateId={}: {}", templateId, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Error activating schedule for templateId={}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Failed to activate schedule for template: " + templateId, e);
		}
	}

	// =========================================================================
	// DEACTIVATE
	// =========================================================================

	@Override
	public void deactivateSchedule(Long templateId) {
		try {
			log.info("Deactivating schedule for templateId={}", templateId);

			int updated = scheduleRepo.deactivateByTemplateId(templateId);
			if (updated == 0) {
				throw new RuntimeException("No schedule config found to deactivate for template: " + templateId);
			}

			log.info("Schedule deactivated for templateId={}", templateId);

		} catch (RuntimeException e) {
			log.warn("Deactivate schedule failed for templateId={}: {}", templateId, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Error deactivating schedule for templateId={}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Failed to deactivate schedule for template: " + templateId, e);
		}
	}

	// =========================================================================
	// DELETE
	// =========================================================================

	@Override
	public void deleteScheduleConfig(Long templateId) {
		try {
			log.info("Deleting schedule config for templateId={}", templateId);

			if (!scheduleRepo.existsByTemplateId(templateId)) {
				throw new RuntimeException("No schedule config found to delete for template: " + templateId);
			}

			scheduleRepo.deleteByTemplateId(templateId);
			log.info("Schedule config deleted for templateId={}", templateId);

		} catch (RuntimeException e) {
			log.warn("Delete schedule config failed for templateId={}: {}", templateId, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Error deleting schedule config for templateId={}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Failed to delete schedule config for template: " + templateId, e);
		}
	}
}
