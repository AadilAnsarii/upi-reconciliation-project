package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconFieldFormatDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldFormatMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconFieldFormatMasterService {

    private final ReconFieldFormatMasterRepository repository;

    public RestWithStatusList getAllFieldFormats() {
        try {
            List<Object> data = repository.findAll()
                    .stream()
                    .map(entity -> (Object) ReconFieldFormatDto.builder()
                            .reconFieldFormatId(entity.getReconFieldFormatId())
                            .reconFieldTypeId(entity.getReconFieldTypeId())
                            .reconFieldFormatDesc(entity.getReconFieldFormatDesc())
                            .reconInsertCode(entity.getReconInsertCode())
                            .reconInsertUser(entity.getReconInsertUser())
                            .reconInsertDate(entity.getReconInsertDate())
                            .reconLastUpdatedUser(entity.getReconLastUpdatedUser())
                            .reconLastUpdatedDate(entity.getReconLastUpdatedDate())
                            .build())
                    .collect(Collectors.toList());

            return RestWithStatusList.builder()
                    .status(CommonConstants.SUCCESS)
                    .statusMsg("Request executed successfully")
                    .data(data)
                    .build();

        } catch (Exception e) {
            return RestWithStatusList.builder()
                    .status(CommonConstants.FAILURE)
                    .statusMsg("Something went wrong: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
}