//package com.jpb.reconciliation.reconciliation.secondary.repository;
//
//import java.sql.Types;
//import java.util.HashMap;
//import java.util.Map;
//
//import javax.annotation.PostConstruct;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.SqlOutParameter;
//import org.springframework.jdbc.core.SqlParameter;
//import org.springframework.jdbc.core.simple.SimpleJdbcCall;
//import org.springframework.stereotype.Repository;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.constants.FileProcessStatus;
//import com.jpb.reconciliation.reconciliation.secondary.entity.FileProcessStatusEntity;
//
//@Repository
//public class SecondaryProcedureRepositoryImpl implements SecondaryProcedureRepository {
//
//	private static final Logger log = LoggerFactory.getLogger(SecondaryProcedureRepositoryImpl.class);
//
//	@Autowired
//	FileProcessStatusRepository fileProcessStatusRepository;
//
////	@Autowired
////	@Qualifier("secondaryJdbcTemplate")
////	private JdbcTemplate secondaryJdbcTemplate;
//
////	private SimpleJdbcCall processManualFileUploadCall;
////	private SimpleJdbcCall processApproveRejectCall;
//
////	private static final String PROCESS_UPLOAD_SP_NAME = "sp_processmanualfileupload";
////	private static final String APPROVE_REJECT_SP_NAME = "sp_process_manualfile_approve_reject";
//
////	@PostConstruct
////	private void initializeJdbcCall() {
////		this.processManualFileUploadCall = new SimpleJdbcCall(secondaryJdbcTemplate)
////				.withProcedureName(PROCESS_UPLOAD_SP_NAME).declareParameters(
////						new SqlParameter("prm_file_id", Types.VARCHAR), new SqlParameter("prm_user_id", Types.NUMERIC),
////						new SqlParameter("prm_file_name", Types.VARCHAR),
////						new SqlParameter("prm_file_type", Types.VARCHAR),
////						new SqlOutParameter("prm_err_msg", Types.VARCHAR));
//
////		this.processApproveRejectCall = new SimpleJdbcCall(secondaryJdbcTemplate)
////				.withProcedureName(APPROVE_REJECT_SP_NAME).declareParameters(
////						new SqlParameter("prm_file_id", Types.NUMERIC), new SqlParameter("prm_user_id", Types.NUMERIC),
////						new SqlParameter("prm_file_name", Types.VARCHAR),
////						new SqlParameter("prm_process_status", Types.VARCHAR),
////						new SqlParameter("prm_file_type", Types.VARCHAR),
////						new SqlOutParameter("prm_err_msg", Types.VARCHAR));
//		this.processApproveRejectCall.compile();
////	}
//
//	@Override
//	public Boolean fileProcessingData(FileProcessStatusEntity newFileEntry) {
//		try {
//			String manualFileUploadStatus = callSecondaryProcedure(newFileEntry);
//			log.info("Procedure Status :::::::::::" + manualFileUploadStatus);
//
//			if (manualFileUploadStatus != null && manualFileUploadStatus.equalsIgnoreCase("OK")) {
//				return true;
//			} else {
//				newFileEntry.setStatus(FileProcessStatus.PROCESSED_FAILURE);
//				fileProcessStatusRepository.save(newFileEntry);
//				return false;
//			}
//
//		} catch (RuntimeException e) {
//			log.error("File processing failed due to runtime exception during procedure call: {}",
//					newFileEntry.getFileName(), e);
//			newFileEntry.setStatus(FileProcessStatus.PROCESSED_FAILURE);
//			fileProcessStatusRepository.save(newFileEntry);
//			return false;
//		} finally {
////			fileProcessStatusRepository.save(newFileEntry);
//		}
//
//	}
//
//	@Transactional("secondaryJdbcTransactionManager")
//	public String callSecondaryProcedure(FileProcessStatusEntity newFileEntry) {
//		String procedureResultMsg = "EXECUTION_FAILED";
//
//		try {
//
//			Map<String, Object> inputParameter = new HashMap<>();
//			inputParameter.put("prm_file_id", newFileEntry.getFileId());
//			inputParameter.put("prm_user_id", newFileEntry.getCheckerId());
//			inputParameter.put("prm_file_type", newFileEntry.getFileType());
//			inputParameter.put("prm_file_name", newFileEntry.getFileName());
//
//			log.info("SECONDARY PROCEDURE INPUT ::::::::::::::: {}", inputParameter);
//			Map<String, Object> resultUpload = processManualFileUploadCall.execute(inputParameter);
//			log.info("SECONDARY PROCEDURE LOG INFO ::::::::::::::: {}", resultUpload);
//			procedureResultMsg = (String) resultUpload.get("prm_err_msg");
//
//		} catch (Exception e) {
//			log.error("Error executing secondary stored procedure {} for ID: {}", PROCESS_UPLOAD_SP_NAME, e);
//			throw new RuntimeException("Secondary Data Procedure failed.", e);
//		}
//
//		return procedureResultMsg;
//	}
//
//	@Override
//	public Boolean approvalProcess(FileProcessStatusEntity fileEntryForProcess,
//			FileProcessStatusEntity fileInputByUser) {
//		String processApproveRejectCallStatus = processApproveRejectCall(fileEntryForProcess, fileInputByUser);
//		log.info("Procedure Status :::::::::::" + processApproveRejectCallStatus);
//		if (processApproveRejectCallStatus != null && processApproveRejectCallStatus.equalsIgnoreCase("OK")) {
//			return true;
//		} else {
//			return false;
//		}
//	}
//
//	@Transactional("secondaryJdbcTransactionManager")
//	public String processApproveRejectCall(FileProcessStatusEntity fileEntryForProcess,
//			FileProcessStatusEntity fileInputByUser) {
//
//		String procedureResult = "EXECUTION_FAILED";
//
//		try {
//
//			Map<String, Object> inputParameter = new HashMap<>();
//			inputParameter.put("prm_file_id", fileEntryForProcess.getFileId());
//			inputParameter.put("prm_user_id", fileEntryForProcess.getCheckerId());
//			inputParameter.put("prm_file_type", fileEntryForProcess.getFileType());
//			inputParameter.put("prm_file_name", fileEntryForProcess.getFileName());
//			inputParameter.put("prm_process_status", fileInputByUser.getStatus());
//
//			log.info("SECONDARY PROCEDURE Name ::::::::::::::: {}", APPROVE_REJECT_SP_NAME);
//			log.info("SECONDARY PROCEDURE INPUT ::::::::::::::: {}", inputParameter);
//			Map<String, Object> resultApRj = processApproveRejectCall.execute(inputParameter);
//			log.info("SECONDARY PROCEDURE LOG INFO ::::::::::::::: {}", resultApRj);
//			procedureResult = (String) resultApRj.get("prm_err_msg");
//
//		} catch (Exception e) {
//			log.error("Error executing seconda	ry stored procedure {} for ID: {}", APPROVE_REJECT_SP_NAME, e);
//			throw new RuntimeException("Secondary Data Procedure failed.", e);
//		}
//
//		return procedureResult;
//	}
//}