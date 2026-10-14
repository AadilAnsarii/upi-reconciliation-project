package com.jpb.reconciliation.reconciliation.controller.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.AuditUserDelegation;
import com.jpb.reconciliation.reconciliation.repository.v2.AuditUserDelegationRepository;
import com.jpb.reconciliation.reconciliation.service.v2.DelegationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/delegations")
@CrossOrigin(origins = "*")
public class DelegationController {

    @Autowired private AuditUserDelegationRepository delegationRepository;
    @Autowired private DelegationService delegationService;

    /** Admin-tooling "create delegation" action (UserManagement.jsx) */
    @PostMapping("/delegate")
    public ResponseEntity<RestWithStatusList> delegate(
            @RequestBody Map<String, Object> body, Authentication authentication) {
        Long delegatorUserId = Long.valueOf(String.valueOf(body.get("delegatorUserId")));
        Long delegateeUserId = Long.valueOf(String.valueOf(body.get("delegateeUserId")));
        String reason = (String) body.get("reason");
        String actor = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return delegationService.delegateNow(delegatorUserId, delegateeUserId, reason, actor);
    }

    /** Admin-tooling "restore" action (UserManagement.jsx) */
    @PostMapping("/restore/{delegationId}")
    public ResponseEntity<RestWithStatusList> restore(
            @PathVariable Long delegationId, Authentication authentication) {
        String actor = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() : "UNKNOWN";
        return delegationService.restoreDelegationById(delegationId, actor);
    }

    /** Get all delegations for a specific delegator user */
    @GetMapping("/delegator/{userId}")
    public ResponseEntity<List<AuditUserDelegation>> getByDelegator(@PathVariable Long userId) {
        return ResponseEntity.ok(delegationRepository.findByDelegatorUserId(userId));
    }

    /** Get all delegations assigned to a specific delegatee user */
    @GetMapping("/delegatee/{userId}")
    public ResponseEntity<List<AuditUserDelegation>> getByDelegatee(@PathVariable Long userId) {
        return ResponseEntity.ok(delegationRepository.findByDelegateeUserId(userId));
    }

    /** Get all delegations for a bank */
    @GetMapping("/bank/{bankId}")
    public ResponseEntity<List<AuditUserDelegation>> getByBank(@PathVariable Long bankId) {
        return ResponseEntity.ok(delegationRepository.findByBankId(bankId));
    }

    /** Get active delegations for a bank */
    @GetMapping("/bank/{bankId}/active")
    public ResponseEntity<List<AuditUserDelegation>> getActiveDelegationsByBank(@PathVariable Long bankId) {
        return ResponseEntity.ok(delegationRepository.findByBankIdAndStatus(bankId, CommonConstants.ACTIVE));
    }

    /** Get the latest active delegation for a delegator (used by UI to show delegation badge) */
    @GetMapping("/delegator/{userId}/active")
    public ResponseEntity<?> getActiveDelegation(@PathVariable Long userId) {
        Optional<AuditUserDelegation> opt = delegationRepository
                .findTopByDelegatorUserIdAndStatusOrderByCreatedAtDesc(userId, CommonConstants.ACTIVE);
        return opt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(null));
    }
}
