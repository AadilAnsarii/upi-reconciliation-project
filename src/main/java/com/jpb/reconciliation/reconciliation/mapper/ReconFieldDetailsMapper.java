package com.jpb.reconciliation.reconciliation.mapper;

import java.util.Date;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.FieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;

public class ReconFieldDetailsMapper {

	public static ReconFieldDetailsMaster mapFieldDtoToEntity(FieldConfigurationDto dto, ReconTemplateDetails template,
			ReconFieldTypeMaster fieldType, ReconFieldFormatMaster fieldFormat) {

		ReconFieldDetailsMaster entity = new ReconFieldDetailsMaster();

		entity.setReconColumnPosn(dto.getColumnPosition());
		entity.setReconShortName(dto.getFieldName());
		entity.setReconFromPosn(String.valueOf(dto.getFromPosition()));
		entity.setReconToPosn(String.valueOf(dto.getToPosition()));
		entity.setReconMaxLength(dto.getFieldLength());
		entity.setReconKeyIdentifier(CommonConstants.ACTIVE_Y.equalsIgnoreCase(dto.getKeyIdentity()) ? 1L : 0L);
		entity.setReconColumnOffset(dto.getColumnOffset());
		entity.setReconMandatoryFlag(dto.getQualifier());

		entity.setReconTemplateDetails(template);
		entity.setReconFieldTypeMaster(fieldType);
		entity.setReconFieldFormatMaster(fieldFormat);

		entity.setReconInsertDate(new Date());
		entity.setReconInsertUser(1L);
		entity.setReconInstanceCode(1L);
		entity.setReconSubTempId(1L);

		return entity;
	}

}
