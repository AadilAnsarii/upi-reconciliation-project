package com.jpb.reconciliation.reconciliation.mapper;

import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.dto.ParamDTO;
import com.jpb.reconciliation.reconciliation.entity.Param;


@Component
public class ParamMapper {

    public ParamDTO toDTO(Param param) {
        return ParamDTO.builder()
                .paramId(param.getParamId())
                .paramName(param.getParamName())
                .paramDesc(param.getParamDesc())
                .paramValue(param.getParamValue())
                .activeYn(param.getActiveYn())
                .createdBy(param.getCreatedBy())
                .createdOn(param.getCreatedOn())
                .updatedBy(param.getUpdatedBy())
                .updatedOn(param.getUpdatedOn())
                .build();
    }
}