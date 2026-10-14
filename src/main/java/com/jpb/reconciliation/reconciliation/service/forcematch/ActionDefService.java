package com.jpb.reconciliation.reconciliation.service.forcematch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.forcematch.ActionDef;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.repository.forcematch.ActionDefRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionDefService {

	private final ActionDefRepository actionRepo;

	// ─────────────────────────────────────────────────────────────────────────
	// GET ALL
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getAll() {
		log.debug("Fetching all action definitions");
		List<Object> data = actionRepo.findAllOrdered().stream().map(a -> (Object) a).collect(Collectors.toList());
		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg("Action definitions fetched successfully")
				.data(data).build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET BY ID
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getById(Long id) {
		log.debug("Fetching action def id={}", id);
		ActionDef a = actionRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ActionDef not found with id: " + id));
		return singleResult(a, "Action definition fetched successfully");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET BY TABLE NAME
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional(readOnly = true)
	public RestWithStatusList getByTable(String tableName) {
		List<Object> data = actionRepo.findByRmtActDataTbl(tableName).stream().map(a -> (Object) a)
				.collect(Collectors.toList());
		return RestWithStatusList.builder().status(CommonConstants.SUCCESS)
				.statusMsg("Action definitions fetched for table: " + tableName).data(data).build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// CREATE
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public RestWithStatusList create(ActionDef request) {
		if (request.getRmtDebitAcct() == null || request.getRmtDebitAcct().trim().isEmpty()) {
			return errorResult("rmtDebitAcct is required");
		}
		if (request.getRmtCreditAcct() == null || request.getRmtCreditAcct().trim().isEmpty()) {
			return errorResult("rmtCreditAcct is required");
		}
		log.info("Creating action def debit={}", request.getRmtDebitAcct());
		return singleResult(actionRepo.save(request), "Action definition created successfully");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// UPDATE
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public RestWithStatusList update(Long id, ActionDef request) {
		ActionDef existing = actionRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ActionDef not found with id: " + id));

		request.setRmtActionId(id);
		request.setRmtInsDate(existing.getRmtInsDate());
		request.setRmtInsUser(existing.getRmtInsUser());

		log.info("Updating action def id={}", id);
		return singleResult(actionRepo.save(request), "Action definition updated successfully");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// DELETE
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public RestWithStatusList delete(Long id) {
		if (!actionRepo.existsById(id)) {
			throw new ResourceNotFoundException("ActionDef not found with id: " + id);
		}
		log.info("Deleting action def id={}", id);
		actionRepo.deleteById(id);
		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg("Action definition deleted successfully")
				.data(new ArrayList<>()).build();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// HELPERS
	// ─────────────────────────────────────────────────────────────────────────
	private RestWithStatusList singleResult(Object data, String msg) {
		List<Object> list = new ArrayList<>();
		list.add(data);
		return RestWithStatusList.builder().status(CommonConstants.SUCCESS).statusMsg(msg).data(list).build();
	}

	private RestWithStatusList errorResult(String msg) {
		return RestWithStatusList.builder().status(CommonConstants.FAILURE).statusMsg(msg).data(new ArrayList<>()).build();
	}
}