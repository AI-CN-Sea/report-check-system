package com.reportcheck.util;

import com.reportcheck.common.AuthInfo;
import com.reportcheck.config.ReportCheckProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final ReportCheckProperties properties;

    public JwtUtil(ReportCheckProperties properties) {
        this.properties = properties;
    }

    public String generateToken(Long userId, String username, String roleCode) {
        Instant now = Instant.now();
        Instant expireTime = now.plusSeconds(properties.getJwtExpireMinutes() * 60);
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "userId", userId,
                        "roleCode", roleCode
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireTime))
                .signWith(getSecretKey())
                .compact();
    }

    public AuthInfo parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number userId = claims.get("userId", Number.class);
        String roleCode = claims.get("roleCode", String.class);
        return new AuthInfo(userId.longValue(), claims.getSubject(), roleCode);
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
