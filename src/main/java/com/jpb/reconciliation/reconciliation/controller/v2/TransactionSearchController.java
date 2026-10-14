package com.jpb.reconciliation.reconciliation.controller.v2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TranSearchReqDto;
import com.jpb.reconciliation.reconciliation.dto.TranSearchResponse;
import com.jpb.reconciliation.reconciliation.dto.filefetch.FileDTO;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.service.transactionsearch.TranSearchService;

import net.sf.jasperreports.engine.JRException;

@RestController
@RequestMapping(path = "/api/v1/transaction-search")
public class TransactionSearchController {
	private final Logger logger = LoggerFactory.getLogger(TransactionSearchController.class);

	@Autowired
	private ReconFileDetailsMasterRepository reconFileDetailsMasterRepository;

	@Autowired
	private TranSearchService tranSearchService;

	@GetMapping(path = "/get-files", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getFiles() {
		RestWithStatusList restWithStatusList = null;
		List<ReconFileDetailsMaster> reconFileDetailsLst = reconFileDetailsMasterRepository.findByRfdTranFileFlag(CommonConstants.ACTIVE_Y);
		logger.info("FILE DETAILS {}", reconFileDetailsLst);

		List<FileDTO> collect = reconFileDetailsLst.stream().map(this::convertToDto).collect(Collectors.toList());

		logger.info("FILE Result {}", collect);
		if (!collect.isEmpty() && collect != null) {
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Files are fetched", new ArrayList<>(collect));
			return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
		} else {
			restWithStatusList = new RestWithStatusList("FAILED", "File not found", new ArrayList<>(collect));
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}

	}

	@PostMapping(path = "/search", consumes = { "multipart/form-data" }, produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<TranSearchResponse> searchTransaction(
			@RequestPart(value = "file", required = false) MultipartFile file,
			@ModelAttribute TranSearchReqDto tranSearchReqDto) throws JRException, IOException {

		try {
			if (file != null && !file.isEmpty()) {
				return tranSearchService.getTransactionRecords(file, tranSearchReqDto);
			} else {
				return tranSearchService.getTransactionRecords(null, tranSearchReqDto);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(
					new TranSearchResponse(CommonConstants.FAILURE, "An unexpected error occurred: " + e.getMessage(), null),
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping(path = "/download-file", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<?> downloadFile(@RequestParam("fileLocation") String fileLocation) {
		File file = new File(fileLocation);
		if (!file.exists()) {
			return new ResponseEntity<>(
					new ResponseDto(CommonConstants.STATUS_404, "FILE " + CommonConstants.MESSAGE_404),
					HttpStatus.NOT_FOUND);
		}
		FileSystemResource fileSystemResource = new FileSystemResource(file);
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + fileLocation)
				.header("Content-Type", "application/csv").body(fileSystemResource);
	}

	private FileDTO convertToDto(ReconFileDetailsMaster entity) {
		FileDTO dto = new FileDTO();
		dto.setReconFileName(entity.getReconFileName());
		dto.setReconFileId(entity.getReconFileId());
		dto.setReconTemplateId(entity.getReconTemplateDetails().getReconTemplateId());
		return dto;
	}
}
