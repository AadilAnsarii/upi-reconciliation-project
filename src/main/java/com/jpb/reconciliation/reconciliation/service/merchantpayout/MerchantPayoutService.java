package com.jpb.reconciliation.reconciliation.service.merchantpayout;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.merchantpayout.MerchantPayoutResponseDto;

@Service
public interface MerchantPayoutService {

	ResponseEntity<RestWithStatusList> searchMerchantPayout(@Valid MerchantPayoutResponseDto merchantPayoutDto);

	ResponseEntity<RestWithStatusList> getAllMerchantPayout();

	ByteArrayInputStream payoutsToCSV(List<MerchantPayoutResponseDto> dtoList);

}
