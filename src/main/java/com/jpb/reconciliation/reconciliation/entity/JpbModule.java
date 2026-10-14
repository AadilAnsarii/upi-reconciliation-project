//package com.jpb.reconciliation.reconciliation.entity;
//
//
//import lombok.*;
//import javax.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "JPB_MODULES_TEST")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class JpbModule {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    
//    @Column(name = "NAME", nullable = false, unique = true, length = 100)
//    private String name;
//    
//    @Column(name = "DESCRIPTION", length = 500)
//    private String description;
//    
//    @Column(name = "DISPLAY_ORDER")
//    private Integer displayOrder;
//    
//    @Column(name = "CREATED_AT", updatable = false)
//    private LocalDateTime createdAt;
//    
//    @Column(name = "UPDATED_AT")
//    private LocalDateTime updatedAt;
//    
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//    }
//    
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//}