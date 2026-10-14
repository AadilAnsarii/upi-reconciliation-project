package com.jpb.reconciliation.reconciliation.mapper;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ProcessFileDetailsMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ProcessMasterDto;
import com.jpb.reconciliation.reconciliation.dto.ReconProcessMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ProcessMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;

public class ProcessMasterMapper {

	Logger logger = LoggerFactory.getLogger(ProcessMasterMapper.class);

	public static List<ProcessMasterDto> mapProcessMasterToProcessMasterDto(List<ProcessMasterEntity> getProcessData,
			String existsMenuFlag) {
		List<ProcessMasterDto> reportFileList = new ArrayList<>();
		for (ProcessMasterEntity process : getProcessData) {
			ProcessMasterDto processMasterDto = new ProcessMasterDto();
			processMasterDto.setLongName(process.getLongName());
			processMasterDto.setProcessMastId(process.getProcessMastId());
			processMasterDto.setProcessType(process.getProcessType());
			processMasterDto.setShrtName(process.getShrtName());

			List<ProcessFileDetailsMasterDto> fileDetails = mapFileDto(process.getFileDetailsMasters(), existsMenuFlag);
			List<ReconProcessMasterDto> processDefDetails = mapProcessDefDto(process.getProcessDefMaster());
			processMasterDto.setFileList(fileDetails);
			processMasterDto.setProcessList(processDefDetails);
			reportFileList.add(processMasterDto);
		}

		return reportFileList;
	}

	private static List<ReconProcessMasterDto> mapProcessDefDto(List<ReconProcessDefMaster> processDefMaster) {
		List<ReconProcessMasterDto> processDefMasterList = new ArrayList<>();
		for (ReconProcessDefMaster processDef : processDefMaster) {
			ReconProcessMasterDto processDefDto = new ReconProcessMasterDto();
			processDefDto.setReconProcessId(processDef.getReconProcessId());
			processDefDto.setReconProcessName(processDef.getReconProcessName());
			processDefMasterList.add(processDefDto);
		}
		return processDefMasterList;
	}

	private static List<ProcessFileDetailsMasterDto> mapFileDto(List<ReconFileDetailsMaster> fileDetailsMasters,
			String existsMenuFlag) {
		List<ProcessFileDetailsMasterDto> fileList = new ArrayList<>();
		for (ReconFileDetailsMaster file : fileDetailsMasters) {
			if (existsMenuFlag.equalsIgnoreCase(CommonConstants.INACTIVE_N)) {
				if (file.getReconExitMenuFlag().equalsIgnoreCase(existsMenuFlag)) {
					ProcessFileDetailsMasterDto fileDetailsDto = new ProcessFileDetailsMasterDto();
					fileDetailsDto.setReconFileId(file.getReconFileId());
					fileDetailsDto.setReconFileName(file.getReconFileName());
					fileDetailsDto.setReconFileLocation(file.getReconFileLocation());
					fileList.add(fileDetailsDto);
				}
			} else if(existsMenuFlag.equalsIgnoreCase(CommonConstants.ACTIVE_Y)) {
				if (file.getReconExitMenuFlag().equalsIgnoreCase(existsMenuFlag)) {
					ProcessFileDetailsMasterDto fileDetailsDto = new ProcessFileDetailsMasterDto();
					fileDetailsDto.setReconFileId(file.getReconFileId());
					fileDetailsDto.setReconFileName(file.getReconFileName());
					fileDetailsDto.setReconFileLocation(file.getReconFileLocation());
					fileList.add(fileDetailsDto);
				}
			}
		}
		return fileList;
	}

}
