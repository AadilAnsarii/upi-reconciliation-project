package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

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
@Schema(description = "Rest with status list for response")
public class RestWithStatusListPagination {
	   @Schema(description = "Status of the operation", example = CommonConstants.SUCCESS)
	    private String status;
	    
	    @Schema(description = "Message providing more information about the status", example = "Request executed successfully")
	    private String statusMsg;
	    
	    @Schema(description = "List of data objects")
	    private List<?> data;
	    
	    @Schema(description = "Pagination metadata")
	    private PageMetadata pageMetadata;
}
