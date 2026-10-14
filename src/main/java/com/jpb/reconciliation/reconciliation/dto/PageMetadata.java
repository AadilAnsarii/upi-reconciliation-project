package com.jpb.reconciliation.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Pagination metadata")
public class PageMetadata {
    
    @Schema(description = "Current page number (0-based)", example = "0")
    private int currentPage;
    
    @Schema(description = "Number of records per page", example = "10")
    private int pageSize;
    
    @Schema(description = "Total number of records", example = "234")
    private long totalElements;
    
    @Schema(description = "Total number of pages", example = "24")
    private int totalPages;
    
    @Schema(description = "Is this the first page", example = "true")
    private boolean isFirst;
    
    @Schema(description = "Is this the last page", example = "false")
    private boolean isLast;
    
    @Schema(description = "Has next page", example = "true")
    private boolean hasNext;
    
    @Schema(description = "Has previous page", example = "false")
    private boolean hasPrevious;
}