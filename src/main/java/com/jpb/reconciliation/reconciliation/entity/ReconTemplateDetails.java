package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "rcn_template_dtl")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // Only use explicitly included fields
@ToString(exclude = {"reconFileDetailsMaster", "fieldDetails"})  // Exclude collections from toString
public class ReconTemplateDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TEMPLATE")
	@SequenceGenerator(name = "SEQ_TEMPLATE", sequenceName = "SEQ_TEMPLATE",allocationSize = 1)
	@Column(name = "RTD_TEMPLATE_ID")
	@EqualsAndHashCode.Include
	private Long reconTemplateId;

	@Column(name = "RTD_SUB_TEMPLATE_ID")
	private Long subTemplateId;

	@Column(name = "RTD_TYPE_ID")
	private Long typeId;

	@Column(name = "RTD_TEMPLATE_TYPE")
	private String templateType;

	@Column(name = "RTD_TEMPLATE_NAME")
	private String templateName;

	@Column(name = "RTD_STAGE_TAB_NAME")
	private String stageTabName;

	@Column(name = "RTD_COLUMN_COUNT")
	private Long columnCount;

	@Column(name = "RTD_EXIST_FLAG")
	private String existFlag;

	@Column(name = "RTD_RVSL_INDICATOR")
	private String reversalIndicator;

	@Column(name = "RTD_DATA_REFERENCE_FLG")
	private String dataReferenceFlag;

	@Column(name = "RTD_ONLINE_REFUND_FLAG")
	private String onlRefundFlag;

	@Column(name = "RTD_ISSACQ_FLAG")
	private String issacqFlag;

	@Column(name = "RTD_DATATABLE_IND")
	private String dataTableInd;

	@Column(name = "RTD_MAST_FLAG")
	private String masterFlag;

	@Column(name = "RTD_MAST_TEMPID")
	private String masterTemplateId;

	@Column(name = "RTD_INST_CODE")
	private Long insertCode;

	@Column(name = "RTD_INS_USER")
	private Long insertUser;

	@Column(name = "RTD_INS_DATE")
	private Date insertDate;

	@Column(name = "RTD_LUPD_USER")
	private Long lastUpdatedUser;

	@Column(name = "RTD_LUPD_DATE")
	private Date reconLastUpdatedDate;

	@Column(name = "RTD_SETTL_FLAG")
	private String settlementFlag;

	@Column(name = "RTD_PRODUCT_TYPE")
	private String productType;

	@OneToMany(mappedBy = "reconTemplateDetails")
	@JsonBackReference
	private Set<ReconFileDetailsMaster> reconFileDetailsMaster;

	@OneToMany(mappedBy = "reconTemplateDetails", fetch = FetchType.LAZY)
	private Set<ReconFieldDetailsMaster> fieldDetails;

}
