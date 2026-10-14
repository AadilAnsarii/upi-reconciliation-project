package com.jpb.reconciliation.reconciliation.dto;


import lombok.*;
import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupDTO {

    // ── Response only fields (DB se aate hain, request mein set nahi karte) ──
    private Long lookupId;
    private String activeYn;
    private Date createdOn;
    private Date updatedOn;

    // ── Required fields (create ke time mandatory) ────────────────────────────
    @NotBlank(message = "Lookup code is required")
    private String lookupCode;

    @NotBlank(message = "Lookup name is required")
    private String lookupName;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;

    @NotBlank(message = "Created by is required")
    private String createdBy;

    // ── Optional fields ───────────────────────────────────────────────────────
    private String shortName;
    private String longName;
    private Long parentLookupId;
    private String updatedBy;
    private String lookupValue;
    private String lookupDesc;
}