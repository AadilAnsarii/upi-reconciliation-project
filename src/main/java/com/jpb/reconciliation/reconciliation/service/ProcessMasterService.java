package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface ProcessMasterService {

	ResponseEntity<RestWithStatusList> getAllProcessData(String existsMenuFlag);

}
