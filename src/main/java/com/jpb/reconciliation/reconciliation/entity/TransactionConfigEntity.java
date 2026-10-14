package com.jpb.reconciliation.reconciliation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Table(name = "RCN_TRANS_CONFIGURATION")
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FIELD_FORMAT")
    @SequenceGenerator(name = "SEQ_FIELD_FORMAT", sequenceName = "SEQ_FIELD_FORMAT", allocationSize = 1)
    @Column(name = "TRANS_CONFIG_ID")
    Long transConfigId;

    @Column(name = "TEMPLATE_ID")
    long templateId;

    @Column(name = "COLUMN_NAMES")
    String columnNames;

    @Column(name = "USER_ID")
    String userId;

}
