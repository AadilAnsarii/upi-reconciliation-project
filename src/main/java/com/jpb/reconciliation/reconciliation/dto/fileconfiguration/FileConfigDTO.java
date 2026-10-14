package com.jpb.reconciliation.reconciliation.dto.fileconfiguration;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileConfigDTO {
	private Long rfdFileId;
	private String fileUpdateFlag;
	private Long rfdDependentFileId;
	private Long rfdDrBlockSize;
	private String rfdDrBlockSizeFlag;
	private String rfdDrFormat;
	private String rfdDridentifierFlag;
	private String rfdEmailSmsFlag;
	private String rfdExtMenuFlag;
	private String rfdExtMenuName;
	private String rfdFtpFilePath;
	private String rfdFtpServerName;
	private String rfdFileDefineConst;
	private String rfdFileDelimiter;
	private String rfdFileDescription;
	private String rfdFileDestPath;
	private String rfdFileDupChkFlag;
	private String rfdFileLocation;
	private String rfdFileName;
	private Long rfdFilenameLength;
	private String rfdFileType;
	private String rfdFtrAvailFlag;
	private String rfdFtrBeginConstVal;
	private Long rfdFtrCtrlTagCnt;
	private Long rfdFtrLength;
	private String rfdFtrType;
	private String rfdHdrAvlFlag;
	private Long rfdHdrBlockSize;
	private Long rfdHdrId;
	private Long rfdHdrKeyCount;
	private String rfdHdrWithDr;
	private LocalDateTime rfdInsDate;
	private Long rfdInsUser;
	private Long rfdInstCode;
	private String rfdJpslRpsl;
	private LocalDateTime rfdLupdDate;
	private Long rfdLupdUser;
	private String rfdMultiDrCheck;
	private Long rfdMultiDrCount;
	private String rfdNameConvFormat;
	private String rfdSettleFlg;
	private String rfdShortName;
	private String rfdXsdName;
	private String rfdGlFlag;
	private String rfdTranFileFlag;
	private Long processMastId;
	private Long rtdTemplateId;
	private String templateName;
}