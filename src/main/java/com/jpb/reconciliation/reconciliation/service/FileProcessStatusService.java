//package com.jpb.reconciliation.reconciliation.service;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.jpb.reconciliation.reconciliation.dto.ReportDto;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.secondary.entity.FileProcessStatusEntity;
//
//
//@Service
//public interface FileProcessStatusService {
//
//	ResponseEntity<RestWithStatusList> uploadFile(MultipartFile file, UserDetails userDetails, String fileType);
//
//	ResponseEntity<RestWithStatusList> getFileList();
//
//	ResponseEntity<RestWithStatusList> fileActionApproval(FileProcessStatusEntity fileInputByUser);
//
//	ResponseEntity<RestWithStatusList> generateFileUploadReport(ReportDto ntslReportRequest);
//
//}
