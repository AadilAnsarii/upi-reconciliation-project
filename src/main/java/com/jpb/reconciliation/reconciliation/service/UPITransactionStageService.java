package com.jpb.reconciliation.reconciliation.service;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UPITransactionRequestDto;

import net.sf.jasperreports.engine.JRException;

@Service
public interface UPITransactionStageService {

	ResponseEntity<RestWithStatusList> searchTransaction(UPITransactionRequestDto upiTransactionRequestDto) throws JRException, IOException;

}
