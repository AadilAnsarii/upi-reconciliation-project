package com.jpb.reconciliation.reconciliation.service.transactionsearch;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.TranSearchReqDto;
import com.jpb.reconciliation.reconciliation.dto.TranSearchResponse;

public interface TranSearchService {
    ResponseEntity<TranSearchResponse> getTransactionRecords(MultipartFile file, TranSearchReqDto tranSearchReqDto);

	ResponseEntity<RestWithMapStatusList> searchReconTransactionRecords(TranSearchReqDto tranSearchReqDto);
}
