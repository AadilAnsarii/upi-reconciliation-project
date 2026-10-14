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
@Schema(description = "Field name details under a template")
public class FieldDetailsDto {

    @JsonProperty("field_id")
    private Long fieldId;           // reconFieldId

    @JsonProperty("field_name")
    private String fieldName;       // reconTabFieldName — the column/field name for report use
}