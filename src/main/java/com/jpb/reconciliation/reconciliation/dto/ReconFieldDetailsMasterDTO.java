package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconFieldDetailsMasterDTO {
    
    private Long reconFieldId;
    private Long reconColumnPosn;
    private Long reconSubTempId;
    private String reconShortName;
    private String reconTabFieldName;
    private String reconFromPosition;
    private String reconToPosition;
    private Long reconMaxLength;
    private Long reconKeyIdentifier;
    private String reconColumnOffset;
    private String reconMandtoryFlag;
    private Long reconInserCode;
    private Long reconInsertUser;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private Date reconInsertDate;
    
    private Long reconLastUpdatedUser;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    private Date reconLastUpdatedDate;
    
    private String reconRankIdentifier;
    private String reconAlterFlag;
    private String reconDisplayedFlag;
    private String reconReportFieldFlag;
    private String reconMatchingFieldFlag;
    private String reconFromPosn;
    private String reconToPosn;
    private Long reconInstanceCode;
    private String reconMandatoryFlag;
    private String reconToPositionDescription;
    
    // Optional: if you want to include template ID reference
    private Long reconTemplateId;
    
    // Additional fields from related entities
    private String fieldTypeName;
    private String fieldFormatName;
}