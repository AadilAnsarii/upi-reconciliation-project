package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.service.authenticate.AppAuthenticateService;

@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

    private final ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	public AuthenticationController(AppAuthenticateService appAuthenticateService, ReconBatchProcessEntityRepository reconBatchProcessEntityRepository) {
		this.appAuthenticateService = appAuthenticateService;
		this.reconBatchProcessEntityRepository = reconBatchProcessEntityRepository;
	}

	@Autowired
	AppAuthenticateService appAuthenticateService;

//	@PostMapping("/google-authenticate")
//	ResponseEntity<RestWithStatusList> googleAuthentication(@RequestBody GoogleAuth authRequest) {
//		
//	}
	
	@GetMapping(value = "/app" , produces = CommonConstants.APPLICATION_JSON)
	ResponseEntity<?> appAuthenticate() {
		return appAuthenticateService.doAppAuthenticate();
	}

}
