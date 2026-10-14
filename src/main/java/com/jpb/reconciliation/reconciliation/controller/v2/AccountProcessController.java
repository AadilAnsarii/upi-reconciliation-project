package com.jpb.reconciliation.reconciliation.controller.v2;

import java.io.IOException; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.account.FileSplitterService;

@RestController
@RequestMapping(path = "/api/v1/acount")
public class AccountProcessController {

	Logger logger = LoggerFactory.getLogger(AccountProcessController.class);

	@Autowired
	FileSplitterService fileSplitterService;

	@PostMapping(path = "/split-data", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> fileSplitterService(@RequestParam("file") MultipartFile file) {
		RestWithStatusList restWithStatusList = null;
		try {
			if (file.isEmpty()) {
				restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Please upload file.", null);
				return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
			}
			fileSplitterService.splitFileByAccountNumber(file);
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "File splitting process initiated successfully!",
					null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
		} catch (IOException | InterruptedException e) {
			logger.info("Error during file splitting: " + e.getMessage());
			Thread.currentThread().interrupt();
			return new ResponseEntity<RestWithStatusList>(
					new RestWithStatusList(CommonConstants.FAILURE, "Error during file splitting: ", null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
