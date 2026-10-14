package com.jpb.reconciliation.reconciliation.service.forcematch;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.controller.FileConfigController;
import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
import com.jpb.reconciliation.reconciliation.repository.ReportRepository;

@Service("forceMatchActionServiceImpl")
public class ForceMatchActionServiceImpl implements ForceMatchActionService {

    private final FileConfigController fileConfigController;

    Logger logger = LoggerFactory.getLogger(ForceMatchActionServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ReportRepository reportRepository;

    @Value("${app.reconReport}")
    private String reportBaseDir;

    public ForceMatchActionServiceImpl(JdbcTemplate jdbcTemplate, FileConfigController fileConfigController) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileConfigController = fileConfigController;
    }

    @Override
    public ResponseEntity<RestWithStatusList> generateForceMatchExceptionReport(ReportDto ntslReportRequest) {
        RestWithStatusList response = new RestWithStatusList();
        String processId = String.valueOf(ntslReportRequest.getProcessId());

        try {
            SimpleJdbcCall procedureCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_EXCEPTION_REPORT")
                    .declareParameters(new SqlParameter("prm_process_id", Types.VARCHAR),
                                     new SqlParameter("prm_report_type", Types.VARCHAR),
                                     new SqlOutParameter("prm_error", Types.VARCHAR));

            Map<String, Object> inParams = new HashMap<>();
            inParams.put("prm_process_id", processId);
            inParams.put("prm_report_type", "ALL");
            logger.info("EXCEPTION PEPORT PROCEDURE INPUT :::" + inParams);
            Map<String, Object> out = procedureCall.execute(inParams);
            String prmError = Objects.toString(out.get("PRM_ERROR"), Objects.toString(out.get("prm_error"), "ERROR"));
            logger.info("EXCEPTION PEPORT PROCEDURE OUTPUT :::::::" + out);
            if ("OK".equalsIgnoreCase(prmError)) {
                String sql = "SELECT * FROM RCN_TEMP_EXCEPTION_REPORT WHERE PROCESS_ID = ?";
                List<Map<String, Object>> allRows = jdbcTemplate.queryForList(sql, processId);

                if (!allRows.isEmpty()) {
                    List<Map<String, Object>> detailRows = allRows.stream()
                            .filter(r -> "DETAIL".equalsIgnoreCase(String.valueOf(r.get("REPORT_TYPE"))))
                            .collect(Collectors.toList());

                    List<Map<String, Object>> summaryRows = allRows.stream()
                            .filter(r -> "SUMMARY".equalsIgnoreCase(String.valueOf(r.get("REPORT_TYPE"))))
                            .collect(Collectors.toList());

                    String fileName = "Exception_Report_" + processId + "_" + System.currentTimeMillis() + ".xlsx";
                    String filePath = reportBaseDir + fileName;
                    createExcelWithSheets(detailRows, summaryRows, filePath);
                    
                    ReportEntity reportRecord = new ReportEntity();
                    reportRecord.setProcessId(ntslReportRequest.getProcessId());
                    reportRecord.setReportDate(LocalDate.now());
                    reportRecord.setReportFileName(ntslReportRequest.getReportFileName());
                    reportRecord.setReportLocation(filePath);
                    reportRecord.setReportName("FORCE_MATCH");
                    reportRecord.setFileName(fileName);
                    reportRepository.save(reportRecord);
                }

                response.setStatus(CommonConstants.SUCCESS);
                response.setStatusMsg("Excel generated with separate Details/Summary sheets.");
                response.setData(allRows.stream().map(m -> (Object) m).collect(Collectors.toList()));
                return ResponseEntity.ok(response);
                
            } else {
                response.setStatus(CommonConstants.FAILURE);
                response.setStatusMsg("Procedure Error: " + prmError);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Report Generation Failed: ", e);
            response.setStatus("ERROR");
            response.setStatusMsg("System Exception: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private void createExcelWithSheets(List<Map<String, Object>> details, List<Map<String, Object>> summary, String path) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(new File(path))) {
            
            fillSheet(workbook, "DETAILS", details);
            fillSheet(workbook, "SUMMARY", summary);
            
            workbook.write(out);
        }
    }

    private void fillSheet(Workbook workbook, String sheetName, List<Map<String, Object>> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        if (data.isEmpty()) {
            sheet.createRow(0).createCell(0).setCellValue("No data found for " + sheetName);
            return;
        }

        // Header Style
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        // Header Row
        List<String> columns = new ArrayList<>(data.get(0).keySet());
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i));
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowIdx = 1;
        for (Map<String, Object> record : data) {
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < columns.size(); i++) {
                Object value = record.get(columns.get(i));
                if (value instanceof Number) {
                    row.createCell(i).setCellValue(((Number) value).doubleValue());
                } else {
                    row.createCell(i).setCellValue(Objects.toString(value, ""));
                }
            }
        }
        
        // Auto-size columns for readability
        for (int i = 0; i < columns.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }
}