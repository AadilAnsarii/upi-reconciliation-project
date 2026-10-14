package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.CBankProductMap;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.repository.MenuMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.CBankProductMapRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CBankProductMapServiceImpl implements CBankProductMapService {

    private static final Logger logger = LoggerFactory.getLogger(CBankProductMapServiceImpl.class);

    @Autowired
    private CBankProductMapRepository cBankProductMapRepository;

    @Autowired
    private MenuMasterRepository menuMasterRepository;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> mapBankProduct(CBankProductMap mapping) {
        if (mapping.getBankId() == null || mapping.getProductId() == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Bank ID and Product ID are required.", null));
        }
        if (cBankProductMapRepository.existsByBankIdAndProductId(mapping.getBankId(), mapping.getProductId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Mapping already exists for this bank and product.", null));
        }
        if (mapping.getValidFrom() == null) {
            mapping.setValidFrom(LocalDate.now());
        }
        if (mapping.getStatus() == null) {
            mapping.setStatus(CommonConstants.ACTIVE);
        }
        CBankProductMap saved = cBankProductMapRepository.save(mapping);
        logger.info("CBankProductMap created: bankId={}, productId={}", saved.getBankId(), saved.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList(CommonConstants.SUCCESS, "Bank-product mapping created.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMappingsByBankId(Long bankId) {
        List<CBankProductMap> mappings = cBankProductMapRepository.findByBankId(bankId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Mappings fetched.", mappings));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMappingsByProductId(Long productId) {
        List<CBankProductMap> mappings = cBankProductMapRepository.findByProductId(productId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Mappings fetched.", mappings));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> removeMappingsByBankId(Long bankId) {
        cBankProductMapRepository.deleteByBankId(bankId);
        logger.info("CBankProductMap removed for bankId={}", bankId);
        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Bank product mappings removed.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateMappingStatus(Long id, String status) {
        Optional<CBankProductMap> opt = cBankProductMapRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList(CommonConstants.FAILURE, "Mapping not found with ID: " + id, null));
        }
        CBankProductMap existing = opt.get();
        existing.setStatus(status);
        cBankProductMapRepository.save(existing);

        // B8 — a product's menus may not outlive the product. When the subscription stops being
        // ACTIVE, every menu mapping this bank holds for it is inactivated along with the role
        // grants pointing at those rows, so no orphan access survives. Re-activating the product
        // brings the same rows back (the grants were left in place on purpose — see below).
        cascadeMenuMappings(existing.getBankId(), existing.getProductId(), CommonConstants.ACTIVE.equalsIgnoreCase(status));

        return ResponseEntity.ok(new RestWithStatusList(CommonConstants.SUCCESS, "Mapping status updated.", null));
    }

    /**
     * Flips this bank's menu mappings for one product between live ('Y') and inactive ('N').
     *
     * Only the appended, bank-owned rows are touched — the catalog is never modified. The role
     * grants in C_ROLE_MENU_MAP are deliberately left alone: the sidebar already refuses to serve
     * an inactive menu (getMenusByRolePrivileges filters on status 'Y' and on the bank's active
     * product scope), so reactivating the product restores exactly the access that existed before
     * without an admin having to re-assign every privilege.
     */
    private void cascadeMenuMappings(Long bankId, Long productId, boolean activate) {
        if (bankId == null || productId == null) return;
        List<ReconMenuMaster> mappings = menuMasterRepository.findByBankIdAndProductId(bankId, productId);
        for (ReconMenuMaster m : mappings) {
            // Never resurrect something a Checker rejected, and never touch a draft/pending row.
            String current = m.getStatus();
            if (activate) {
                if (!CommonConstants.INACTIVE_N.equals(current)) continue;
                m.setStatus(CommonConstants.ACTIVE_Y);
            } else {
                if (!CommonConstants.ACTIVE_Y.equals(current)) continue;
                m.setStatus(CommonConstants.INACTIVE_N);
            }
            menuMasterRepository.save(m);
        }
        logger.info("Product {} for bank {} {} — {} menu mapping(s) cascaded.",
                productId, bankId, activate ? "reactivated" : "deactivated", mappings.size());
    }
}



