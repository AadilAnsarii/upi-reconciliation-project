package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "C_PARAM")
public class Param {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "param_seq")
    @SequenceGenerator(name = "param_seq", sequenceName = "C_PARAM_SEQ", allocationSize = 1)
    @Column(name = "PARAM_ID")
    private Long paramId;

    @Column(name = "PARAM_NAME", nullable = false, length = 50)
    private String paramName;

    @Column(name = "PARAM_DESC", length = 150)
    private String paramDesc;

    @Column(name = "PARAM_VALUE", length = 4000)
    private String paramValue;

    @Column(name = "ACTIVE_YN", nullable = false, length = 1)
    private String activeYn;

    @Column(name = "CREATED_BY", nullable = false, length = 25)
    private String createdBy;

    @Column(name = "CREATED_ON", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;

    @Column(name = "UPDATED_BY", length = 25)
    private String updatedBy;

    @Column(name = "UPDATED_ON")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedOn;
}