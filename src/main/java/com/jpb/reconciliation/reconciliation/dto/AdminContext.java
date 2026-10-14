package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContext {
    private String username;
    private String bankCode;
    private String branchCode;
    private Long userId;

    public AdminContext(String username, String bankCode, String branchCode) {
        this.username = username;
        this.bankCode = bankCode;
        this.branchCode = branchCode;
    }
}
