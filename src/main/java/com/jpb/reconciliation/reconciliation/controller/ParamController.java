//package com.jpb.reconciliation.reconciliation.controller;
//
//import java.util.Collections;
//import java.util.List;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
//import com.jpb.reconciliation.reconciliation.dto.ParamDTO;
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.jpb.reconciliation.reconciliation.service.ParamService;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.tags.Tag;
//
//@Tag(name = "Param API", description = "Operations for application Parameters")
//@RestController
//@RequestMapping("/api/v1/params")
//public class ParamController {
//
//    private final ParamService pservice;
//
//    public ParamController(ParamService pservice) {
//        this.pservice = pservice;
//    }
//
//    @Operation(summary = "Get Param by name")
//    @ApiResponse(responseCode = CommonConstants.STATUS_200, description = "Param fetched successfully")
//    @GetMapping("/{paramName}")
//    public ResponseEntity<RestWithStatusList> getByParamName(
//                            @PathVariable String paramName) {
//
//        List<ParamDTO> list = pservice.getByParamName(paramName);
//
//        if (list.isEmpty()) {
//            return ResponseEntity.ok(
//                RestWithStatusList.builder()
//                    .status(CommonConstants.FAILURE)
//                    .statusMsg("No active param found with name: " + paramName)
//                    .data(Collections.emptyList())
//                    .build()
//            );
//        }
//
//        return ResponseEntity.ok(
//            RestWithStatusList.builder()
//                .status(CommonConstants.SUCCESS)
//                .statusMsg("Param fetched successfully")
//                .data(Collections.unmodifiableList(list))
//                .build()
//        );
//    }
//}