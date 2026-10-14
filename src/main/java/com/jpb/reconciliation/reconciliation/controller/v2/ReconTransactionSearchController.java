package com.jpb.reconciliation.reconciliation.controller.v2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.TranSearchReqDto;
import com.jpb.reconciliation.reconciliation.service.transactionsearch.TranSearchService;

import net.sf.jasperreports.engine.JRException;

@RestController
@RequestMapping(path = "/api/v1/recon-transaction")
public class ReconTransactionSearchController {

	@Autowired
	private TranSearchService tranSearchService;

	@PostMapping(path = "/search", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithMapStatusList> searchReconTransaction(@RequestBody TranSearchReqDto tranSearchReqDto)
			throws JRException, IOException {

		try {
			return tranSearchService.searchReconTransactionRecords(tranSearchReqDto);
		} catch (Exception e) {
			return new ResponseEntity<>(
					new RestWithMapStatusList(CommonConstants.FAILURE, "An unexpected error occurred: " + e.getMessage(), null),
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}
}
