package com.jpb.reconciliation.reconciliation.entity.merchantpayout;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "REC_MERCHANT")
@Data
@EqualsAndHashCode(exclude = "partners")
@ToString(exclude = "partners")
public class MerchantEntity {

	@Id
	@Column(name = "M_ID")
	private Long merchantId;

	@Column(name = "MERCHANT_VPA")
	private String merchantVpa;

	@Column(name = "MERCHANT_NAME")
	private String merchantName;
	
	@Column(name = "MERCHANT_AVL_BALANCE")
	private BigDecimal merchantAvailableBalance;

	@Column(name = "CREATED_AT", updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "CREATED_BY")
	private BigDecimal createdBY;

	@Column(name = "UPDATED_AT")
	private LocalDateTime updatedAt;

	@Column(name = "UPDATED_BY")
	private BigDecimal updatedBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARTNER_ID")
	@JsonBackReference
	private PartnerEntity partners;
}
