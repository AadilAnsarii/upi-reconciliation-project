package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class SuperUserLoginDto {

    private String bankCode;

    private String superUserId;

    private String password;
}