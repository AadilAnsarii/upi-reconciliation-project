package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.dto.ReconProcessRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconProcessResponse;
import com.jpb.reconciliation.reconciliation.entity.ProcessMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;

@Service
public class ReconProcessService {

	@Autowired
	private ReconProcessDefMasterRepository reconProcessRepository;

	@Autowired
	private ReconTemplateDetailsRepository reconTemplateRepository;

	@Autowired
	private ReconFileDetailsMasterRepository reconFileRepository;

	@Autowired
	private ProcessMasterRepository processMasterRepository;

	@Transactional
	public ReconProcessResponse createReconProcess(ReconProcessRequest request) {
		ReconProcessDefMaster entity = mapRequestToEntity(request);
		entity.setReconInsertDate(LocalDateTime.now());
		entity.setReconInsertUser(request.getInsUser());

		ReconProcessDefMaster savedEntity = reconProcessRepository.save(entity);

		return mapEntityToResponseWithFullDetails(savedEntity);
	}

	public ReconProcessResponse getReconProcessById(Long processId) {
		Optional<ReconProcessDefMaster> entity = reconProcessRepository.findById(processId);
		return entity.map(this::mapEntityToResponseWithFullDetails).orElse(null);
	}

	public List<ReconProcessResponse> getAllReconProcesses() {
		List<ReconProcessDefMaster> entities = reconProcessRepository.findAll();
		return entities.stream().map(this::mapEntityToResponseWithFullDetails).collect(Collectors.toList());
	}

	public List<ReconProcessResponse> getReconProcessesByBank(Long bnkCode) {
		List<ReconProcessDefMaster> entities = reconProcessRepository.findByReconInsertCode(bnkCode);
		return entities.stream().map(this::mapEntityToResponseWithFullDetails).collect(Collectors.toList());
	}

	public ReconProcessResponse getReconProcessByName(String processName) {
		Optional<ReconProcessDefMaster> entity = reconProcessRepository.findByReconProcessName(processName);
		return entity.map(this::mapEntityToResponseWithFullDetails).orElse(null);
	}

	@Transactional
	public ReconProcessResponse updateReconProcess(Long processId, ReconProcessRequest request) {
		Optional<ReconProcessDefMaster> existingEntity = reconProcessRepository.findById(processId);

		if (existingEntity.isPresent()) {
			ReconProcessDefMaster entity = existingEntity.get();
			updateEntityFromRequest(entity, request);
			entity.setReconLastUpdatedDate(LocalDateTime.now());
			entity.setReconLastUpdatedUser(request.getInsUser());

			ReconProcessDefMaster savedEntity = reconProcessRepository.save(entity);
			return mapEntityToResponseWithFullDetails(savedEntity);
		}

		return null;
	}

	@Transactional
	public void deleteReconProcess(Long processId) {
		reconProcessRepository.deleteById(processId);
	}

	// ========== PRIVATE MAPPING METHODS ==========

	private ReconProcessDefMaster mapRequestToEntity(ReconProcessRequest request) {
		ReconProcessDefMaster entity = new ReconProcessDefMaster();

		// Basic fields
		entity.setReconProcessName(request.getProcessName());
		entity.setReconInputCount(request.getInputCount());
		entity.setReconRetentionPeriod(request.getRetentionPeriod());
		entity.setReconRetentionVolume(request.getRetentionVolume());
		entity.setReconMatchingType(request.getMatchingType());
		entity.setReconProcessJPBRPSL(request.getJpsRpsl());
		entity.setReconMatchigFlag(request.getIdenticalMatching());

		// Additional fields
		entity.setReconInsertCode(request.getBankCode());
		entity.setReconInterChangePosition(request.getInchgPosition());
		entity.setReconInterChnageId(request.getInterchangeId());
		entity.setReconIssAcqFlag(request.getIssAcqFlag());
		entity.setReconManRecFlag(request.getManrecFlag());
		entity.setReconMasterTemp(request.getMastTemp());
		entity.setReconMenuFlag(request.getRecMenuFlag());
		entity.setReconTableType(request.getTableType());
		entity.setReconDisputeFlag(request.getDisputeFlag());
		entity.setReconEmailSMSFlag(request.getEmailSmsFlag());

		// Process Master ID - set via the relationship
		if (request.getProcessMastId() != null) {
			// Option 1: Fetch from repository (RECOMMENDED)
			Optional<ProcessMasterEntity> processMaster = processMasterRepository.findById(request.getProcessMastId());
			if (processMaster.isPresent()) {
				entity.setProcessmaster(processMaster.get());
			} else {
				// Option 2: Create new bnkance with just ID
				ProcessMasterEntity pm = new ProcessMasterEntity();
				pm.setProcessMastId(request.getProcessMastId());
				entity.setProcessmaster(pm);
			}
		}

		// Update file type mappings (Dynamic table + flag generation)
		if (request.getFileTypeMappings() != null) {

			for (ReconProcessRequest.FileTypeMapping mapping : request.getFileTypeMappings()) {

				String stageTableName = null;

				// Find matching template based on fileTypeNumber
				if (request.getTemplateMappings() != null) {
					for (ReconProcessRequest.TemplateMapping template : request.getTemplateMappings()) {

						if (template.getTemplateNumber() == mapping.getFileTypeNumber()) {
							stageTableName = template.getStageTabName();
							break;
						}
					}
				}

				String[] dynamicNames = generateDynamicNames(stageTableName, request.getTranChannel());
				String dataTableName = dynamicNames[0];
				String recFlagName = dynamicNames[1];

				switch (mapping.getFileTypeNumber()) {

				case 1:
					entity.setReconFileType1(mapping.getFileTypeId());
					entity.setReconDataTableName1(dataTableName);
					entity.setReconFlagName1(recFlagName);
					break;

				case 2:
					entity.setReconFileType2(mapping.getFileTypeId());
					entity.setReconDataTableName2(dataTableName);
					entity.setReconFlagName2(recFlagName);
					break;

				case 3:
					entity.setReconFileType3(mapping.getFileTypeId());
					entity.setReconDataTableName3(dataTableName);
					entity.setReconFlagName3(recFlagName);
					break;

				case 4:
					entity.setReconFileType4(mapping.getFileTypeId());
					entity.setReconDataTableName4(dataTableName);
					entity.setReconFlagName4(recFlagName);
					break;
				}
			}
		}

		// Map template mappings to temp fields - STORE ACTUAL TEMPLATE IDs
		if (request.getTemplateMappings() != null) {
			for (ReconProcessRequest.TemplateMapping mapping : request.getTemplateMappings()) {
				// Use actual template ID if provided, otherwise use template number
				Long templateIdToStore = (mapping.getTemplateId() != null) ? mapping.getTemplateId()
						: mapping.getTemplateNumber().longValue();

				switch (mapping.getTemplateNumber()) {
				case 1:
					entity.setReconTemp1(templateIdToStore);
					break;
				case 2:
					entity.setReconTemp2(templateIdToStore);
					break;
				case 3:
					entity.setReconTemp3(templateIdToStore);
					break;
				case 4:
					entity.setReconTemp4(templateIdToStore);
					break;
				}
			}
		}

		// Map matching fields
		if (request.getMatchingFields() != null) {
			for (ReconProcessRequest.MatchingFieldMapping mapping : request.getMatchingFields()) {
				String fieldValue = String.join(",", mapping.getSelectedFields());
				switch (mapping.getFieldNumber()) {
				case 1:
					entity.setReconMatchingField1(fieldValue);
					break;
				case 2:
					entity.setReconMatchingField2(fieldValue);
					break;
				case 3:
					entity.setReconMatchingField3(fieldValue);
					break;
				case 4:
					entity.setReconMatchingField4(fieldValue);
					break;
				}
			}
		}

		return entity;
	}

	private String[] generateDynamicNames(String stageTableName, String tranChannel) {

		if (stageTableName == null || stageTableName.isEmpty()) {
			return new String[] { null, null };
		}

		// Example: REC_EJA_ADM_STAGE_T

		// Remove REC_ prefix
		String withoutRec = stageTableName.replaceFirst("^REC_", "");

		// Remove _STAGE_T suffix
		String baseName = withoutRec.replace("_STAGE_T", "");

		// Generate required names
		String dataTableName = "REC_" + baseName + tranChannel + "_DATA";
		String recFlagName = "DYN_" + baseName + "_REC_FLAG";

		return new String[] { dataTableName, recFlagName };
	}

	private void updateEntityFromRequest(ReconProcessDefMaster entity, ReconProcessRequest request) {
		// Update basic fields
		entity.setReconProcessName(request.getProcessName());
		entity.setReconInputCount(request.getInputCount());
		entity.setReconRetentionPeriod(request.getRetentionPeriod());
		entity.setReconRetentionVolume(request.getRetentionVolume());
		entity.setReconMatchingType(request.getMatchingType());
		entity.setReconProcessJPBRPSL(request.getJpsRpsl());
		entity.setReconMatchigFlag(request.getIdenticalMatching());

		// Update additional fields
		entity.setReconInsertCode(request.getBankCode());
		entity.setReconInterChangePosition(request.getInchgPosition());
		entity.setReconInterChnageId(request.getInterchangeId());
		entity.setReconIssAcqFlag(request.getIssAcqFlag());
		entity.setReconManRecFlag(request.getManrecFlag());
		entity.setReconMasterTemp(request.getMastTemp());
		entity.setReconMenuFlag(request.getRecMenuFlag());
		entity.setReconTableType(request.getTableType());
		entity.setReconDisputeFlag(request.getDisputeFlag());
		entity.setReconEmailSMSFlag(request.getEmailSmsFlag());

		// Update file type mappings
		if (request.getFileTypeMappings() != null) {
			for (ReconProcessRequest.FileTypeMapping mapping : request.getFileTypeMappings()) {
				switch (mapping.getFileTypeNumber()) {
				case 1:
					entity.setReconFileType1(mapping.getFileTypeId());
					entity.setReconDataTableName1(mapping.getDataTableName());
					entity.setReconFlagName1(mapping.getRecFlagName());
					break;
				case 2:
					entity.setReconFileType2(mapping.getFileTypeId());
					entity.setReconDataTableName2(mapping.getDataTableName());
					entity.setReconFlagName2(mapping.getRecFlagName());
					break;
				case 3:
					entity.setReconFileType3(mapping.getFileTypeId());
					entity.setReconDataTableName3(mapping.getDataTableName());
					entity.setReconFlagName3(mapping.getRecFlagName());
					break;
				case 4:
					entity.setReconFileType4(mapping.getFileTypeId());
					entity.setReconDataTableName4(mapping.getDataTableName());
					entity.setReconFlagName4(mapping.getRecFlagName());
					break;
				}
			}
		}

		// Update template mappings
		if (request.getTemplateMappings() != null) {
			for (ReconProcessRequest.TemplateMapping mapping : request.getTemplateMappings()) {
				Long templateIdToStore = (mapping.getTemplateId() != null) ? mapping.getTemplateId()
						: mapping.getTemplateNumber().longValue();

				switch (mapping.getTemplateNumber()) {
				case 1:
					entity.setReconTemp1(templateIdToStore);
					break;
				case 2:
					entity.setReconTemp2(templateIdToStore);
					break;
				case 3:
					entity.setReconTemp3(templateIdToStore);
					break;
				case 4:
					entity.setReconTemp4(templateIdToStore);
					break;
				}
			}
		}

		// Update matching fields
		if (request.getMatchingFields() != null) {
			for (ReconProcessRequest.MatchingFieldMapping mapping : request.getMatchingFields()) {
				String fieldValue = String.join(",", mapping.getSelectedFields());
				switch (mapping.getFieldNumber()) {
				case 1:
					entity.setReconMatchingField1(fieldValue);
					break;
				case 2:
					entity.setReconMatchingField2(fieldValue);
					break;
				case 3:
					entity.setReconMatchingField3(fieldValue);
					break;
				case 4:
					entity.setReconMatchingField4(fieldValue);
					break;
				}
			}
		}
	}

	private ReconProcessResponse mapEntityToResponseWithFullDetails(ReconProcessDefMaster entity) {
		ReconProcessResponse response = ReconProcessResponse.builder().processId(entity.getReconProcessId())
				.processName(entity.getReconProcessName()).inputCount(entity.getReconInputCount())
				.retentionPeriod(entity.getReconRetentionPeriod()).retentionVolume(entity.getReconRetentionVolume())
				.matchingType(entity.getReconMatchingType()).jpsRpsl(entity.getReconProcessJPBRPSL())
				.identicalMatching(entity.getReconMatchigFlag()).bnkCode(entity.getReconInsertCode())
				.insDate(entity.getReconInsertDate()).insUser(entity.getReconInsertUser())
				.lupdDate(entity.getReconLastUpdatedDate()).lupdUser(entity.getReconLastUpdatedUser())
				.inchgPosition(entity.getReconInterChangePosition()).interchangeId(entity.getReconInterChnageId())
				.issAcqFlag(entity.getReconIssAcqFlag()).manrecFlag(entity.getReconManRecFlag())
				.mastTemp(entity.getReconMasterTemp()).matchingFlag(entity.getReconMatchigFlag())
				.recMenuFlag(entity.getReconMenuFlag()).tableType(entity.getReconTableType())
				.processMastId(entity.getProcessmaster() != null ? entity.getProcessmaster().getProcessMastId() : null)
				.reconDisputeFlag(entity.getReconDisputeFlag()).emailSmsFlag(entity.getReconEmailSMSFlag()).build();

		// Map file types WITH full details
		List<ReconProcessResponse.FileTypeInfo> fileTypes = new ArrayList<>();
		if (entity.getReconFileType1() != null) {
			fileTypes.add(buildFileTypeInfoWithDetails(1, entity.getReconFileType1(), entity.getReconDataTableName1(),
					entity.getReconFlagName1()));
		}
		if (entity.getReconFileType2() != null) {
			fileTypes.add(buildFileTypeInfoWithDetails(2, entity.getReconFileType2(), entity.getReconDataTableName2(),
					entity.getReconFlagName2()));
		}
		if (entity.getReconFileType3() != null) {
			fileTypes.add(buildFileTypeInfoWithDetails(3, entity.getReconFileType3(), entity.getReconDataTableName3(),
					entity.getReconFlagName3()));
		}
		if (entity.getReconFileType4() != null) {
			fileTypes.add(buildFileTypeInfoWithDetails(4, entity.getReconFileType4(), entity.getReconDataTableName4(),
					entity.getReconFlagName4()));
		}
		response.setFileTypes(fileTypes);

		// Map templates WITH full details
		List<ReconProcessResponse.TemplateInfo> templates = new ArrayList<>();
		if (entity.getReconTemp1() != null) {
			ReconProcessResponse.TemplateInfo templateInfo = buildTemplateInfo(1, entity.getReconTemp1());
			if (templateInfo != null) {
				templates.add(templateInfo);
			}
		}
		if (entity.getReconTemp2() != null) {
			ReconProcessResponse.TemplateInfo templateInfo = buildTemplateInfo(2, entity.getReconTemp2());
			if (templateInfo != null) {
				templates.add(templateInfo);
			}
		}
		if (entity.getReconTemp3() != null) {
			ReconProcessResponse.TemplateInfo templateInfo = buildTemplateInfo(3, entity.getReconTemp3());
			if (templateInfo != null) {
				templates.add(templateInfo);
			}
		}
		if (entity.getReconTemp4() != null) {
			ReconProcessResponse.TemplateInfo templateInfo = buildTemplateInfo(4, entity.getReconTemp4());
			if (templateInfo != null) {
				templates.add(templateInfo);
			}
		}
		response.setTemplates(templates);

		// Map matching fields
		List<ReconProcessResponse.MatchingFieldInfo> matchingFields = new ArrayList<>();
		if (entity.getReconMatchingField1() != null && !entity.getReconMatchingField1().isEmpty()) {
			matchingFields.add(buildMatchingFieldInfo(1, entity.getReconMatchingField1()));
		}
		if (entity.getReconMatchingField2() != null && !entity.getReconMatchingField2().isEmpty()) {
			matchingFields.add(buildMatchingFieldInfo(2, entity.getReconMatchingField2()));
		}
		if (entity.getReconMatchingField3() != null && !entity.getReconMatchingField3().isEmpty()) {
			matchingFields.add(buildMatchingFieldInfo(3, entity.getReconMatchingField3()));
		}
		if (entity.getReconMatchingField4() != null && !entity.getReconMatchingField4().isEmpty()) {
			matchingFields.add(buildMatchingFieldInfo(4, entity.getReconMatchingField4()));
		}
		response.setMatchingFields(matchingFields);

		return response;
	}

	private ReconProcessResponse.FileTypeInfo buildFileTypeInfoWithDetails(Integer number, Long fileTypeId,
			String dataTableName, String recFlagName) {

		ReconProcessResponse.FileDetailInfo fileDetails = null;
		String fileTypeName = null;

		if (fileTypeId != null) {
			Optional<ReconFileDetailsMaster> fileEntity = reconFileRepository.findById(fileTypeId);
			if (fileEntity.isPresent()) {
				ReconFileDetailsMaster file = fileEntity.get();
				fileTypeName = file.getReconShortName(); // or getReconFileName()

				// Fetch template name if template ID exists
				String templateName = null;
				if (file.getReconTemplateDetails().getReconTemplateId() != null) {
					Optional<ReconTemplateDetails> templateEntity = reconTemplateRepository
							.findById(file.getReconTemplateDetails().getReconTemplateId());
					if (templateEntity.isPresent()) {
						templateName = templateEntity.get().getTemplateName();
					}
				}

				fileDetails = ReconProcessResponse.FileDetailInfo.builder().fileId(file.getReconFileId())
						.fileName(file.getReconFileName()).shortName(file.getReconShortName())
						.fileType(file.getReconFileType()).fileDescription(file.getReconFileDescription())
						.fileLocation(file.getReconFileLocation()).fileDelimiter(file.getReconFileDelimiter())
						.hdrAvlFlag(file.getReconHdrAvailableFlag()).ftrAvailFlag(file.getReconFtrAvailFlag())
						.templateId(file.getReconTemplateDetails().getReconTemplateId()).templateName(templateName)
						.build();
			}
		}

		return ReconProcessResponse.FileTypeInfo.builder().fileTypeNumber(number).fileTypeId(fileTypeId)
				.fileTypeName(fileTypeName).dataTableName(dataTableName).recFlagName(recFlagName)
				.fileDetails(fileDetails).build();
	}

	private ReconProcessResponse.TemplateInfo buildTemplateInfo(Integer number, Long templateId) {
		if (templateId == null) {
			return null;
		}

		Optional<ReconTemplateDetails> templateEntity = reconTemplateRepository.findById(templateId);
		if (templateEntity.isPresent()) {
			ReconTemplateDetails template = templateEntity.get();

			return ReconProcessResponse.TemplateInfo.builder().templateNumber(number)
					.templateId(template.getReconTemplateId()).templateName(template.getTemplateName())
					.stagingTableName(template.getStageTabName()).columnCount(template.getColumnCount())
					.templateType(template.getTemplateType()).build();
		}

		return null;
	}

	private ReconProcessResponse.MatchingFieldInfo buildMatchingFieldInfo(Integer number, String matchingFieldValue) {
		List<String> selectedFields = new ArrayList<>();
		if (matchingFieldValue != null && !matchingFieldValue.isEmpty()) {
			selectedFields = Arrays.asList(matchingFieldValue.split(","));
		}

		return ReconProcessResponse.MatchingFieldInfo.builder().fieldNumber(number).matchingField(matchingFieldValue)
				.selectedFields(selectedFields).build();
	}
}