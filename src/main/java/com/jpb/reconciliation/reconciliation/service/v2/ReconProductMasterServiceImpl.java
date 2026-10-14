package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconProductMasterRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ReconProductMasterServiceImpl implements ReconProductMasterService {

    private static final Logger logger = LoggerFactory.getLogger(ReconProductMasterServiceImpl.class);

    @Autowired
    private ReconProductMasterRepository reconProductMasterRepository;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createProduct(ReconProductMaster product, String createdBy) {
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Product name is required.", null));
        }
        if (reconProductMasterRepository.existsByProductName(product.getProductName().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Product name already exists.", null));
        }
        product.setProductName(product.getProductName().trim());
        product.setCreatedAt(LocalDateTime.now());
        product.setCreatedBy(createdBy);
        if (product.getStatus() == null) {
            product.setStatus(CommonConstants.ACTIVE);
        }
        ReconProductMaster saved = reconProductMasterRepository.save(product);
        logger.info("ReconProductMaster created: {} by {}", saved.getProductName(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS, "Product created successfully.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllProducts() {
        List<ReconProductMaster> products = reconProductMasterRepository.findAll();
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Products fetched.", products));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getProductById(Long productId) {
        Optional<ReconProductMaster> opt = reconProductMasterRepository.findById(productId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Product not found with ID: " + productId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Product found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getProductsByStatus(String status) {
        List<ReconProductMaster> products = reconProductMasterRepository.findByStatus(status);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Products fetched by status.", products));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateProduct(Long productId, ReconProductMaster product, String updatedBy) {
        Optional<ReconProductMaster> opt = reconProductMasterRepository.findById(productId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Product not found with ID: " + productId, null));
        }
        ReconProductMaster existing = opt.get();
        if (product.getProductName() != null) existing.setProductName(product.getProductName().trim());
        if (product.getProductDesc() != null) existing.setProductDesc(product.getProductDesc());
        reconProductMasterRepository.save(existing);
        logger.info("ReconProductMaster updated: {} by {}", productId, updatedBy);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Product updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long productId, String status) {
        Optional<ReconProductMaster> opt = reconProductMasterRepository.findById(productId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Product not found with ID: " + productId, null));
        }
        ReconProductMaster existing = opt.get();
        existing.setStatus(status);
        reconProductMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Product status updated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteProduct(Long productId) {
        Optional<ReconProductMaster> opt = reconProductMasterRepository.findById(productId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Product not found with ID: " + productId, null));
        }
        ReconProductMaster existing = opt.get();
        existing.setStatus(CommonConstants.INACTIVE);
        reconProductMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Product deactivated successfully.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkProductNameExists(String productName) {
        boolean exists = reconProductMasterRepository.existsByProductName(productName);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, exists ? "EXISTS" : "AVAILABLE", null));
    }
}



