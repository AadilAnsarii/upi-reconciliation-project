package com.jpb.reconciliation.reconciliation.service.merchantpayout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.merchantpayout.MerchantDto;
import com.jpb.reconciliation.reconciliation.dto.merchantpayout.MerchantPayoutResponseDto;
import com.jpb.reconciliation.reconciliation.entity.merchantpayout.PartnerEntity;
import com.jpb.reconciliation.reconciliation.repository.merchantpayout.PartnerRepository;

@Service
public class MerchantPayoutServiceImpl implements MerchantPayoutService {

	Logger logger = LoggerFactory.getLogger(MerchantPayoutServiceImpl.class);

	@Autowired
	PartnerRepository partnerRepository;

	@Override
	public ResponseEntity<RestWithStatusList> searchMerchantPayout(@Valid MerchantPayoutResponseDto merchantPayoutDto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestWithStatusList> getAllMerchantPayout() {
		RestWithStatusList restWithStatusList = null;
		List<Object> merchantPayoutList = new ArrayList<>();
		List<PartnerEntity> partnerList = partnerRepository.findAll();
		logger.info("PARTNER LIST WITH MERCHNATS ::::::::" + partnerList);
		if (!partnerList.isEmpty()) {
			merchantPayoutList.addAll(partnerList);
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Partner Mechant Available", merchantPayoutList);
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Partner Mechant Not Available", merchantPayoutList);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ByteArrayInputStream payoutsToCSV(List<MerchantPayoutResponseDto> dtoList) {
		CSVFormat format = CSVFormat.DEFAULT.withHeader("PARTNER_ID", "PARTNER_NAME", "RUNNING_BALANCE",
				"RETENTION_AMOUNT", "PAYOUT_FLAG", "CREATED_AT", "M_ID", "MERCHANT_NAME", "MERCHANT_VPA",
				"MERCHANT_AVL_BALANCE");

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format)) {

			for (MerchantPayoutResponseDto dto : dtoList) {
				if (dto.getMerchants() != null && !dto.getMerchants().isEmpty()) {
					for (MerchantDto m : dto.getMerchants()) {
						csvPrinter.printRecord(dto.getPartnerId(), dto.getPartnerName(), dto.getRunningBalance(),
								dto.getRetentionAmount(), dto.getPayoutFlag(), dto.getPayoutCreatedAt(),
								m.getMerchantId(), m.getMerchantName());
					}
				} else {
					csvPrinter.printRecord(dto.getPartnerId(), dto.getPartnerName(), dto.getRunningBalance(),
							dto.getRetentionAmount(), dto.getPayoutFlag(), dto.getPayoutCreatedAt(), "N/A", "N/A");
				}
			}

			csvPrinter.flush();
			return new ByteArrayInputStream(out.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException("Fail to import data to CSV file: " + e.getMessage());
		}
	}

}
