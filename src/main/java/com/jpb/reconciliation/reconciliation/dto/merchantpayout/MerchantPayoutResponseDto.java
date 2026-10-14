package com.jpb.reconciliation.reconciliation.dto.merchantpayout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;

import lombok.Data;

@Data
public class MerchantPayoutResponseDto {
	private Long partnerId;
	private String partnerName;
	private Boolean runningBalance;
	private BigDecimal retentionAmount;
	private Boolean payoutFlag;
	private LocalDateTime payoutCreatedAt;
	private BigDecimal partnerAvailableBalance;
	
	private List<MerchantDto> merchants;

}
