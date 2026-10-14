package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "RCN_PASSWORD_MANAGER")
//@ToString
public class PasswordManager {
    
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PASSWORD")
	@SequenceGenerator(name = "SEQ_PASSWORD", sequenceName = "SEQ_PASSWORD",allocationSize = 1)
	@Column(name = "pwd_id")
	private Long pwdId;
    
	@Column(name = "user_password")
	private String userPassword;

	@Column(name = "expiration_date")
	private LocalDateTime expirationDate;

	private String token;

	@OneToOne
	@JoinColumn(name = "user_id") // Foreign key to MasterUserEntity
	@JsonBackReference
	private ReconUser reconUser; // Back-reference to MasterUserEntity

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

}
