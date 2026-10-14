package com.jpb.reconciliation.reconciliation.entity.merchantpayout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "RCN_PARTNER")
@Data
@EqualsAndHashCode(exclude = "merchants")
@ToString(exclude = "merchants")
public class PartnerEntity {

	@Id
	@Column(name = "PARTNER_ID")
	private Long partnerId;

	@Column(name = "PARTNER_NAME")
	private String partnerName;

	@Column(name = "RUNNING_BALANCE")
	private BigDecimal runningBalance;

	@Column(name = "RETENTION_AMOUNT")
	private BigDecimal retentionAmount;
	
	@Column(name = "PARTNER_AVL_BALANCE")
	private BigDecimal partnerAvailableBalance;

	@Column(name = "PAYOUT_FLAG")
	private Boolean payoutFlag;

	@Column(name = "CREATED_AT", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "CREATED_BY")
	private BigDecimal createdBY;

	@Column(name = "UPDATED_AT")
	private LocalDateTime updatedAt;

	@Column(name = "UPDATED_BY")
	private BigDecimal updatedBy;

	@OneToMany(mappedBy = "partners", cascade = CascadeType.ALL)
	@JsonManagedReference
	private List<MerchantEntity> merchants;
}
