package com.jpb.reconciliation.reconciliation.service.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconSourceSystemMast;
import com.jpb.reconciliation.reconciliation.repository.ReconSourceSystemMastRepository;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

@Service
@Transactional(readOnly = true)
public class ReconSourceSystemMastServiceImpl implements ReconSourceSystemMastService {

    Logger logger = LoggerFactory.getLogger(ReconSourceSystemMastServiceImpl.class);

    @Autowired
    private ReconSourceSystemMastRepository repository;

    // =========================================================================
    // GET ALL ACTIVE SOURCE SYSTEMS — recon_source_system_mast
    // =========================================================================

    @Override
    public ResponseEntity<RestWithStatusList> getActiveSources() {
        try {
            List<ReconSourceSystemMast> sources = repository.findByIsActive(CommonConstants.ACTIVE_Y);

            if (sources.isEmpty()) {
                return ResponseEntity.ok(
                        ResponseBuilder.failureList("No active source system records found"));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (ReconSourceSystemMast s : sources) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("sourceId",   s.getSourceId());
                row.put("sourceCode", s.getSourceCode());
                row.put("sourceName", s.getSourceName());
                row.put("description", s.getDescription());
                row.put("isActive",   s.getIsActive());
                rows.add(row);
            }

            return ResponseEntity.ok(
                    ResponseBuilder.okList("Active source systems fetched successfully", rows));

        } catch (Exception e) {
            logger.error("Error fetching active source systems: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.errorList("Error retrieving active source systems: " + e.getMessage()));
        }
    }

}