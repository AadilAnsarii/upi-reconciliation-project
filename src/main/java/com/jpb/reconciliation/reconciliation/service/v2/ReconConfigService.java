package com.jpb.reconciliation.reconciliation.service.v2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigRequest.FilterConditionRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigRequest.MatchingRuleRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigRequest.SourceFileRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigResponse;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigResponse.FilterConditionInfo;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigResponse.MatchingRuleInfo;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigResponse.SourceFileInfo;
import com.jpb.reconciliation.reconciliation.entity.RcnRuleMast;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.RcnRuleMastRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;

/**
 * Service that orchestrates the full 6-step recon configuration wizard.
 *
 * Data flow:
 *   Step 1  → RCN_PROCESS_DEF_MAST  (basic metadata)
 *   Step 2  → RCN_PROCESS_DEF_MAST  (file-type + table-name columns)
 *   Step 3  → RCN_RULE_MAST          (matching rules, RRM_PROCESS_ID set)
 *   Step 4  → RCN_RULE_MAST          (filter/template rows, RRM_TMPLT_ID set)
 *   Step 5  → RCN_PROCESS_DEF_MAST  (schedule / retention columns)
 *   Step 6  → read-only review
 */
@Service
public class ReconConfigService {

    private static final Logger log = LoggerFactory.getLogger(ReconConfigService.class);

    @Autowired private ReconProcessDefMasterRepository processRepo;
    @Autowired private RcnRuleMastRepository           ruleRepo;
    @Autowired private ReconTemplateDetailsRepository  templateRepo;
    @Autowired private ReconFileDetailsMasterRepository fileRepo;
    @Autowired private ProcessMasterRepository          processMasterRepo;

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE  (POST /add)
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ReconConfigResponse createReconConfig(ReconConfigRequest req) {
        log.info("Creating recon config: {}", req.getReconName());

        // Guard: duplicate name check
        if (processRepo.findByReconProcessName(req.getReconName()).isPresent()) {
            throw new IllegalArgumentException("A recon process with name '" + req.getReconName() + "' already exists.");
        }

        // ── Step 1 + 2 + 5: persist process master ──────────────────────────
        ReconProcessDefMaster process = buildProcessEntity(req);
        process.setReconInsertDate(LocalDateTime.now());
        process.setReconInsertUser(req.getInsUser());
        ReconProcessDefMaster saved = processRepo.save(process);

        // ── Step 3: persist matching rules ────────────────────────────────────
        List<RcnRuleMast> savedRules = new ArrayList<>();
        if (req.getMatchingRules() != null) {
            savedRules = saveMatchingRules(req.getMatchingRules(), saved.getReconProcessId(), req);
        }

        // ── Step 4: persist filter conditions ────────────────────────────────
        List<RcnRuleMast> savedFilters = new ArrayList<>();
        if (req.getFilterConditions() != null && !req.getFilterConditions().isEmpty()) {
            savedFilters = saveFilterRules(req.getFilterConditions(), saved.getReconProcessId(), req);
        }

        log.info("Recon config created with processId={}", saved.getReconProcessId());
        return buildResponse(saved, savedRules, savedFilters, req);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // READ BY ID  (GET /view/{id})
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ReconConfigResponse getById(Long processId) {
        ReconProcessDefMaster process = processRepo.findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Recon process not found with id: " + processId));

        List<RcnRuleMast> matchingRules = ruleRepo
            .findActiveMatchingRulesByProcess(processId);

        List<RcnRuleMast> filterRules = ruleRepo
            .findByRrmProcessIdAndRrmTmpltIdIsNull(processId)
            .stream()
            .filter(r -> r.getRrmRuleType() == null)   // template/filter rows have no rule_type
            .collect(Collectors.toList());

        return buildResponse(process, matchingRules, filterRules, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // READ ALL  (GET /view/all)
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ReconConfigResponse> getAll() {
        return processRepo.findAll().stream()
            .map(p -> {
                List<RcnRuleMast> rules = ruleRepo.findActiveMatchingRulesByProcess(
                    p.getReconProcessId());
                return buildResponse(p, rules, new ArrayList<>(), null);
            })
            .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE  (PUT /update/{id})
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ReconConfigResponse updateReconConfig(Long processId, ReconConfigRequest req) {
        log.info("Updating recon config processId={}", processId);

        ReconProcessDefMaster existing = processRepo.findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Recon process not found with id: " + processId));

        // Re-apply all fields from request
        applyRequestToEntity(existing, req);
        existing.setReconLastUpdatedDate(LocalDateTime.now());
        existing.setReconLastUpdatedUser(req.getInsUser());
        ReconProcessDefMaster saved = processRepo.save(existing);

        // Delete old rules, re-save new ones (simplest safe strategy)
        ruleRepo.deleteByRrmProcessId(processId);

        List<RcnRuleMast> savedRules = new ArrayList<>();
        if (req.getMatchingRules() != null) {
            savedRules = saveMatchingRules(req.getMatchingRules(), processId, req);
        }

        List<RcnRuleMast> savedFilters = new ArrayList<>();
        if (req.getFilterConditions() != null && !req.getFilterConditions().isEmpty()) {
            savedFilters = saveFilterRules(req.getFilterConditions(), processId, req);
        }

        log.info("Recon config updated processId={}", processId);
        return buildResponse(saved, savedRules, savedFilters, req);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE  (DELETE /delete/{id})
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteReconConfig(Long processId) {
        if (!processRepo.existsById(processId)) {
            throw new IllegalArgumentException("Recon process not found with id: " + processId);
        }
        ruleRepo.deleteByRrmProcessId(processId);
        processRepo.deleteById(processId);
        log.info("Recon config deleted processId={}", processId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Build a fresh ReconProcessDefMaster from the request */
    private ReconProcessDefMaster buildProcessEntity(ReconConfigRequest req) {
        ReconProcessDefMaster e = new ReconProcessDefMaster();
        applyRequestToEntity(e, req);
        return e;
    }

    /** Apply all request fields to an existing-or-new entity */
    private void applyRequestToEntity(ReconProcessDefMaster e, ReconConfigRequest req) {

        // Step 1 fields
        e.setReconProcessName(req.getReconName());
        e.setReconMatchingType(req.getMatchingType());
        e.setReconInsertCode(req.getInstCode());
        e.setReconEmailSMSFlag(req.getEmailSmsFlag());
        e.setReconManRecFlag(req.getManrecFlag());
        e.setReconProcessJPBRPSL("JPB");          // system constant per existing data

        // Step 5 fields
        e.setReconRetentionPeriod(req.getRetentionPeriod());
        e.setReconRetentionVolume(req.getRetentionVolume());

        // ProcessMaster FK
        if (req.getProcessMastId() != null) {
            processMasterRepo.findById(req.getProcessMastId()).ifPresent(e::setProcessmaster);
        }

        // Step 2: map up to 4 source files into the numbered columns
        if (req.getSourceFiles() != null) {
            for (SourceFileRequest sf : req.getSourceFiles()) {
                ReconTemplateDetails tmpl = templateRepo.findById(sf.getTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + sf.getTemplateId()));

                String[] dynNames = generateDynamicNames(tmpl.getStageTabName(), req.getChannel());
                String dataTableName = dynNames[0];
                String recFlagName   = dynNames[1];

                switch (sf.getSourceNumber()) {
                    case 1:
                        e.setReconFileType1(sf.getFileTypeId());
                        e.setReconDataTableName1(dataTableName);
                        e.setReconFlagName1(recFlagName);
                        e.setReconTemp1(sf.getTemplateId());
                        break;
                    case 2:
                        e.setReconFileType2(sf.getFileTypeId());
                        e.setReconDataTableName2(dataTableName);
                        e.setReconFlagName2(recFlagName);
                        e.setReconTemp2(sf.getTemplateId());
                        break;
                    case 3:
                        e.setReconFileType3(sf.getFileTypeId());
                        e.setReconDataTableName3(dataTableName);
                        e.setReconFlagName3(recFlagName);
                        e.setReconTemp3(sf.getTemplateId());
                        break;
                    case 4:
                        e.setReconFileType4(sf.getFileTypeId());
                        e.setReconDataTableName4(dataTableName);
                        e.setReconFlagName4(recFlagName);
                        e.setReconTemp4(sf.getTemplateId());
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid source number: " + sf.getSourceNumber() + ". Must be 1-4.");
                }

                // Derive matching fields from the rule definitions (stored in RPM_MATCHING_FIELD*)
                // This is populated from Step 3 matching rules below
            }
        }
    }

    /** Step 3: create RCN_RULE_MAST rows for matching rules */
    private List<RcnRuleMast> saveMatchingRules(
            List<MatchingRuleRequest> rules, Long processId, ReconConfigRequest req) {

        // Resolve source table names for WHERE clause generation
        String tableA = resolveDataTableName(req, 1);
        String tableB = resolveDataTableName(req, 2);

        // Collect matching fields per source for RPM_MATCHING_FIELD* (comma-separated)
        List<String> fieldsA = new ArrayList<>();
        List<String> fieldsB = new ArrayList<>();

        List<RcnRuleMast> toSave = new ArrayList<>();
        for (MatchingRuleRequest rule : rules) {
            RcnRuleMast r = new RcnRuleMast();
            r.setRrmProcessId(processId);
            r.setRrmPriority(String.valueOf(rule.getPriority()));
            r.setRrmRuleType("2");       // "2" = standard matching rule per existing data
            r.setRrmRuleStat(1);         // 1 = active
            r.setRrmSecMatFlg(CommonConstants.INACTIVE_N);

            // Build SQL WHERE clause: "WHERE A.field=B.field [AND ...]"
            String whereClause = buildMatchingWhereClause(rule);
            r.setRrmQuery(whereClause);
            r.setRrmWhereClause(whereClause);

            // Rank info: "fieldA|fieldB"
            String rankInfo = rule.getSourceAField() + "|" + rule.getSourceBField();
            r.setRrmRankInfo1(rankInfo);
            r.setRrmRankInfo2(rankInfo);

            // Table names and flag columns
            if (tableA != null && tableB != null) {
                r.setRrmTableName(tableA + "," + tableB);
                r.setRrmDynFlag(
                    resolveFlagName(req, 1) + "," + resolveFlagName(req, 2));
                r.setRrmIdCols(
                    resolveIdCol(req, 1) + "," + resolveIdCol(req, 2));
            }

            fieldsA.add(rule.getSourceAField());
            fieldsB.add(rule.getSourceBField());
            toSave.add(r);
        }

        List<RcnRuleMast> saved = ruleRepo.saveAll(toSave);

        // Back-fill RPM_MATCHING_FIELD* on the process entity
        // (fetch and update; the caller already saved the process)
        processRepo.findById(processId).ifPresent(p -> {
            p.setReconMatchingField1(String.join(",", fieldsA));
            p.setReconMatchingField2(String.join(",", fieldsB));
            processRepo.save(p);
        });

        return saved;
    }

    /** Step 4: create RCN_RULE_MAST template/filter rows */
    private List<RcnRuleMast> saveFilterRules(
            List<FilterConditionRequest> conditions, Long processId, ReconConfigRequest req) {

        List<RcnRuleMast> toSave = new ArrayList<>();

        // Group by applyTo so we generate one filter row per source
        Map<String, List<FilterConditionRequest>> grouped = conditions.stream()
            .collect(Collectors.groupingBy(FilterConditionRequest::getApplyTo));

        for (Map.Entry<String, List<FilterConditionRequest>> entry : grouped.entrySet()) {
            String applyTo = entry.getKey();
            List<FilterConditionRequest> group = entry.getValue();

            String whereClause = buildFilterWhereClause(group, req.getFilterLogic());

            // Determine which template IDs to use based on applyTo
            List<Long> templateIds = resolveTemplateIds(req, applyTo);
            for (Long tmpltId : templateIds) {
                RcnRuleMast r = new RcnRuleMast();
                r.setRrmTmpltId(tmpltId);
                r.setRrmProcessId(processId);
                r.setRrmSecMatFlg(CommonConstants.INACTIVE_N);
                r.setRrmWhereClause(whereClause);
                // RRM_RULE_TYPE and RRM_PRIORITY are null for template/filter rows
                toSave.add(r);
            }
        }

        return ruleRepo.saveAll(toSave);
    }

    // ── SQL generation helpers ─────────────────────────────────────────────────

    private String buildMatchingWhereClause(MatchingRuleRequest rule) {
        StringBuilder sb = new StringBuilder("WHERE A.")
            .append(rule.getSourceAField())
            .append("=B.")
            .append(rule.getSourceBField());

        if (rule.getTolerance() != null && rule.getTolerance() > 0) {
            sb.append(" AND ABS(A.")
              .append(rule.getSourceAField())
              .append(" - B.")
              .append(rule.getSourceBField())
              .append(") <= ")
              .append(rule.getTolerance());
        }
        return sb.toString();
    }

    private String buildFilterWhereClause(
            List<FilterConditionRequest> conditions, String logic) {

        if (conditions == null || conditions.isEmpty()) return "WHERE 1=1";

        String joiner = " " + (logic != null ? logic : "AND") + " ";
        String body = conditions.stream()
            .map(this::buildSingleCondition)
            .collect(Collectors.joining(joiner));

        return "WHERE " + body;
    }

    private String buildSingleCondition(FilterConditionRequest c) {
        String col = c.getColumn();
        String op  = c.getOperator();
        String val = c.getValue();

        String opUpper = op.toUpperCase();
        if ("IS NULL".equals(opUpper)) {
            return col + " IS NULL";
        } else if ("IS NOT NULL".equals(opUpper)) {
            return col + " IS NOT NULL";
        } else if ("IN".equals(opUpper)) {
            StringBuilder inClause = new StringBuilder(col + " IN (");
            String[] parts = val.split(",");
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) inClause.append(",");
                inClause.append("'").append(parts[i].trim()).append("'");
            }
            inClause.append(")");
            return inClause.toString();
        } else if ("LIKE".equals(opUpper)) {
            return col + " LIKE '" + val + "'";
        } else {
            return col + " " + op + " '" + val + "'";
        }
    }

    // ── Dynamic name generation (mirrors existing service logic) ──────────────

    private String[] generateDynamicNames(String stageTableName, String tranChannel) {
        if (stageTableName == null || stageTableName.trim().isEmpty()) {
            return new String[]{ null, null };
        }
        String withoutRec  = stageTableName.replaceFirst("^REC_", "");
        String baseName    = withoutRec.replace("_STAGE_T", "").replace("_STAGE_ALL", "");
        String channel     = (tranChannel != null) ? tranChannel : "";
        String dataTable   = "REC_" + baseName + channel + "_DATA";
        String recFlag     = "DYN_" + baseName + "_REC_FLAG";
        return new String[]{ dataTable, recFlag };
    }

    // ── Resolver helpers ──────────────────────────────────────────────────────

    private String resolveDataTableName(ReconConfigRequest req, int sourceNum) {
        if (req.getSourceFiles() == null) return null;
        return req.getSourceFiles().stream()
            .filter(s -> s.getSourceNumber() == sourceNum)
            .findFirst()
            .map(s -> {
                ReconTemplateDetails t = templateRepo.findById(s.getTemplateId()).orElse(null);
                return t == null ? null :
                    generateDynamicNames(t.getStageTabName(), req.getChannel())[0];
            }).orElse(null);
    }

    private String resolveFlagName(ReconConfigRequest req, int sourceNum) {
        if (req.getSourceFiles() == null) return null;
        return req.getSourceFiles().stream()
            .filter(s -> s.getSourceNumber() == sourceNum)
            .findFirst()
            .map(s -> {
                ReconTemplateDetails t = templateRepo.findById(s.getTemplateId()).orElse(null);
                return t == null ? null :
                    generateDynamicNames(t.getStageTabName(), req.getChannel())[1];
            }).orElse(null);
    }

    private String resolveIdCol(ReconConfigRequest req, int sourceNum) {
        if (req.getSourceFiles() == null) return null;
        return req.getSourceFiles().stream()
            .filter(s -> s.getSourceNumber() == sourceNum)
            .findFirst()
            .map(s -> {
                ReconTemplateDetails t = templateRepo.findById(s.getTemplateId()).orElse(null);
                if (t == null) return null;
                String baseName = t.getStageTabName()
                    .replaceFirst("^REC_", "")
                    .replace("_STAGE_T", "")
                    .replace("_STAGE_ALL", "");
                return "DYN_" + baseName + "_ID_COL";
            }).orElse(null);
    }

    private List<Long> resolveTemplateIds(ReconConfigRequest req, String applyTo) {
        List<Long> ids = new ArrayList<>();
        if (req.getSourceFiles() == null) return ids;
        for (SourceFileRequest sf : req.getSourceFiles()) {
            boolean include = "BOTH".equalsIgnoreCase(applyTo)
                || (sf.getSourceNumber() == 1 && "A".equalsIgnoreCase(applyTo))
                || (sf.getSourceNumber() == 2 && "B".equalsIgnoreCase(applyTo));
            if (include) ids.add(sf.getTemplateId());
        }
        return ids;
    }

    // ── Response builder ──────────────────────────────────────────────────────

    private ReconConfigResponse buildResponse(
            ReconProcessDefMaster p,
            List<RcnRuleMast> matchingRules,
            List<RcnRuleMast> filterRules,
            ReconConfigRequest req) {

        // ── Source files ──
        List<SourceFileInfo> sourceFiles = buildSourceFileInfoList(p);

        // ── Matching rules ──
        List<MatchingRuleInfo> ruleInfos = matchingRules.stream()
            .map(r -> MatchingRuleInfo.builder()
                .ruleId(r.getRrmRuleId())
                .priority(r.getRrmPriority() != null ? Integer.parseInt(r.getRrmPriority()) : null)
                .generatedQuery(r.getRrmQuery())
                .build())
            .collect(Collectors.toList());

        // ── Filter conditions ──
        List<FilterConditionInfo> filterInfos = filterRules.stream()
            .map(r -> FilterConditionInfo.builder()
                .generatedWhereClause(r.getRrmWhereClause())
                .build())
            .collect(Collectors.toList());

        // ── Matching flow description (Step 6 UI) ──
        String matchingFlow = buildMatchingFlowDescription(p, matchingRules);

        // ── Config JSON summary (Step 6 CONFIG JSON block) ──
        Map<String, Object> configJson = buildConfigJson(p, ruleInfos, filterInfos, req);

        return ReconConfigResponse.builder()
            .processId(p.getReconProcessId())
            .reconName(p.getReconProcessName())
            .reconType(p.getReconIssAcqFlag())
            .matchingType(p.getReconMatchingType())
            .instCode(p.getReconInsertCode())
            .insUser(p.getReconInsertUser())
            .insDate(p.getReconInsertDate())
            .lupdUser(p.getReconLastUpdatedUser())
            .lupdDate(p.getReconLastUpdatedDate())
            .emailSmsFlag(p.getReconEmailSMSFlag())
            .manrecFlag(p.getReconManRecFlag())
            .retentionPeriod(p.getReconRetentionPeriod())
            .retentionVolume(p.getReconRetentionVolume())
            .processMastId(p.getProcessmaster() != null
                ? p.getProcessmaster().getProcessMastId() : null)
            .sourceFiles(sourceFiles)
            .matchingRulesCount(ruleInfos.size())
            .matchingRules(ruleInfos)
            .filtersCount(filterInfos.size())
            .filterConditions(filterInfos)
            .matchingFlow(matchingFlow)
            .configJson(configJson)
            .build();
    }

    private List<SourceFileInfo> buildSourceFileInfoList(ReconProcessDefMaster p) {
        List<SourceFileInfo> list = new ArrayList<>();
        addSourceFileInfo(list, 1, p.getReconFileType1(),
            p.getReconDataTableName1(), p.getReconFlagName1(), p.getReconTemp1());
        addSourceFileInfo(list, 2, p.getReconFileType2(),
            p.getReconDataTableName2(), p.getReconFlagName2(), p.getReconTemp2());
        addSourceFileInfo(list, 3, p.getReconFileType3(),
            p.getReconDataTableName3(), p.getReconFlagName3(), p.getReconTemp3());
        addSourceFileInfo(list, 4, p.getReconFileType4(),
            p.getReconDataTableName4(), p.getReconFlagName4(), p.getReconTemp4());
        return list;
    }

    private void addSourceFileInfo(List<SourceFileInfo> list, int num,
            Long fileTypeId, String dataTable, String flagName, Long templateId) {
        if (fileTypeId == null) return;

        String fileTypeName  = null;
        String templateName  = null;
        String stageTabName  = null;

        if (fileTypeId != null) {
            Optional<ReconFileDetailsMaster> fm = fileRepo.findById(fileTypeId);
            if (fm.isPresent()) fileTypeName = fm.get().getReconShortName();
        }
        if (templateId != null) {
            Optional<ReconTemplateDetails> tm = templateRepo.findById(templateId);
            if (tm.isPresent()) {
                templateName = tm.get().getTemplateName();
                stageTabName = tm.get().getStageTabName();
            }
        }

        list.add(SourceFileInfo.builder()
            .sourceNumber(num)
            .fileTypeId(fileTypeId)
            .fileTypeName(fileTypeName)
            .templateId(templateId)
            .templateName(templateName)
            .stagingTableName(stageTabName)
            .dataTableName(dataTable)
            .recFlagName(flagName)
            .build());
    }

    private String buildMatchingFlowDescription(
            ReconProcessDefMaster p, List<RcnRuleMast> rules) {

        String tableA = p.getReconDataTableName1() != null ? p.getReconDataTableName1() : "Source A";
        String tableB = p.getReconDataTableName2() != null ? p.getReconDataTableName2() : "Source B";

        if (rules.isEmpty()) return tableA + " ↔ " + tableB + " (no rules defined)";

        String fields = rules.stream()
            .map(r -> r.getRrmRankInfo1() != null
                ? r.getRrmRankInfo1().replace("|", " = ") : "")
            .collect(Collectors.joining(", "));

        return tableA + " ↔ " + tableB + " via [" + fields + "]";
    }

    private Map<String, Object> buildConfigJson(
            ReconProcessDefMaster p,
            List<MatchingRuleInfo> rules,
            List<FilterConditionInfo> filters,
            ReconConfigRequest req) {

        Map<String, Object> json = new HashMap<>();
        json.put("reconName",     p.getReconProcessName());
        json.put("reconType",     p.getReconIssAcqFlag());
        json.put("channel",       req != null ? req.getChannel() : null);
        json.put("frequency",     req != null ? req.getFrequency() : null);
        json.put("dateLogic",     req != null ? req.getDateLogic() : null);
        json.put("matchingType",  p.getReconMatchingType());
        json.put("matchingRules", rules);
        json.put("filters",       filters);
        json.put("retentionDays", p.getReconRetentionPeriod());
        return json;
    }
}