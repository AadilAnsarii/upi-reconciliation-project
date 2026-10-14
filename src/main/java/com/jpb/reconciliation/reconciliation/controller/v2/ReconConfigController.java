package com.jpb.reconciliation.reconciliation.controller.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.v2.ReconConfigService;

/**
 * REST controller for the 6-step Recon Config wizard  (API v2).
 * All responses — success and error — are wrapped in RestWithStatusList.
 *
 * ┌───────────────────────────────────────────────────────────────────────┐
 * │  Method  │ Path                               │ Wizard step           │
 * ├───────────────────────────────────────────────────────────────────────┤
 * │  POST    │ /api/v2/recon/config/add           │ Submit full wizard    │
 * │  GET     │ /api/v2/recon/config/view/all      │ List all configs      │
 * │  GET     │ /api/v2/recon/config/view/{id}     │ Step 6 Review by ID   │
 * │  GET     │ /api/v2/recon/config/view/name/{n} │ Lookup by name        │
 * │  PUT     │ /api/v2/recon/config/update/{id}   │ Full update           │
 * │  DELETE  │ /api/v2/recon/config/delete/{id}   │ Delete config         │
 * └───────────────────────────────────────────────────────────────────────┘
 *
 * Response envelope (RestWithStatusList):
 * {
 *   "status":    CommonConstants.SUCCESS | "FAILED" | "VALIDATION_ERROR" | "NOT_FOUND",
 *   "statusMsg": "human readable message",
 *   "data":      [ ... ]   // list of result objects, null on error
 * }
 */
@RestController
@RequestMapping("/api/v2/recon/config")
@CrossOrigin(origins = "*")
public class ReconConfigController {

    private static final Logger log = LoggerFactory.getLogger(ReconConfigController.class);

    // ── Status constants ───────────────────────────────────────────────────────
    private static final String STATUS_SUCCESS          = CommonConstants.SUCCESS;
    private static final String STATUS_FAILED           = "FAILED";
    private static final String STATUS_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String STATUS_NOT_FOUND        = "NOT_FOUND";

    @Autowired
    private ReconConfigService reconConfigService;


    @PostMapping("/add")
    public ResponseEntity<RestWithStatusList> addReconConfig(
            @Valid @RequestBody ReconConfigRequest request,
            BindingResult bindingResult) {

        // ── Validation errors 
        if (bindingResult.hasErrors()) {
            Map<String, String> fieldErrors = new HashMap<>();
            bindingResult.getFieldErrors()
                    .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));
            log.warn("POST /add validation failed: {}", fieldErrors);

            List<Object> errorData = new ArrayList<Object>();
            errorData.add(fieldErrors);

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_VALIDATION_ERROR,
                            "Validation failed. Please correct the highlighted fields.",
                            errorData),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            log.info("POST /add  reconName={}", request.getReconName());
            ReconConfigResponse response = reconConfigService.createReconConfig(request);

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_SUCCESS,
                            "Recon config created successfully.",
                            Collections.singletonList(response)),
                    HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            log.warn("POST /add bad request: {}", e.getMessage());
            return buildError(STATUS_FAILED, e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("POST /add unexpected error", e);
            return buildError(STATUS_FAILED,
                    "Failed to create recon config: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // READ ALL  –  GET /api/v2/recon/config/view/all
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/view/all")
    public ResponseEntity<RestWithStatusList> getAllReconConfigs() {
        try {
            log.debug("GET /view/all");
            List<ReconConfigResponse> list = reconConfigService.getAll();

            List<Object> data = new ArrayList<Object>(list);

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_SUCCESS,
                            "Recon configs fetched successfully. Total: " + list.size(),
                            data),
                    HttpStatus.OK);

        } catch (Exception e) {
            log.error("GET /view/all unexpected error", e);
            return buildError(STATUS_FAILED,
                    "Failed to fetch recon configs: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // READ BY ID  –  GET /api/v2/recon/config/view/{processId}
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/view/{processId}")
    public ResponseEntity<RestWithStatusList> getReconConfigById(
            @PathVariable Long processId) {
        try {
            log.debug("GET /view/{}", processId);
            ReconConfigResponse response = reconConfigService.getById(processId);

            if (response == null) {
                return buildError(STATUS_NOT_FOUND,
                        "Recon config not found with id: " + processId,
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_SUCCESS,
                            "Recon config fetched successfully.",
                            Collections.singletonList(response)),
                    HttpStatus.OK);

        } catch (Exception e) {
            log.error("GET /view/{} unexpected error", processId, e);
            return buildError(STATUS_FAILED,
                    "Failed to fetch recon config: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // READ BY NAME  –  GET /api/v2/recon/config/view/name/{processName}
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/view/name/{processName}")
    public ResponseEntity<RestWithStatusList> getReconConfigByName(
            @PathVariable String processName) {
        try {
            log.debug("GET /view/name/{}", processName);

            ReconConfigResponse response = reconConfigService.getAll().stream()
                    .filter(r -> processName.equalsIgnoreCase(r.getReconName()))
                    .findFirst()
                    .orElse(null);

            if (response == null) {
                return buildError(STATUS_NOT_FOUND,
                        "Recon config not found with name: " + processName,
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_SUCCESS,
                            "Recon config fetched successfully.",
                            Collections.singletonList(response)),
                    HttpStatus.OK);

        } catch (Exception e) {
            log.error("GET /view/name/{} unexpected error", processName, e);
            return buildError(STATUS_FAILED,
                    "Failed to fetch recon config by name: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE  –  PUT /api/v2/recon/config/update/{processId}
    // ══════════════════════════════════════════════════════════════════════════

    @PutMapping("/update/{processId}")
    public ResponseEntity<RestWithStatusList> updateReconConfig(
            @PathVariable Long processId,
            @Valid @RequestBody ReconConfigRequest request,
            BindingResult bindingResult) {

        // ── Validation errors ────────────────────────────────────────────────
        if (bindingResult.hasErrors()) {
            Map<String, String> fieldErrors = new HashMap<>();
            bindingResult.getFieldErrors()
                    .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));
            log.warn("PUT /update/{} validation failed: {}", processId, fieldErrors);

            List<Object> errorData = new ArrayList<Object>();
            errorData.add(fieldErrors);

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_VALIDATION_ERROR,
                            "Validation failed. Please correct the highlighted fields.",
                            errorData),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            log.info("PUT /update/{}  reconName={}", processId, request.getReconName());
            ReconConfigResponse response = reconConfigService.updateReconConfig(processId, request);

            if (response == null) {
                return buildError(STATUS_NOT_FOUND,
                        "Recon config not found with id: " + processId,
                        HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_SUCCESS,
                            "Recon config updated successfully.",
                            Collections.singletonList(response)),
                    HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("PUT /update/{} bad request: {}", processId, e.getMessage());
            return buildError(STATUS_FAILED, e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            log.error("PUT /update/{} unexpected error", processId, e);
            return buildError(STATUS_FAILED,
                    "Failed to update recon config: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE  –  DELETE /api/v2/recon/config/delete/{processId}
    // ══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/delete/{processId}")
    public ResponseEntity<RestWithStatusList> deleteReconConfig(
            @PathVariable Long processId) {
        try {
            log.info("DELETE /delete/{}", processId);
            reconConfigService.deleteReconConfig(processId);

            return new ResponseEntity<>(
                    new RestWithStatusList(STATUS_SUCCESS,
                            "Recon config with id " + processId + " deleted successfully.",
                            null),
                    HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("DELETE /delete/{} not found: {}", processId, e.getMessage());
            return buildError(STATUS_NOT_FOUND,
                    "Recon config not found with id: " + processId,
                    HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            log.error("DELETE /delete/{} unexpected error", processId, e);
            return buildError(STATUS_FAILED,
                    "Failed to delete recon config: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPER  –  builds a RestWithStatusList error response
    // ══════════════════════════════════════════════════════════════════════════

    private ResponseEntity<RestWithStatusList> buildError(
            String status, String message, HttpStatus httpStatus) {

        return new ResponseEntity<>(
                new RestWithStatusList(status, message, null),
                httpStatus);
    }
}