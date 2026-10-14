package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@Table(name = "RCN_FILE_DTL_MAST")
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = { "processmaster" })
public class ReconFileDetailsMaster {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_seq")
    @SequenceGenerator(name = "file_seq", sequenceName = "RFD_FILE_ID_SEQ", allocationSize = 1)
	@Column(name = "RFD_FILE_ID")
	private Long reconFileId;
	
	@Column(name = "RFD_FILE_NAME")
	private String reconFileName;
	
	@Column(name = "RFD_SHORT_NAME")
	private String reconShortName;
	
	@Column(name = "RFD_NAME_CONV_FORMAT")
	private String reconNameConvFormat;
	
	@Column(name = "RFD_FILE_DEFINE_CONST")
	private String reconFileDefinConst;
	
	@Column(name = "RFD_FILENAME_LENGTH")
	private Long reconFileNameLength;
	
	@Column(name = "RFD_FILE_DUP_CHK_FLAG")
	private String reconFileDuplicateCheckFlag;
	
	@Column(name = "RFD_FILE_TYPE")
	private String reconFileType;
	
	@Column(name = "RFD_FILE_DELIMITER")
	private String reconFileDelimiter;
	
	@Column(name = "RFD_FILE_LOCATION")
	private String reconFileLocation;
	
	@Column(name = "RFD_FILE_DEST_PATH")
	private String reconFileDestinationPath;
	
	@Column(name = "RFD_HDR_ID")
	private Long reconHdrId;
	
	@Column(name = "RFD_HDR_AVL_FLAG")
	private String reconHdrAvailableFlag;
	
	@Column(name = "RFD_HDR_BLOCK_SIZE")
	private Long reconHdrBlockSize;
	
	@Column(name = "RFD_HDR_KEY_COUNT")
	private Long reconHdrKeyCount;
	
	@Column(name = "RFD_HDR_WITH_DR")
	private String reconHdrWithDr;
	
	@Column(name = "RFD_FTR_AVAIL_FLAG")
	private String reconFtrAvailFlag;
	
	@Column(name = "RFD_FTR_BEGIN_CONST_VAL")
	private String reconFtrBeginConstVal;
	
	@Column(name = "RFD_FTR_TYPE")
	private String reconFtrType;
	
	@Column(name = "RFD_FTR_CTRL_TAG_CNT")
	private Long reconFtrControlTagCount;
	
	@Column(name = "RFD_FTR_LENGTH" )
	private Long reconFtrLength;
	
	@Column(name = "RFD_DR_FORMAT")
	private String reconDrFormat;
	
	@Column(name = "RFD_MULTI_DR_CHECK")
	private String reconMultiDrCheck;
	
	@Column(name = "RFD_MULTI_DR_COUNT")
	private Long reconMultiDrCount;
	
	@Column(name = "RFD_DR_BLOCK_SIZE_FLAG")
	private String reconDrBlockSizeFlag;
	
	@Column(name = "RFD_DR_BLOCK_SIZE")
	private Long reconDrBlockSize;
	
	@Column(name = "RFD_INST_CODE")
	private Long reconInstCode;
	
	@Column(name = "RFD_INS_USER")
	private Long reconInsertUser;
	
	@Column(name = "RFD_INS_DATE")
	private LocalDateTime reconInsertDate;
	
	@Column(name = "RFD_LUPD_USER")
	private Long reconLastUpdatedUser;
	
	@Column(name = "RFD_LUPD_DATE")
	private LocalDateTime reconLastUpdatedDate;
	
	@Column(name = "RFD_EXT_MENU_NAME")
	private String reconExitMenuName;
	
	@Column(name = "RFD_EXT_MENU_FLAG")
	private String reconExitMenuFlag;
	
	@Column(name = "RFD_DRIDENTIFIER_FLAG")
	private String reconDridenti1fierFlag;
	
	@Column(name = "RFD_FILE_DESCRIPTION")
	private String reconFileDescription;
	
	@Column(name = "RFD_XSD_NAME")
	private String reconXSDName;
	
	@Column(name = "RFD_DEPENDENT_FILE_ID")
	private Long reconDependentFileId;
	
	@Column(name = "RFD_FTP_SERVER_NAME")
	private String reconFTPServerName;
	
	@Column(name = "RFD_FTP_FILE_PATH")
	private String reconFTPFilePath;
	
	@Column(name = "RFD_EMAIL_SMS_FLAG")
	private String reconEmailSMSFlag;
	
	@Column(name = "RFD_SETTLE_FLG")
	private String reconSettleFlag;
	
	@Column(name = "RFD_JPSL_RPSL")
	private String reconJpslRpsl;
	
	@Column(name = "FILE_UPDATE_FLAG")
	private String fileUpdateFlag;


	@Column(name = "RFD_TRAN_FILE_FLAG")
	private String rfdTranFileFlag;
	
	@Column(name = "RFD_GL_FLAG")
	private String rfdGlFlag;

	@ManyToOne
	@JoinColumn(name = "RTD_TEMPLATE_ID", nullable = false)
	@JsonManagedReference
	private ReconTemplateDetails reconTemplateDetails;
	
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROCESS_MAST_ID")
	@JsonBackReference
	private ProcessMasterEntity processmaster;
	
}
