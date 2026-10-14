package com.jpb.reconciliation.reconciliation.dto;


import com.jpb.reconciliation.reconciliation.enums.EncryptionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionConfigRequest {

    @Builder.Default
    private Boolean encryptionRequired = false;

    private EncryptionType encryptionType;
    private String keyReference;
    private String decryptionCommand;
    private String preProcessingSteps;
    private String postProcessingSteps;
}
