package com.jpb.reconciliation.reconciliation.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.jpb.reconciliation.reconciliation.dto.ReconFieldDetailsMasterDTO;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;

public class ReconFieldDetailsMasterMapper {
    
    private ReconFieldDetailsMasterMapper() {
        // Private constructor to prevent bnkantiation
    }
    
    public static ReconFieldDetailsMasterDTO toDTO(ReconFieldDetailsMaster entity) {
        if (entity == null) {
            return null;
        }
        
        return ReconFieldDetailsMasterDTO.builder()
            .reconFieldId(entity.getReconFieldId())
            .reconColumnPosn(entity.getReconColumnPosn())
            .reconSubTempId(entity.getReconSubTempId())
            .reconShortName(entity.getReconShortName())
            .reconTabFieldName(entity.getReconTabFieldName())
            .reconFromPosition(entity.getReconFromPosition())
            .reconToPosition(entity.getReconToPosition())
            .reconMaxLength(entity.getReconMaxLength())
            .reconKeyIdentifier(entity.getReconKeyIdentifier())
            .reconColumnOffset(entity.getReconColumnOffset())
            .reconMandtoryFlag(entity.getReconMandtoryFlag())
            .reconInserCode(entity.getReconInserCode())
            .reconInsertUser(entity.getReconInsertUser())
            .reconInsertDate(entity.getReconInsertDate())
            .reconLastUpdatedUser(entity.getReconLastUpdatedUser())
            .reconLastUpdatedDate(entity.getReconLastUpdatedDate())
            .reconRankIdentifier(entity.getReconRankIdentifier())
            .reconAlterFlag(entity.getReconAlterFlag())
            .reconDisplayedFlag(entity.getReconDisplayedFlag())
            .reconReportFieldFlag(entity.getReconReportFieldFlag())
            .reconMatchingFieldFlag(entity.getReconMatchingFieldFlag())
            .reconFromPosn(entity.getReconFromPosn())
            .reconToPosn(entity.getReconToPosn())
            .reconInstanceCode(entity.getReconInstanceCode())
            .reconMandatoryFlag(entity.getReconMandatoryFlag())
            .reconToPositionDescription(entity.getReconToPositionDescription())
            .reconTemplateId(entity.getReconTemplateDetails() != null ? 
                entity.getReconTemplateDetails().getReconTemplateId() : null)
            .fieldTypeName(entity.getReconFieldTypeMaster() != null ? 
                entity.getReconFieldTypeMaster().getFieldTypeDes() : null)
            .fieldFormatName(entity.getReconFieldFormatMaster() != null ? 
                entity.getReconFieldFormatMaster().getReconFieldFormatDesc() : null)
            .build();
    }
    
    public static List<ReconFieldDetailsMasterDTO> toDTOList(List<ReconFieldDetailsMaster> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        return entities.stream()
            .map(ReconFieldDetailsMasterMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    public static Set<ReconFieldDetailsMasterDTO> toDTOSet(Set<ReconFieldDetailsMaster> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptySet();
        }
        
        return entities.stream()
            .map(ReconFieldDetailsMasterMapper::toDTO)
            .collect(Collectors.toSet());
    }
    
    public static ReconFieldDetailsMaster toEntity(ReconFieldDetailsMasterDTO dto) {
        if (dto == null) {
            return null;
        }
        
        ReconFieldDetailsMaster entity = new ReconFieldDetailsMaster();
        entity.setReconFieldId(dto.getReconFieldId());
        entity.setReconColumnPosn(dto.getReconColumnPosn());
        entity.setReconSubTempId(dto.getReconSubTempId());
        entity.setReconShortName(dto.getReconShortName());
        entity.setReconTabFieldName(dto.getReconTabFieldName());
        entity.setReconFromPosition(dto.getReconFromPosition());
        entity.setReconToPosition(dto.getReconToPosition());
        entity.setReconMaxLength(dto.getReconMaxLength());
        entity.setReconKeyIdentifier(dto.getReconKeyIdentifier());
        entity.setReconColumnOffset(dto.getReconColumnOffset());
        entity.setReconMandtoryFlag(dto.getReconMandtoryFlag());
        entity.setReconInserCode(dto.getReconInserCode());
        entity.setReconInsertUser(dto.getReconInsertUser());
        entity.setReconInsertDate(dto.getReconInsertDate());
        entity.setReconLastUpdatedUser(dto.getReconLastUpdatedUser());
        entity.setReconLastUpdatedDate(dto.getReconLastUpdatedDate());
        entity.setReconRankIdentifier(dto.getReconRankIdentifier());
        entity.setReconAlterFlag(dto.getReconAlterFlag());
        entity.setReconDisplayedFlag(dto.getReconDisplayedFlag());
        entity.setReconReportFieldFlag(dto.getReconReportFieldFlag());
        entity.setReconMatchingFieldFlag(dto.getReconMatchingFieldFlag());
        entity.setReconFromPosn(dto.getReconFromPosn());
        entity.setReconToPosn(dto.getReconToPosn());
        entity.setReconInstanceCode(dto.getReconInstanceCode());
        entity.setReconMandatoryFlag(dto.getReconMandatoryFlag());
        entity.setReconToPositionDescription(dto.getReconToPositionDescription());
        
        return entity;
    }
}