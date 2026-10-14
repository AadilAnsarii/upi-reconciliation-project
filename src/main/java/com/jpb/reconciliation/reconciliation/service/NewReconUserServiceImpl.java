package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder;
import com.jpb.reconciliation.reconciliation.service.v2.ApprovalJson;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconPasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleProductMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NewReconUserServiceImpl implements NewReconUserService {

	private static final Logger logger = LoggerFactory.getLogger(NewReconUserServiceImpl.class);
	private static final SecureRandom RNG = new SecureRandom();

	@Autowired
	private ReconUserRepository reconUserRepository;

	@Autowired
	private ReconPasswordManagerRepository reconPasswordManagerRepository;

	@Autowired
	private com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder approvalAuditRecorder;

	@Autowired
	private com.jpb.reconciliation.reconciliation.service.v2.HierarchyScopeService hierarchyScopeService;

	@Autowired
	private com.jpb.reconciliation.reconciliation.service.v2.WorkflowNotifier workflowNotifier;

	@Autowired
	private ReconBankMasterRepository reconBankMasterRepository;

	@Autowired
	private com.jpb.reconciliation.reconciliation.service.v2.EmailRegistryService emailRegistry;

	@Autowired
	private ReconRoleMasterRepository reconRoleMasterRepository;

	@Autowired
	private ReconRoleProductMapRepository roleProductMapRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private com.jpb.reconciliation.reconciliation.repository.v2.AuditReplacementRepository auditReplacementRepository;

	@Autowired
	private EmailService emailService;

	@Value("${app.frontend.url:http://localhost:5173}")
	private String frontendUrl;

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> createUser(ReconUser user, String createdBy) {
		if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
			return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE, "Full name is required.", null));
		}
		if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
			return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE, "Username is required.", null));
		}
		if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
			return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE, "Email is required.", null));
		}
		if (user.getUserType() == null || user.getUserType().trim().isEmpty()) {
			return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE, "User type is required.", null));
		}
		if (reconUserRepository.existsByUsername(user.getUsername().trim().toLowerCase())) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Username already exists.", null));
		}
		String emailLc = user.getEmail().trim().toLowerCase();
		// A BLOCKED account is effectively deleted, so its email may be reused; every
		// other status
		// still blocks reuse. Centralised in EmailRegistryService so
		// bank/branch/user/kal-admin all
		// agree. When reuse IS allowed we release the blocked holder's email so it
		// stays unique.
		if (emailRegistry.isActivelyRegistered(emailLc)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Email already registered.", null));
		}
		// The External Supervisor is a person, not a login account — but it must not be
		// the user's
		// own address (a user cannot be their own supervisor).
		String supervisorLc = user.getExternalSupervisorEmail() != null
				? user.getExternalSupervisorEmail().trim().toLowerCase()
				: null;
		if (supervisorLc != null && !supervisorLc.isEmpty() && supervisorLc.equals(emailLc)) {
			return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
					"External supervisor email cannot be the same as the user's own email.", null));
		}
		emailRegistry.releaseBlockedHolders(emailLc);
		user.setUsername(user.getUsername().trim().toLowerCase());
		user.setEmail(emailLc);

		// Role dropdown in AddUser.jsx sends the role's NAME (e.g. "MAKER"), not its ID
		// —
		// "DEFAULT" means no specific role. Resolve to ROLE_ID here (was silently
		// dropped
		// before since ReconUser has no matching "role" field).
		if (user.getRoleId() == null && user.getRoleName() != null && !user.getRoleName().trim().isEmpty()
				&& !"DEFAULT".equalsIgnoreCase(user.getRoleName().trim())) {
			reconRoleMasterRepository.findByRoleName(user.getRoleName().trim())
					.ifPresent(r -> user.setRoleId(r.getRoleId()));
		}

		// Links this user to whoever created them, for hierarchy display (My
		// Organization /
		// OrgHierarchy) — mirrors ReconBankMasterServiceImpl.buildAdminUser's
		// parentUserId.
		Optional<ReconUser> actorOpt = reconUserRepository.findByUsername(createdBy);
		actorOpt.map(ReconUser::getUserId).ifPresent(user::setParentUserId);
		boolean actorIsAdmin = actorOpt.isPresent() && UserConstants.isAdminUserType(actorOpt.get().getUserType());

		String defaultPwd = null;
		if (user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty()) {
			user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash().trim()));
			user.setPasswordSet(1);
		} else {
			user.setPasswordSet(0);
		}

		if ("KAL_ADMIN".equals(user.getUserType())) {
			user.setApprovedYn(CommonConstants.ACTIVE_Y);
			user.setStatus(CommonConstants.ACTIVE);
			if (user.getRoleId() == null) {
				Optional<ReconRoleMaster> kalAdminRole = reconRoleMasterRepository.findByRoleCode("KAL_ADMIN");
				if (kalAdminRole.isPresent()) {
					user.setRoleId(kalAdminRole.get().getRoleId());
				}
			}
		} else if (actorIsAdmin) {
			// Admin creates a user directly — no maker-checker approval needed. Goes
			// straight
			// to REQUEST (awaiting the user's own email verification / password setup), and
			// gets emailed immediately.
			user.setApprovedYn(CommonConstants.ACTIVE_Y);
			user.setApprovedBy(createdBy);
			user.setStatus("REQUEST");
			if (user.getPasswordSet() == null || user.getPasswordSet() == 0) {
				// passwordSet stays 0 — it's a DEFAULT/temporary password, not one the user
				// chose. 1 would make verify-email misreport them as OLD_USER and skip the
				// mandatory verify-credentials -> set-password flow (same bug fixed here also
				// existed in approveUser below). Matches buildAdminUser's own convention.
				defaultPwd = generatePassword();
				user.setPasswordHash(passwordEncoder.encode(defaultPwd));
				user.setPasswordSet(0);
			}
		} else {
			// A Maker created this user — goes to the Checker queue first. Password/email
			// are deferred to approveUser(), once a Checker actually approves it.
			// PENDING_APPROVAL — not ACTIVE_PENDING, which is reserved exclusively for the
			// Inactive->reactivating scheduling flow
			// (UserStatusServiceImpl/StatusSchedulerService).
			user.setApprovedYn(CommonConstants.INACTIVE_N);
			if (user.getStatus() == null) {
				user.setStatus("PENDING_APPROVAL");
			}
		}
		user.setCreatedAt(LocalDateTime.now());
		user.setCreatedBy((createdBy != null && !createdBy.equals("UNKNOWN") && !createdBy.isEmpty()) ? createdBy
				: (user.getCreatedBy() != null ? user.getCreatedBy() : CommonConstants.SYSTEM));
		ReconUser saved = reconUserRepository.saveAndFlush(user);

		// Only a Maker-created user enters the Checker queue; an Admin-created one is
		// already
		// usable and never has a decision to record.
		if ("PENDING_APPROVAL".equals(saved.getStatus())) {
			approvalAuditRecorder.recordSubmission(
					com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ENTITY_USER,
					saved.getUserId(),
					com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ACTION_CREATE, createdBy);
			workflowNotifier.notifySubmission("User", saved.getFullName(), saved.getUsername(), createdBy,
					checker -> isVisibleToChecker(checker, saved.getCreatedBy(), saved.getRoleId()));
		}

		ReconPasswordManager pwd = new ReconPasswordManager();
		pwd.setReconUser(saved);
		pwd.setUserPassword(saved.getPasswordHash());
		pwd.setCreatedAt(LocalDateTime.now());
		pwd.setCreatedBy(createdBy);
		pwd.setExpirationDate(LocalDateTime.now().plusDays(90));
		reconPasswordManagerRepository.save(pwd);

		if (defaultPwd != null) {
			sendUserWelcomeEmail(saved, defaultPwd);
		}

		logger.info("ReconUser created: {} by {}", saved.getUsername(), createdBy);
		return ResponseEntity.status(HttpStatus.CREATED).body(
				new RestWithStatusList(CommonConstants.SUCCESS, "User created successfully.", Collections.singletonList(saved)));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getAllUsers() {
		List<ReconUser> users = reconUserRepository.findAll();
		users.forEach(this::enrichWithReplacement);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched.", users));
	}

	/**
	 * Surfaces replacement awareness on a regular user row — mirrors
	 * ReconBankMasterServiceImpl.enrichWithReplacement() (bank rows) and
	 * ReconUserController.putReplacementInfo() (Map-based /my-team, /hierarchy
	 * rows). This endpoint (v2GetAllUsers, used by OrgBankUserStatus.jsx for a
	 * Bank/Branch Admin) previously returned the raw entity with no replacement
	 * fields at all, so a replaced-or-being-replaced user's "(Replaced)" badge
	 * never appeared in User Status for an Admin viewer.
	 */
	private void enrichWithReplacement(ReconUser u) {
		List<com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement> asReplacementOf = auditReplacementRepository
				.findByReplacementUserIdAndStatusIn(u.getUserId(), Arrays.asList(CommonConstants.ACTIVE, "FINALIZED"));
		if (!asReplacementOf.isEmpty()) {
			u.setReplacementAdminRow(true);
			u.setReplacementStatus("FINALIZED".equals(asReplacementOf.get(0).getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
			u.setReplacedByUsername(null);
			return;
		}
		u.setReplacementAdminRow(false);
		List<com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement> reps = auditReplacementRepository
				.findByOriginalUserIdAndStatusIn(u.getUserId(), Arrays.asList(CommonConstants.ACTIVE, "FINALIZED"));
		// A FINALIZED replacement only still applies while the original remains BLOCKED (the
		// state finalize left them in). If an Admin has since manually reactivated this original
		// back to a live status, the FINALIZED record is historical — showing "Perm. Replaced" on
		// a normal, currently-active user is stale/wrong.
		if (!reps.isEmpty() && (CommonConstants.ACTIVE.equals(reps.get(0).getStatus()) || "BLOCKED".equals(u.getStatus()))) {
			com.jpb.reconciliation.reconciliation.entity.v2.AuditReplacement rep = reps.get(0);
			ReconUser repUser = reconUserRepository.findById(rep.getReplacementUserId()).orElse(null);
			u.setReplacementStatus("FINALIZED".equals(rep.getStatus()) ? "PERMANENT" : CommonConstants.ACTIVE);
			u.setReplacedByUsername(repUser != null ? repUser.getUsername() : null);
		} else {
			u.setReplacementStatus(null);
			u.setReplacedByUsername(null);
		}
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUserById(Long userId) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		enrichWithReplacement(opt.get());
		return ResponseEntity
				.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User found.", Collections.singletonList(opt.get())));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUserByUsername(String username) {
		Optional<ReconUser> opt = reconUserRepository.findByUsername(username);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found: " + username, null));
		}
		return ResponseEntity
				.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User found.", Collections.singletonList(opt.get())));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUsersByBankId(Long bankId) {
		List<ReconUser> users = reconUserRepository.findByBankId(bankId);
		enrichWithRoleNames(users);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched by bank.", users));
	}

	/**
	 * Populates the transient roleName from ROLE_ID for display — roleName on
	 * ReconUser is
	 *
	 * @Transient and request-only (AddUser.jsx's Role dropdown sends it IN as a
	 *            name, resolved to roleId on create), so it was never set on the
	 *            way OUT. Every list screen reading u.roleName (User List's Role
	 *            column, etc.) saw it as permanently null/"—" without this.
	 *
	 *            Batched by design: one findByRoleIdIn over the list's distinct
	 *            role IDs instead of a findById per user — a per-user lookup
	 *            turns an N-row list into N extra DB round-trips (worse now that
	 *            a Bank Admin's user list rolls up every branch's users too).
	 */
	private void enrichWithRoleNames(List<ReconUser> users) {
		Set<Long> roleIds = users.stream().map(ReconUser::getRoleId).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, String> roleNameById = roleIds.isEmpty() ? Collections.emptyMap()
				: reconRoleMasterRepository.findByRoleIdIn(new ArrayList<>(roleIds)).stream()
						.collect(Collectors.toMap(ReconRoleMaster::getRoleId, ReconRoleMaster::getRoleName));
		users.forEach(u -> u.setRoleName(u.getRoleId() == null ? null : roleNameById.get(u.getRoleId())));
	}

	/**
	 * Emails a user when their role actually changed — no email when the role was
	 * left alone, and none when we can't resolve an address. Best-effort: a mail
	 * failure must never roll back the update that triggered it.
	 */
	private void notifyRoleChanged(ReconUser user, Long previousRoleId, String changedBy) {
		try {
			Long newRoleId = user.getRoleId();
			if (newRoleId == null || newRoleId.equals(previousRoleId))
				return;
			if (user.getEmail() == null || user.getEmail().trim().isEmpty())
				return;

			String oldName = previousRoleId == null ? null
					: reconRoleMasterRepository.findById(previousRoleId).map(ReconRoleMaster::getRoleName).orElse(null);
			String newName = reconRoleMasterRepository.findById(newRoleId).map(ReconRoleMaster::getRoleName)
					.orElse("Role #" + newRoleId);
			String actor = reconUserRepository.findByUsername(changedBy).map(ReconUser::getFullName).orElse(changedBy);

			emailService.sendRoleChangedNotification(user.getEmail(),
					user.getFullName() != null ? user.getFullName() : user.getUsername(), oldName, newName, actor);
		} catch (RuntimeException e) {
			logger.warn("Role-changed email failed for user {}: {}", user.getUserId(), e.getMessage());
		}
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUsersVisibleTo(String username) {
		// A non-admin sees only their own descendants. The Administration screens can
		// replace and
		// re-assign users, so showing a child their own parent (or an Admin) would hand
		// them a way
		// to swap that parent out and take their place.
		List<ReconUser> users = hierarchyScopeService.caller(username).map(hierarchyScopeService::visibleUsers)
				.orElse(java.util.Collections.emptyList());
		users.forEach(this::enrichWithReplacement);
		enrichWithRoleNames(users);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched.", users));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUsersByBankCode(String bankCode) {
		Optional<com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster> bankOpt = reconBankMasterRepository
				.findByBankCode(bankCode);
		if (!bankOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "No data", Collections.emptyList()));
		}
		List<ReconUser> users = reconUserRepository.findByBankId(bankOpt.get().getBankId());
		users.forEach(this::enrichWithReplacement);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched by bank.", users));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUsersByRoleId(Long roleId) {
		List<ReconUser> users = reconUserRepository.findByRoleId(roleId);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched by role.", users));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUsersByStatus(String status) {
		List<ReconUser> users = reconUserRepository.findByStatus(status);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched by status.", users));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUsersByType(String userType) {
		List<ReconUser> users = reconUserRepository.findByUserType(userType);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Users fetched by type.", users));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> updateUser(Long userId, ReconUser user, String updatedBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		ReconUser existing = opt.get();

		// The Role dropdown sends the role NAME (@JsonAlias "role"), never its ID — resolve it to
		// ROLE_ID exactly the way createUser does, BEFORE either branch below. Without this both the
		// Admin-immediate apply and the Maker-approval stash key off a null roleId, so a role change
		// (e.g. RECONCILIATOR -> CONFIGURE) was silently dropped.
		if (user.getRoleId() == null && user.getRoleName() != null && !user.getRoleName().trim().isEmpty()) {
			reconRoleMasterRepository.findByRoleName(user.getRoleName().trim())
					.ifPresent(r -> user.setRoleId(r.getRoleId()));
		}

		// Maker-checker on UPDATE: an Admin's edit applies immediately; a Maker's edit
		// is held as
		// a PENDING approval request (proposed changes stashed as JSON, live user
		// untouched) until
		// a Checker approves — same rule the CREATE flow already follows.
		boolean isAdmin = reconUserRepository.findByUsername(updatedBy)
				.map(a -> UserConstants.isAdminUserType(a.getUserType())).orElse(false);
		if (!isAdmin) {
			java.util.Map<String, Object> changes = new java.util.LinkedHashMap<>();
			if (user.getFullName() != null)
				changes.put("fullName", user.getFullName());
			if (user.getMobileNumber() != null)
				changes.put("mobileNumber", user.getMobileNumber());
			if (user.getDesignation() != null)
				changes.put("designation", user.getDesignation());
			if (user.getDepartment() != null)
				changes.put("department", user.getDepartment());
			if (user.getContactRank() != null)
				changes.put("contactRank", user.getContactRank());
			if (user.getRoleId() != null)
				changes.put("roleId", user.getRoleId());
			String json = ApprovalJson.write(changes);
			approvalAuditRecorder.recordSubmission(ApprovalAuditRecorder.ENTITY_USER, userId,
					ApprovalAuditRecorder.ACTION_UPDATE, updatedBy, json);
			logger.info("ReconUser update by Maker {} submitted for approval (user {})", updatedBy, userId);
			return ResponseEntity.ok(new RestWithStatusList("SUBMITTED_FOR_APPROVAL",
					"Your changes have been submitted to the Checker for approval.", null));
		}

		Long previousRoleId = existing.getRoleId();

		if (user.getFullName() != null)
			existing.setFullName(user.getFullName());
		if (user.getMobileNumber() != null)
			existing.setMobileNumber(user.getMobileNumber());
		if (user.getDesignation() != null)
			existing.setDesignation(user.getDesignation());
		if (user.getDepartment() != null)
			existing.setDepartment(user.getDepartment());
		if (user.getContactRank() != null)
			existing.setContactRank(user.getContactRank());
		if (user.getRoleId() != null)
			existing.setRoleId(user.getRoleId());
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(updatedBy);
		reconUserRepository.save(existing);

		// A role change silently rewrites what this person can do in the platform, so
		// they are told.
		notifyRoleChanged(existing, previousRoleId, updatedBy);

		logger.info("ReconUser updated: {} by {}", userId, updatedBy);
		return ResponseEntity.ok(
				new RestWithStatusList(CommonConstants.SUCCESS, "User updated successfully.", Collections.singletonList(existing)));
	}

	private static final java.util.regex.Pattern MOBILE_PATTERN = java.util.regex.Pattern.compile("^[6-9]\\d{9}$");

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> updateOwnMobileNumber(String username, String mobileNumber) {
		String mobile = mobileNumber == null ? null : mobileNumber.trim();
		if (mobile == null || !MOBILE_PATTERN.matcher(mobile).matches()) {
			return ResponseEntity.badRequest().body(
					new RestWithStatusList(CommonConstants.FAILURE, "Enter a valid 10-digit mobile number starting with 6-9.", null));
		}
		Optional<ReconUser> opt = reconUserRepository.findByUsername(username);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found.", null));
		}
		ReconUser existing = opt.get();
		existing.setMobileNumber(mobile);
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(username);
		reconUserRepository.save(existing);
		logger.info("Mobile number self-updated for user {}", username);
		return ResponseEntity
				.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Mobile number updated.", Collections.singletonList(existing)));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> updateStatus(Long userId, String status, String updatedBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		ReconUser existing = opt.get();
		// The Checker Queue rejects a user via update-status?status=REJECTED — there is
		// no
		// separate reject endpoint, so that is where the decision has to be captured.
		// Only ever
		// valid from PENDING_APPROVAL: if two Checkers race on the same user, or one
		// Checker's
		// reject arrives after another already decided, the second call must not win.
		if ("REJECTED".equalsIgnoreCase(status)) {
			if (!"PENDING_APPROVAL".equals(existing.getStatus())) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
			}
			int rowsUpdated = reconUserRepository.compareAndSetStatus(userId, "PENDING_APPROVAL", "REJECTED");
			if (rowsUpdated == 0) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
			}
			existing = reconUserRepository.findById(userId).orElse(existing);
			existing.setUpdatedAt(LocalDateTime.now());
			existing.setUpdatedBy(updatedBy);
			reconUserRepository.save(existing);
			approvalAuditRecorder.recordDecision(
					com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ENTITY_USER, userId,
					updatedBy, "REJECTED", null);
			notifyMakerOfUserDecision(existing, "Rejected", updatedBy);
			logger.info("ReconUser status updated to {} for ID: {} by {}", status, userId, updatedBy);
			return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User status updated.", null));
		}
		// Every other transition keeps its original unconditional behaviour — this
		// method is used
		// elsewhere for status changes that aren't a Checker decision on a pending
		// item.
		existing.setStatus(status);
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(updatedBy);
		reconUserRepository.save(existing);
		logger.info("ReconUser status updated to {} for ID: {} by {}", status, userId, updatedBy);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User status updated.", null));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> approveUser(Long userId, String approvedBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		ReconUser existing = opt.get();
		// Only a user still awaiting a Checker's decision can be approved. Without
		// this, a second
		// Checker approving after the first already decided (approved or rejected)
		// would silently
		// re-run this whole method — regenerating the password and re-sending the
		// welcome email.
		if (!"PENDING_APPROVAL".equals(existing.getStatus())) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
		}
		// Atomic claim: if another Checker's decision landed between our read above and
		// this write,
		// rowsUpdated is 0 and we must not proceed (first completed decision always
		// wins).
		int rowsUpdated = reconUserRepository.compareAndSetStatus(userId, "PENDING_APPROVAL", "REQUEST");
		if (rowsUpdated == 0) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "This request has already been processed.", null));
		}
		existing = reconUserRepository.findById(userId).orElse(existing);
		existing.setApprovedYn(CommonConstants.ACTIVE_Y);
		existing.setApprovedBy(approvedBy);
		// A Checker approving a Maker-created user is the moment it becomes usable —
		// same
		// REQUEST status (awaiting the user's own email verification) an
		// Admin-direct-created
		// user gets immediately. The password is generated here (not at creation) since
		// this
		// is the first point the user is actually meant to log in. (Status is already
		// REQUEST —
		// set by the CAS above — this line is kept so the field stays visibly
		// authoritative here.)
		existing.setStatus("REQUEST");
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(approvedBy);
		if (existing.getPasswordSet() == null || existing.getPasswordSet() == 0) {
			String defaultPwd = generatePassword();
			existing.setPasswordHash(passwordEncoder.encode(defaultPwd));
			// Stays 0 — a temporary/default password, not one the user chose. Same fix as
			// createUser() above: 1 would make verify-email misreport OLD_USER and skip the
			// mandatory verify-credentials -> set-password flow.
			existing.setPasswordSet(0);
			ReconUser saved = reconUserRepository.save(existing);

			ReconPasswordManager pwdHist = new ReconPasswordManager();
			pwdHist.setReconUser(saved);
			pwdHist.setUserPassword(saved.getPasswordHash());
			pwdHist.setCreatedAt(LocalDateTime.now());
			pwdHist.setCreatedBy(approvedBy);
			pwdHist.setExpirationDate(LocalDateTime.now().plusDays(90));
			reconPasswordManagerRepository.save(pwdHist);

			sendUserWelcomeEmail(saved, defaultPwd);
		} else {
			reconUserRepository.save(existing);
		}
		approvalAuditRecorder.recordDecision(
				com.jpb.reconciliation.reconciliation.service.v2.ApprovalAuditRecorder.ENTITY_USER, userId, approvedBy,
				"APPROVED", null);
		notifyMakerOfUserDecision(existing, "Approved", approvedBy);
		logger.info("ReconUser approved: {} by {}", userId, approvedBy);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User approved successfully.", null));
	}

	/**
	 * Tells the Maker what the Checker decided about the user they created — the
	 * same courtesy a Role submission already got. CREATED_BY is the Maker: a user
	 * record has no SUBMITTED_BY column, because creating one *is* the submission
	 * (it lands straight in PENDING_APPROVAL).
	 */
	private void notifyMakerOfUserDecision(ReconUser subject, String decision, String decidedBy) {
		if (subject == null || subject.getCreatedBy() == null)
			return;
		Optional<ReconUser> maker = reconUserRepository.findByUsername(subject.getCreatedBy());
		if (!maker.isPresent() || maker.get().getEmail() == null)
			return;
		emailService.sendWorkflowDecisionNotification(maker.get().getEmail(), maker.get().getFullName(), "User",
				subject.getFullName(), subject.getUsername(), decision, decidedBy);
	}

	private String generatePassword() {
		int digits = 1000 + RNG.nextInt(9000);
		return "Recon@" + digits;
	}

	private void sendUserWelcomeEmail(ReconUser user, String defaultPwd) {
		try {
			if (user.getBankId() == null)
				return;
			Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findPrimaryById(user.getBankId());
			if (!bankOpt.isPresent())
				return;
			ReconBankMaster bank = bankOpt.get();
			boolean isBranch = "BRANCH".equals(bank.getBankLevel());
			String verifyLink = frontendUrl + "/user-verify" + "?bankCode=" + bank.getBankCode()
					+ (isBranch ? "&branchCode=" + bank.getBankCode() : "") + "&username=" + user.getUsername();
			emailService.sendUserWelcome(user.getEmail(), user.getFullName(), bank.getBankCode(),
					isBranch ? "Branch Code" : "Bank Code", user.getUsername(), defaultPwd, verifyLink);
		} catch (Exception e) {
			logger.error("Failed to send welcome email for user {}: {}", user.getUsername(), e.getMessage());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> deleteUser(Long userId) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		ReconUser existing = opt.get();
		existing.setStatus(CommonConstants.INACTIVE);
		existing.setUpdatedAt(LocalDateTime.now());
		reconUserRepository.save(existing);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User deactivated successfully.", null));
	}

	@Override
	public ResponseEntity<RestWithStatusList> checkUsernameExists(String username) {
		boolean exists = reconUserRepository.existsByUsername(username);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, exists ? "EXISTS" : "AVAILABLE", null));
	}

	@Override
	public ResponseEntity<RestWithStatusList> checkEmailExists(String email) {
		// Only a NON-blocked holder makes an email unavailable — a blocked account's
		// email is
		// reusable for a fresh account (EmailRegistryService is the single source of
		// truth).
		boolean exists = emailRegistry.isActivelyRegistered(email);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, exists ? "EXISTS" : "AVAILABLE", null));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> blockUser(Long userId, String reason, String updatedBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		ReconUser existing = opt.get();
		if ("BLOCKED".equals(existing.getStatus())) {
			return ResponseEntity.badRequest()
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User is already BLOCKED.", null));
		}
		existing.setPreBlockStatus(existing.getStatus());
		existing.setStatus("BLOCKED");
		existing.setBlockReason(reason);
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(updatedBy);
		reconUserRepository.save(existing);
		logger.info("ReconUser BLOCKED: userId={} preBlockStatus={} by {}", userId, existing.getPreBlockStatus(),
				updatedBy);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User blocked successfully.", null));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> unblockUser(Long userId, String updatedBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		ReconUser existing = opt.get();
		if (!"BLOCKED".equals(existing.getStatus())) {
			return ResponseEntity.badRequest()
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User is not currently BLOCKED.", null));
		}
		String restoreStatus = existing.getPreBlockStatus() != null ? existing.getPreBlockStatus() : CommonConstants.ACTIVE;
		existing.setStatus(restoreStatus);
		existing.setPreBlockStatus(null);
		existing.setBlockReason(null);
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(updatedBy);
		reconUserRepository.save(existing);
		logger.info("ReconUser UNBLOCKED: userId={} restored to {} by {}", userId, restoreStatus, updatedBy);
		return ResponseEntity
				.ok(new RestWithStatusList(CommonConstants.SUCCESS, "User unblocked. Status restored to: " + restoreStatus, null));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> scheduleInactivate(Long userId, java.time.LocalDateTime scheduledAt,
			String scheduledBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
			return ResponseEntity.badRequest()
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Scheduled time must be in the future.", null));
		}
		ReconUser existing = opt.get();
		existing.setInactivateScheduledAt(scheduledAt);
		existing.setInactivateScheduledBy(scheduledBy);
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(scheduledBy);
		reconUserRepository.save(existing);
		logger.info("ReconUser INACTIVATE scheduled at {} for userId={} by {}", scheduledAt, userId, scheduledBy);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Inactivation scheduled at: " + scheduledAt, null));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> scheduleReactivate(Long userId, java.time.LocalDateTime scheduledAt,
			String scheduledBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
			return ResponseEntity.badRequest()
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Scheduled time must be in the future.", null));
		}
		ReconUser existing = opt.get();
		existing.setReactivateScheduledAt(scheduledAt);
		existing.setReactivateScheduledBy(scheduledBy);
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(scheduledBy);
		reconUserRepository.save(existing);
		logger.info("ReconUser REACTIVATE scheduled at {} for userId={} by {}", scheduledAt, userId, scheduledBy);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Reactivation scheduled at: " + scheduledAt, null));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> scheduleBlock(Long userId, java.time.LocalDateTime scheduledAt,
			String scheduledBy, String reason) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
			return ResponseEntity.badRequest()
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Scheduled time must be in the future.", null));
		}
		ReconUser existing = opt.get();
		existing.setBlockScheduledAt(scheduledAt);
		existing.setBlockScheduledBy(scheduledBy);
		existing.setBlockReason(reason);
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(scheduledBy);
		reconUserRepository.save(existing);
		logger.info("ReconUser BLOCK scheduled at {} for userId={} by {}", scheduledAt, userId, scheduledBy);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Block scheduled at: " + scheduledAt, null));
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> cancelSchedule(Long userId, String scheduleType, String updatedBy) {
		Optional<ReconUser> opt = reconUserRepository.findById(userId);
		if (!opt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "User not found with ID: " + userId, null));
		}
		ReconUser existing = opt.get();
		if (scheduleType == null) {
			return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
					"scheduleType is required (INACTIVATE / REACTIVATE / BLOCK).", null));
		}
		switch (scheduleType.toUpperCase()) {
		case "INACTIVATE":
			existing.setInactivateScheduledAt(null);
			existing.setInactivateScheduledBy(null);
			break;
		case "REACTIVATE":
			existing.setReactivateScheduledAt(null);
			existing.setReactivateScheduledBy(null);
			break;
		case "BLOCK":
			existing.setBlockScheduledAt(null);
			existing.setBlockScheduledBy(null);
			break;
		default:
			return ResponseEntity.badRequest().body(new RestWithStatusList(CommonConstants.FAILURE,
					"Invalid scheduleType. Use INACTIVATE, REACTIVATE, or BLOCK.", null));
		}
		existing.setUpdatedAt(LocalDateTime.now());
		existing.setUpdatedBy(updatedBy);
		reconUserRepository.save(existing);
		logger.info("ReconUser {} schedule cancelled for userId={} by {}", scheduleType, userId, updatedBy);
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, scheduleType + " schedule cancelled.", null));
	}

	@Override
	public ResponseEntity<RestWithStatusList> getPendingUsersForChecker(String checkerUsername) {
		Optional<ReconUser> checkerOpt = reconUserRepository.findByUsername(checkerUsername);
		if (!checkerOpt.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new RestWithStatusList(CommonConstants.FAILURE, "Checker not found: " + checkerUsername, null));
		}
		ReconUser checker = checkerOpt.get();
		List<ReconUser> pending = reconUserRepository.findByStatus("PENDING_APPROVAL");
		List<ReconUser> visible = pending.stream()
				.filter(u -> isVisibleToChecker(checker, u.getCreatedBy(), u.getRoleId())).collect(Collectors.toList());
		return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Pending users fetched.", visible));
	}

	// Same scoping rule as
	// ReconRoleMasterServiceImpl.isVisibleToChecker/getPendingRolesForChecker
	// — duplicated here rather than shared, since the two services live in
	// different packages
	// (service vs service.v2) and this is a handful of lines, not worth a shared
	// utility class for.
	private boolean isVisibleToChecker(ReconUser checker, String submitterUsername, Long itemRoleId) {
		if ("KAL_ADMIN".equals(checker.getUserType()))
			return true;

		Optional<ReconUser> submitterOpt = reconUserRepository.findByUsername(submitterUsername);
		if (!submitterOpt.isPresent() || !Objects.equals(submitterOpt.get().getBankId(), checker.getBankId())) {
			return false;
		}

		if (UserConstants.isAdminUserType(checker.getUserType()))
			return true;

		Set<Long> checkerScope = resolveProductScope(checker.getRoleId());
		Set<Long> itemScope = resolveProductScope(itemRoleId);
		return checkerScope.isEmpty() || itemScope.isEmpty() || !Collections.disjoint(checkerScope, itemScope);
	}

	private Set<Long> resolveProductScope(Long roleId) {
		if (roleId == null)
			return Collections.emptySet();
		return new HashSet<>(roleProductMapRepository.findProductIdsByRoleId(roleId));
	}
}
