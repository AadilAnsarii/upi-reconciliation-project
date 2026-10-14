package com.jpb.reconciliation.reconciliation.mapper;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplatesDetailsDTO;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;

public class ReconTemplateDetailsMapper {

	public static ReconTemplateDetails mapToReconTemplateDetails(ReconTemplateDetailsDto reconTemplateDetailsDto,
			ReconTemplateDetails reconTemplateDetails) {

//		reconTemplateDetails.setInsertCode(reconTemplateDetailsDto.getInsertCode());
//		reconTemplateDetails.setInsertUser(reconTemplateDetailsDto.getInsertUser());
//		reconTemplateDetails.setReconColumnCnt(reconTemplateDetailsDto.getReconColumnCnt());
//		reconTemplateDetails.setReconDataTableInd(reconTemplateDetailsDto.getReconDataTableInd());
//		reconTemplateDetails.setReconExistFlag(reconTemplateDetailsDto.getReconExistFlag());
//		reconTemplateDetails.setReconInsertDate(reconTemplateDetailsDto.getReconInsertDate());
//		reconTemplateDetails.setReconIssacqFlag(reconTemplateDetailsDto.getReconIssacqFlag());
//		reconTemplateDetails.setReconLastUpdatedDate(reconTemplateDetailsDto.getReconLastUpdatedDate());
//		reconTemplateDetails.setReconLastUpdatedUser(reconTemplateDetailsDto.getReconLastUpdatedUser());
//		reconTemplateDetails.setReconMasterFlag(reconTemplateDetailsDto.getReconMasterFlag());
//		reconTemplateDetails.setReconMasterTemplateId(reconTemplateDetailsDto.getReconMasterTemplateId());
//		reconTemplateDetails.setReconOnlRefFlag(reconTemplateDetailsDto.getReconOnlRefFlag());
//		reconTemplateDetails.setReconRefFlag(reconTemplateDetailsDto.getReconRefFlag());
//		reconTemplateDetails.setReconReversalInd(reconTemplateDetailsDto.getReconReversalInd());
//		reconTemplateDetails.setReconStageTabName(reconTemplateDetailsDto.getReconStageTabName());
//		reconTemplateDetails.setReconTemplateId(reconTemplateDetailsDto.getReconTemplateId());
//		reconTemplateDetails.setReconTemplateName(reconTemplateDetailsDto.getReconTemplateName());
//		reconTemplateDetails.setReconTypeId(reconTemplateDetailsDto.getReconTypeId());
//		reconTemplateDetails.setSubTemplateId(reconTemplateDetailsDto.getSubTemplateId());
		return reconTemplateDetails;
	}

	public static ReconTemplateDetails mapTemplateDtoToTemplate(TemplateFieldDto templateFieldrequest,
			ReconTemplateDetails templateNew) {
		templateNew.setTemplateType(templateFieldrequest.getTemplateType());
		templateNew.setTemplateName(templateFieldrequest.getTemplateName());
		templateNew.setColumnCount(templateFieldrequest.getColumnCount());
		templateNew.setReversalIndicator(templateFieldrequest.getReversalIndicator());
		templateNew.setDataReferenceFlag(templateFieldrequest.getDataReference());
		templateNew.setOnlRefundFlag(templateFieldrequest.getOnlineRefund());
		templateNew.setSubTemplateId(1L);

		templateNew.setInsertDate(new Date());
		templateNew.setInsertUser(1L);
		templateNew.setInsertCode(1L);

		return templateNew;
	}

	public static ReconTemplatesDetailsDTO toDTO(ReconTemplateDetails entity) {
		if (entity == null) {
			return null;
		}

		ReconTemplatesDetailsDTO dto = ReconTemplatesDetailsDTO.builder().reconTemplateId(entity.getReconTemplateId())
				.subTemplateId(entity.getSubTemplateId()).typeId(entity.getTypeId())
				.templateType(entity.getTemplateType()).templateName(entity.getTemplateName())
				.stageTabName(entity.getStageTabName()).columnCount(entity.getColumnCount())
				.existFlag(entity.getExistFlag()).reversalIndicator(entity.getReversalIndicator())
				.dataReferenceFlag(entity.getDataReferenceFlag()).onlRefundFlag(entity.getOnlRefundFlag())
				.issacqFlag(entity.getIssacqFlag()).dataTableInd(entity.getDataTableInd())
				.masterFlag(entity.getMasterFlag()).masterTemplateId(entity.getMasterTemplateId())
				.settlementFlag(entity.getSettlementFlag()).productType(entity.getProductType())
				.insertCode(entity.getInsertCode()).insertUser(entity.getInsertUser())
				.insertDate(entity.getInsertDate()).lastUpdatedUser(entity.getLastUpdatedUser())
				.reconLastUpdatedDate(entity.getReconLastUpdatedDate())
				.templateType(entity.getTemplateType())
				.build();

		// Map ONLY field details (excluding file details)
		if (entity.getFieldDetails() != null && !entity.getFieldDetails().isEmpty()) {
			dto.setFieldDetails(ReconFieldDetailsMasterMapper.toDTOSet(entity.getFieldDetails()));
		}

		return dto;
	}

	public static List<ReconTemplatesDetailsDTO> toDTOList(List<ReconTemplateDetails> entities) {
		if (entities == null || entities.isEmpty()) {
			return Collections.emptyList();
		}

		return entities.stream().map(ReconTemplateDetailsMapper::toDTO).collect(Collectors.toList());
	}

	public static Set<ReconTemplatesDetailsDTO> toDTOSet(Set<ReconTemplateDetails> entities) {
		if (entities == null || entities.isEmpty()) {
			return Collections.emptySet();
		}

		return entities.stream().map(ReconTemplateDetailsMapper::toDTO).collect(Collectors.toSet());
	}

	public static ReconTemplateDetails toEntity(ReconTemplatesDetailsDTO dto) {
		if (dto == null) {
			return null;
		}

		ReconTemplateDetails entity = new ReconTemplateDetails();
		entity.setReconTemplateId(dto.getReconTemplateId());
		entity.setSubTemplateId(dto.getSubTemplateId());
		entity.setTypeId(dto.getTypeId());
		entity.setTemplateType(dto.getTemplateType());
		entity.setTemplateName(dto.getTemplateName());
		entity.setStageTabName(dto.getStageTabName());
		entity.setColumnCount(dto.getColumnCount());
		entity.setExistFlag(dto.getExistFlag());
		entity.setReversalIndicator(dto.getReversalIndicator());
		entity.setDataReferenceFlag(dto.getDataReferenceFlag());
		entity.setOnlRefundFlag(dto.getOnlRefundFlag());
		entity.setIssacqFlag(dto.getIssacqFlag());
		entity.setDataTableInd(dto.getDataTableInd());
		entity.setMasterFlag(dto.getMasterFlag());
		entity.setMasterTemplateId(dto.getMasterTemplateId());
		entity.setSettlementFlag(dto.getSettlementFlag());
		entity.setProductType(dto.getProductType());
		entity.setInsertCode(dto.getInsertCode());
		entity.setInsertUser(dto.getInsertUser());
		entity.setInsertDate(dto.getInsertDate());
		entity.setLastUpdatedUser(dto.getLastUpdatedUser());
		entity.setReconLastUpdatedDate(dto.getReconLastUpdatedDate());

		return entity;
	}
}
