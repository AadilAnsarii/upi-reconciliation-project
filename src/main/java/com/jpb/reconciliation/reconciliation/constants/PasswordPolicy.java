package com.jpb.reconciliation.reconciliation.constants;

/**
 * Server-side password strength policy. The frontend enforces the same rules for UX, but the
 * backend must enforce them too so a bypassed/scripted client cannot set a weak password.
 *
 * Rule: at least 8 characters, with at least one uppercase letter, one digit and one special
 * character.
 */
public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static final int MIN_LENGTH = 8;

    /** Human-readable requirement, reused verbatim in every rejection message. */
    public static final String REQUIREMENT =
            "Password must be at least 8 characters and include an uppercase letter, a number and a special character.";

    /** True only when the password satisfies every rule above. Null/blank fails. */
    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) return false;
        boolean upper = false, digit = false, special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isDigit(c)) digit = true;
            else if (!Character.isLetterOrDigit(c)) special = true;
        }
        return upper && digit && special;
    }
}
