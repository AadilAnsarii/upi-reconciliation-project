package com.jpb.reconciliation.reconciliation.dto;

public class ForgotPasswordResponseDto {

    private String statusCode;
    private String statusMsg;

    public ForgotPasswordResponseDto() {
    }

    public ForgotPasswordResponseDto(String statusCode, String statusMsg) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }
}
