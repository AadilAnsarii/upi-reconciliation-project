package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "RCN_ROLE_MASTER")
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ROLE_MAST")
	@SequenceGenerator(name = "SEQ_ROLE_MAST", sequenceName = "SEQ_ROLE_MAST",allocationSize = 1)
	@Column(name="ROLE_ID")
	private Long roleId;

	@Column(name = "ROLE_NAME")
	private String roleName;
	
	@Column(name = "ROLE_CODE")
	private String roleCode;

	@CreatedDate
	@Column(updatable = false, name = "crated_at")
	private LocalDateTime createdAt;

	@CreatedBy
	@Column(updatable = false, name = "created_by")
	private String createdBy;

	@LastModifiedDate
	@Column(insertable = false, name = "updated_at")
	private LocalDateTime updatedAt;

	@LastModifiedBy
	@Column(insertable = false, name = "updated_by")
	private String updatedBy;
	
//	@OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//	private Set<ReconMenuMaster> menus = new HashSet<>();
}
