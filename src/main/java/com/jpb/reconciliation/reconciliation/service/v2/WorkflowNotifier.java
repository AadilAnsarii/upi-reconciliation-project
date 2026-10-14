package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.CRoleMenuMap;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CRoleMenuMapRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Emails the Checker(s) when a Maker submits a Role / Menu / User for approval.
 *
 * The decision half of the cycle already notified the Maker (sendWorkflowDecisionNotification),
 * but the submission half never notified anyone — a Maker could submit an item and no Checker
 * would learn about it until they happened to open the Checker Queue.
 *
 * Recipients are derived, never guessed: candidates are the accounts that can reach a Checker
 * Queue at all, and each one is then run through the *caller's own* visibility predicate — the
 * same {@code isVisibleToChecker} the queue itself uses. So the set of people emailed is exactly
 * the set of people who will actually see the item, with no second copy of the scoping rules.
 */
@Component
public class WorkflowNotifier {

    /** The privilege that makes an account a Checker. Same name the SOD rule keys on. */
    private static final String CHECKER_MENU_NAME = "Checker Dashboard";

    private static final Logger logger = LoggerFactory.getLogger(WorkflowNotifier.class);

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private MenuMasterRepository menuMasterRepository;

    @Autowired
    private CRoleMenuMapRepository roleMenuMapRepository;

    @Autowired
    private EmailService emailService;

    /**
     * @param itemType   "Role", "Menu" or "User" — used verbatim in the subject line.
     * @param visibleTo  the caller's own queue-visibility rule for a candidate checker.
     */
    public void notifySubmission(String itemType, String itemName, String itemCode,
                                 String submitterUsername, Predicate<ReconUser> visibleTo) {
        try {
            String submitterName = reconUserRepository.findByUsername(submitterUsername)
                    .map(ReconUser::getFullName).orElse(submitterUsername);

            for (ReconUser checker : candidateCheckers(submitterUsername)) {
                if (!visibleTo.test(checker)) continue;
                emailService.sendWorkflowSubmittedNotification(
                        checker.getEmail(), checker.getFullName(), itemType, itemName, itemCode, submitterName);
            }
        } catch (RuntimeException e) {
            // A submission must never fail because its notification did.
            logger.warn("Failed to notify checkers of {} submission '{}'", itemType, itemName, e);
        }
    }

    /**
     * Every account that could act as a Checker: anyone holding the Checker Dashboard privilege,
     * plus the Admins of the submitter's own bank (an Admin's Checker Queue is unfiltered at
     * their level, so a Maker's submission lands there too).
     *
     * Deduplicated by email — a bank's Admin can also carry a Checker role, and the same person
     * must never receive the request twice. The submitter is excluded: SOD forbids a Maker from
     * checking their own work, so mailing them would be noise.
     */
    private List<ReconUser> candidateCheckers(String submitterUsername) {
        Map<String, ReconUser> byEmail = new LinkedHashMap<>();

        for (ReconMenuMaster menu : checkerDashboardMenus()) {
            for (CRoleMenuMap map : roleMenuMapRepository.findByMenuId(menu.getMenuId())) {
                for (ReconUser u : reconUserRepository.findByRoleId(map.getId().getRoleId())) {
                    collect(byEmail, u, submitterUsername);
                }
            }
        }

        Optional<ReconUser> submitter = reconUserRepository.findByUsername(submitterUsername);
        if (submitter.isPresent() && submitter.get().getBankId() != null) {
            for (ReconUser u : reconUserRepository.findByBankId(submitter.get().getBankId())) {
                if (UserConstants.isAdminUserType(u.getUserType())) collect(byEmail, u, submitterUsername);
            }
        }

        return new ArrayList<>(byEmail.values());
    }

    /**
     * Matched by name across every menu type, because a user-portal role is granted a rewritten
     * "twin" row of the same menu (see ReconRoleMasterServiceImpl.getOrCreateUserTwinMenu) that
     * carries its own MENU_ID — keying on a single id would miss every Checker on the /user portal.
     */
    private List<ReconMenuMaster> checkerDashboardMenus() {
        List<ReconMenuMaster> menus = new ArrayList<>(menuMasterRepository.findAllByMenuNameAndMenuType(CHECKER_MENU_NAME, "Main"));
        menus.addAll(menuMasterRepository.findAllByMenuNameAndMenuType(CHECKER_MENU_NAME, "Submenu"));
        return menus;
    }

    private void collect(Map<String, ReconUser> byEmail, ReconUser user, String submitterUsername) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) return;
        if (submitterUsername != null && submitterUsername.equals(user.getUsername())) return;
        if (!CommonConstants.ACTIVE.equals(user.getStatus())) return;
        byEmail.putIfAbsent(user.getEmail().toLowerCase(), user);
    }
}
