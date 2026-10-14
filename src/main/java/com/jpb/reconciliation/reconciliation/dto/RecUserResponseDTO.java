package com.jpb.reconciliation.reconciliation.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecUserResponseDTO {

    private Long          userId;
    private String        employeeCode;
    private String        fullName;
    private String        email;
    private String        mobile;
    private String        department;
    private String        designation;
    private Integer       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
