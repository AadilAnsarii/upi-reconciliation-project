//package com.jpb.reconciliation.reconciliation.service;
//
//import java.util.Collections;
//import java.util.List;
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
//import com.jpb.reconciliation.reconciliation.dto.ParamDTO;
//import com.jpb.reconciliation.reconciliation.entity.Param;
//import com.jpb.reconciliation.reconciliation.mapper.ParamMapper;
//import com.jpb.reconciliation.reconciliation.repository.ParamRepo;
//
//
//
//@Service
//@Transactional
//public class ParamServiceImpl implements ParamService {
//
//    private final ParamRepo prepo;
//    private final ParamMapper mapper;
//
//    public ParamServiceImpl(ParamRepo prepo, ParamMapper mapper) {
//        this.prepo = prepo;
//        this.mapper = mapper;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ParamDTO> getByParamName(String paramName) {
//
//        Param param = prepo.findByParamNameAndActiveYn(paramName, CommonConstants.ACTIVE);
//
//        if (param == null) {
//            return Collections.emptyList();
//        }
//
//        return Collections.singletonList(mapper.toDTO(param));
//    }
//}