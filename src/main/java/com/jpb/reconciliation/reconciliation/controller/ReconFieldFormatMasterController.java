package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList; 
import com.jpb.reconciliation.reconciliation.service.ReconFieldFormatMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

@RestController
@RequestMapping("/api/v1/recon-field-formats")
@RequiredArgsConstructor
public class ReconFieldFormatMasterController {

    private final ReconFieldFormatMasterService service;

    @GetMapping
    public ResponseEntity<RestWithStatusList> getAllFieldFormats() {
        RestWithStatusList response = service.getAllFieldFormats();

        HttpStatus httpStatus = CommonConstants.SUCCESS.equals(response.getStatus())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(httpStatus).body(response);
    }
}