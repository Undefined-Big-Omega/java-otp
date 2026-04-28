package com.promoitotp.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

public final class Tokens {

    private Tokens() {}

    private static final SecretKey KEY;
    private static final long TTL_MS;

    static {
        Properties p = Props.load("app.properties");
        String secret = Props.get(p, "jwt.secret");
        if (secret.length() < 32)
            throw new IllegalStateException("jwt.secret must be at least 32 characters");
        KEY    = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        TTL_MS = Props.getInt(p, "jwt.ttl.seconds", 3600) * 1000L;
    }

    public static String issue(long userId, String login, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("login", login)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + TTL_MS))
                .signWith(KEY)
                .compact();
    }

    public static Claims verify(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
