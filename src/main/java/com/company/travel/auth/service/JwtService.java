package com.company.travel.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {

        private final EffectiveAuthorityService effectiveAuthorityService;

        @Value("${jwt.secret}")
        private String secret;

        @Value("${jwt.access-token-expiration}")
        private long expirationTime;

        public JwtService(
                        EffectiveAuthorityService effectiveAuthorityService) {

                this.effectiveAuthorityService = effectiveAuthorityService;
        }

        public String generateAccessToken(
                        Long userId,
                        String username,
                        Long userGroupId,
                        String userGroupCode) {

                // 1. Get the user's current effective authorities
                Set<String> authorities = effectiveAuthorityService.getEffectiveAuthorities(userId);

                // 2. Get the current time
                Date now = new Date();

                // 3. Calculate token expiration time
                Date expiration = new Date(now.getTime() + expirationTime);

                // 4. Create the secret key
                SecretKey key = getSigningKey();

                // 5. Build and sign the JWT
                return Jwts.builder()
                                .subject(username)
                                .claim("userId", userId)
                                .claim("username", username)
                                .claim("userGroupId", userGroupId)
                                .claim("userGroupCode", userGroupCode)
                                .claim("authorities", authorities)
                                .issuedAt(now)
                                .expiration(expiration)
                                .signWith(key)
                                .compact();
        }

        public Claims parseToken(String token) {

                // Validate the signature and expiration,
                // then return the JWT claims.
                return Jwts.parser()
                                .verifyWith(getSigningKey())
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
        }

        private SecretKey getSigningKey() {

                return Keys.hmacShaKeyFor(
                                secret.getBytes(StandardCharsets.UTF_8));
        }
}