package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigDTO;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.FileConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.fileconfiguration.TemplateDTO;
import com.jpb.reconciliation.reconciliation.entity.ProcessMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileConfigServiceImpl implements FileConfigService {

	private final ReconFileDetailsMasterRepository fileConfigRepository;
	private final ReconTemplateDetailsRepository templateRepository;
	private final ProcessMasterRepository processRepository;

	@Override
	@Transactional(readOnly = true)
	public List<TemplateDTO> getAllTemplates() {
		log.info("Fetching all active templates");
		List<ReconTemplateDetails> templates = templateRepository.findAllTemplates();
		return templates.stream().map(this::convertToTemplateDTO).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public TemplateDTO getTemplateById(Long templateId) {
		log.info("Fetching template by ID: {}", templateId);
		ReconTemplateDetails template = templateRepository.findById(templateId)
				.orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + templateId));

		return convertToTemplateDTO(template);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<FileConfigDTO> getAllFileConfigs(int page, int size, Long templateId, String fileName) {
		log.info("Fetching file configurations - page: {}, size: {}, templateId: {}, fileName: {}", page, size,
				templateId, fileName);

		Pageable pageable = PageRequest.of(page, size);
		Page<ReconFileDetailsMaster> fileConfigs = fileConfigRepository.findByFilters(templateId, fileName, pageable);
		log.info("Fetching file configurations :::::{}" + fileConfigs);
		return fileConfigs.map(this::convertToFileConfigDTO);
	}

	@Override
	@Transactional(readOnly = true)
	public FileConfigDTO getFileConfigById(Long fileId) {
		log.info("Fetching file configuration by ID: {}", fileId);
		ReconFileDetailsMaster fileConfig = fileConfigRepository.findById(fileId)
				.orElseThrow(() -> new ResourceNotFoundException("File configuration not found with ID: " + fileId));

		return convertToFileConfigDTO(fileConfig);
	}

	@Override
	public ResponseEntity<RestWithStatusList> createFileConfig(FileConfigRequest request, Long userId) {
		log.info("Creating new file configuration for file: {}", request.getRfdFileName());

		// Validate and fetch template
		ReconTemplateDetails template = templateRepository.findById(request.getRtdTemplateId()).orElseThrow(
				() -> new ResourceNotFoundException("Template not found with ID: " + request.getRtdTemplateId()));

		// Validate and fetch process master
		ProcessMasterEntity process = processRepository.findById(request.getProcessMastId()).orElseThrow(
				() -> new ResourceNotFoundException("Process not found with ID: " + request.getProcessMastId()));

		ReconFileDetailsMaster fileConfig = new ReconFileDetailsMaster();

		// File basic info
		fileConfig.setReconFileName(request.getRfdFileName());
		fileConfig.setReconShortName(request.getRfdShortName());
		fileConfig.setReconFileDescription(request.getRfdFileDescription());
		fileConfig.setReconFileType(request.getRfdFileType());
		fileConfig.setReconFileLocation(request.getRfdFileLocation());
		fileConfig.setReconFileDelimiter(request.getRfdFileDelimiter());
		fileConfig.setReconFileDestinationPath(request.getRfdFileDestPath());
		fileConfig.setReconFileDuplicateCheckFlag(request.getRfdFileDupChkFlag());
		fileConfig.setReconFileDefinConst(request.getRfdFileDefineConst());
		fileConfig.setReconFileNameLength(request.getRfdFilenameLength());
		fileConfig.setReconNameConvFormat(request.getRfdNameConvFormat());
		fileConfig.setReconDependentFileId(request.getRfdDependentFileId());
		fileConfig.setFileUpdateFlag(request.getFileUpdateFlag());

		// Header info
		fileConfig.setReconHdrAvailableFlag(request.getRfdHdrAvlFlag());
		fileConfig.setReconHdrBlockSize(request.getRfdHdrBlockSize());
		fileConfig.setReconHdrId(request.getRfdHdrId());
		fileConfig.setReconHdrKeyCount(request.getRfdHdrKeyCount());
		fileConfig.setReconHdrWithDr(request.getRfdHdrWithDr());

		// Footer info
		fileConfig.setReconFtrAvailFlag(request.getRfdFtrAvailFlag());
		fileConfig.setReconFtrBeginConstVal(request.getRfdFtrBeginConstVal());
		fileConfig.setReconFtrLength(request.getRfdFtrLength());
		fileConfig.setReconFtrType(request.getRfdFtrType());
		fileConfig.setReconFtrControlTagCount(request.getRfdFtrCtrlTagCnt());

		// Data record info
		fileConfig.setReconDrBlockSize(request.getRfdDrBlockSize());
		fileConfig.setReconDrBlockSizeFlag(request.getRfdDrBlockSizeFlag());
		fileConfig.setReconDrFormat(request.getRfdDrFormat());
		fileConfig.setReconMultiDrCheck(request.getRfdMultiDrCheck());
		fileConfig.setReconMultiDrCount(request.getRfdMultiDrCount());

		// FTP info
		fileConfig.setReconFTPServerName(request.getRfdFtpServerName());
		fileConfig.setReconFTPFilePath(request.getRfdFtpFilePath());

		// Flags
		fileConfig.setReconEmailSMSFlag(request.getRfdEmailSmsFlag());
		fileConfig.setReconExitMenuFlag(request.getRfdExtMenuFlag());
		fileConfig.setReconExitMenuName(request.getRfdExtMenuName());
		fileConfig.setRfdGlFlag(request.getRfdGlFlag());
		fileConfig.setRfdTranFileFlag(request.getRfdTranFileFlag());
		fileConfig.setReconSettleFlag(request.getRfdSettleFlg());
		fileConfig.setReconJpslRpsl(request.getRfdJpslRpsl());
		fileConfig.setReconExitMenuFlag(CommonConstants.INACTIVE_N);

		// Other
		fileConfig.setReconXSDName(request.getRfdXsdName());
		fileConfig.setReconInstCode(request.getRfdInstCode());

		// Relations
		fileConfig.setReconTemplateDetails(template);
		fileConfig.setProcessmaster(process);

		// Audit
		fileConfig.setReconInsertDate(LocalDateTime.now());
		fileConfig.setReconInsertUser(userId);

		ReconFileDetailsMaster savedConfig = fileConfigRepository.save(fileConfig);
		log.info("File configuration created successfully with ID: {}", savedConfig.getReconFileId());

		return new ResponseEntity<>(
				new RestWithStatusList(CommonConstants.SUCCESS,
						"File configuration created successfully with ID: " + savedConfig.getReconFileId(), null),
				HttpStatus.OK);
	}

	@Override
	public FileConfigDTO updateFileConfig(Long fileId, FileConfigRequest request, Long userId) {
	    log.info("Updating file configuration with ID: {}", fileId);

	    ReconFileDetailsMaster existingConfig = fileConfigRepository.findById(fileId)
	            .orElseThrow(() -> new ResourceNotFoundException(
	                    "File configuration not found with ID: " + fileId));

	    // Validate and fetch template
	    ReconTemplateDetails template = templateRepository.findById(request.getRtdTemplateId())
	            .orElseThrow(() -> new ResourceNotFoundException(
	                    "Template not found with ID: " + request.getRtdTemplateId()));

	    // Validate and fetch process master
	    ProcessMasterEntity process = processRepository.findById(request.getProcessMastId())
	            .orElseThrow(() -> new ResourceNotFoundException(
	                    "Process not found with ID: " + request.getProcessMastId()));

	    // File basic info
	    existingConfig.setReconFileName(request.getRfdFileName());
	    existingConfig.setReconShortName(request.getRfdShortName());
	    existingConfig.setReconFileDescription(request.getRfdFileDescription());
	    existingConfig.setReconFileType(request.getRfdFileType());
	    existingConfig.setReconFileLocation(request.getRfdFileLocation());
	    existingConfig.setReconFileDelimiter(request.getRfdFileDelimiter());
	    existingConfig.setReconFileDestinationPath(request.getRfdFileDestPath());
	    existingConfig.setReconFileDuplicateCheckFlag(request.getRfdFileDupChkFlag());
	    existingConfig.setReconFileDefinConst(request.getRfdFileDefineConst());
	    existingConfig.setReconFileNameLength(request.getRfdFilenameLength());
	    existingConfig.setReconNameConvFormat(request.getRfdNameConvFormat());
	    existingConfig.setReconDependentFileId(request.getRfdDependentFileId());
	    existingConfig.setFileUpdateFlag(request.getFileUpdateFlag());

	    // Header info
	    existingConfig.setReconHdrAvailableFlag(request.getRfdHdrAvlFlag());
	    existingConfig.setReconHdrBlockSize(request.getRfdHdrBlockSize());
	    existingConfig.setReconHdrId(request.getRfdHdrId());
	    existingConfig.setReconHdrKeyCount(request.getRfdHdrKeyCount());
	    existingConfig.setReconHdrWithDr(request.getRfdHdrWithDr());

	    // Footer info
	    existingConfig.setReconFtrAvailFlag(request.getRfdFtrAvailFlag());
	    existingConfig.setReconFtrBeginConstVal(request.getRfdFtrBeginConstVal());
	    existingConfig.setReconFtrLength(request.getRfdFtrLength());
	    existingConfig.setReconFtrType(request.getRfdFtrType());
	    existingConfig.setReconFtrControlTagCount(request.getRfdFtrCtrlTagCnt());

	    // Data record info
	    existingConfig.setReconDrBlockSize(request.getRfdDrBlockSize());
	    existingConfig.setReconDrBlockSizeFlag(request.getRfdDrBlockSizeFlag());
	    existingConfig.setReconDrFormat(request.getRfdDrFormat());
//	    existingConfig.setReconDridentifierFlag(request.getRfdDridentifierFlag());
	    existingConfig.setReconMultiDrCheck(request.getRfdMultiDrCheck());
	    existingConfig.setReconMultiDrCount(request.getRfdMultiDrCount());

	    // FTP info
	    existingConfig.setReconFTPServerName(request.getRfdFtpServerName());
	    existingConfig.setReconFTPFilePath(request.getRfdFtpFilePath());

	    // Flags
	    existingConfig.setReconEmailSMSFlag(request.getRfdEmailSmsFlag());
	    existingConfig.setReconExitMenuFlag(request.getRfdExtMenuFlag());
	    existingConfig.setReconExitMenuName(request.getRfdExtMenuName());
	    existingConfig.setRfdGlFlag(request.getRfdGlFlag());
	    existingConfig.setRfdTranFileFlag(request.getRfdTranFileFlag());
	    existingConfig.setReconSettleFlag(request.getRfdSettleFlg());
	    existingConfig.setReconJpslRpsl(request.getRfdJpslRpsl());

	    // Other
	    existingConfig.setReconXSDName(request.getRfdXsdName());
	    existingConfig.setReconInstCode(request.getRfdInstCode());

	    // Relations
	    existingConfig.setReconTemplateDetails(template);
	    existingConfig.setProcessmaster(process);

	    // Audit fields — preserve original insert info, only update LUPD
	    existingConfig.setReconLastUpdatedDate(LocalDateTime.now());
	    existingConfig.setReconLastUpdatedUser(userId);

	    ReconFileDetailsMaster updatedConfig = fileConfigRepository.save(existingConfig);
	    log.info("File configuration updated successfully with ID: {}", updatedConfig.getReconFileId());

	    return convertToFileConfigDTO(updatedConfig);
	}

	@Override
	public ResponseEntity<RestWithStatusList> deleteFileConfig(Long fileId) {
		log.info("Deleting file configuration with ID: {}", fileId);

		ReconFileDetailsMaster fileConfig = fileConfigRepository.findById(fileId)
				.orElseThrow(() -> new ResourceNotFoundException("File configuration not found with ID: " + fileId));

		fileConfigRepository.delete(fileConfig); // pass the entity, not the ID

		log.info("File configuration deleted successfully with ID: {}", fileId);

		return new ResponseEntity<>(
				new RestWithStatusList(CommonConstants.SUCCESS, "File configuration deleted successfully with ID: " + fileId, null),
				HttpStatus.OK);
	}

	// Helper methods for DTO conversion
	private TemplateDTO convertToTemplateDTO(ReconTemplateDetails template) {
		TemplateDTO dto = new TemplateDTO();
		BeanUtils.copyProperties(template, dto);
		return dto;
	}

	private FileConfigDTO convertToFileConfigDTO(ReconFileDetailsMaster entity) {
		FileConfigDTO dto = new FileConfigDTO();

		// Map all fields with correct entity field names
		dto.setRfdFileId(entity.getReconFileId());
		dto.setRfdFileName(entity.getReconFileName());
		dto.setRfdShortName(entity.getReconShortName());
		dto.setRfdNameConvFormat(entity.getReconNameConvFormat());
		dto.setRfdFileDefineConst(entity.getReconFileDefinConst());
		dto.setRfdFilenameLength(entity.getReconFileNameLength());
		dto.setRfdFileDupChkFlag(entity.getReconFileDuplicateCheckFlag());
		dto.setRfdFileType(entity.getReconFileType());
		dto.setRfdFileDelimiter(entity.getReconFileDelimiter());
		dto.setRfdFileLocation(entity.getReconFileLocation());
		dto.setRfdFileDestPath(entity.getReconFileDestinationPath());
		dto.setRfdFileDescription(entity.getReconFileDescription());

		// Header fields
		dto.setRfdHdrId(entity.getReconHdrId());
		dto.setRfdHdrAvlFlag(entity.getReconHdrAvailableFlag());
		dto.setRfdHdrBlockSize(entity.getReconHdrBlockSize());
		dto.setRfdHdrKeyCount(entity.getReconHdrKeyCount());
		dto.setRfdHdrWithDr(entity.getReconHdrWithDr());

		// Footer fields
		dto.setRfdFtrAvailFlag(entity.getReconFtrAvailFlag());
		dto.setRfdFtrBeginConstVal(entity.getReconFtrBeginConstVal());
		dto.setRfdFtrType(entity.getReconFtrType());
		dto.setRfdFtrCtrlTagCnt(entity.getReconFtrControlTagCount());
		dto.setRfdFtrLength(entity.getReconFtrLength());

		// Detail Record fields
		dto.setRfdDrFormat(entity.getReconDrFormat());
		dto.setRfdMultiDrCheck(entity.getReconMultiDrCheck());
		dto.setRfdMultiDrCount(entity.getReconMultiDrCount());
		dto.setRfdDrBlockSizeFlag(entity.getReconDrBlockSizeFlag());
		dto.setRfdDrBlockSize(entity.getReconDrBlockSize());

		// Audit fields
		dto.setRfdInstCode(entity.getReconInstCode());
		dto.setRfdInsUser(entity.getReconInsertUser());
		dto.setRfdInsDate(entity.getReconInsertDate());
		dto.setRfdLupdUser(entity.getReconLastUpdatedUser());
		dto.setRfdLupdDate(entity.getReconLastUpdatedDate());

		// Other fields
		dto.setRfdExtMenuName(entity.getReconExitMenuName());
		dto.setRfdExtMenuFlag(entity.getReconExitMenuFlag());
		dto.setRfdDridentifierFlag(entity.getReconDridenti1fierFlag());
		dto.setRfdXsdName(entity.getReconXSDName());
		dto.setRfdDependentFileId(entity.getReconDependentFileId());
		dto.setRfdFtpServerName(entity.getReconFTPServerName());
		dto.setRfdFtpFilePath(entity.getReconFTPFilePath());
		dto.setRfdEmailSmsFlag(entity.getReconEmailSMSFlag());
		dto.setRfdSettleFlg(entity.getReconSettleFlag());
		dto.setRfdJpslRpsl(entity.getReconJpslRpsl());
		dto.setFileUpdateFlag(entity.getFileUpdateFlag());
		dto.setRfdTranFileFlag(entity.getRfdTranFileFlag());
		dto.setRfdGlFlag(entity.getRfdGlFlag());

		// Template relationship - NULL SAFE
		if (entity.getReconTemplateDetails() != null) {
			dto.setRtdTemplateId(entity.getReconTemplateDetails().getReconTemplateId());
			dto.setTemplateName(entity.getReconTemplateDetails().getTemplateName());
		}

		// Process relationship - NULL SAFE
		if (entity.getProcessmaster() != null) {
			dto.setProcessMastId(entity.getProcessmaster().getProcessMastId());
		}

		return dto;
	}
}