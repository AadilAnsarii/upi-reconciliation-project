package com.jpb.reconciliation.reconciliation.constants.v2;

/**
 * Centralised string constants for reconciliation template operations.
 * Use these instead of inline string literals throughout service and controller layers.
 */
public final class TemplateConstants {

    private TemplateConstants() {
        // utility class — no instances
    }

    // ── Template actions ──────────────────────────────────────────────────────
    public static final String ACTION_DRAFT = "DRAFT";
    public static final String ACTION_SAVE  = "SAVE";

    // ── Template status ───────────────────────────────────────────────────────
    public static final String STATUS_DRAFT    = "DRAFT";
    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    // ── Delivery modes ────────────────────────────────────────────────────────
    public static final String DELIVER_MODE_SFTP   = "SFTP";
    public static final String DELIVER_MODE_MANUAL = "MANUAL";

    // ── Template types ────────────────────────────────────────────────────────
    public static final String TYPE_CSV          = "CSV";
    public static final String TYPE_FIXED_WIDTH  = "FIXED WIDTH";
    public static final String TYPE_FIXED_WIDTH2 = "FIXED_WIDTH";   // underscore variant
    public static final String TYPE_XML          = "XML";
    public static final String TYPE_EXCEL        = "EXCEL";

    // ── Boolean / indicator flags ─────────────────────────────────────────────
    public static final String FLAG_YES = "Y";
    public static final String FLAG_NO  = "N";

    // ── Default audit actor ───────────────────────────────────────────────────
    public static final String DEFAULT_ACTOR = "SYSTEM";
}
