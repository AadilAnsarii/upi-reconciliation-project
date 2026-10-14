//package com.jpb.reconciliation.reconciliation.controller;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//import javax.validation.Valid;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
//import com.jpb.reconciliation.reconciliation.dto.LookupDTO;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.service.LookupService;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//
//@Tag(name = "Lookup API", description = "CRUD operations for Lookup master data")
//@RestController
//@RequestMapping("/api/v1/lookups")
//public class LookupController {
//
//    private final LookupService lservice;
//
//    public LookupController(LookupService lservice) {
//        this.lservice = lservice;
//    }
//
//    // ─── CREATE ───────────────────────────────────────────────────────
//    @Operation(summary = "Create a new Lookup", description = "Creates a new active lookup entry")
//    @ApiResponses({
//        @ApiResponse(responseCode = "201", description = "Lookup created successfully"),
//        @ApiResponse(responseCode = "409", description = "Lookup code already exists"),
//        @ApiResponse(responseCode = "400", description = "Validation failed")
//    })
//    @PostMapping
//    public ResponseEntity<RestWithStatusList> create(@Valid @RequestBody LookupDTO dto) {
//        LookupDTO created = lservice.createLookup(dto);
//        return ResponseEntity.status(HttpStatus.CREATED).body(
//            RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Lookup created successfully")
//                .data(Collections.singletonList(created))
//                .build()
//        );
//    }
//
//    // ─── GET BY ID ────────────────────────────────────────────────────
//    @Operation(summary = "Get Lookup by ID")
//    @ApiResponses({
//        @ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Lookup fetched successfully"),
//        @ApiResponse(responseCode = CommonConstants.STATUS_404, description = "Lookup not found")
//    })
//    @GetMapping("/{lookupId}")
//    public ResponseEntity<RestWithStatusList> getLookupById(@PathVariable Long lookupId) {
//        LookupDTO dto = lservice.getLookupById(lookupId);
//        return ResponseEntity.ok(
//            RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Lookup fetched successfully")
//                .data(Collections.singletonList(dto))
//                .build()
//        );
//    }
//
//    // ─── GET ALL ──────────────────────────────────────────────────────
//    @Operation(summary = "Get all active Lookups")
//    @ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Lookups fetched successfully")
//    @GetMapping
//    public ResponseEntity<RestWithStatusList> getAllLookups() {
//        Map<String, List<LookupDTO>> grouped = lservice.getGroupedLookups();
//
//        boolean hasActiveData = !grouped.isEmpty()
//                && grouped.values().stream().anyMatch(list -> !list.isEmpty());
//
//        if (!hasActiveData) {
//            return ResponseEntity.ok(
//                RestWithStatusList.builder()
//                    .status(CommonConstants.FAILURE)
//                    .statusMsg("No active lookup records found")
//                    .data(Collections.emptyList())
//                    .build()
//            );
//        }
//
//        return ResponseEntity.ok(
//            RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Lookups fetched successfully")
//                .data(Collections.singletonList(grouped))
//                .build()
//        );
//    }
//
//    // ─── GET BY NAME ──────────────────────────────────────────────────
//    @Operation(summary = "Get Lookups by name")
//    @ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Lookups fetched successfully")
//    @GetMapping("/lookup-name/{name}")
//    public ResponseEntity<RestWithStatusList> getByName(@PathVariable String name) {
//        List<LookupDTO> list = lservice.getByNameAndActive(name);
//        return ResponseEntity.ok(
//            RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Lookups fetched by name: " + name)
//                .data(Collections.unmodifiableList(list))
//                .build()
//        );
//    }
//
//    // ─── GET GROUPED ──────────────────────────────────────────────────
//    @Operation(
//        summary = "Get Lookups grouped by parent",
//        description = "Returns all active lookups grouped by their parent lookup name."
//    )
//    @ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Grouped lookups fetched successfully")
//    @GetMapping("/grouped")
//    public ResponseEntity<RestWithStatusList> getGrouped() {
//        Map<String, List<LookupDTO>> grouped = lservice.getGroupedLookups();
//
//        boolean hasActiveData = !grouped.isEmpty()
//                && grouped.values().stream().anyMatch(list -> !list.isEmpty());
//
//        if (!hasActiveData) {
//            return ResponseEntity.ok(
//                RestWithStatusList.builder()
//                    .status(CommonConstants.FAILURE)
//                    .statusMsg("No active lookup records found")
//                    .data(Collections.emptyList())
//                    .build()
//            );
//        }
//
//        return ResponseEntity.ok(
//            RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Grouped lookups fetched successfully")
//                .data(Collections.singletonList(grouped))
//                .build()
//        );
//    }
//
//    // ─── DELETE ───────────────────────────────────────────────────────
//    @Operation(summary = "Soft delete Lookup by ID", description = "Sets activeYn to N")
//    @ApiResponses({
//        @ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Lookup deactivated successfully"),
//        @ApiResponse(responseCode = CommonConstants.STATUS_404, description = "Lookup not found")
//    })
//    @DeleteMapping("/{lookupId}")
//    public ResponseEntity<RestWithStatusList> delete(@PathVariable Long lookupId) {
//        lservice.deleteLookup(lookupId);
//        return ResponseEntity.ok(
//            RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Lookup deactivated successfully")
//                .data(Collections.emptyList())
//                .build()
//        );
//    }
//}