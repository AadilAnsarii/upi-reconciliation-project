package com.jpb.reconciliation.reconciliation.service.authenticate;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface AppAuthenticateService {

	ResponseEntity<?> doAppAuthenticate();

}
