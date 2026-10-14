package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconFieldDetailsDto {
	private Long rfmColPosn;
    private String rfmShortName;
    private String rftFieldTypeDesc;
    private String rffFieldFormatDesc;
    private String rfmColOffset;
    private String keyName;
	private String reconFromPosn;
	private String reconToPosn;
}
