package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.AdminContext;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContextResolver {

    private final ReconUserRepository reconUserRepository;
    private final ReconBankMasterRepository reconBankMasterRepository;

    public AdminContext resolve(Authentication auth) {
        if (auth == null) {
            throw new IllegalStateException("No authentication present");
        }
        String principal = auth.getName();
        Optional<ReconUser> userOpt = reconUserRepository.findByUsername(principal);
        if (!userOpt.isPresent()) {
            userOpt = reconUserRepository.findByEmail(principal);
        }
        if (!userOpt.isPresent()) {
            throw new IllegalStateException("User not found: " + principal);
        }
        ReconUser user = userOpt.get();
        String bankCode   = null;
        String branchCode = null;

        if (user.getBankId() != null) {
            Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findById(user.getBankId());
            if (bankOpt.isPresent()) {
                ReconBankMaster bank = bankOpt.get();
                if (bank.getParentBankId() != null) {
                    branchCode = bank.getBankCode();
                    bankCode = reconBankMasterRepository.findById(bank.getParentBankId())
                            .map(ReconBankMaster::getBankCode).orElse(null);
                } else {
                    bankCode = bank.getBankCode();
                }
            }
        }

        log.info("[RESOLVE] user='{}' userType='{}' bankCode='{}' branchCode='{}'",
                user.getUsername(), user.getUserType(), bankCode, branchCode);
        return new AdminContext(user.getUsername(), bankCode, branchCode, user.getUserId());
    }
}
