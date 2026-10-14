package com.jpb.reconciliation.reconciliation.dto;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class TTUMReportDto {
    
    private Long ttumConfigId;
    private String ttumDescription;
    private Long ttumEntityId;  
    private Long ttumProcessId;
    private String ttumTypeDescription;
    private String ttumType;
    private Long jrxmlId;
    private String outputFormat;
    private Long insertCode;
    private String ttumCatType;
    private Long settleFileId;  
    private String isCBSTTUM;
    private String reconProcessName;
    
    @NotNull(message = "userId must not be null")
    private Long userId;
}
