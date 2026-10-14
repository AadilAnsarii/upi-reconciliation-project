package com.jpb.reconciliation.reconciliation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RECON_PRODUCT_MASTER")
public class ReconProductMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "PRODUCT_NAME", length = 100, nullable = false, unique = true)
    private String productName;

    @Column(name = "PRODUCT_DESC", length = 500)
    private String productDesc;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = CommonConstants.ACTIVE;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;
}
