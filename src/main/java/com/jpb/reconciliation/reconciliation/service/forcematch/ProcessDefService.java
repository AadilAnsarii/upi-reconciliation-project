package com.jpb.reconciliation.reconciliation.service.forcematch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.forcematch.ActionDef;
import com.jpb.reconciliation.reconciliation.entity.forcematch.ProcessDef;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.repository.forcematch.ActionDefRepository;
import com.jpb.reconciliation.reconciliation.repository.forcematch.ProcessDefRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDefService {

	private final ProcessDefRepository processRepo;
	private final ActionDefRepository actionRepo;

	// ─── Category label map ───────────────────────────────────────────────────
	private static final Map<Long, String> CATG_LABELS;
	static {
		CATG_LABELS = new java.util.HashMap<>();
		CATG_LABELS.put(1L, "Plain Knock Off");
		CATG_LABELS.put(2L, "Keep Unreconciled");
		CATG_LABELS.put(3L, "Adjustment Entry");
		CATG_LABELS.put(4L, "Late Presentment");
		CATG_LABELS.put(5L, "LPR Reversal");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET ALL
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getAll() {
		log.debug("Fetching all process configs");
		List<ProcessDef> list = processRepo.findAll();
		Map<Long, ActionDef> actionMap = buildActionMap();

		List<Object> data = list.stream().map(p -> (Object) enriched(p, actionMap.get(p.getRmpActionId())))
				.collect(Collectors.toList());

		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg("Process configurations fetched successfully")
				.data(data).build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET BY ID
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getById(Long id) {
		log.debug("Fetching process config id={}", id);
		ProcessDef p = processRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ProcessDef not found with id: " + id));

		ActionDef a = p.getRmpActionId() != null ? actionRepo.findById(p.getRmpActionId()).orElse(null) : null;

		return singleResult(enriched(p, a), "Process configuration fetched successfully");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET BY PROCESS ID
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getByProcessId(Long processId) {
		log.debug("Fetching configs for processId={}", processId);
		Map<Long, ActionDef> actionMap = buildActionMap();

		List<Object> data = processRepo.findByProcessIdOrdered(processId).stream()
				.map(p -> (Object) enriched(p, actionMap.get(p.getRmpActionId()))).collect(Collectors.toList());

		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg("Process configurations fetched successfully")
				.data(data).build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET DISTINCT PROCESS IDs
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getDistinctProcessIds() {
		List<Object> ids = new ArrayList<>(processRepo.findDistinctProcessIds());
		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg("Process IDs fetched successfully").data(ids)
				.build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET BY STATUS
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getByStatus(String status) {
		Map<Long, ActionDef> actionMap = buildActionMap();
		List<Object> data = processRepo.findByRmpActionConfigStatus(status).stream()
				.map(p -> (Object) enriched(p, actionMap.get(p.getRmpActionId()))).collect(Collectors.toList());

		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg("Filtered by status: " + status).data(data)
				.build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// CREATE
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public RestWithStatusList create(ProcessDef request) {
		if (request.getRmpProcessId() == null) {
			return errorResult("rmpProcessId is required");
		}
		if (request.getRmpActionCatgId() == null) {
			return errorResult("rmpActionCatgId is required");
		}
		log.info("Creating process config processId={}", request.getRmpProcessId());
		ProcessDef saved = processRepo.save(request);
		return singleResult(saved, "Process configuration created successfully");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// UPDATE
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public RestWithStatusList update(Long id, ProcessDef request) {
		ProcessDef existing = processRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ProcessDef not found with id: " + id));

		request.setRmpActionId(id);
		request.setRmpInsDate(existing.getRmpInsDate());
		request.setRmpInsUser(existing.getRmpInsUser());

		log.info("Updating process config id={}", id);
		ProcessDef saved = processRepo.save(request);
		return singleResult(saved, "Process configuration updated successfully");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// TOGGLE STATUS (Y ↔ N)
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public RestWithStatusList toggleStatus(Long id) {
		ProcessDef p = processRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ProcessDef not found with id: " + id));

		String newStatus = CommonConstants.ACTIVE_Y.equals(p.getRmpActionConfigStatus()) ? CommonConstants.INACTIVE_N : CommonConstants.ACTIVE_Y;
		p.setRmpActionConfigStatus(newStatus);
		log.info("Toggled status id={} → {}", id, newStatus);

		ProcessDef saved = processRepo.save(p);
		return singleResult(saved, "Status updated to " + newStatus);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// DELETE
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public RestWithStatusList delete(Long id) {
		if (!processRepo.existsById(id)) {
			throw new ResourceNotFoundException("ProcessDef not found with id: " + id);
		}
		log.info("Deleting process config id={}", id);
		processRepo.deleteById(id);

		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg("Process configuration deleted successfully")
				.data(new ArrayList<>()).build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// PRIVATE HELPERS
	// ─────────────────────────────────────────────────────────────────────────

	/** Wraps entity + joined fields into an enriched Map for the response */
	private Map<String, Object> enriched(ProcessDef p, ActionDef a) {
		Map<String, Object> map = new java.util.LinkedHashMap<>();
		map.put("rmpActionId", p.getRmpActionId());
		map.put("rmpProcessId", p.getRmpProcessId());
		map.put("rmpActionCatgId", p.getRmpActionCatgId());
		map.put("categoryDesc", CATG_LABELS.get(p.getRmpActionCatgId()));
		map.put("rmpManrecDescription", p.getRmpManrecDescription());
		map.put("rmpOrderOfExecution", p.getRmpOrderOfExecution());
		map.put("rmpMappingLevel", p.getRmpMappingLevel());
		map.put("rmpTransactionDay", p.getRmpTransactionDay());
		map.put("rmpTemp1", p.getRmpTemp1());
		map.put("rmpTemp2", p.getRmpTemp2());
		map.put("rmpTemp3", p.getRmpTemp3());
		map.put("rmpTemp4", p.getRmpTemp4());
		map.put("rmpTempId1", p.getRmpTempId1());
		map.put("rmpTempId2", p.getRmpTempId2());
		map.put("rmpTempId3", p.getRmpTempId3());
		map.put("rmpTempId4", p.getRmpTempId4());
		map.put("rpmField1", p.getRpmField1());
		map.put("rpmField2", p.getRpmField2());
		map.put("rpmField3", p.getRpmField3());
		map.put("rpmField4", p.getRpmField4());
		map.put("rpmEjErrchkFlg", p.getRpmEjErrchkFlg());
		map.put("rpmApprReqFlg", p.getRpmApprReqFlg());
		map.put("rmpDiffFlag", p.getRmpDiffFlag());
		map.put("rmpDiffAmtExpr", p.getRmpDiffAmtExpr());
		map.put("rmpActionConfigStatus", p.getRmpActionConfigStatus());
		map.put("rmpInstCode", p.getRmpInstCode());
		map.put("rmpInsUser", p.getRmpInsUser());
		map.put("rmpLupdUser", p.getRmpLupdUser());
		map.put("rmpInsDate", p.getRmpInsDate());
		map.put("rmpLupdDate", p.getRmpLupdDate());
		// Joined from ActionDef
		if (a != null) {
			map.put("rmtRemarks", a.getRmtRemarks());
			map.put("rmtActDataTbl", a.getRmtActDataTbl());
			map.put("rmtDebitAcct", a.getRmtDebitAcct());
			map.put("rmtCreditAcct", a.getRmtCreditAcct());
		}
		return map;
	}

	private Map<Long, ActionDef> buildActionMap() {
		return actionRepo.findAllOrdered().stream()
				.collect(Collectors.toMap(ActionDef::getRmtActionId, Function.identity()));
	}

	private RestWithStatusList singleResult(Object data, String msg) {
		List<Object> list = new ArrayList<>();
		list.add(data);
		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg(msg).data(list).build();
	}

	private RestWithStatusList errorResult(String msg) {
		return RestWithStatusList.builder().status(CommonConstants.FAILURE).statusMsg(msg).data(new ArrayList<>()).build();
	}
}