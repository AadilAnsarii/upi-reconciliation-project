package com.jpb.reconciliation.reconciliation.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReconProcessWithFilesDto {
    private Long processId;
    private String processName;
    private TemplateWithFileAndFieldsDto file1;  
    private TemplateWithFileAndFieldsDto file2;  
    private TemplateWithFileAndFieldsDto file3;  
    private TemplateWithFileAndFieldsDto file4;  
}
