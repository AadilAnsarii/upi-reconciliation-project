package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rcn_key_idy_mast")
public class ReconKeyIdentifyMaster {
	
	@Id
	@Column(name = "RKI_KEY_ID")
	private Long keyIdentityId;
	
	@Column(name = "RKI_KEY_NAME")
	private String keyName;
}
