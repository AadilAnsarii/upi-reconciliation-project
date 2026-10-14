package com.jpb.reconciliation.reconciliation.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;


/**
 * Centralises construction of RestWithMapStatusList and RestWithStatusList.
 *
 * ── RestWithMapStatusList  (data shape: Map<String, List<Map<String,Object>>>)
 *    Used by V2 controllers — structured, multi-key data map.
 *    Methods: ok(...), okPaged(...), failure(...), error(...), validationFail(...)
 *
 * ── RestWithStatusList     (data shape: List<Object>)
 *    Used by V1 / simple controllers — flat list of objects.
 *    Methods: okList(...), okSingle(...), okEmpty(...),
 *             failureList(...), errorList(...)
 *
 * Paginated APIs (RestWithMapStatusList) always return TWO keys:
 *   "templates"  (or "fileConfigs" etc.) → the data rows
 *   "pagination"                         → one-element list with page metadata
 *
 * Example response body for viewTemplate (RestWithMapStatusList):
 * {
 *   "status":    CommonConstants.SUCCESS,
 *   "statusMsg": "Templates retrieved successfully.",
 *   "data": {
 *     "templates":  [ { "templateId": 1, "templateName": "CBS_TXN", ... } ],
 *     "pagination": [ {
 *       "currentPage":   0,
 *       "pageSize":      10,
 *       "totalElements": 42,
 *       "totalPages":    5,
 *       "isFirst":       true,
 *       "isLast":        false,
 *       "hasNext":       true,
 *       "hasPrevious":   false
 *     }]
 *   }
 * }
 *
 * Example response body for simple save (RestWithStatusList):
 * {
 *   "status":    CommonConstants.SUCCESS,
 *   "statusMsg": "Schedule config saved successfully.",
 *   "data": [ { "scheduleId": 1, "templateId": 101, ... } ]
 * }
 */
public final class ResponseBuilder {

    private ResponseBuilder() {}

    public static final String STATUS_SUCCESS = CommonConstants.SUCCESS;
    public static final String STATUS_FAILURE = CommonConstants.FAILURE;
    public static final String STATUS_ERROR   = "ERROR";

    // =========================================================================
    // RestWithMapStatusList — structured Map-based responses (V2 controllers)
    // =========================================================================

    // ── Simple success ─────────────────────────────────────────────────────

    /** Single data key, non-empty list of rows */
    public static RestWithMapStatusList ok(String msg,
                                           String key,
                                           List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put(key, rows != null ? rows : Collections.emptyList());
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, data);
    }

    /** Pre-built multi-key data map */
    public static RestWithMapStatusList ok(String msg,
                                           Map<String, List<Map<String, Object>>> data) {
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, data);
    }

    /** No data — confirmations (delete, activate, etc.) */
    public static RestWithMapStatusList ok(String msg) {
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, Collections.emptyMap());
    }

    // ── Paginated success ──────────────────────────────────────────────────

    /**
     * Builds a response that ALWAYS contains both the data key and the
     * "pagination" key — even when the page is empty.
     *
     * This means the frontend can always read data["pagination"][0].totalElements
     * without null-checking, regardless of whether any rows were returned.
     *
     * @param msg         status message
     * @param dataKey     e.g. "templates", "fileConfigs"
     * @param rows        converted DTO rows (may be empty list)
     * @param page        Spring Page object (provides all metadata)
     */
    public static RestWithMapStatusList okPaged(String msg,
                                                String dataKey,
                                                List<Map<String, Object>> rows,
                                                Page<?> page) {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put(dataKey,      rows != null ? rows : Collections.emptyList());
     //   data.put("pagination", List.of(buildPaginationMap(page)));
        data.put("pagination", Collections.singletonList(buildPaginationMap(page)));
        return new RestWithMapStatusList(STATUS_SUCCESS, msg, data);
    }

    // ── Failure / Error ────────────────────────────────────────────────────

    public static RestWithMapStatusList failure(String msg) {
        return new RestWithMapStatusList(STATUS_FAILURE, msg, Collections.emptyMap());
    }

    public static RestWithMapStatusList error(String msg) {
        return new RestWithMapStatusList(STATUS_ERROR, msg, Collections.emptyMap());
    }

    /** Validation errors packed under key "errors" */
    public static RestWithMapStatusList validationFail(List<String> errors) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String e : errors) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("error", e);
            rows.add(row);
        }
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put("errors", rows);
        return new RestWithMapStatusList(STATUS_FAILURE, "Validation failed", data);
    }

    // =========================================================================
    // RestWithStatusList — flat List<Object> responses (V1 / simple controllers)
    // =========================================================================

    /**
     * Success response with a list of objects.
     * Use when returning multiple records e.g. a list of DTOs.
     *
     * Example:
     *   return ResponseBuilder.okList("Records fetched.", records);
     *
     * Produces:
     *   { "status": CommonConstants.SUCCESS, "statusMsg": "Records fetched.", "data": [ {...}, {...} ] }
     *
     * @param msg   status message
     * @param items list of response objects (DTOs, Maps, etc.) — null-safe, treated as empty
     */
    public static RestWithStatusList okList(String msg, List<?> items) {
        List<Object> data = new ArrayList<>();
        if (items != null) {
            data.addAll(items);
        }
        return new RestWithStatusList(STATUS_SUCCESS, msg, data);
    }

    /**
     * Success response wrapping a single object.
     * Use when returning one saved / fetched record.
     *
     * Example:
     *   return ResponseBuilder.okSingle("Schedule config saved.", response);
     *
     * Produces:
     *   { "status": CommonConstants.SUCCESS, "statusMsg": "Schedule config saved.", "data": [ {...} ] }
     *
     * @param msg  status message
     * @param item the single response object — if null, data will be an empty list
     */
    public static RestWithStatusList okSingle(String msg, Object item) {
        List<Object> data = new ArrayList<>();
        if (item != null) {
            data.add(item);
        }
        return new RestWithStatusList(STATUS_SUCCESS, msg, data);
    }

    /**
     * Success response with no data payload.
     * Use for confirmations — delete, activate, deactivate, etc.
     *
     * Example:
     *   return ResponseBuilder.okEmpty("Schedule config deleted.");
     *
     * Produces:
     *   { "status": CommonConstants.SUCCESS, "statusMsg": "Schedule config deleted.", "data": [] }
     *
     * @param msg status message
     */
    public static RestWithStatusList okEmpty(String msg) {
        return new RestWithStatusList(STATUS_SUCCESS, msg, Collections.emptyList());
    }

    /**
     * Failure response with no data payload (RestWithStatusList).
     * Use when a business rule check fails in a simple / V1 controller.
     *
     * Example:
     *   return ResponseBuilder.failureList("Template not found.");
     *
     * Produces:
     *   { "status": CommonConstants.FAILURE, "statusMsg": "Template not found.", "data": [] }
     *
     * @param msg failure message
     */
    public static RestWithStatusList failureList(String msg) {
        return new RestWithStatusList(STATUS_FAILURE, msg, Collections.emptyList());
    }

    /**
     * Error response with no data payload (RestWithStatusList).
     * Use when an unexpected exception is caught in a simple / V1 controller.
     *
     * Example:
     *   return ResponseBuilder.errorList("Unexpected error occurred.");
     *
     * Produces:
     *   { "status": "ERROR", "statusMsg": "Unexpected error occurred.", "data": [] }
     *
     * @param msg error message
     */
    public static RestWithStatusList errorList(String msg) {
        return new RestWithStatusList(STATUS_ERROR, msg, Collections.emptyList());
    }

    // =========================================================================
    // DTO → Map helpers  (shared by both response types)
    // =========================================================================

    /**
     * Converts any object to Map<String, Object> via Jackson.
     * Use in service layer when converting a single DTO.
     */
    public static Map<String, Object> toMap(Object obj,
                                             com.fasterxml.jackson.databind.ObjectMapper mapper) {
        if (obj == null) return Collections.emptyMap();
        try {
            //noinspection unchecked
            return mapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", obj.toString());
            return m;
        }
    }

    /**
     * Converts a list of objects to List<Map<String, Object>>.
     * Use in service layer when converting a list of DTOs.
     */
    public static List<Map<String, Object>> toMapList(
            List<?> items,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items != null) {
            for (Object item : items) {
                result.add(toMap(item, mapper));
            }
        }
        return result;
    }

    // =========================================================================
    // Pagination map builder  (used by okPaged — RestWithMapStatusList only)
    // =========================================================================

    /**
     * Builds the pagination metadata map from a Spring Page.
     * Single source of truth — used by okPaged() above.
     * No service class should duplicate this logic.
     *
     * Keys returned:
     *   currentPage   – 0-based page number
     *   pageSize      – requested page size
     *   totalElements – total records matching the query
     *   totalPages    – total number of pages
     *   isFirst       – true if this is the first page
     *   isLast        – true if this is the last page
     *   hasNext       – true if a next page exists
     *   hasPrevious   – true if a previous page exists
     */
    public static Map<String, Object> buildPaginationMap(Page<?> p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currentPage",   p.getNumber());
        m.put("pageSize",      p.getSize());
        m.put("totalElements", p.getTotalElements());
        m.put("totalPages",    p.getTotalPages());
        m.put("isFirst",       p.isFirst());
        m.put("isLast",        p.isLast());
        m.put("hasNext",       p.hasNext());
        m.put("hasPrevious",   p.hasPrevious());
        return m;
    }
}