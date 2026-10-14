package com.jpb.reconciliation.reconciliation.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtHelper {

	public static final long JWT_TOKEN_VALIDITY = 1000 * 60 * 60 * 8; // 8 hours

	public static final long JWT_TOKEN_REFRESH = 1000 * 60 * 60 * 8; // 8 hours
	
	public static final long JWT_TOKEN_REVOKE = 0;

	private String secret = "afafasfafafasfasfasfafacasdasfasxASFACASDFACASDFASFASFDAFASFASDAADSCSDFADCVSGCFVADXCcadwavfsfarvf";

	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
	}

	private Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	public String generateToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		return doGenerateToken(claims, userDetails.getUsername(), JWT_TOKEN_VALIDITY);
	}

	public String generateToken(UserDetails userDetails, String role) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("role", role);
		return doGenerateToken(claims, userDetails.getUsername(), JWT_TOKEN_VALIDITY);
	}

	public String getRoleFromToken(String token) {
		try {
			return getClaimFromToken(token, claims -> (String) claims.get("role"));
		} catch (Exception e) {
			return null;
		}
	}

	public String generateTokenForRefresh(String userName) {
		Map<String, Object> claims = new HashMap<>();
		return doGenerateToken(claims, userName, JWT_TOKEN_REFRESH);
	}
	
	private String doGenerateToken(Map<String, Object> claims, String subject, long validity) {
		return Jwts.builder().setClaims(claims).setSubject(subject)
				.setId(UUID.randomUUID().toString()) // <--- NEW: Set a unique JWT ID
				
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + validity))
				.signWith(SignatureAlgorithm.HS512, secret).compact();
	}

	public Boolean validateToken(String token, String userName, boolean isBlacklisted) {
		final String username = getUsernameFromToken(token);
		return (username.equals(userName) && !isTokenExpired(token) && !isBlacklisted);
	}

	public String generateRefreshToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_REFRESH))
				.signWith(SignatureAlgorithm.HS512, secret).compact();
	}
	
    public String getJtiFromToken(String token) {
        return getClaimFromToken(token, Claims::getId);
    }

}