package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TTUMReportDto;

@Service
public interface TTUMReportService {

	ResponseEntity<RestWithStatusList> getAllTTUMList();

	ResponseEntity<RestWithStatusList> generateTTUMReport(TTUMReportDto ttumGenerateReportRequest);

}
