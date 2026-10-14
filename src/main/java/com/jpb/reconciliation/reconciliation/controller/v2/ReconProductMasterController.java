package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconProductMaster;
import com.jpb.reconciliation.reconciliation.service.v2.ReconProductMasterService;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/product")
@CrossOrigin(origins = "*")
public class ReconProductMasterController {

    private static final Logger logger = LoggerFactory.getLogger(ReconProductMasterController.class);

    @Autowired
    private ReconProductMasterService reconProductMasterService;

    @Operation(summary = "Create a new product")
    @PostMapping(value = "/create", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> createProduct(
            @RequestBody ReconProductMaster product,
            Authentication authentication) {
        String createdBy = resolveUser(authentication);
        logger.info("Create product request: {} by {}", product.getProductName(), createdBy);
        return reconProductMasterService.createProduct(product, createdBy);
    }

    @Operation(summary = "Get all products")
    @GetMapping(value = "/get-all", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getAllProducts() {
        return reconProductMasterService.getAllProducts();
    }

    @Operation(summary = "Get product by ID")
    @GetMapping(value = "/get/{productId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getProductById(@PathVariable Long productId) {
        return reconProductMasterService.getProductById(productId);
    }

    @Operation(summary = "Get products by status")
    @GetMapping(value = "/get-by-status", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getProductsByStatus(@RequestParam String status) {
        return reconProductMasterService.getProductsByStatus(status);
    }

    @Operation(summary = "Update product details")
    @PutMapping(value = "/update/{productId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateProduct(
            @PathVariable Long productId,
            @RequestBody ReconProductMaster product,
            Authentication authentication) {
        String updatedBy = resolveUser(authentication);
        logger.info("Update product request for ID: {} by {}", productId, updatedBy);
        return reconProductMasterService.updateProduct(productId, product, updatedBy);
    }

    @Operation(summary = "Update product status")
    @PatchMapping(value = "/update-status/{productId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> updateStatus(
            @PathVariable Long productId,
            @RequestParam String status) {
        return reconProductMasterService.updateStatus(productId, status);
    }

    @Operation(summary = "Soft delete product")
    @DeleteMapping(value = "/delete/{productId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> deleteProduct(@PathVariable Long productId) {
        return reconProductMasterService.deleteProduct(productId);
    }

    @Operation(summary = "Check if product name exists")
    @GetMapping(value = "/check-name", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> checkProductNameExists(@RequestParam String productName) {
        return reconProductMasterService.checkProductNameExists(productName);
    }

    private String resolveUser(Authentication authentication) {
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
    }
}
