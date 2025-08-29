package com.boxai.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final Key key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = deriveKey(secret);
    }

    public String createToken(Long userId, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setClaims(extraClaims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(60L * 60L * 24L * 7L)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    static class Base64Padding {
        static String ensure(String secret) {
            String s = secret.trim();
            int mod = s.length() % 4;
            if (mod == 0) return s;
            return s + "====".substring(mod);
        }
    }

    private static Key deriveKey(String secret) {
        try {
            byte[] raw;
            try {
                raw = Decoders.BASE64.decode(Base64Padding.ensure(secret));
            } catch (Exception ex) {
                // 非 Base64 明文，使用 SHA-256 派生固定长度 key
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                raw = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            }
            return Keys.hmacShaKeyFor(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot init JWT key", e);
        }
    }
}


