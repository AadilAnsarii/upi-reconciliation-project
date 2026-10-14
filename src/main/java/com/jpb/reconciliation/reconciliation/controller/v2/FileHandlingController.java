//package com.jpb.reconciliation.reconciliation.controller;
//
//import java.io.File;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
//import com.jpb.reconciliation.reconciliation.constants.FileProcessStatus;
//import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.secondary.entity.FileProcessStatusEntity;
//import com.jpb.reconciliation.reconciliation.secondary.repository.FileProcessStatusRepository;
//import com.jpb.reconciliation.reconciliation.service.FileProcessStatusService;
//
//@RestController
//@RequestMapping("/api/v1/file-process/")
//public class FileHandlingController {
//
//	@Autowired
//	FileProcessStatusService fileProcessStatusService;
//
//	Logger logger = LoggerFactory.getLogger(FileHandlingController.class);
//
//	@Autowired
//	FileProcessStatusRepository fileProcessStatusRepository;
//
//	@PostMapping(path = "uploadfile", produces = CommonConstants.APPLICATION_JSON)
//	ResponseEntity<RestWithStatusList> uploadFile(@RequestParam("file") MultipartFile file,
//			@RequestParam("fileType") String fileType, @AuthenticationPrincipal UserDetails userDetails) {
//		RestWithStatusList restWithStatusList = null;
//		String fileName = file.getOriginalFilename();
//		List<FileProcessStatusEntity> checkFileUpload = fileProcessStatusRepository.findByFileNameAndStatus(fileName,FileProcessStatus.APPROVED);
//		logger.info("File List With Name  ::::::::" + checkFileUpload);
//		logger.info("File Name ::::::::" + file.getOriginalFilename());
//		if (checkFileUpload.isEmpty()) {
//			if (!file.isEmpty()) {
//				if (file.getOriginalFilename().contains(" ")) {
//					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Please remove spaces from upload file",
//							null);
//					return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//				} else {
//					return fileProcessStatusService.uploadFile(file, userDetails, fileType);
//				}
//
//			} else {
//				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Please select a file to upload.", null);
//				return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//			}
//		} else {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
//					"The file has already been uploaded. Please upload the other files", null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//		}
//
//	}
//
//	@GetMapping(path = "get-file", produces = CommonConstants.APPLICATION_JSON)
//	ResponseEntity<RestWithStatusList> getFileList() {
//		return fileProcessStatusService.getFileList();
//	}
//
//	@PostMapping(path = "download-file", produces = CommonConstants.APPLICATION_JSON)
//	ResponseEntity<?> downloadFile(@RequestBody FileProcessStatusEntity fileInputByUse) {
//		File file = new File(fileInputByUse.getFilePath());
//		if (!file.exists()) {
//			return new ResponseEntity<>(
//					new ResponseDto(CommonConstants.STATUS_404, "FILE " + CommonConstants.MESSAGE_404),
//					HttpStatus.NOT_FOUND);
//		}
//		FileSystemResource fileSystemResource = new FileSystemResource(file);
//		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + fileInputByUse.getFileName())
//				.header("Content-Type", "application/csv").body(fileSystemResource);
//	}
//
//	@PostMapping(path = "file-approval", produces = CommonConstants.APPLICATION_JSON)
//	ResponseEntity<RestWithStatusList> actionTakenByUser(@RequestBody FileProcessStatusEntity fileInputByUser) {
//		RestWithStatusList restWithStatusList = null;
//		if (fileInputByUser != null) {
//			return fileProcessStatusService.fileActionApproval(fileInputByUser);
//		} else {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Please send valid request", null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//		}
//	}
//}


