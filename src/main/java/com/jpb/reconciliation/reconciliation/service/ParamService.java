package com.jpb.reconciliation.reconciliation.service;

import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.ParamDTO;


public interface ParamService {

    List<ParamDTO> getByParamName(String paramName);
}