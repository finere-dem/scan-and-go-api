package com.finere.scan_and_go_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_ORG_ID = "orgId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.privateKey = hasText(properties.privateKeyPem())
                ? RsaKeyLoader.loadPrivateKeyFromPem(properties.privateKeyPem())
                : RsaKeyLoader.loadPrivateKey(properties.privateKeyPath());
        this.publicKey = hasText(properties.publicKeyPem())
                ? RsaKeyLoader.loadPublicKeyFromPem(properties.publicKeyPem())
                : RsaKeyLoader.loadPublicKey(properties.publicKeyPath());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String generateAccessToken(CustomUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of(
                        "userId", user.getId().toString(),
                        CLAIM_ORG_ID, user.getOrgId() != null ? user.getOrgId().toString() : "",
                        CLAIM_ROLE, user.getAuthorities().iterator().next().getAuthority(),
                        CLAIM_TYPE, TYPE_ACCESS
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(CustomUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of("userId", user.getId().toString(), CLAIM_TYPE, TYPE_REFRESH))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.refreshTokenTtlDays(), ChronoUnit.DAYS)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, String expectedUsername) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(expectedUsername)
                    && claims.getExpiration().after(new Date())
                    && TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.get("userId", String.class));
    }
}
