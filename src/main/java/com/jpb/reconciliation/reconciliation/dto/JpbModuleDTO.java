package com.jpb.reconciliation.reconciliation.dto;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JpbModuleDTO {
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
}