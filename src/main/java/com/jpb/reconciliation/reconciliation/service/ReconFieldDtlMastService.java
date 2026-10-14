package com.jpb.reconciliation.reconciliation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.repository.ReconFieldDetailsMasterRepository;

@Service
public class ReconFieldDtlMastService {

    @Autowired
    private ReconFieldDetailsMasterRepository reconFieldDetailsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteFieldsByTemplateId(Long templateId) {
        reconFieldDetailsRepository.deleteByTemplateId(templateId);
    }
}