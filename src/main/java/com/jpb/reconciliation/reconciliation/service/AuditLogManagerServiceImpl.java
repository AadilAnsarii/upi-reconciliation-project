package com.jpb.reconciliation.reconciliation.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.AuditLogManager;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.AuditLogManagerRepository;

@Service
public class AuditLogManagerServiceImpl implements AuditLogManagerService {

    Logger logger = LoggerFactory.getLogger(AuditLogManagerServiceImpl.class);

    @Autowired
    private AuditLogManagerRepository auditLogManagerRepository;

    @Override
    public void extractionAudit(ReconBatchProcessEntity reconProcessManager, ReconUser userData) {
        AuditLogManager audit = new AuditLogManager();
        audit.setModule(reconProcessManager.getProcessType());
        audit.setSubModule("-");
        audit.setEvent(reconProcessManager.getProcessType());
        audit.setEventData("ProcessID:" + reconProcessManager.getProcessId() + "|" + "ProcessName:"
                + reconProcessManager.getProcessType() + "|" + "ProcessType:-" + reconProcessManager.getProcessType());
        audit.setEventStatus(reconProcessManager.getStatus());
        audit.setUserId(reconProcessManager.getInsertUser());
        audit.setUserIp("-");
        audit.setAuditDateTime(new Date());
        audit.setOldData("-");
        audit.setRoleId(userData != null ? userData.getRoleId() : null);
        logger.info("Extraction AUDIT LOG :::::::::" + audit);
        auditLogManagerRepository.save(audit);
    }

    @Override
    public void commonAudit(ReconUser userData, String module, Object data) {
        AuditLogManager audit = new AuditLogManager();
        ReconMenuMaster menu = (ReconMenuMaster) data;
        audit.setModule(module);
        audit.setSubModule("-");
        audit.setEvent(menu.getMenuName());
        audit.setEventData("MENUID:" + menu.getMenuId() + "|" + "MENUNAME:" + menu.getMenuName() + "|" + "MenuType:" + menu.getMenuType());
        audit.setEventStatus("Created");
        audit.setUserId(null);
        audit.setUserIp("-");
        audit.setAuditDateTime(new Date());
        audit.setOldData("-");
        audit.setRoleId(userData != null ? userData.getRoleId() : null);
        auditLogManagerRepository.save(audit);
    }

}
