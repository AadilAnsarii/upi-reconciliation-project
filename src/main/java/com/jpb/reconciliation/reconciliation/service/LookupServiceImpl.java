//package com.jpb.reconciliation.reconciliation.service;
//
//import java.util.Date;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
//import com.jpb.reconciliation.reconciliation.dto.LookupDTO;
//import com.jpb.reconciliation.reconciliation.entity.Lookup;
//import com.jpb.reconciliation.reconciliation.exception.DuplicateResourceException;
//import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
//import com.jpb.reconciliation.reconciliation.mapper.LookupMapper;
//import com.jpb.reconciliation.reconciliation.repository.LookupRepo;
//
//
//
//@Service
//@Transactional
//public class LookupServiceImpl implements LookupService {
//
//    private final LookupRepo lrepo;
//    private final LookupMapper mapper;
//
//    public LookupServiceImpl(LookupRepo lrepo, LookupMapper mapper) {
//        this.lrepo = lrepo;
//        this.mapper = mapper;
//    }
//
//    // ─── CREATE ───────────────────────────────────────────────────────
//    @Override
//    public LookupDTO createLookup(LookupDTO dto) {
//
//        boolean codeExists = !lrepo
//                .findByLookupCodeAndActiveYn(dto.getLookupCode(), CommonConstants.ACTIVE)
//                .isEmpty();
//        if (codeExists) {
//            throw new DuplicateResourceException(
//                "Lookup with code '" + dto.getLookupCode() + "' already exists");
//        }
//
//        Lookup lookup = mapper.toEntity(dto);
//        lookup.setActiveYn(CommonConstants.ACTIVE);
//        lookup.setCreatedOn(new Date());
//
//        return mapper.toDTO(lrepo.save(lookup));
//    }
//
//    // ─── GET BY ID ────────────────────────────────────────────────────
//    @Override
//    @Transactional(readOnly = true)
//    public LookupDTO getLookupById(long lookupId) {
//        return mapper.toDTO(
//            lrepo.findById(lookupId).orElseThrow(() ->
//                new ResourceNotFoundException("Lookup not found with ID: " + lookupId))
//        );
//    }
//
//    // ─── GET ALL ──────────────────────────────────────────────────────
//    @Override
//    @Transactional(readOnly = true)
//    public List<LookupDTO> getAllLookup() {
//        return lrepo.findByActiveYn(CommonConstants.ACTIVE)
//                .stream()
//                .map(mapper::toDTO)
//                .collect(Collectors.toList());
//    }
//
//    // ─── GET BY NAME ──────────────────────────────────────────────────
//    @Override
//    @Transactional(readOnly = true)
//    public List<LookupDTO> getByNameAndActive(String name) {
//        List<Lookup> list = lrepo.findByLookupNameAndActiveYn(name, CommonConstants.ACTIVE);
//        if (list.isEmpty()) {
//            throw new ResourceNotFoundException(
//                "No lookups found with name: " + name);
//        }
//        return list.stream().map(mapper::toDTO).collect(Collectors.toList());
//    }
//
//    // ─── DELETE (soft delete) ─────────────────────────────────────────
//    @Override
//    public void deleteLookup(long lookupId) {
//        Lookup lookup = lrepo.findById(lookupId).orElseThrow(() ->
//                new ResourceNotFoundException("Lookup not found with ID: " + lookupId));
//        lookup.setActiveYn(CommonConstants.INACTIVE);
//        lookup.setUpdatedOn(new Date());
//        lrepo.save(lookup);
//    }
//
//    // ─── GROUPED DATA ─────────────────────────────────────────────────
//    @Override
//    @Transactional(readOnly = true)
//    public Map<String, List<LookupDTO>> getGroupedLookups() {
//
//        List<String> distinctNames = lrepo.findDistinctLookupNames();
//
//        Map<String, List<LookupDTO>> grouped = new LinkedHashMap<>();
//
//        for (String name : distinctNames) {
//            List<LookupDTO> records = lrepo
//                    .findByLookupNameAndActiveYn(name, CommonConstants.ACTIVE)
//                    .stream()
//                    .map(mapper::toDTO)
//                    .collect(Collectors.toList());
//
//            grouped.put(name, records);
//        }
//
//        return grouped;
//    }
//}