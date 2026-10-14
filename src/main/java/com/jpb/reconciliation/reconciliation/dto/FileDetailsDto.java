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
@Schema(description = "File details with its field definitions")
public class FileDetailsDto {

    @JsonProperty("file_id")
    private Long fileId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_type")
    private String fileType;

}