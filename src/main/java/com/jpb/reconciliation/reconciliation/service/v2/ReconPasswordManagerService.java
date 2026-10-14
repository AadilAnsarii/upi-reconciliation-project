package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

public interface ReconPasswordManagerService {

    ResponseEntity<RestWithStatusList> setPassword(Long userId, String newPassword, String updatedBy);

    ResponseEntity<RestWithStatusList> changePassword(Long userId, String oldPassword, String newPassword, String updatedBy);

    ResponseEntity<RestWithStatusList> generateResetToken(Long userId, String createdBy);

    ResponseEntity<RestWithStatusList> resetPasswordByToken(String token, String newPassword);

    ResponseEntity<RestWithStatusList> getByUserId(Long userId);
}
