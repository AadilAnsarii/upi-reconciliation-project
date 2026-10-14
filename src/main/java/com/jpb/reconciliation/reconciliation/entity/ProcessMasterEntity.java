package com.jpb.reconciliation.reconciliation.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Data;

@Entity
@Data
@Table(name = "PROCESS_MASTER_TBL")
public class ProcessMasterEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PROCESS_MASTER")
	@SequenceGenerator(name = "SEQ_PROCESS_MASTER", sequenceName = "SEQ_PROCESS_MASTER",allocationSize = 1)
	@Column(name = "PROCESS_ID")
	private Long processMastId;

	@Column(name = "PROCESS_TYPE")
	private String processType;

	@Column(name = "LONG_NAME")
	private String longName;

	@Column(name = "SHRT_NAME")
	private String shrtName;
     
	@OneToMany(mappedBy = "processmaster", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JsonManagedReference
	private List<ReconFileDetailsMaster> fileDetailsMasters;
	
	@OneToMany(mappedBy = "processmaster", fetch= FetchType.LAZY, cascade = CascadeType.ALL)
	@JsonManagedReference
	private List<ReconProcessDefMaster> processDefMaster;
}
