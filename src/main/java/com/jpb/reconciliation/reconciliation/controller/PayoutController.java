package com.jpb.reconciliation.reconciliation.controller;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.merchantpayout.MerchantPayoutResponseDto;
import com.jpb.reconciliation.reconciliation.service.merchantpayout.MerchantPayoutService;

@RestController
@RequestMapping(path = "/api/v1/payout/")
public class PayoutController {

	@Autowired
	MerchantPayoutService merchantPayoutService;

	@PostMapping(value = "search-merchant", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> searchMerchantPayout(
			@Valid @RequestBody MerchantPayoutResponseDto merchantPayoutDto) {
		return merchantPayoutService.searchMerchantPayout(merchantPayoutDto);
	}

	@GetMapping(value = "get-partner-merchant", produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<RestWithStatusList> getMerchantPayout() {
		return merchantPayoutService.getAllMerchantPayout();
	}

	@PostMapping(value = "/export-partners-csv", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Resource> exportPartners(@RequestBody List<MerchantPayoutResponseDto> dtoList) {
		ByteArrayInputStream in = merchantPayoutService.payoutsToCSV(dtoList);
		InputStreamResource file = new InputStreamResource(in);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=merchant_payouts.csv")
				.contentType(MediaType.parseMediaType("text/csv")).body(file);
	}

}
