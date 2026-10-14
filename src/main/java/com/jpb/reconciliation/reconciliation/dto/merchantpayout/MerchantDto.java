package com.jpb.reconciliation.reconciliation.dto.merchantpayout;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MerchantDto {
	
	private Long merchantId;
	private String merchantVpa;
	private String merchantName;
	private LocalDateTime merchantCreatedAt;
	private BigDecimal merchantAvailableBalance;

}
