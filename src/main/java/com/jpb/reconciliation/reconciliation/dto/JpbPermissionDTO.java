package com.jpb.reconciliation.reconciliation.dto;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JpbPermissionDTO {
    private Long moduleId;
    private String moduleName;
    private boolean hasAccess;
    private boolean canView;
    private boolean canCreate;
    private boolean canEdit;
    private boolean canApprove;
    private boolean canDownload;
}