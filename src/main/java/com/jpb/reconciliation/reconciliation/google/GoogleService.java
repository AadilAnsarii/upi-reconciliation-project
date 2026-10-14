package com.jpb.reconciliation.reconciliation.google;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface GoogleService {

	ResponseEntity<RestWithStatusList> authenticateWithGoogle(GoogleRequest googleRequest, HttpServletResponse response);

}
