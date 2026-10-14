package com.jpb.reconciliation.reconciliation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UserPasswordChangeRequest;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.v2.DelegationService;
import com.jpb.reconciliation.reconciliation.service.v2.ReconUserService;

import io.swagger.v3.oas.annotations.Operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping(path = "/api/v1/user")
public class ReconUserController {

	Logger logger = LoggerFactory.getLogger(ReconUserController.class);

	@Autowired
	ReconUserService reconUserService;

	@Autowired
	ReconUserRepository reconUserRepository;

	@Autowired
	ReconBankMasterRepository reconBankMasterRepository;

	@Autowired
	DelegationService delegationService;

	@Autowired
	AuditReplacementRepository auditReplacementRepository;

	@PostMapping(value = "/create-user", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> createUser(@RequestBody ReconUserDto reconUserDto) {
		RestWithStatusList restWithStatusList = null;
//		if (reconUserDto.getRoleName().equalsIgnoreCase("MAKER")) {
			return reconUserService.createUser(reconUserDto);
//		} else {
//			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "You do not have the rights to create a user.",
//					null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//		}

	}

	@GetMapping(value = "/getuserby-userid/{userId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUser(@PathVariable Long userId) {
		return reconUserService.getUserByUserId(userId);
	}

	@GetMapping(value = "/getuser", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserDetails userDetails) {
		return reconUserService.getUserData(userDetails.getUsername());
	}

	@DeleteMapping(value = "/remove-user/{userId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<ResponseDto> removedUser(@PathVariable Long userId) {
		return reconUserService.removeUser(userId);
	}

	@PostMapping(value = "/change-password", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<ResponseDto> changePassword(@RequestBody UserPasswordChangeRequest changePasswordRequest) {
		logger.info("User Request for changing password" + changePasswordRequest);
		return reconUserService.changePassword(changePasswordRequest);
	}

	@PutMapping(path = "/update-user", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> updateUser(@RequestBody ReconUserDto userUpdateRequest) {
		return reconUserService.updateUser(userUpdateRequest);
	}

	@GetMapping(value = "/get-approved-users", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getApprovedUsers(@RequestParam String approvedYN) {
		return reconUserService.getApprovedUSers(approvedYN);
	}

	@PostMapping(value = "/approve-reject-user", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> approveOrRejectUser(@RequestBody ReconUserDto approveUserRequest,@AuthenticationPrincipal UserDetails userDetails) {
		return reconUserService.approveOrRejectUser(approveUserRequest, userDetails);
	}

	@GetMapping(value = "/getallusers", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllUserDetails() {
		return reconUserService.getAllUsers();
	}

	@Operation(summary = "Delegate a user's direct reports to an ancestor (the 'Confirm Delegation' flow) — "
			+ "transfers children immediately and schedules the delegator's own inactivation")
	@PostMapping(value = "/{id}/delegate", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> delegateUser(
			@PathVariable Long id,
			@RequestParam Long delegateeId,
			@RequestParam(required = false) String reason,
			@AuthenticationPrincipal UserDetails userDetails) {
		String triggeredBy = userDetails != null ? userDetails.getUsername() : "UNKNOWN";
		return delegationService.delegateNow(id, delegateeId, reason, triggeredBy);
	}

	@Operation(summary = "Ancestors of a user (their reporting chain, nearest first) — populates the "
			+ "'Select ancestor to delegate to' list on the Delegation tab. Was previously missing "
			+ "entirely: the frontend called this exact path, got a 404, caught it, and silently showed "
			+ "'No eligible ancestors found' for every single delegation attempt in the app.")
	@GetMapping(value = "/{id}/ancestors", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUserAncestors(@PathVariable Long id) {
		Optional<ReconUser> startOpt = reconUserRepository.findById(id);
		if (!startOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User not found", Collections.emptyList()));
		}
		List<Map<String, Object>> result = new ArrayList<>();
		Set<Long> seen = new java.util.HashSet<>();
		seen.add(id);
		Long parentId = startOpt.get().getParentUserId();
		// Bounded walk up parentUserId — guards against a cyclic chain in the data ever hanging
		// the request (this codebase's test data has had exactly that kind of corruption before).
		int hops = 0;
		while (parentId != null && hops++ < 25) {
			if (!seen.add(parentId)) break;
			Optional<ReconUser> ancOpt = reconUserRepository.findById(parentId);
			if (!ancOpt.isPresent()) break;
			ReconUser anc = ancOpt.get();
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", anc.getUserId());
			row.put("fullName", anc.getFullName());
			row.put("username", anc.getUsername());
			row.put("designation", anc.getUserType());
			result.add(row);
			parentId = anc.getParentUserId();
		}
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Ancestors fetched", result));
	}

	@GetMapping(value = "/get-by-bank/{bankCode}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUsersByBankCode(@PathVariable String bankCode) {
		Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findByBankCode(bankCode);
		if (!bankOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "No data", Collections.emptyList()));
		}
		Long mainBankId = bankOpt.get().getBankId();
		List<Map<String, Object>> result = new ArrayList<>();

		// Users directly under the bank itself (not under any branch)
		List<ReconUser> directUsers = reconUserRepository.findByBankId(mainBankId);
		for (ReconUser u : directUsers) {
			if (!"BANK_USER".equals(u.getUserType())) continue;
			result.add(buildUserRow(u, bankCode));
		}

		// Users under each of this bank's branches
		List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(mainBankId);
		for (ReconBankMaster branch : branches) {
			String bCode = branch.getBankCode();
			List<ReconUser> users = reconUserRepository.findByBankId(branch.getBankId());
			for (ReconUser u : users) {
				if (!"BRANCH_USER".equals(u.getUserType())) continue;
				result.add(buildUserRow(u, bCode));
			}
		}
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched", result));
	}

	@GetMapping(value = "/get-by-branch/{branchCode}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUsersByBranchCode(@PathVariable String branchCode) {
		Optional<ReconBankMaster> branchOpt = reconBankMasterRepository.findByBankCode(branchCode);
		if (!branchOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "No data", Collections.emptyList()));
		}
		Long branchBankId = branchOpt.get().getBankId();
		List<ReconUser> users = reconUserRepository.findByBankId(branchBankId);
		List<Map<String, Object>> result = new ArrayList<>();
		for (ReconUser u : users) {
			if (!"BRANCH_USER".equals(u.getUserType())) continue;
			result.add(buildUserRow(u, branchCode));
		}
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Branch users fetched", result));
	}

	private Map<String, Object> buildUserRow(ReconUser u, String branchCode) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", u.getUserId());
		row.put("fullName", u.getFullName());
		row.put("username", u.getUsername());
		row.put("email", u.getEmail());
		row.put("mobileNumber", u.getMobileNumber());
		row.put("role", u.getUserType());
		row.put("status", u.getStatus());
		row.put("branchCode", branchCode);
		row.put("blockReason", u.getBlockReason());
		row.put("blockScheduledAt", u.getBlockScheduledAt());
		row.put("inactivateScheduledAt", u.getInactivateScheduledAt());
		row.put("reactivateScheduledAt", u.getReactivateScheduledAt());
		row.put("preBlockStatus", u.getPreBlockStatus());
		putReplacementInfo(row, u.getUserId());
		row.put("parentId", u.getParentUserId());
		return row;
	}

	// Mirrors ReconBankMasterController.getAllBankAdmins()'s replacement enrichment, for a
	// regular-user row instead of a bank-admin row. This used to hardcode replacementStatus/
	// replacedByUsername/replacementAdminRow to null/false regardless of real AuditReplacement
	// state, so a replaced-or-being-replaced regular user's "(Replaced)" badge never appeared
	// anywhere under My Organization > Hierarchy / User Status.
	private void putReplacementInfo(Map<String, Object> row, Long userId) {
		List<AuditReplacement> asReplacementOf = auditReplacementRepository
				.findByReplacementUserIdAndStatusIn(userId, Arrays.asList(CommonConstants.ACTIVE, "FINALIZED"));
		if (!asReplacementOf.isEmpty()) {
			AuditReplacement asRep = asReplacementOf.get(0);
			row.put("replacementAdminRow", true);
			row.put("replacementStatus", "FINALIZED".equals(asRep.getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
			row.put("replacedByUsername", null);
			return;
		}
		row.put("replacementAdminRow", false);
		List<AuditReplacement> reps = auditReplacementRepository
				.findByOriginalUserIdAndStatusIn(userId, Arrays.asList(CommonConstants.ACTIVE, "FINALIZED"));
		// A FINALIZED replacement only still applies while the original remains BLOCKED (the
		// state finalize left them in). If an Admin has since manually reactivated this original
		// back to a live status, the FINALIZED record is historical — showing "Perm. Replaced" on
		// a normal, currently-active user is stale/wrong.
		boolean stillSuperseded = !reps.isEmpty() && (CommonConstants.ACTIVE.equals(reps.get(0).getStatus())
				|| reconUserRepository.findById(userId).map(u -> "BLOCKED".equals(u.getStatus())).orElse(false));
		if (stillSuperseded) {
			AuditReplacement rep = reps.get(0);
			ReconUser repUser = reconUserRepository.findById(rep.getReplacementUserId()).orElse(null);
			row.put("replacementStatus", "FINALIZED".equals(rep.getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
			row.put("replacedByUsername", repUser != null ? repUser.getUsername() : null);
		} else {
			row.put("replacementStatus", null);
			row.put("replacedByUsername", null);
		}
	}

	/*
	 * ── Caller-scoped endpoints (My Organization for a plain USER) ──────────────────
	 *
	 * A Bank/Branch Admin may grant Overview / Hierarchy / User Status to a child user via
	 * Privileges. Those pages must then show ONLY that user's own team — never bank or branch
	 * admin data. Hiding it in the UI is not enough: a child user could call the bank-wide
	 * endpoints directly. So the scope is derived from the JWT here, exactly like the old
	 * backend did (AddUserServiceImpl.getUsersByCreator / getUserHierarchy, which resolved the
	 * caller and keyed on their own id). No path/query parameter can widen it.
	 */

	@Operation(summary = "Users directly under the authenticated caller (their own team)")
	@GetMapping(value = "/my-team", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getMyTeam(@AuthenticationPrincipal UserDetails userDetails) {
		Optional<ReconUser> meOpt = resolveCaller(userDetails);
		if (!meOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Caller not found", null));
		}
		ReconUser me = meOpt.get();
		String code = resolveEntityCode(me.getBankId());
		List<Map<String, Object>> result = new ArrayList<>();
		for (ReconUser child : reconUserRepository.findByParentUserId(me.getUserId())) {
			result.add(buildUserRow(child, code));
		}
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Team fetched", result));
	}

	@Operation(summary = "Reporting-hierarchy subtree rooted at the authenticated caller")
	@GetMapping(value = "/hierarchy", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getMyHierarchy(@AuthenticationPrincipal UserDetails userDetails) {
		Optional<ReconUser> meOpt = resolveCaller(userDetails);
		if (!meOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Caller not found", null));
		}
		ReconUser me = meOpt.get();
		String code = resolveEntityCode(me.getBankId());

		// Nest by parentUserId across the caller's own bank, then keep only the subtree hanging
		// off the caller. Building the map once avoids a query per level.
		Map<Long, List<ReconUser>> byParent = new java.util.HashMap<>();
		for (ReconUser u : reconUserRepository.findByBankId(me.getBankId())) {
			if (u.getParentUserId() == null) continue;
			byParent.computeIfAbsent(u.getParentUserId(), k -> new ArrayList<>()).add(u);
		}

		List<Map<String, Object>> tree = new ArrayList<>();
		for (ReconUser child : byParent.getOrDefault(me.getUserId(), Collections.emptyList())) {
			tree.add(buildUserNode(child, byParent, code));
		}
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User hierarchy fetched successfully", tree));
	}

	private Optional<ReconUser> resolveCaller(UserDetails userDetails) {
		if (userDetails == null) return Optional.empty();
		return reconUserRepository.findByUsername(userDetails.getUsername());
	}

	// A branch is its own RECON_BANK_MASTER row, so one lookup covers bank and branch users.
	private String resolveEntityCode(Long bankId) {
		if (bankId == null) return null;
		return reconBankMasterRepository.findById(bankId).map(ReconBankMaster::getBankCode).orElse(null);
	}

	@Operation(summary = "Get reporting-hierarchy tree of users under a branch (nested by parentUserId)")
	@GetMapping(value = "/hierarchy/branch/{branchCode}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUserHierarchyByBranch(@PathVariable String branchCode) {
		Optional<ReconBankMaster> branchOpt = reconBankMasterRepository.findByBankCode(branchCode);
		if (!branchOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "No data", Collections.emptyList()));
		}
		List<Map<String, Object>> tree = buildUserHierarchyTree(branchOpt.get().getBankId(), branchCode);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Branch hierarchy fetched", tree));
	}

	@Operation(summary = "Get reporting-hierarchy tree of users directly under a bank (not under any branch)")
	@GetMapping(value = "/hierarchy/bank/{bankCode}/direct", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUserHierarchyByBankDirect(@PathVariable String bankCode) {
		Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findByBankCode(bankCode);
		if (!bankOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "No data", Collections.emptyList()));
		}
		List<Map<String, Object>> tree = buildUserHierarchyTree(bankOpt.get().getBankId(), bankCode);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank direct hierarchy fetched", tree));
	}

	// Users of the given bankId, nested by parentUserId. Roots = users whose
	// parent isn't also a user of this same bankId (e.g. admin created by a KalAdmin/BankAdmin
	// who sits outside this institution's own user scope).
	private List<Map<String, Object>> buildUserHierarchyTree(Long bankId, String codeForRow) {
		List<ReconUser> allUsers = new ArrayList<>();
		for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
			// Only regular (non-admin) users belong in the hierarchy tree.
			if ("BANK_USER".equals(u.getUserType()) || "BRANCH_USER".equals(u.getUserType())) {
				allUsers.add(u);
			}
		}
		java.util.Set<Long> idsInScope = new java.util.HashSet<>();
		for (ReconUser u : allUsers) idsInScope.add(u.getUserId());

		Map<Long, List<ReconUser>> byParent = new java.util.HashMap<>();
		List<ReconUser> roots = new ArrayList<>();
		for (ReconUser u : allUsers) {
			Long parentId = u.getParentUserId();
			if (parentId == null || !idsInScope.contains(parentId)) {
				roots.add(u);
			} else {
				byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(u);
			}
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (ReconUser root : roots) {
			result.add(buildUserNode(root, byParent, codeForRow));
		}
		return result;
	}

	private Map<String, Object> buildUserNode(ReconUser u, Map<Long, List<ReconUser>> byParent, String codeForRow) {
		Map<String, Object> node = buildUserRow(u, codeForRow);
		List<ReconUser> children = byParent.getOrDefault(u.getUserId(), Collections.emptyList());
		List<Map<String, Object>> childNodes = new ArrayList<>();
		for (ReconUser c : children) {
			childNodes.add(buildUserNode(c, byParent, codeForRow));
		}
		node.put("children", childNodes);
		return node;
	}

}
