package com.jpb.reconciliation.reconciliation.service.authenticate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jpb.reconciliation.reconciliation.dto.auth.AppAuthRequest;
import com.jpb.reconciliation.reconciliation.dto.auth.Application;
import com.jpb.reconciliation.reconciliation.dto.auth.AuthenticateData;
import com.jpb.reconciliation.reconciliation.dto.auth.Mobile;
import com.jpb.reconciliation.reconciliation.dto.auth.Secure;
import com.jpb.reconciliation.reconciliation.entity.User;
import com.jpb.reconciliation.reconciliation.util.CryptoUtil;

@Service
public class AppAuthenticateServiceImpl implements AppAuthenticateService {

	@Value("${AUTH.CLIENT_ID}")
	private String clientId;

	@Value("${AUTH.SECRET_CODE}")
	private String secretCode;

	@Value("${AUTH.TOKEN_API_URL}")
	private String tokenApiUrl;

	RestTemplate restTemplate = new RestTemplate();

	Logger logger = LoggerFactory.getLogger(AppAuthenticateServiceImpl.class);

	@Override
	public ResponseEntity<?> doAppAuthenticate() {

		try {
			String key = CryptoUtil.generateAesKey();
			String encryptedSecretCode = CryptoUtil.encryptAES(secretCode, key, "AES/ECB/PKCS5PADDING");

			String encryptedAesKey = "";
			encryptedAesKey = CryptoUtil.encryptRSA(key);

			AppAuthRequest appAuthRequest = new AppAuthRequest();
			Application application = new Application();
			application.setClientId(clientId);

			Mobile mobile = new Mobile();
			mobile.setMobileNumber("9822394898");
			mobile.setCountryCode("91");
			application.setMobile(mobile);

			application.setEmailAddress("akshay.ramani@ext.jiobank.in");
			appAuthRequest.setApplication(application);
			appAuthRequest.setScope("SESSION");

			List<AuthenticateData> authenticateList = new ArrayList<>();
			AuthenticateData authenticateData = new AuthenticateData();
			authenticateData.setMode("20");
			authenticateData.setValue(encryptedSecretCode);
			authenticateList.add(authenticateData);
			appAuthRequest.setAuthenticateList(authenticateList);

			Secure secure = new Secure();
			secure.setEncryptionKey(encryptedAesKey);
			appAuthRequest.setSecure(secure);

			appAuthRequest.setPurpose(1);
			appAuthRequest.setExtraInfo("98");

			UUID uuid = UUID.randomUUID();
			// Create headers
			HttpHeaders headers = new HttpHeaders();
			headers.set("x-trace-id", uuid.toString());
			headers.set("Content-Type", ContentType.APPLICATION_JSON.toString());
			logger.info("API REQUEST HEADERS: x-trace-id:" + uuid.toString() + " Content-Type:"
					+ ContentType.APPLICATION_JSON.toString());
			logger.info("TOKEN API ENDPOINT:" + tokenApiUrl);
			logger.info("APP AUTHENTICATION REQUEST  :::::::::::" + appAuthRequest);
			
			 HttpEntity<AppAuthRequest> requestEntity = new HttpEntity<>(appAuthRequest, headers);
			 logger.info("requestEntity APP AUTHENTICATION REQUEST  :::::::::::" + requestEntity.toString());
			 ResponseEntity<?> response = restTemplate.postForEntity(tokenApiUrl, requestEntity, Object.class);
			 logger.info("response APP AUTHENTICATION REQUEST  :::::::::::" + response.getBody());
		} catch (Exception e) {
		}
		return null;
	}

}
