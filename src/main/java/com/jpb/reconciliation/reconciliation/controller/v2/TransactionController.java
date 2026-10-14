package com.jpb.reconciliation.reconciliation.controller.v2;

import java.io.File; 
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UPITransactionRequestDto;
import com.jpb.reconciliation.reconciliation.service.UPITransactionStageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.sf.jasperreports.engine.JRException;

@Tag(name = "Transaction operations", description = "Operations related to transactions, such as searching transaction, generating transaction report and download transaction report. ")
@RestController
@RequestMapping(path = "/api/v1/transaction")
public class TransactionController {

	@Autowired
	UPITransactionStageService upiTransactionStageService;

	@Operation(summary = "Search Transactions", description = "Retrieve all transaction records that match the criteria provided in the request body.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Successful Operation", content = @Content(mediaType = CommonConstants.APPLICATION_JSON, examples = @ExampleObject(value = "{\"status\": \"SUCCESS\", \"statusMsg\" : \"Request execute successfully\", \"data\":[{}]}"))),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(mediaType = CommonConstants.APPLICATION_JSON, examples = @ExampleObject(value = "{\"status\": \"FAILURE\", \"statusMsg\" : \"Invalid request parameters\", \"data\":[{}]}"))),
			@ApiResponse(responseCode = "501", description = "Internal server error", content = @Content(mediaType = CommonConstants.APPLICATION_JSON, examples = @ExampleObject(value = "{\"status\":\"FAILURE\", \"statusMsg\":\"Internal server error\", \"data\" :[{}]}"))),
			@ApiResponse(responseCode = "401", description = "Transaction data not found", content = @Content(mediaType = CommonConstants.APPLICATION_JSON, examples = @ExampleObject(value = "{\"status\":\"FAILURE\", \"statusMsg\": \"Transaction data not found\", \"data\":[{}]}"))) })
	@PostMapping(value = "/search-transaction", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> searchTransation(
			@RequestBody UPITransactionRequestDto upiTransactionRequestDto) throws JRException, IOException {

		try {
			return upiTransactionStageService.searchTransaction(upiTransactionRequestDto);
		} catch (IOException e) {
			return new ResponseEntity<>(
					new RestWithStatusList(CommonConstants.FAILURE, "Error during report generation: " + e.getMessage(), null),
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			return new ResponseEntity<>(
					new RestWithStatusList(CommonConstants.FAILURE, "An unexpected error occurred: " + e.getMessage(), null),
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@PostMapping(value = "/download", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> downloadTransactionReport(@RequestBody ReportDto report) {
		File file = new File(report.getReportLocation());
		if (!file.exists()) {
			return new ResponseEntity<>(
					new ResponseDto(CommonConstants.STATUS_404, "FILE " + CommonConstants.MESSAGE_404),
					HttpStatus.NOT_FOUND);
		}
		FileSystemResource fileSystemResource = new FileSystemResource(file);
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + report.getReportFileName())
				.header("Content-Type", "application/csv").body(fileSystemResource);
	}

}
