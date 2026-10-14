package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamDTO {

    // ── Response only fields ──
    private Long paramId;
    private String activeYn;
    private Date createdOn;
    private Date updatedOn;

    // ── Data fields ──
    private String paramName;
    private String paramDesc;
    private String paramValue;
    private String createdBy;
    private String updatedBy;
}