package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestWithMapStatusList {

	private String status;
	private String statusMsg;
	private Map<String, List<Map<String, Object>>> data;
}
