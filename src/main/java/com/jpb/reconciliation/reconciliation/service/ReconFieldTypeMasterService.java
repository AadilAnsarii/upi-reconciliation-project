package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconFieldTypeMasterDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldTypeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconFieldTypeMasterService {

    private final ReconFieldTypeMasterRepository repository;

    public RestWithStatusList getAllFieldTypes() {
        try {
            List<ReconFieldTypeMasterDTO> fieldTypes = repository.findAll()
                    .stream()
                    .map(entity -> ReconFieldTypeMasterDTO.builder()
                            .fieldTypeId(entity.getFieldTypeId())
                            .fieldTypeDes(entity.getFieldTypeDes())
                            .insertUser(entity.getInsertUser())
                            .insertDate(entity.getInsertDate())
                            .lastUpdatedUser(entity.getLastUpdatedUser())
                            .lastUpdatedDate(entity.getLastUpdatedDate())
                            .insertCode(entity.getInsertCode())
                            .build())
                    .collect(Collectors.toList());

            return RestWithStatusList.builder()
                    .status(CommonConstants.SUCCESS)
                    .statusMsg("Request executed successfully")
                    .data((List<Object>) (List<?>) fieldTypes)
                    .build();
        } catch (Exception e) {
            return RestWithStatusList.builder()
                    .status(CommonConstants.FAILURE)
                    .statusMsg("Something went wrong")
                    .data(null)
                    .build();
        }
    }
}