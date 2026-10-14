package com.jpb.reconciliation.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranSearchResponse {
    @Schema(description = "Status of the operation", example = CommonConstants.SUCCESS)
    private String status;
    @Schema(description = "Message providing more information about the status", example = "Request executed successfully")
    private String statusMsg;
    private  String fileLocation;
    public List<Object> data;


    public TranSearchResponse(String status, String s, Object o) {
    }
}
