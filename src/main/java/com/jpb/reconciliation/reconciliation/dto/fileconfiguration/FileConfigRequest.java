package com.jpb.reconciliation.reconciliation.dto.fileconfiguration;

import lombok.Data;

@Data
public class FileConfigRequest {

    private Long rtdTemplateId;
    private Long processMastId;

    // File basic info
    private String rfdFileName;
    private String rfdShortName;
    private String rfdFileDescription;
    private String rfdFileType;
    private String rfdFileLocation;
    private String rfdFileDelimiter;
    private String rfdFileDestPath;
    private String rfdFileDupChkFlag;
    private String rfdFileDefineConst;
    private Long rfdFilenameLength;
    private String rfdNameConvFormat;
    private Long rfdDependentFileId;
    private String fileUpdateFlag;

    // Header info
    private String rfdHdrAvlFlag;
    private Long rfdHdrBlockSize;
    private Long rfdHdrId;
    private Long rfdHdrKeyCount;
    private String rfdHdrWithDr;

    // Footer info
    private String rfdFtrAvailFlag;
    private String rfdFtrBeginConstVal;
    private Long rfdFtrLength;
    private String rfdFtrType;
    private Long rfdFtrCtrlTagCnt;

    // Data record info
    private Long rfdDrBlockSize;
    private String rfdDrBlockSizeFlag;
    private String rfdDrFormat;
    private String rfdDridentifierFlag;
    private String rfdMultiDrCheck;
    private Long rfdMultiDrCount;

    // FTP info
    private String rfdFtpServerName;
    private String rfdFtpFilePath;

    // Flags
    private String rfdEmailSmsFlag;
    private String rfdExtMenuFlag;
    private String rfdExtMenuName;
    private String rfdGlFlag;
    private String rfdTranFileFlag;
    private String rfdSettleFlg;
    private String rfdJpslRpsl;

    // Other
    private String rfdXsdName;
    private Long rfdInstCode;
}