package com.gabaritaplus.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final int MINIMUM_COMPATIBILITY_CHARS = 16;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-minutes}")
    private long accessTokenExpirationMinutes;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    private volatile SecretKey signingKey;

    @PostConstruct
    void validateConfiguration() {
        SecretKey key = getSigningKey();
        log.info("Configuracao JWT carregada com sucesso. keyLengthBytes={}", key.getEncoded().length);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", extractRoles(userDetails.getAuthorities()));
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES)))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS)))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMinutes * 60;
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        SecretKey currentKey = signingKey;
        if (currentKey != null) {
            return currentKey;
        }

        synchronized (this) {
            if (signingKey == null) {
                signingKey = Keys.hmacShaKeyFor(resolveKeyBytes(jwtSecret));
            }
            return signingKey;
        }
    }

    private byte[] resolveKeyBytes(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT secret nao configurado.");
        }

        String sanitizedSecret = configuredSecret.trim();
        byte[] rawBytes = sanitizedSecret.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length >= MINIMUM_SECRET_BYTES) {
            return rawBytes;
        }

        if (sanitizedSecret.startsWith("base64:")) {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(sanitizedSecret.substring("base64:".length()));
            if (decodedBytes.length < MINIMUM_SECRET_BYTES) {
                throw new IllegalStateException("JWT secret em base64 precisa gerar pelo menos 32 bytes.");
            }
            return decodedBytes;
        }

        if (rawBytes.length < MINIMUM_COMPATIBILITY_CHARS) {
            throw new IllegalStateException("JWT secret precisa ter pelo menos 32 bytes ou usar o formato base64:...");
        }

        log.warn(
                "JWT secret com menos de 32 bytes detectado. Aplicando derivacao SHA-256 por compatibilidade. " +
                        "Rotacione o segredo para um valor aleatorio com 32+ bytes ou use base64:..."
        );
        return sha256(rawBytes);
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 nao disponivel para derivacao da chave JWT.", exception);
        }
    }

    private Collection<String> extractRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
