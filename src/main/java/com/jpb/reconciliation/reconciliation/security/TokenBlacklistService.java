package com.jpb.reconciliation.reconciliation.security;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class TokenBlacklistService {

    private Set<String> blacklistedTokenJtIs = Collections.synchronizedSet(new HashSet<>());

    public void blacklistToken(String jti) {
        if (jti != null && !jti.trim().isEmpty()) {
            blacklistedTokenJtIs.add(jti);
            System.out.println("DEBUG: Blacklisted JTI: " + jti); // For debugging
        }
    }

    public boolean isTokenBlacklisted(String jti) {
        boolean isBlacklisted = blacklistedTokenJtIs.contains(jti);
        System.out.println("DEBUG: Checking JTI: " + jti + ", Blacklisted: " + isBlacklisted); 
        return isBlacklisted;
    }

}