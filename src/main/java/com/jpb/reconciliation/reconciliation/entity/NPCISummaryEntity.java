package com.jpb.reconciliation.reconciliation.entity;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name="NPCI_SUMMARY_TBL")
@Data
public class NPCISummaryEntity {
	
	@Id
	@Column(name = "NPCI_SUMMARY_ID")
	private Long npciSummaryId;
	
    @Column(name = "PRODUCT_TYPE")
    private String productType;

    @Column(name = "NPCI_FILE_DATE")
    private String npciFileDate;

    @Column(name = "NPCI_CYLE")
    private String npciCycle;

    @Column(name = "NPCI_RAW_DATA_COUNT")
    private BigDecimal npciRawDataCount;

    @Column(name = "NPCI_RAW_DATA_AMOUNT")
    private BigDecimal npciRawDataAmount;

    @Column(name = "NTSL_RAW_DATA_COUNT")
    private BigDecimal ntslRawDataCount;

    @Column(name = "NTSL_RAW_DATA_AMOUNT")
    private BigDecimal ntslRawDataAmount;
}
