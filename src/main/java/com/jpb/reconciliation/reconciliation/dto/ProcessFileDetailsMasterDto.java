package com.jpb.reconciliation.reconciliation.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessFileDetailsMasterDto {
	private Long reconFileId;
	private String reconFileName;
	private String reconShortName;
	private String reconNameConvFormat;
	private String reconFileDefinConst;
	private Long reconFileNameLength;
	private String reconFileDuplicateCheckFlag;
	private String reconFileType;
	private String reconFileDelimiter;
	private String reconFileLocation;
	private String reconFileDestinationPath;
	private Long reconHdrId;
	private String reconHdrAvailableFlag;
	private Long reconHdrBlockSize;
	private Long reconHdrKeyCount;
	private String reconHdrWithDr;
	private String reconFtrAvailFlag;
	private String reconFtrBeginConstVal;
	private String reconFtrType;
	private Long reconFtrControlTagCount;
	private Long reconFtrLength;
	private String reconDrFormat;
	private String reconMultiDrCheck;
	private Long reconMultiDrCount;
	private String reconDrBlockSizeFlag;
	private Long reconDrBlockSize;
	private Long reconInstCode;
	private Long reconInsertUser;
	private Date reconInsertDate;
	private Long reconLastUpdatedUser;
	private Date reconLastUpdatedDate;
	private String reconExitMenuName;
	private String reconExitMenuFlag;
	private String reconDridenti1fierFlag;
	private String reconFileDescription;
	private String reconXSDName;
	private Long reconDependentFileId;
	private String reconFTPServerName;
	private String reconFTPFilePath;
	private String reconEmailSMSFlag;
	private String reconSettleFlag;
	private String reconJpslRpsl;
}
