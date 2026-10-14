package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;

/**
 * Response DTO for access check operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessCheckResponse {
    private Long roleId;
    private Long moduleId;
    private boolean hasAccess;
}
