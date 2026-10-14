package com.jpb.reconciliation.reconciliation.dto;


import java.time.LocalDateTime;

import com.jpb.reconciliation.reconciliation.enums.SystemField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconMappingResponse {
    private Long mappingId;
    private Long templateId;
    private SystemField systemField;
    private Long templateFieldId;
    private String templateFieldName;
    private String transformationRule;
    private Boolean isMandatoryMapping;
    private String createdBy;
    private LocalDateTime createdAt;
}
