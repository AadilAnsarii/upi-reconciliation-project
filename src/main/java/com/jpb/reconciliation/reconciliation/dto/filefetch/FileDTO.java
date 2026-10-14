package com.jpb.reconciliation.reconciliation.dto.filefetch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileDTO {

    private String reconFileName;
    private Long reconFileId;
    private Long reconTemplateId;
}
