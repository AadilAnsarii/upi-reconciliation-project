package com.jpb.reconciliation.reconciliation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LookupResponseDTO {

    private Long id;
    private String lookupName;
    private String lookupValue;
    private String activeYn;
    private Long parentId;
}
