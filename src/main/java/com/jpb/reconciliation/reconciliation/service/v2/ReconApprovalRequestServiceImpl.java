package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconApprovalRequest;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconApprovalRequestRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReconApprovalRequestServiceImpl implements ReconApprovalRequestService {

    private static final Logger logger = LoggerFactory.getLogger(ReconApprovalRequestServiceImpl.class);

    @Autowired
    private ReconApprovalRequestRepository reconApprovalRequestRepository;

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository reconRoleMasterRepository;

    @Autowired
    private com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository menuMasterRepository;

    @Autowired
    private com.jpb.reconciliation.reconciliation.service.EmailService emailService;

    private static final String ACTION_UPDATE = "UPDATE";
    private static final String STATUS_PENDING = "PENDING";

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> submitRequest(ReconApprovalRequest request) {
        if (request.getEntityType() == null || request.getEntityId() == null || request.getMakerId() == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Entity type, entity ID, and maker ID are required.", null));
        }
        request.setStatus("PENDING");
        request.setSubmittedAt(LocalDateTime.now());
        ReconApprovalRequest saved = reconApprovalRequestRepository.save(request);
        logger.info("ReconApprovalRequest submitted: entityType={}, entityId={}, makerId={}",
                saved.getEntityType(), saved.getEntityId(), saved.getMakerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS, "Approval request submitted.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getPendingRequests() {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByStatus("PENDING");
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Pending approval requests fetched.", requests));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestById(Long requestId) {
        Optional<ReconApprovalRequest> opt = reconApprovalRequestRepository.findById(requestId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Approval request not found with ID: " + requestId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Request found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestsByEntityType(String entityType) {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByEntityTypeAndStatus(entityType, "PENDING");
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Requests fetched by entity type.", requests));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestsByMakerId(Long makerId) {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByMakerId(makerId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Requests fetched by maker.", requests));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestsByCheckerId(Long checkerId) {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByCheckerId(checkerId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Requests fetched by checker.", requests));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> approveRequest(Long requestId, Long checkerId, String remarks) {
        Optional<ReconApprovalRequest> opt = reconApprovalRequestRepository.findById(requestId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Approval request not found with ID: " + requestId, null));
        }
        ReconApprovalRequest existing = opt.get();
        existing.setStatus("APPROVED");
        existing.setDecision("APPROVED");
        existing.setCheckerId(checkerId);
        existing.setCheckedAt(LocalDateTime.now());
        existing.setRemarks(remarks);
        reconApprovalRequestRepository.save(existing);
        logger.info("ReconApprovalRequest approved: requestId={}, checkerId={}", requestId, checkerId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Request approved.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> rejectRequest(Long requestId, Long checkerId, String remarks) {
        Optional<ReconApprovalRequest> opt = reconApprovalRequestRepository.findById(requestId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Approval request not found with ID: " + requestId, null));
        }
        ReconApprovalRequest existing = opt.get();
        existing.setStatus("REJECTED");
        existing.setDecision("REJECTED");
        existing.setCheckerId(checkerId);
        existing.setCheckedAt(LocalDateTime.now());
        existing.setRemarks(remarks);
        reconApprovalRequestRepository.save(existing);
        logger.info("ReconApprovalRequest rejected: requestId={}, checkerId={}", requestId, checkerId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Request rejected.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getHistoryFor(Long userId, Long bankId, boolean isAdmin) {
        List<ReconApprovalRequest> rows;
        if (isAdmin && bankId == null) {
            rows = reconApprovalRequestRepository.findAll();           // KAL_ADMIN → everything
        } else if (isAdmin) {
            rows = reconApprovalRequestRepository.findByMakerBankId(bankId);  // bank/branch admin → own institution
        } else {
            rows = reconApprovalRequestRepository.findByMakerId(userId);      // normal user → own requests
        }

        // The row stores maker/checker IDs only; a read-only history page needs names. Resolve
        // each id once into a small cache rather than per row.
        Map<Long, String> nameCache = new LinkedHashMap<>();
        List<Object> out = new ArrayList<>();
        for (ReconApprovalRequest a : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("requestId", a.getRequestId());
            m.put("entityType", a.getEntityType());
            m.put("entityId", a.getEntityId());
            m.put("actionType", a.getActionType());
            m.put("status", a.getStatus());
            m.put("decision", a.getDecision());
            m.put("remarks", a.getRemarks());
            m.put("submittedAt", a.getSubmittedAt());
            m.put("checkedAt", a.getCheckedAt());
            m.put("requestedBy", resolveName(a.getMakerId(), nameCache));
            m.put("approvedBy", resolveName(a.getCheckerId(), nameCache));
            out.add(m);
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Approval history fetched.", out));
    }

    private String resolveName(Long userId, Map<Long, String> cache) {
        if (userId == null) return null;
        return cache.computeIfAbsent(userId, id ->
                reconUserRepository.findById(id).map(ReconUser::getFullName).orElse("User #" + id));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getPendingUpdatesForChecker(String checkerUsername) {
        Optional<ReconUser> checkerOpt = reconUserRepository.findByUsername(checkerUsername);
        if (!checkerOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Checker not found: " + checkerUsername, null));
        }
        Long bankId = checkerOpt.get().getBankId();
        List<ReconApprovalRequest> rows = (bankId == null)
                ? reconApprovalRequestRepository.findByActionTypeAndStatus(ACTION_UPDATE, STATUS_PENDING)  // KAL_ADMIN → all
                : reconApprovalRequestRepository.findPendingUpdatesByBank(ACTION_UPDATE, STATUS_PENDING, bankId);

        Map<Long, String> nameCache = new LinkedHashMap<>();
        List<Object> out = new ArrayList<>();
        for (ReconApprovalRequest a : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("requestId", a.getRequestId());
            m.put("entityType", a.getEntityType());
            m.put("entityId", a.getEntityId());
            m.put("entityName", resolveEntityName(a.getEntityType(), a.getEntityId()));
            m.put("actionType", a.getActionType());
            m.put("status", a.getStatus());
            m.put("submittedAt", a.getSubmittedAt());
            m.put("requestedBy", resolveName(a.getMakerId(), nameCache));
            m.put("changes", ApprovalJson.read(a.getProposedChanges()));
            out.add(m);
        }
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Pending update requests fetched.", out));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> decideUpdateRequest(Long requestId, String checkerUsername,
                                                                  String decision, String remarks) {
        Optional<ReconApprovalRequest> opt = reconApprovalRequestRepository.findById(requestId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Approval request not found with ID: " + requestId, null));
        }
        ReconApprovalRequest req = opt.get();
        if (!ACTION_UPDATE.equals(req.getActionType()) || !STATUS_PENDING.equals(req.getStatus())) {
            return ResponseEntity.ok(new RestWithStatusList(CommonConstants.FAILURE, "This request is not a pending update.", null));
        }
        boolean approved = "APPROVED".equalsIgnoreCase(decision);
        if (approved) {
            applyProposedChanges(req.getEntityType(), req.getEntityId(), ApprovalJson.read(req.getProposedChanges()), checkerUsername);
        }
        req.setDecision(approved ? "APPROVED" : "REJECTED");
        req.setStatus(approved ? "APPROVED" : "REJECTED");
        req.setCheckerId(reconUserRepository.findByUsername(checkerUsername).map(ReconUser::getUserId).orElse(null));
        req.setCheckedAt(LocalDateTime.now());
        req.setRemarks(remarks);
        reconApprovalRequestRepository.save(req);
        logger.info("Update request {} {} by {}", requestId, req.getStatus(), checkerUsername);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS,
                approved ? "Update approved and applied." : "Update rejected.", null));
    }

    private String resolveEntityName(String entityType, Long entityId) {
        if (entityId == null) return null;
        if (ApprovalAuditRecorder.ENTITY_USER.equals(entityType)) {
            return reconUserRepository.findById(entityId).map(ReconUser::getFullName).orElse("User #" + entityId);
        }
        if (ApprovalAuditRecorder.ENTITY_ROLE.equals(entityType)) {
            return reconRoleMasterRepository.findById(entityId)
                    .map(com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster::getRoleName).orElse("Role #" + entityId);
        }
        if (ApprovalAuditRecorder.ENTITY_MENU.equals(entityType)) {
            return menuMasterRepository.findByMenuId(entityId)
                    .map(com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster::getMenuName).orElse("Menu #" + entityId);
        }
        return null;
    }

    // Applies a maker's approved proposed changes onto the live entity, field by field. Only the
    // fields the corresponding update endpoint itself allows are honoured — anything else in the
    // JSON is ignored, so a stale/tampered payload can never write an unexpected column.
    private void applyProposedChanges(String entityType, Long entityId, Map<String, Object> changes, String by) {
        if (changes == null || changes.isEmpty()) return;
        if (ApprovalAuditRecorder.ENTITY_USER.equals(entityType)) {
            reconUserRepository.findById(entityId).ifPresent(u -> {
                Long previousRoleId = u.getRoleId();
                if (changes.containsKey("fullName")) u.setFullName(asString(changes.get("fullName")));
                if (changes.containsKey("mobileNumber")) u.setMobileNumber(asString(changes.get("mobileNumber")));
                if (changes.containsKey("designation")) u.setDesignation(asString(changes.get("designation")));
                if (changes.containsKey("department")) u.setDepartment(asString(changes.get("department")));
                if (changes.containsKey("contactRank")) u.setContactRank(asString(changes.get("contactRank")));
                if (changes.containsKey("roleId")) u.setRoleId(asLong(changes.get("roleId")));
                u.setUpdatedAt(LocalDateTime.now());
                u.setUpdatedBy(by);
                reconUserRepository.save(u);
                // The role only really changes at approval time on this path, so this is where the
                // user gets told about it.
                notifyRoleChanged(u, previousRoleId, by);
            });
        } else if (ApprovalAuditRecorder.ENTITY_ROLE.equals(entityType)) {
            reconRoleMasterRepository.findById(entityId).ifPresent(r -> {
                if (changes.containsKey("roleName")) r.setRoleName(asString(changes.get("roleName")));
                if (changes.containsKey("roleDesc")) r.setRoleDesc(asString(changes.get("roleDesc")));
                if (changes.containsKey("roleType")) r.setRoleType(asString(changes.get("roleType")));
                r.setUpdatedAt(LocalDateTime.now());
                r.setUpdatedBy(by);
                reconRoleMasterRepository.save(r);
            });
        } else if (ApprovalAuditRecorder.ENTITY_MENU.equals(entityType)) {
            menuMasterRepository.findByMenuId(entityId).ifPresent(mn -> {
                if (changes.containsKey("menuName")) mn.setMenuName(asString(changes.get("menuName")));
                if (changes.containsKey("menuDescription")) mn.setMenuDescription(asString(changes.get("menuDescription")));
                if (changes.containsKey("menuUrl")) mn.setMenuUrl(asString(changes.get("menuUrl")));
                menuMasterRepository.save(mn);
            });
        }
    }

    /** Best-effort: tells a user their role changed. A mail failure never rolls back the approval. */
    private void notifyRoleChanged(ReconUser user, Long previousRoleId, String changedBy) {
        try {
            Long newRoleId = user.getRoleId();
            if (newRoleId == null || newRoleId.equals(previousRoleId)) return;
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) return;

            String oldName = previousRoleId == null ? null
                    : reconRoleMasterRepository.findById(previousRoleId)
                        .map(com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster::getRoleName).orElse(null);
            String newName = reconRoleMasterRepository.findById(newRoleId)
                    .map(com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster::getRoleName)
                    .orElse("Role #" + newRoleId);
            String actor = reconUserRepository.findByUsername(changedBy).map(ReconUser::getFullName).orElse(changedBy);

            emailService.sendRoleChangedNotification(user.getEmail(),
                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    oldName, newName, actor);
        } catch (RuntimeException e) {
            logger.warn("Role-changed email failed for user {}: {}", user.getUserId(), e.getMessage());
        }
    }

    private String asString(Object v) { return v == null ? null : String.valueOf(v); }

    private Long asLong(Object v) {
        if (v == null) return null;
        try { return Long.valueOf(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }
}



