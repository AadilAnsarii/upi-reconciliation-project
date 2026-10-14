package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name="spring_security_users")
public class User {

	@Id
	@Column(name = "user_id")
	private Long userId;
	
	@Column(name = "user_name")
	private String userName;
	
	private String email;
	
	private String password;
	
	private String role="USER";
	

}
