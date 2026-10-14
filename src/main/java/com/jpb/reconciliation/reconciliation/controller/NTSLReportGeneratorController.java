package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.forcematch.ForceMatchActionService;

@RestController
@RequestMapping("/api/v1/ntsl")
public class NTSLReportGeneratorController {

//	@Autowired
//	NTSLReportGeneratorService ntslReportGeneratorService;
	
	@Autowired
	ForceMatchActionService forceMatchService;

//	@Autowired
//	FileProcessStatusService fileProcessStatusService;

	@PostMapping(path = "/generate-ntsl-report")
	ResponseEntity<RestWithStatusList> generateNtslReport(@RequestBody ReportDto ntslReportRequest) {
		RestWithStatusList restWithStatusList = null;
		if (ntslReportRequest != null) {
			if (ntslReportRequest.getReportName().equalsIgnoreCase("FORCE_MATCH")) {
				return forceMatchService.generateForceMatchExceptionReport(ntslReportRequest);
			}
//				if (ntslReportRequest.getReportFileName().equalsIgnoreCase("AEPS NTSL")
//						|| ntslReportRequest.getReportFileName().equalsIgnoreCase("IMPS NTSL")) {
//					return ntslReportGeneratorService.generateNtslReport(ntslReportRequest);
//				} else if (ntslReportRequest.getReportFileName().equalsIgnoreCase("FILE_UPLOAD_REPORT")) {
//					return fileProcessStatusService.generateFileUploadReport(ntslReportRequest);
//				} else {
//					restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE,
//							"The " + ntslReportRequest.getReportFileName() + " process is not used to generate report.",
//							null);
//					return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//				}
//			} else {
//				return forceMatchService.generateForceMatchExceptionReport(ntslReportRequest);
//			}
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Please send valid request", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}
		return null;
	}
}
