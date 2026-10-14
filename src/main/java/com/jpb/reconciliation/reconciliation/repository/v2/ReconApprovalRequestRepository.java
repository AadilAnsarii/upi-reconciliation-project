package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconApprovalRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconApprovalRequestRepository extends JpaRepository<ReconApprovalRequest, Long> {

    List<ReconApprovalRequest> findByStatus(String status);

    List<ReconApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<ReconApprovalRequest> findByMakerId(Long makerId);

    List<ReconApprovalRequest> findByCheckerId(Long checkerId);

    List<ReconApprovalRequest> findByEntityTypeAndStatus(String entityType, String status);

    Optional<ReconApprovalRequest> findByEntityTypeAndEntityIdAndStatus(String entityType, Long entityId, String status);

    List<ReconApprovalRequest> findByActionTypeAndStatus(String actionType, String status);

    // Pending update requests raised by makers of this bank/branch (scoped through the maker,
    // since requests carry no bankId). Used to populate a Checker's "Update Requests" queue.
    @Query("SELECT a FROM ReconApprovalRequest a WHERE a.actionType = :actionType AND a.status = :status "
         + "AND a.makerId IN (SELECT u.userId FROM ReconUser u WHERE u.bankId = :bankId) ORDER BY a.submittedAt DESC")
    List<ReconApprovalRequest> findPendingUpdatesByBank(@Param("actionType") String actionType,
                                                        @Param("status") String status, @Param("bankId") Long bankId);

    // Approval History scope: every request whose Maker belongs to this bank. A branch is its
    // own RECON_BANK_MASTER row, so one bankId covers a Bank Admin's bank or a Branch Admin's
    // branch. Requests have no bankId of their own, so it is derived through the Maker.
    @Query("SELECT a FROM ReconApprovalRequest a WHERE a.makerId IN "
         + "(SELECT u.userId FROM ReconUser u WHERE u.bankId = :bankId) ORDER BY a.submittedAt DESC")
    List<ReconApprovalRequest> findByMakerBankId(@Param("bankId") Long bankId);
}
