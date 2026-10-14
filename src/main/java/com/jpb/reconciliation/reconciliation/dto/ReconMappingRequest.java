package com.jpb.reconciliation.reconciliation.dto;


import javax.validation.constraints.NotNull;

import com.jpb.reconciliation.reconciliation.enums.SystemField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconMappingRequest {

    @NotNull(message = "System field is mandatory")
    private SystemField systemField;

    @NotNull(message = "Template field ID is mandatory")
    private Long templateFieldId;

    private String transformationRule;

    @Builder.Default
    private Boolean isMandatoryMapping = false;
}
