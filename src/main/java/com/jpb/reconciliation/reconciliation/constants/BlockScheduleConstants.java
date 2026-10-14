package com.jpb.reconciliation.reconciliation.constants;

public class BlockScheduleConstants {

    private BlockScheduleConstants() {}

    // ============================================================
    // TESTING VALUE — currently 1 MINUTE so the block warning ->
    // grace window -> undo -> final BLOCKED flow can be verified
    // quickly without waiting a day.
    //
    // TO CONVERT TO PRODUCTION (24-hour grace window), change ONLY
    // this one line:
    //     BLOCK_DELAY_MINUTES = 1440     // 24 hours * 60 minutes
    //
    // No other file needs to change — every consumer (bank block
    // controller, UserStatusServiceImpl, StatusSchedulerService, and
    // the warning emails' wording) reads this constant.
    // ============================================================
    public static final long BLOCK_DELAY_MINUTES = 1; // TEST VALUE — see comment above. PROD: 1440

    /** Human wording for the grace window, used in emails and API messages ("BLOCKED in 24 hours"). */
    public static String delayLabel() {
        if (BLOCK_DELAY_MINUTES % 60 == 0) {
            long hours = BLOCK_DELAY_MINUTES / 60;
            return hours + (hours == 1 ? " hour" : " hours");
        }
        return BLOCK_DELAY_MINUTES + (BLOCK_DELAY_MINUTES == 1 ? " minute" : " minutes");
    }
}
