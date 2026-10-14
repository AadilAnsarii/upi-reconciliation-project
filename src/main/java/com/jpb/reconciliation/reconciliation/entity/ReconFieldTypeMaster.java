package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "rcn_field_type_mast")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // Critical
@ToString(exclude = "fieldDetails")
public class ReconFieldTypeMaster {

	@Id
	@Column(name = "RFT_FIELD_TYPE_ID")
	@EqualsAndHashCode.Include
	private Long fieldTypeId;

	@Column(name = "RFT_FIELD_TYPE_DESC")
	private String fieldTypeDes;

	@Column(name = "RFT_INS_USER")
	private Long insertUser;

	@Column(name = "RFT_INS_DATE")
	private Date insertDate;

	@Column(name = "RFT_LUPD_USER")
	private Long lastUpdatedUser;

	@Column(name = "RFT_LUPD_DATE")
	private Date lastUpdatedDate;

	@Column(name = "RFT_INST_CODE")
	private Long insertCode;

	@OneToMany(mappedBy = "reconFieldTypeMaster", fetch = FetchType.LAZY)
	private Set<ReconFieldDetailsMaster> fieldDetails;

//	
}
