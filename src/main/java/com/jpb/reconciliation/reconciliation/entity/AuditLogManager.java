package com.jpb.reconciliation.reconciliation.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "UMS_AUDIT_LOG")
public class AuditLogManager {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
	@SequenceGenerator(name = "SEQ", sequenceName = "SEQ_AUDIT", allocationSize = 1)
	@Column(name="AUDIT_LOG_ID")
	private Long auditLogId;

	@Column(name = "MODULE")
	private String module;

	@Column(name = "SUB_MODULE")
	private String subModule;

	@Column(name = "EVENT")
	private String event;

	@Column(name = "EVENT_DATA", length = 4000)
	private String eventData;

	@Column(name = "EVENT_STATUS")
	private String eventStatus;

	@Column(name = "USER_ID")
	private Long userId;

	@Column(name = "USER_IP")
	private String userIp;

	@Column(name = "AUDIT_DATE_TIME")
	private Date auditDateTime;

	@Column(name = "OLD_DATA")
	private String oldData;

	@Column(name = "ROLE_ID")
	private Long roleId;

}
