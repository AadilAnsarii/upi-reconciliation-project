package com.jpb.reconciliation.reconciliation.service.account;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface FileSplitterService {

	ResponseEntity<RestWithStatusList> generateEachAcknoledgementFiles();

	void splitFileByAccountNumber(MultipartFile file) throws IOException, InterruptedException;

}
