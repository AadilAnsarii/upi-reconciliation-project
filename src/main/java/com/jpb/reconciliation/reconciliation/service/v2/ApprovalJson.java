package com.jpb.reconciliation.reconciliation.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

/**
 * Tiny JSON helper for the maker-checker UPDATE flow — serialises a maker's proposed field
 * changes into RECON_APPROVAL_REQUEST.PROPOSED_CHANGES and reads them back when a Checker
 * approves. Failures are swallowed to a safe empty result so an audit concern never breaks the
 * business call.
 */
public final class ApprovalJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApprovalJson() { }

    public static String write(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(changes);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> read(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyMap();
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
