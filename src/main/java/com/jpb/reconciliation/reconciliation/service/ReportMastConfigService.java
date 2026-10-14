package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.FieldDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.FileDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.ReconProcessWithFilesDto;
import com.jpb.reconciliation.reconciliation.dto.ReportMastConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.TemplateWithFileAndFieldsDto;
import com.jpb.reconciliation.reconciliation.entity.ExceptionReconReportEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.mapper.ReportConfigMapper;
import com.jpb.reconciliation.reconciliation.repository.ExceptionReconReportRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMastConfigService {

	private final ReconTemplateDetailsRepository templateRepo;
	private final ReconFileDetailsMasterRepository fileRepo;
	private final ReconFieldDetailsMasterRepository fieldRepo;
	private final ReconProcessDefMasterRepository processRepo;
	private final ExceptionReconReportRepository reportRepo;

	@Autowired
	private ReportConfigMapper reportConfigMapper;

	private static final String SUCCESS = CommonConstants.SUCCESS;
	private static final String ERROR = "ERROR";

	// ══════════════════════════════════════════════════════════════════════════════
	// CRUD — ExceptionReconReportEntity
	// ══════════════════════════════════════════════════════════════════════════════

	public ResponseEntity<RestWithStatusList> createReportConfig(ReportMastConfigRequest req) {
		try {
			ExceptionReconReportEntity entity = mapToEntity(req);

			if (isNullOrEmpty(entity.getReportQuery())) {
				entity.setReportQuery(buildReportQuery(req));
			}

			if (isNullOrEmpty(entity.getReportHeader())) {
				entity.setReportHeader(buildReportHeader(req));
			}

			ExceptionReconReportEntity saved = reportRepo.save(entity);
			log.info("Report config created, ID: {}", saved.getReportId());
			List<Object> dataList = new ArrayList<>();
			dataList.add(saved);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ok("Report config created successfully", dataList));
		} catch (Exception e) {
			log.error("createReportConfig error: {}", e.getMessage(), e);
			return error("Failed to create report config: " + e.getMessage());
		}
	}

	public ResponseEntity<RestWithStatusList> getAllReportConfigs() {
		try {
			List<ExceptionReconReportEntity> list = reportRepo.findAll();
			List<Object> dataList = list.stream()
					.map(reportConfigMapper::toDTO)
					.collect(Collectors.toList());
			return ResponseEntity.ok(
					ok("Request executed successfully | Total: " + list.size(), dataList));
		} catch (Exception e) {
			log.error("getAllReportConfigs error: {}", e.getMessage(), e);
			return error("Failed to fetch report configs: " + e.getMessage());
		}
	}

	public ResponseEntity<RestWithStatusList> getReportConfigById(Long reportId) {
		try {
			ExceptionReconReportEntity entity = reportRepo.findByReportId(reportId);
			if (entity == null)
				return notFound("Report config not found for ID: " + reportId);
			List<Object> dataList = new ArrayList<>();
			dataList.add(entity);
			return ResponseEntity.ok(ok("Report config fetched successfully", dataList));
		} catch (Exception e) {
			log.error("getReportConfigById error: {}", e.getMessage(), e);
			return error("Failed to fetch report config: " + e.getMessage());
		}
	}

	public ResponseEntity<RestWithStatusList> updateReportConfig(
			Long reportId, ReportMastConfigRequest req) {
		if (reportRepo.findByReportId(reportId) == null)
			return notFound("Report config not found for ID: " + reportId);
		try {
			ExceptionReconReportEntity updated = mapToEntity(req);
			updated.setReportId(reportId);

			if (isNullOrEmpty(updated.getReportQuery())) {
				updated.setReportQuery(buildReportQuery(req));
			}

			if (isNullOrEmpty(updated.getReportHeader())) {
				updated.setReportHeader(buildReportHeader(req));
			}

			ExceptionReconReportEntity saved = reportRepo.save(updated);
			List<Object> dataList = new ArrayList<>();
			dataList.add(saved);
			return ResponseEntity.ok(ok("Report config updated successfully", dataList));
		} catch (Exception e) {
			log.error("updateReportConfig error: {}", e.getMessage(), e);
			return error("Failed to update report config: " + e.getMessage());
		}
	}

	public ResponseEntity<RestWithStatusList> deleteReportConfig(Long reportId) {
		if (reportRepo.findByReportId(reportId) == null)
			return notFound("Report config not found for ID: " + reportId);
		reportRepo.deleteById(reportId);
		log.info("Report config deleted, ID: {}", reportId);
		return ResponseEntity.ok(ok("Report config deleted successfully", new ArrayList<>()));
	}

	public ResponseEntity<RestWithStatusList> searchReportConfigs(
			String reportName, String processId) {
		try {
			List<ExceptionReconReportEntity> result =
					reportRepo.searchByNameAndProcess(reportName, processId);
			List<Object> dataList = new ArrayList<>(result);
			return ResponseEntity.ok(
					ok("Request executed successfully | Total: " + result.size(), dataList));
		} catch (Exception e) {
			log.error("searchReportConfigs error: {}", e.getMessage(), e);
			return error("Search failed: " + e.getMessage());
		}
	}

	// ══════════════════════════════════════════════════════════════════════════════
	// EXTRACTION FLOW — ALL templates
	// ══════════════════════════════════════════════════════════════════════════════

	public ResponseEntity<RestWithStatusList> getAllExtractionTemplatesWithFields() {
		try {
			List<ReconTemplateDetails> allTemplates = templateRepo.findAllTemplates();

			if (allTemplates == null || allTemplates.isEmpty()) {
				return ResponseEntity.ok(ok("No templates found", new ArrayList<>()));
			}

			List<Object> data = new ArrayList<>();

			for (ReconTemplateDetails template : allTemplates) {

				if (template == null) {
					log.warn("Skipping null template entry in findAllTemplates result");
					continue;
				}

				Long templateId = template.getReconTemplateId();
				log.debug("Processing templateId: {}", templateId);

				// ── Fetch all files for this template ────────────────────────
				List<ReconFileDetailsMaster> files = new ArrayList<>();
				try {
					files = fileRepo
							.findAllByReconTemplateDetails_ReconTemplateId(templateId);
				} catch (Exception ex) {
					log.warn("Could not fetch files for templateId {}: {}",
							templateId, ex.getMessage());
				}

				// ── Fetch fields and deduplicate by reconFieldId ─────────────
				List<ReconFieldDetailsMaster> fields = new ArrayList<>();
				try {
					List<ReconFieldDetailsMaster> rawFields =
							fieldRepo.findFullFieldDetailsByTemplateId(templateId);

					if (rawFields != null && !rawFields.isEmpty()) {

						// Deduplicate by reconFieldId preserving insertion order
						LinkedHashMap<Long, ReconFieldDetailsMaster> dedupMap =
								new LinkedHashMap<>();

						for (ReconFieldDetailsMaster f : rawFields) {
							if (f != null && f.getReconFieldId() != null) {
								dedupMap.putIfAbsent(f.getReconFieldId(), f);
							}
						}

						fields = new ArrayList<>(dedupMap.values());

						log.debug("templateId={} | rawFields={} | afterDedup={}",
								templateId, rawFields.size(), fields.size());
					}

				} catch (Exception ex) {
					log.warn("Could not fetch fields for templateId {}: {}",
							templateId, ex.getMessage());
				}

				// ── Build response entries ────────────────────────────────────
				if (files == null || files.isEmpty()) {
					// No linked file — one entry with null fileDetails
					data.add(buildEntry(template, null, fields));
				} else {
					// One entry per file under this template
					for (ReconFileDetailsMaster file : files) {
						data.add(buildEntry(template, file, fields));
					}
				}
			}

			return ResponseEntity.ok(
					ok("Request executed successfully | Total entries: " + data.size(), data));

		} catch (Exception e) {
			log.error("getAllExtractionTemplatesWithFields error: {}", e.getMessage(), e);
			return error("Failed to fetch extraction templates: " + e.getMessage());
		}
	}

	// ══════════════════════════════════════════════════════════════════════════════
	// RECONCILIATION FLOW — ALL processes (supports up to 4 templates per process)
	// ══════════════════════════════════════════════════════════════════════════════

	public ResponseEntity<RestWithStatusList> getAllReconProcessesWithFiles() {
		try {
			List<ReconProcessDefMaster> allProcesses = processRepo.findAll();

			if (allProcesses == null || allProcesses.isEmpty()) {
				return ResponseEntity.ok(ok("No recon processes found", new ArrayList<>()));
			}

			List<Object> data = new ArrayList<>();

			for (ReconProcessDefMaster process : allProcesses) {
				if (process == null)
					continue;

				data.add(ReconProcessWithFilesDto.builder()
						.processId(process.getReconProcessId())
						.processName(process.getReconProcessName())
						.file1(buildFromTemplate(process.getReconTemp1()))
						.file2(buildFromTemplate(process.getReconTemp2()))
						.file3(buildFromTemplate(process.getReconTemp3()))
						.file4(buildFromTemplate(process.getReconTemp4()))
						.build());
			}

			return ResponseEntity.ok(
					ok("Request executed successfully | Total processes: " + data.size(), data));

		} catch (Exception e) {
			log.error("getAllReconProcessesWithFiles error: {}", e.getMessage(), e);
			return error("Failed to fetch recon processes: " + e.getMessage());
		}
	}

	// ══════════════════════════════════════════════════════════════════════════════
	// PRIVATE HELPERS
	// ══════════════════════════════════════════════════════════════════════════════

	/**
	 * Loads template + file + fields for a single templateId.
	 * Returns null if templateId is null.
	 */
	private TemplateWithFileAndFieldsDto buildFromTemplate(Long templateId) {
		if (templateId == null)
			return null;
		try {
			ReconTemplateDetails template =
					templateRepo.findByReconTemplateId(templateId);

			List<ReconFileDetailsMaster> files =
					fileRepo.findAllByReconTemplateDetails_ReconTemplateId(templateId);
			ReconFileDetailsMaster file =
					(files != null && !files.isEmpty()) ? files.get(0) : null;

			List<ReconFieldDetailsMaster> rawFields =
					fieldRepo.findFullFieldDetailsByTemplateId(templateId);

			// Deduplicate by reconFieldId preserving insertion order
			List<ReconFieldDetailsMaster> fields = new ArrayList<>();
			if (rawFields != null && !rawFields.isEmpty()) {
				LinkedHashMap<Long, ReconFieldDetailsMaster> dedupMap =
						new LinkedHashMap<>();
				for (ReconFieldDetailsMaster f : rawFields) {
					if (f != null && f.getReconFieldId() != null) {
						dedupMap.putIfAbsent(f.getReconFieldId(), f);
					}
				}
				fields = new ArrayList<>(dedupMap.values());
			}

			return buildEntry(template, file, fields);

		} catch (Exception ex) {
			log.warn("Could not build file entry for templateId {}: {}",
					templateId, ex.getMessage());
			return null;
		}
	}

	/**
	 * Builds one TemplateWithFileAndFieldsDto.
	 * Every field access is null-safe — no NPE possible.
	 */
	private TemplateWithFileAndFieldsDto buildEntry(
			ReconTemplateDetails template,
			ReconFileDetailsMaster file,
			List<ReconFieldDetailsMaster> fields) {

		// ── templateDetails ──────────────────────────────────────────────────
		TemplateDetailsDto templateDetails = null;
		if (template != null) {
			templateDetails = TemplateDetailsDto.builder()
					.templateId(template.getReconTemplateId())
					.templateName(template.getTemplateName())
					.templateType(template.getTemplateType())
					.stageTableName(template.getStageTabName())
					.build();
		}

		// ── fileDetails ──────────────────────────────────────────────────────
		FileDetailsDto fileDetails = null;
		if (file != null) {
			fileDetails = FileDetailsDto.builder()
					.fileId(safeFileId(file))
					.fileName(file.getReconFileName())
					.fileType(file.getReconFileType())
					.build();
		}

		// ── fieldDetails — only field_id + field_name ────────────────────────
		List<FieldDetailsDto> fieldDetails = new ArrayList<>();
		if (fields != null) {
			for (ReconFieldDetailsMaster f : fields) {
				if (f == null)
					continue;
				fieldDetails.add(FieldDetailsDto.builder()
						.fieldId(f.getReconFieldId())
						.fieldName(f.getReconTabFieldName())
						.build());
			}
		}

		return TemplateWithFileAndFieldsDto.builder()
				.templateDetails(templateDetails)
				.fileDetails(fileDetails)
				.fieldDetails(fieldDetails)
				.build();
	}

	// Null-safe file ID accessor
	private Long safeFileId(ReconFileDetailsMaster file) {
		try {
			return file.getReconFileId();
		} catch (Exception e) {
			return null;
		}
	}

	private ExceptionReconReportEntity mapToEntity(ReportMastConfigRequest req) {
		ExceptionReconReportEntity e = new ExceptionReconReportEntity();
		e.setFileName(req.getFileName());
		e.setProcessId(req.getProcessId());
		e.setReportDate(LocalDate.now());
		e.setReportKey(req.getReportKey());
		e.setReportQuery(req.getReportQuery());

		if (isNullOrEmpty(req.getReportHeader())) {
			e.setReportHeader(buildReportHeader(req));
		} else {
			e.setReportHeader(req.getReportHeader());
		}

		return e;
	}

	private String buildReportHeader(ReportMastConfigRequest req) {
		if (req.getSelectedColumns() == null || req.getSelectedColumns().isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append("select '");
		List<String> cols = req.getSelectedColumns();
		for (int i = 0; i < cols.size(); i++) {
			sb.append(cols.get(i));
			if (i < cols.size() - 1)
				sb.append("~");
		}
		sb.append("' HEADER from dual");
		return sb.toString();
	}

	private String buildReportQuery(ReportMastConfigRequest req) {
		if (req.getSelectedColumns() == null || req.getSelectedColumns().isEmpty())
			return "";

		StringBuilder colExpr = new StringBuilder();
		List<String> cols = req.getSelectedColumns();
		for (int i = 0; i < cols.size(); i++) {
			colExpr.append("NVL(").append(cols.get(i)).append(", 0)");
			if (i < cols.size() - 1)
				colExpr.append(" || '~' || \n         ");
		}

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ").append(colExpr).append("\n  AS DATA\n");
		sb.append("FROM ").append(req.getSelectedFile());

		if (req.getWhereConditions() != null && !req.getWhereConditions().isEmpty()) {
			sb.append("\nWHERE ");
			List<ReportMastConfigRequest.WhereCondition> conditions =
					req.getWhereConditions();
			for (int i = 0; i < conditions.size(); i++) {
				ReportMastConfigRequest.WhereCondition wc = conditions.get(i);
				sb.append(wc.getColumn())
				  .append(" ").append(wc.getOperator())
				  .append(" '").append(wc.getValue()).append("'");
				if (i < conditions.size() - 1 && wc.getLogicalOp() != null) {
					sb.append("\n  ").append(wc.getLogicalOp()).append(" ");
				}
			}
		}
		return sb.toString();
	}

	private boolean isNullOrEmpty(String value) {
		return value == null || value.trim().isEmpty();
	}

	private RestWithStatusList ok(String msg, List<Object> data) {
		return RestWithStatusList.builder()
				.status(SUCCESS)
				.statusMsg(msg)
				.data(data)
				.build();
	}

	private ResponseEntity<RestWithStatusList> error(String msg) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(RestWithStatusList.builder()
						.status(ERROR)
						.statusMsg(msg)
						.data(new ArrayList<>())
						.build());
	}

	private ResponseEntity<RestWithStatusList> notFound(String msg) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(RestWithStatusList.builder()
						.status(ERROR)
						.statusMsg(msg)
						.data(new ArrayList<>())
						.build());
	}
}