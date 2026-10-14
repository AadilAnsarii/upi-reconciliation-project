package com.jpb.reconciliation.reconciliation.mapper;

import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.dto.LookupDTO;
import com.jpb.reconciliation.reconciliation.entity.Lookup;


@Component
public class LookupMapper {

    // DTO → Entity (request aane pe, save karne se pehle)
    public Lookup toEntity(LookupDTO dto) {
        return Lookup.builder()
                .lookupCode(dto.getLookupCode())
                .lookupName(dto.getLookupName())
                .shortName(dto.getShortName())
                .longName(dto.getLongName())
                .parentLookupId(dto.getParentLookupId())
                .sortOrder(dto.getSortOrder())
                .createdBy(dto.getCreatedBy())
                .lookupValue(dto.getLookupValue())
                .lookupDesc(dto.getLookupDesc())
                // activeYn, createdOn, updatedBy, updatedOn → Service set karega
                .build();
    }

    // Entity → DTO (response mein bhejne ke liye)
    public LookupDTO toDTO(Lookup l) {
        return LookupDTO.builder()
                .lookupId(l.getLookupId())
                .lookupCode(l.getLookupCode())
                .lookupName(l.getLookupName())
                .shortName(l.getShortName())
                .longName(l.getLongName())
                .parentLookupId(l.getParentLookupId())
                .sortOrder(l.getSortOrder())
                .activeYn(l.getActiveYn())
                .createdBy(l.getCreatedBy())
                .createdOn(l.getCreatedOn())
                .updatedBy(l.getUpdatedBy())
                .updatedOn(l.getUpdatedOn())
                .lookupValue(l.getLookupValue())
                .lookupDesc(l.getLookupDesc())
                .build();
    }
}