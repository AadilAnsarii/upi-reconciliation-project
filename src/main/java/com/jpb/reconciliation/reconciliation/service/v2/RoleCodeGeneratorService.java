package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A custom role (created via Add Role, e.g. MAKER/CHECKER/etc.) gets a unique 4-digit
 * ROLE_CODE, never typed by hand. Starts at "0001" and counts up, checking each candidate
 * against RECON_ROLE_MASTER.ROLE_CODE — if "0001" is already taken, tries "0002", and so on.
 */
@Service
public class RoleCodeGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(RoleCodeGeneratorService.class);

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    /** Peek/reserve — called while the Add Role form is open, to show the code to the user. */
    public String peekNextCode(String roleName) {
        String next = findNextAvailableCode();
        logger.info("Reserved role code for roleName={} -> {}", roleName, next);
        return next;
    }

    /** Fresh code — fallback if the reserved code from the form went stale before submit. */
    public String generateNextCode(String roleName) {
        String next = findNextAvailableCode();
        logger.info("Fresh role code for roleName={} -> {}", roleName, next);
        return next;
    }

    private String findNextAvailableCode() {
        int n = 1;
        String code;
        do {
            code = String.format("%04d", n);
            n++;
        } while (reconRoleMasterRepository.existsByRoleCode(code));
        return code;
    }
}
