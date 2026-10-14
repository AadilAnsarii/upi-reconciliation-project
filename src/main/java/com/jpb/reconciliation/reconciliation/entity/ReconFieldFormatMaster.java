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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "rcn_field_format_mast")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // Critical
@ToString(exclude = "fieldDetails")
public class ReconFieldFormatMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FIELD_FORMAT")
	@SequenceGenerator(name = "SEQ_FIELD_FORMAT", sequenceName = "SEQ_FIELD_FORMAT", allocationSize = 1)
	@Column(name = "RFF_FIELD_FORMAT_ID")
	@EqualsAndHashCode.Include
	private Long reconFieldFormatId;

	@Column(name = "RFF_FIELD_TYPE_ID")
	private Long reconFieldTypeId;

	@Column(name = "RFF_FIELD_FORMAT_DESC")
	private String reconFieldFormatDesc;

	@Column(name = "RFF_INST_CODE")
	private Long reconInsertCode;

	@Column(name = "RFF_INS_USER")
	private Long reconInsertUser;

	@Column(name = "RFF_INS_DATE")
	private Date reconInsertDate;

	@Column(name = "RFF_LUPD_USER")
	private Long reconLastUpdatedUser;

	@Column(name = "RFF_LUPD_DATE")
	private Date reconLastUpdatedDate;

	@OneToMany(mappedBy = "reconFieldFormatMaster", fetch = FetchType.LAZY)
	private Set<ReconFieldDetailsMaster> fieldDetails;

}
