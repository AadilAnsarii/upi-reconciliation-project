package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

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
@Schema(description = "Template with its file details and field definitions")
public class TemplateWithFileAndFieldsDto {

    @JsonProperty("templateDetails")
    @Schema(description = "Template master information from ReconTemplateDetails")
    private TemplateDetailsDto templateDetails;

    @JsonProperty("fileDetails")
    @Schema(description = "File information from ReconFileDetailsMaster")
    private FileDetailsDto fileDetails;

    @JsonProperty("fieldDetails")
    @Schema(description = "Field/column definitions from ReconFieldDetailsMaster")
    private List<FieldDetailsDto> fieldDetails;
}