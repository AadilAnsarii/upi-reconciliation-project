package com.jpb.reconciliation.reconciliation.service.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldFormatMast;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconFieldTypeMast;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFieldFormatMastRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconFieldTypeMastRepository;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

@Service
@Transactional(readOnly = true)
public class ReconFieldMastServiceImpl implements ReconFieldMastService {

    Logger logger = LoggerFactory.getLogger(ReconFieldMastServiceImpl.class);

    @Autowired ReconFieldTypeMastRepository   fieldTypeRepository;
    @Autowired ReconFieldFormatMastRepository fieldFormatRepository;
    @Autowired ObjectMapper                   objectMapper;

    // =========================================================================
    // GET ALL FIELD TYPES — recon_field_type_mast
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> getAllFieldTypes() {
        try {
            List<ReconFieldTypeMast> types = fieldTypeRepository.findAll();

            if (types.isEmpty()) {
                return ResponseEntity.ok(
                        ResponseBuilder.ok("No field types found.", "fieldTypes",
                                Collections.emptyList()));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (ReconFieldTypeMast t : types) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("fieldTypeId",   t.getFieldTypeId());
                row.put("fieldTypeCode", t.getFieldTypeCode());
                row.put("fieldTypeDesc", t.getFieldTypeDesc());
                rows.add(row);
            }

            return ResponseEntity.ok(
                    ResponseBuilder.ok("Field types retrieved successfully.", "fieldTypes", rows));

        } catch (Exception e) {
            logger.error("Error fetching field types: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.error("Error retrieving field types: " + e.getMessage()));
        }
    }

    // =========================================================================
    // GET ALL FIELD FORMATS — recon_field_format_mast
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> getAllFieldFormats() {
        try {
            List<ReconFieldFormatMast> formats = fieldFormatRepository.findAll();

            if (formats.isEmpty()) {
                return ResponseEntity.ok(
                        ResponseBuilder.ok("No field formats found.", "fieldFormats",
                                Collections.emptyList()));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (ReconFieldFormatMast f : formats) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("fieldFormatId",   f.getFieldFormatId());
                row.put("fieldTypeId",     f.getFieldTypeId());
                row.put("fieldFormatCode", f.getFieldFormatCode());
                row.put("fieldFormatDesc", f.getFieldFormatDesc());
                rows.add(row);
            }

            return ResponseEntity.ok(
                    ResponseBuilder.ok("Field formats retrieved successfully.", "fieldFormats", rows));

        } catch (Exception e) {
            logger.error("Error fetching field formats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.error("Error retrieving field formats: " + e.getMessage()));
        }
    }
}
