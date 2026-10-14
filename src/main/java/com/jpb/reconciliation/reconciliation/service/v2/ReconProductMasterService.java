package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import org.springframework.http.ResponseEntity;

public interface ReconProductMasterService {

    ResponseEntity<RestWithStatusList> createProduct(ReconProductMaster product, String createdBy);

    ResponseEntity<RestWithStatusList> getAllProducts();

    ResponseEntity<RestWithStatusList> getProductById(Long productId);

    ResponseEntity<RestWithStatusList> getProductsByStatus(String status);

    ResponseEntity<RestWithStatusList> updateProduct(Long productId, ReconProductMaster product, String updatedBy);

    ResponseEntity<RestWithStatusList> updateStatus(Long productId, String status);

    ResponseEntity<RestWithStatusList> deleteProduct(Long productId);

    ResponseEntity<RestWithStatusList> checkProductNameExists(String productName);
}
