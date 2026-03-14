package com.sharemyrecipe.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-ms}") long accessTtlMs,
            @Value("${app.jwt.refresh-ttl-ms}") long refreshTtlMs) {
        // Ensure minimum 256-bit key for HS256
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTtlMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTtlMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public long getAccessTtlMs() {
        return accessTtlMs;
    }

    public long getRefreshTtlMs() {
        return refreshTtlMs;
    }
}
