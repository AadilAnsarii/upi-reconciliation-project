package com.jpb.reconciliation.reconciliation.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UPITransactionRequestDto {
	@Schema(description = "Unique identifier for the transaction posting batch id", example = "202501281C23423423423424")
	private String postingBatchId;
	@Schema(description = "Reference number associated with transaction", example = "801777205150")
	private String referenceNumber;
	@Schema(description = "Account number of the payer", example = "RPHUB03563509300")
	private String payerAccountNumber;
	@Schema(description = "Account number of the payee", example = "RPHUB03563509300")
	private String payeeAccountNumber;
	@Schema(description = "Transaction amount", example = "100.00")
	private BigDecimal transactionAmount;
	private Date fromDate;
	private Date toDate;
	@Schema(description = "Name of the report file", example = "transaction")
	private String fileName;
	@Schema(description = "Indicates whether a report is needed. 'Y' for Yes, 'N' for No", example = CommonConstants.ACTIVE_Y)
	private String reportYN;
}
