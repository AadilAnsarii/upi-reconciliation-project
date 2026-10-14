package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

public interface UserStatusService {

    ResponseEntity<RestWithStatusList> scheduleInactivate(Long userId, String scheduledBy);

    ResponseEntity<RestWithStatusList> undoInactivate(Long userId, String undoneBy);

    ResponseEntity<RestWithStatusList> scheduleReactivate(Long userId, String scheduledBy);

    ResponseEntity<RestWithStatusList> undoReactivate(Long userId, String undoneBy);

    ResponseEntity<RestWithStatusList> scheduleBlock(Long userId, String reason, String scheduledBy);

    ResponseEntity<RestWithStatusList> undoBlock(Long userId, String undoneBy);

    ResponseEntity<RestWithStatusList> blockImmediate(Long userId, String reason, String blockedBy);

    ResponseEntity<RestWithStatusList> unblock(Long userId, String unblockedBy);
}
