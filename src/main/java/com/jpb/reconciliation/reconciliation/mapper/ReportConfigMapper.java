package com.jpb.reconciliation.reconciliation.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.dto.ReportMastConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReportMastConfigRequest.WhereCondition;
import com.jpb.reconciliation.reconciliation.entity.ExceptionReconReportEntity;

@Component
public class ReportConfigMapper {

    public ReportMastConfigRequest toDTO(ExceptionReconReportEntity entity) {
        ReportMastConfigRequest dto = new ReportMastConfigRequest();

        dto.setReportId(entity.getReportId());
        dto.setFileName(entity.getFileName());
        dto.setProcessId(entity.getProcessId());
        dto.setReportDate(entity.getReportDate());
        dto.setReportHeader(entity.getReportHeader());
        dto.setReportKey(entity.getReportKey());
        dto.setReportQuery(entity.getReportQuery());
        dto.setProcessType(deriveProcessType(entity.getReportKey()));
        dto.setReportType("CSV");
        dto.setSelectedFile(extractTableName(entity.getReportQuery()));
        dto.setSelectedColumns(extractColumns(entity.getReportQuery()));
        dto.setWhereConditions(extractWhereConditions(entity.getReportQuery()));

        return dto;
    }

    // ── Derive process type from report key ───────────────────────────────────
    private String deriveProcessType(String reportKey) {
        if (reportKey == null)                return "UNKNOWN";
        if (reportKey.contains("RECON"))      return "RECONCILIATION";
        if (reportKey.contains("STL"))        return "SETTLEMENT";
        if (reportKey.contains("EXCEPTION"))  return "EXCEPTION";
        return "UNKNOWN";
    }

    // ── Parse FROM <table> ────────────────────────────────────────────────────
    private String extractTableName(String query) {
        if (query == null) return null;
        Pattern p = Pattern.compile("FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(query);
        return m.find() ? m.group(1) : null;
    }

    // ── Parse NVL(COLUMN, 0) occurrences ─────────────────────────────────────
    private List<String> extractColumns(String query) {
        if (query == null) return Collections.emptyList();
        List<String> cols = new ArrayList<>();
        Pattern p = Pattern.compile("NVL\\((\\w+),\\s*0\\)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(query);
        while (m.find()) {
            cols.add(m.group(1));
        }
        return cols;
    }

    // ── Parse WHERE conditions (supports AND / OR chaining) ───────────────────
    private List<WhereCondition> extractWhereConditions(String query) {
        if (query == null) return Collections.emptyList();

        List<WhereCondition> conditions = new ArrayList<>();

        int whereIdx = query.toUpperCase().indexOf("WHERE");
        if (whereIdx == -1) return conditions;

        String whereClause = query.substring(whereIdx + 5).trim();

        Pattern p = Pattern.compile(
            "(AND|OR)?\\s*(\\w+)\\s*(=|!=|<>|>=|<=|>|<|LIKE|IN)\\s*'?([^'\\s,)]+)'?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(whereClause);

        boolean first = true;
        while (m.find()) {
            WhereCondition wc = new WhereCondition();
            wc.setColumn(m.group(2));
            wc.setOperator(m.group(3).toUpperCase());
            wc.setValue(m.group(4));
            wc.setLogicalOp(first ? null : (m.group(1) != null ? m.group(1).toUpperCase() : "AND"));
            conditions.add(wc);
            first = false;
        }

        return conditions;
    }
}