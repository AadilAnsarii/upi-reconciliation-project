package com.jpb.reconciliation.reconciliation.constants.v2;

/**
 * Centralised string constants for SFTP server operations.
 * Use these instead of inline string literals throughout service and controller layers.
 */
public final class SftpConstants {

    private SftpConstants() {
        // utility class — no instances
    }

    // ── Auth types ────────────────────────────────────────────────────────────
    public static final String AUTH_PASSWORD = "PASSWORD";
    public static final String AUTH_SSH_KEY  = "SSH_KEY";

    // ── Active / inactive flags ───────────────────────────────────────────────
    public static final String ACTIVE   = "Y";
    public static final String INACTIVE = "N";

    // ── Default audit actor ───────────────────────────────────────────────────
    public static final String DEFAULT_ACTOR = "SYSTEM";
}
