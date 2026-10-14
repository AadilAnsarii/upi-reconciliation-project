package com.jpb.reconciliation.reconciliation.mapper;

import java.util.ArrayList;
import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.merchantpayout.MerchantPayoutResponseDto;
import com.jpb.reconciliation.reconciliation.entity.merchantpayout.MerchantEntity;
import com.jpb.reconciliation.reconciliation.entity.merchantpayout.PartnerEntity;

public class MerchantPayoutMapper {

	public static List<MerchantPayoutResponseDto> mapPartnerWithMerchnat(List<PartnerEntity> partnerList) {
		List<MerchantPayoutResponseDto> MerchantPayoutDto = new ArrayList<>();
		for (PartnerEntity merchantPartner : partnerList) {
			MerchantPayoutResponseDto merchantPayout = new MerchantPayoutResponseDto();
			merchantPayout.setPartnerId(merchantPartner.getPartnerId());
			merchantPayout.setPartnerName(merchantPartner.getPartnerName());
			merchantPayout.setPayoutCreatedAt(merchantPartner.getCreatedAt());
			merchantPayout.setPayoutFlag(merchantPartner.getPayoutFlag());
//			merchantPayout.setRedemptionAmount(merchantPartner.getRedemptionAmount());
//			merchantPayout.setRunningFlag(merchantPartner.getRunningFlag());

			List<MerchantEntity> merchantList = merchantPartner.getMerchants();

//			for (MerchantEntity merchant : merchantList) {
//				merchantPayout.setMerchantId(merchant.getMerchantId());
//				merchantPayout.setMerchantCreatedAt(merchant.getCreatedAt());
//				merchantPayout.setMerchantName(merchant.getMerchantName());
//				merchantPayout.setMerchantVpa(merchant.getMerchantVpa());
//			}

		}
		return null;
	}

}
