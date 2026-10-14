package com.jpb.reconciliation.reconciliation.service.forcematch;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface ForceMatchActionService {

	ResponseEntity<RestWithStatusList> generateForceMatchExceptionReport(ReportDto ntslReportRequest);

}
