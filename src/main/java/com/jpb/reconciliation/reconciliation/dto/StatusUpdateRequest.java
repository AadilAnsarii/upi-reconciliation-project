package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;

/**
 * Request DTO for updating role status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusUpdateRequest {
    private String status;  // ACTIVE or INACTIVE
}