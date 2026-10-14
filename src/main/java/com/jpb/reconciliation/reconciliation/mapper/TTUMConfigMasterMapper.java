package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.TTUMReportDto;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.TTUMConfigMasterEntity;

public class TTUMConfigMasterMapper {

	public static TTUMReportDto mapToTTUMReportDto(ReconProcessDefMaster processFile, TTUMConfigMasterEntity configMast,
			TTUMReportDto ttumReportDto) {
	    if (processFile != null) {
	        ttumReportDto.setReconProcessName(processFile.getReconProcessName());
	    }
	
	    if (configMast != null) {
	        ttumReportDto.setTtumConfigId(configMast.getTtumConfigId());
	        ttumReportDto.setTtumDescription(configMast.getTtumDescription());
	        ttumReportDto.setTtumEntityId(configMast.getTtumEntityId());
	        ttumReportDto.setTtumTypeDescription(configMast.getTtumTypeDescription());
	        ttumReportDto.setTtumType(configMast.getTtumType());
	        ttumReportDto.setJrxmlId(configMast.getJrxmlId());
	        ttumReportDto.setOutputFormat(configMast.getOutputFormat());
	        ttumReportDto.setInsertCode(configMast.getInsertCode());
	        ttumReportDto.setTtumCatType(configMast.getTtumCatType());
	        ttumReportDto.setIsCBSTTUM(configMast.getIsCBSTTUM());
	        ttumReportDto.setTtumProcessId(configMast.getTtumProcessId());
	    }
		return ttumReportDto;
	}

}
