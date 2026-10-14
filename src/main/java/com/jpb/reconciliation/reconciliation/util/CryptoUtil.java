package com.jpb.reconciliation.reconciliation.util;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class CryptoUtil {
	
	Logger logger = LoggerFactory.getLogger(CryptoUtil.class);
	
	@Value("${AUTH.PUBLIC_KEY}")
	private String publicKey;

	public static String generateAesKey() {
		KeyGenerator gen = null;
		SecretKey secret = null;
		byte[] binary = null;
		String key = null;

		try {
			gen = KeyGenerator.getInstance("AES");
			secret = gen.generateKey();
			binary = secret.getEncoded();
			key = String.format("%032X", new BigInteger(1, binary));
		} catch (Exception var5) {
		}
		return key;
	}

	public static String encryptAES(String plainData, String aesKey, String transformation)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
			IllegalBlockSizeException {
		SecretKeySpec secretKey = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
		Cipher cipher = Cipher.getInstance(transformation);
		cipher.init(1, secretKey);
		return Base64.getEncoder().encodeToString(cipher.doFinal(plainData.getBytes()));
	}

	public static String encryptRSA(String key) throws Exception {
		PublicKey publicKey = loadPublicKey();
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(1, publicKey);
		byte[] bytes = cipher.doFinal(key.getBytes(StandardCharsets.UTF_8));
		return new String(Base64.getEncoder().encode(bytes));
	}
	
	
	
	private static PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory publicKeyFactory = KeyFactory.getInstance("RSA");
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(readFileBytes("C:\\Akshay_Ramani\\Jio_Bank_Projects\\JIOBANK_RECON_APPLICATION\\jpb-recon-system-application\\src\\main\\resources\\publickey\\publicKey.key"));
		PublicKey publicKey = publicKeyFactory.generatePublic(publicKeySpec);
		return publicKey;
	}
	
	
	private static byte[] readFileBytes(String filename) throws IOException {
		Path path = Paths.get(filename);
		if(!Files.exists(path)){
			System.out.println("FIle not found");
		}
		return Files.readAllBytes(path);
	}

}
