package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReconFieldTypeMasterDTO {
    private Long fieldTypeId;
    private String fieldTypeDes;
    private Long insertUser;
    private Date insertDate;
    private Long lastUpdatedUser;
    private Date lastUpdatedDate;
    private Long insertCode;
}