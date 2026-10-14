package com.jpb.reconciliation.reconciliation.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The permanent identity codes of the bootstrap menus (Dashboard / My Organization /
 * Administration) that every institution receives at onboarding.
 *
 * SYSTEM_MENU_CODE is a menu's permanent identity; MENU_NAME is only a display label and may be
 * renamed, localised or re-branded at any time. Codes are therefore explicit and hand-defined —
 * never generated from the name. This table is the single place the bootstrap seeders resolve a
 * code from, and it mirrors sql/menu_system_code_migration.sql, which back-filled the same codes
 * onto the rows that already existed.
 *
 * Feature menus are NOT listed here: they live in the RECON_MENU_MASTER catalog (BANK_ID NULL) and
 * their appended rows inherit the code straight off the catalog row.
 */
public final class SystemMenuCodes {

    private static final Map<String, String> BY_NAME;

    static {
        Map<String, String> m = new HashMap<>();
        // Masters
        m.put("My Organization", "ORG");
        m.put("Administration", "ADM");
        m.put("Dashboard", "DASH");
        // System fallback menu — a hidden, immutable row every business role falls back to when it
        // has no accessible application menu. See sql/default_dashboard_system_menu.sql.
        m.put("Default Dashboard", "DEFAULT_DASHBOARD");
        // My Organization
        m.put("Overview", "ORG_OVERVIEW");
        m.put("My Hierarchy", "ORG_HIERARCHY");
        m.put("Admin Status", "ORG_ADMIN_STATUS");
        m.put("Branch Admin Status", "ORG_BRANCH_ADMIN_STATUS");
        m.put("Banks & Branches", "ORG_BANKS_BRANCHES");
        m.put("Bank Onboarding", "ORG_BANK_ONBOARDING");
        m.put("Branch Onboarding", "ORG_BRANCH_ONBOARDING");
        m.put("Branches", "ORG_BRANCHES");
        m.put("User Status", "ORG_USER_STATUS");
        m.put("My Queue", "ORG_MY_QUEUE");
        // Administration
        m.put("Add Role", "ADM_ADD_ROLE");
        m.put("Add User", "ADM_ADD_USER");
        m.put("Add Menu", "ADM_ADD_MENU");
        m.put("Role List", "ADM_ROLE_LIST");
        m.put("User List", "ADM_USER_LIST");
        m.put("Menu List", "ADM_MENU_LIST");
        m.put("User Management", "ADM_USER_MGMT");
        m.put("Handover & Delegation History", "ADM_HANDOVER_HISTORY");
        m.put("Approval Request History", "ADM_APPROVAL_HISTORY");
        m.put("Maker Dashboard", "ADM_MAKER_DASH");
        m.put("Checker Dashboard", "ADM_CHECKER_DASH");
        BY_NAME = Collections.unmodifiableMap(m);
    }

    private SystemMenuCodes() { }

    /** The code for a bootstrap menu, or null when the name isn't one of them. */
    public static String of(String menuName) {
        return menuName == null ? null : BY_NAME.get(menuName.trim());
    }

    private static final Set<String> ALL_CODES = Collections.unmodifiableSet(new HashSet<>(BY_NAME.values()));

    /** True when this SYSTEM_MENU_CODE identifies a bootstrap admin menu (Dashboard / My
     *  Organization / Administration and everything under them) rather than a feature-catalog one.
     *  Keyed on the code (the permanent identity), not the — renamable — display name. */
    public static boolean isBootstrapCode(String systemMenuCode) {
        return systemMenuCode != null && ALL_CODES.contains(systemMenuCode);
    }
}
