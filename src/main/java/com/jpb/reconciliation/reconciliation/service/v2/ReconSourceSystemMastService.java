package com.jpb.reconciliation.reconciliation.service.v2;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface ReconSourceSystemMastService {

    ResponseEntity<RestWithStatusList> getActiveSources();

}