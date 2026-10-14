package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.Map;

import com.jpb.reconciliation.reconciliation.dto.LookupDTO;



public interface LookupService {

    LookupDTO createLookup(LookupDTO dto);

    LookupDTO getLookupById(long lookupId);

    List<LookupDTO> getAllLookup();

    List<LookupDTO> getByNameAndActive(String name);

    void deleteLookup(long lookupId);

    // Grouped API — LOOKUP_NAME se group karke data return karta hai
    // e.g. { "TEMPLATE_TYPE": [...], "FILE_ENCODING": [...] }
    Map<String, List<LookupDTO>> getGroupedLookups();
}