package com.jpb.reconciliation.reconciliation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CParamResponseDTO {
    private Long id;
    private String paramName;
    private String paramValue;
    private String activeYn;
}