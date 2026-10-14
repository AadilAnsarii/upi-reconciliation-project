package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

import java.sql.Date;

@Data
public class TranSearchReqDto {
    private Long processId;
    private Long tempId;
    private String referenceNumber;
    private Date fromDate;
    private Date toDate;

}
