package com.jpb.reconciliation.reconciliation.dto.fileconfiguration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDTO {
    private Long reconTemplateId;
    private String templateName;
    private String stageTabName;
}