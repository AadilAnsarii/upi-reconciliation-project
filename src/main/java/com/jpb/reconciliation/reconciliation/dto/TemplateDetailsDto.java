package com.jpb.reconciliation.reconciliation.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Template master/header information")
public class TemplateDetailsDto {

    @JsonProperty("template_id")
    private Long templateId;

    @JsonProperty("template_name")
    private String templateName;

    @JsonProperty("template_type")
    private String templateType;
    
    private String stageTableName;
}