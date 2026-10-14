package com.jpb.reconciliation.reconciliation.dto;


import java.time.LocalDateTime;

import com.jpb.reconciliation.reconciliation.enums.EncryptionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionConfigResponse {
    private Long encryptionConfigId;
    private Long templateId;
    private Boolean encryptionRequired;
    private EncryptionType encryptionType;
    private String keyReference;
    private String decryptionCommand;
    private String preProcessingSteps;
    private String postProcessingSteps;
    private LocalDateTime createdAt;
}
