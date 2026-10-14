package com.jpb.reconciliation.reconciliation.constants;

public class ProductExpiryConstants {

    private ProductExpiryConstants() {}

    // ============================================================
    // TESTING VALUE — currently 5 MINUTES so the expiry -> grace ->
    // permanent-inactive flow can be verified quickly without waiting.
    //
    // TO CONVERT TO PRODUCTION (7-day grace period), change ONLY this
    // one line:
    //     GRACE_PERIOD_MINUTES = 10080     // 7 days * 24 hours * 60 minutes
    //
    // No other file needs to change — every consumer (ProductExpiryScheduler)
    // reads this constant.
    // ============================================================
    public static final long GRACE_PERIOD_MINUTES = 5; // TEST VALUE — see comment above. PROD: 10080

    // Statuses used by the expiry/grace flow
    public static final String CAPABILITY_STATUS_SUSPENDED = "SUSPENDED";
    public static final String USER_STATUS_PRODUCT_EXPIRY_HOLD = "PRODUCT_EXPIRY_HOLD";
    public static final String REVOKE_REASON_PRODUCT_DEACTIVATED = "PRODUCT_DEACTIVATED";
}
