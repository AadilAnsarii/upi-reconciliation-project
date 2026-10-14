package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.KalUserDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

public interface KalAdminAuthService {

    ResponseEntity<RestWithStatusList> createKalAdmin(KalUserDto dto);
}
