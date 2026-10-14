package com.jpb.reconciliation.reconciliation.entity;


import java.time.LocalDateTime;
import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconSftpServerMast;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

@Entity
@Table(name = "recon_file_ingest_config")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"template", "sftpServer"})
public class ReconFileIngestConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FILE_INGEST_CONFIG")
    @SequenceGenerator(name = "SEQ_FILE_INGEST_CONFIG", sequenceName = "seq_file_ingest_config", allocationSize = 1)
    @Column(name = "ingest_config_id")
    @EqualsAndHashCode.Include
    private Long ingestConfigId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonManagedReference
    private ReconFileTmpltMast template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sftp_server_id")
    @JsonBackReference
    private ReconSftpServerMast sftpServer;

    // File identity
    @Column(name = "file_name",        length = 200) private String fileName;
    @Column(name = "short_name",       length = 50)  private String shortName;
    @Column(name = "file_description", length = 500) private String fileDescription;
    @Column(name = "file_type",        length = 20)  private String fileType;
    @Column(name = "file_location",    length = 500) private String fileLocation;
    @Column(name = "file_delimiter",   length = 5)   private String fileDelimiter;
    @Column(name = "dest_path",        length = 500) private String destPath;
    @Column(name = "dup_check_flag",   length = 1)   private String dupCheckFlag;
    @Column(name = "file_define_const",length = 200) private String fileDefineConst;
    @Column(name = "filename_length")                private Long   filenameLength;
    @Column(name = "name_conv_format", length = 200) private String nameConvFormat;
    @Column(name = "dependent_file_id")              private Long   dependentFileId;
    @Column(name = "file_update_flag", length = 1)   private String fileUpdateFlag;

    // Header
    @Column(name = "hdr_avail_flag",   length = 1)   private String hdrAvailFlag;
    @Column(name = "hdr_block_size")                 private Long   hdrBlockSize;
    @Column(name = "hdr_id")                         private Long   hdrId;
    @Column(name = "hdr_key_count")                  private Long   hdrKeyCount;
    @Column(name = "hdr_with_dr",      length = 1)   private String hdrWithDr;

    // Footer
    @Column(name = "ftr_avail_flag",       length = 1)  private String ftrAvailFlag;
    @Column(name = "ftr_begin_const_val",  length = 100)private String ftrBeginConstVal;
    @Column(name = "ftr_length")                        private Long   ftrLength;
    @Column(name = "ftr_type",             length = 20) private String ftrType;
    @Column(name = "ftr_ctrl_tag_cnt")                  private Long   ftrCtrlTagCnt;

    // Data record
    @Column(name = "dr_block_size")                  private Long   drBlockSize;
    @Column(name = "dr_block_size_flag",   length=1) private String drBlockSizeFlag;
    @Column(name = "dr_format",            length=50)private String drFormat;
    @Column(name = "multi_dr_check",       length=1) private String multiDrCheck;
    @Column(name = "multi_dr_count")                 private Long   multiDrCount;
    @Column(name = "dr_identifier_flag",   length=1) private String drIdentifierFlag;

    // SFTP
    @Column(name = "sftp_file_path",            length=500) private String sftpFilePath;
    @Column(name = "sftp_server_name_legacy",   length=200) private String sftpServerNameLegacy;

    // Flags
    @Column(name = "email_sms_flag",   length = 1)  private String emailSmsFlag;
    @Column(name = "exit_menu_flag",   length = 1)  private String exitMenuFlag;
    @Column(name = "exit_menu_name",   length = 100)private String exitMenuName;
    @Column(name = "gl_flag",          length = 1)  private String glFlag;
    @Column(name = "tran_file_flag",   length = 1)  private String tranFileFlag;
    @Column(name = "settle_flag",      length = 1)  private String settleFlag;
    @Column(name = "jpsl_rpsl",        length = 20) private String jpslRpsl;
    @Column(name = "xsd_name",         length = 200)private String xsdName;
    @Column(name = "inst_code")                     private Long   instCode;
    @Column(name = "process_mast_id")               private Long   processMastId;

    // Audit
    @Column(name = "created_by")  private Long   createdBy;
    @Column(name = "created_at",  nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_by")  private Long   updatedBy;
    @Column(name = "updated_at")  private LocalDateTime updatedAt;

    // Legacy aliases — keeps FileConfigServiceImpl compiling without changes
    public Long   getReconFileId()                 { return this.ingestConfigId; }
    public String getReconFileName()               { return this.fileName; }
    public String getReconShortName()              { return this.shortName; }
    public String getReconFileDescription()        { return this.fileDescription; }
    public String getReconFileType()               { return this.fileType; }
    public String getReconFileLocation()           { return this.fileLocation; }
    public String getReconFileDelimiter()          { return this.fileDelimiter; }
    public String getReconFileDestinationPath()    { return this.destPath; }
    public String getReconFileDuplicateCheckFlag() { return this.dupCheckFlag; }
    public String getReconFileDefinConst()         { return this.fileDefineConst; }
    public Long   getReconFileNameLength()         { return this.filenameLength; }
    public String getReconNameConvFormat()         { return this.nameConvFormat; }
    public Long   getReconDependentFileId()        { return this.dependentFileId; }
    public String getFileUpdateFlag()              { return this.fileUpdateFlag; }
    public String getReconHdrAvailableFlag()       { return this.hdrAvailFlag; }
    public Long   getReconHdrBlockSize()           { return this.hdrBlockSize; }
    public Long   getReconHdrId()                  { return this.hdrId; }
    public Long   getReconHdrKeyCount()            { return this.hdrKeyCount; }
    public String getReconHdrWithDr()              { return this.hdrWithDr; }
    public String getReconFtrAvailFlag()           { return this.ftrAvailFlag; }
    public String getReconFtrBeginConstVal()       { return this.ftrBeginConstVal; }
    public Long   getReconFtrLength()              { return this.ftrLength; }
    public String getReconFtrType()                { return this.ftrType; }
    public Long   getReconFtrControlTagCount()     { return this.ftrCtrlTagCnt; }
    public Long   getReconDrBlockSize()            { return this.drBlockSize; }
    public String getReconDrBlockSizeFlag()        { return this.drBlockSizeFlag; }
    public String getReconDrFormat()               { return this.drFormat; }
    public String getReconMultiDrCheck()           { return this.multiDrCheck; }
    public Long   getReconMultiDrCount()           { return this.multiDrCount; }
    public String getReconDridenti1fierFlag()      { return this.drIdentifierFlag; }
    public String getReconFTPFilePath()            { return this.sftpFilePath; }
    public String getReconFTPServerName()          { return this.sftpServerNameLegacy; }
    public String getReconEmailSMSFlag()           { return this.emailSmsFlag; }
    public String getReconExitMenuFlag()           { return this.exitMenuFlag; }
    public String getReconExitMenuName()           { return this.exitMenuName; }
    public String getRfdGlFlag()                   { return this.glFlag; }
    public String getRfdTranFileFlag()             { return this.tranFileFlag; }
    public String getReconSettleFlag()             { return this.settleFlag; }
    public String getReconJpslRpsl()               { return this.jpslRpsl; }
    public String getReconXSDName()                { return this.xsdName; }
    public Long   getReconInstCode()               { return this.instCode; }
    public Long   getReconInsertUser()             { return this.createdBy; }
    public LocalDateTime getReconInsertDate()      { return this.createdAt; }
    public Long   getReconLastUpdatedUser()        { return this.updatedBy; }
    public LocalDateTime getReconLastUpdatedDate() { return this.updatedAt; }
    public ReconFileTmpltMast getReconTemplateDetails() { return this.template; }
}
