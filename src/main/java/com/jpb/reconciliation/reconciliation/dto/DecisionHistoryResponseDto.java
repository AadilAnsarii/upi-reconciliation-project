package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DecisionHistoryResponseDto {

    private Long auditLogId;

    // "ROLE" or "USER"
    private String module;

    // Role name or User name
    private String subModule;

    // "Approved" / "Disapproved" / CommonConstants.INACTIVE
    private String event;

    // Extra details — roleId or userId
    private String eventData;

    // CommonConstants.SUCCESS
    private String eventStatus;

    // Checker ka userId
    private Long decidedByUserId;

    // Checker ka username (from eventData)
    private String decidedByUsername;

    // When decision was made
    private Date auditDateTime;

    // Remarks / reason (stored in oldData field)
    private String remarks;

    private Long roleId;
}