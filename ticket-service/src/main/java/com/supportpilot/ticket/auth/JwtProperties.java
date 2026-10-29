package com.supportpilot.ticket.auth;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {

    private static final int MIN_SECRET_BYTES = 32; // HS256 needs a key of at least 256 bits

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is not set (use the APP_JWT_SECRET env var)");
        }
        if (Base64.getDecoder().decode(secret).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be base64 and decode to at least 32 bytes");
        }
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
    }
}
