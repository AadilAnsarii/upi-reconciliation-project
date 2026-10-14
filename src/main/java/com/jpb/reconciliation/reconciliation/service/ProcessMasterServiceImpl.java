package com.jpb.reconciliation.reconciliation.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ProcessMasterDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ProcessMasterEntity;
import com.jpb.reconciliation.reconciliation.mapper.ProcessMasterMapper;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;

@Service
public class ProcessMasterServiceImpl implements ProcessMasterService {

	@Autowired
	ProcessMasterRepository reportMasterRepository;

	Logger logger = LoggerFactory.getLogger(ProcessMasterServiceImpl.class);

	@Override
	public ResponseEntity<RestWithStatusList> getAllProcessData(String existsMenuFlag) {
		List<ProcessMasterEntity> getProcessData = reportMasterRepository.findAll();
		logger.info("PROCESS MASTER DATA WITH FILE DATA :::::::::::" + getProcessData);
		List<ProcessMasterDto> mapReportFileData = ProcessMasterMapper.mapProcessMasterToProcessMasterDto(getProcessData, existsMenuFlag);
		logger.info("mapReportFileData :::::::::::" + mapReportFileData);
		RestWithStatusList restWithStatusList;
		List<Object> processList = new ArrayList<>();
		if (!mapReportFileData.isEmpty()) {
			for (ProcessMasterDto reportFileData : mapReportFileData) {
				processList.add(reportFileData);
			}
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "PROCESS TYPES NOT FOUND", processList);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "PROCESS TYPES FOUND SUCCESSFULLY", processList);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

}
