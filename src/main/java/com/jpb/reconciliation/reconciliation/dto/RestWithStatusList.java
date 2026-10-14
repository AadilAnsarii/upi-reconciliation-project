package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestWithStatusList {

    private String status;
    private String statusMsg;
    private List<?> data;
}