package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.CBankProductMap;

import org.springframework.http.ResponseEntity;

public interface CBankProductMapService {

    ResponseEntity<RestWithStatusList> mapBankProduct(CBankProductMap mapping);

    ResponseEntity<RestWithStatusList> getMappingsByBankId(Long bankId);

    ResponseEntity<RestWithStatusList> getMappingsByProductId(Long productId);

    ResponseEntity<RestWithStatusList> removeMappingsByBankId(Long bankId);

    ResponseEntity<RestWithStatusList> updateMappingStatus(Long id, String status);
}
