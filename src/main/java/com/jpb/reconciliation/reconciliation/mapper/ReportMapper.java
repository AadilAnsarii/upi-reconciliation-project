package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.ReportDto;
import com.jpb.reconciliation.reconciliation.entity.ReportEntity;

public class ReportMapper {

	public static ReportDto mapToReportEntityToReportDto(ReportEntity reportData, ReportDto reportDto) {
		reportDto.setProcessId(reportData.getProcessId());
		reportDto.setReportFileName(reportData.getReportFileName());
		reportDto.setReportName(reportData.getReportName());
		reportDto.setReportId(reportData.getReportId());
		reportDto.setReportLocation(reportData.getReportLocation());
		reportDto.setReportDate(reportData.getReportDate());
		return reportDto;
	}

}
