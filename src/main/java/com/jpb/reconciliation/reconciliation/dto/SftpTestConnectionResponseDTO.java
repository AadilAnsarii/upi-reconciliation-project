package com.jpb.reconciliation.reconciliation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SftpTestConnectionResponseDTO {
    private boolean success;
    private String message;
}