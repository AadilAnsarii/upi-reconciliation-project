package com.jpb.reconciliation.reconciliation.service.v2;

import org.springframework.http.ResponseEntity;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;

public interface ReconFieldMastService {

    ResponseEntity<RestWithMapStatusList> getAllFieldTypes();

    ResponseEntity<RestWithMapStatusList> getAllFieldFormats();
}
