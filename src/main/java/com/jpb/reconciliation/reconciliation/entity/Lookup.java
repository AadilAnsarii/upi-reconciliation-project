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
@Table(name = "M_LOOKUP")
public class Lookup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lookup_seq")
    @SequenceGenerator(name = "lookup_seq", sequenceName = "M_LOOKUP_SEQ", allocationSize = 1)
    @Column(name = "LOOKUP_ID")
    private Long lookupId;

    @Column(name = "LOOKUP_CODE", nullable = false, length = 50)
    private String lookupCode;

    @Column(name = "LOOKUP_NAME", nullable = false, length = 50)
    private String lookupName;

    @Column(name = "SHORT_NAME", length = 150)
    private String shortName;

    @Column(name = "LONG_NAME", length = 255)
    private String longName;

    @Column(name = "PARENT_LOOKUP_ID")
    private Long parentLookupId;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

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

    @Column(name = "LOOKUP_VALUE", length = 255)
    private String lookupValue;

    @Column(name = "LOOKUP_DESC", length = 500)
    private String lookupDesc;
}