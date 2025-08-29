package com.boxai.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private Key key;
    @Value("${app.jwt.secret}")
    private String secret;

    public static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    @PostConstruct
    public void init() {
        this.key = deriveKey(secret);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
                Long userId = Long.valueOf(claims.getSubject());
                CURRENT_USER.set(userId);
            }
            filterChain.doFilter(request, response);
        } finally {
            CURRENT_USER.remove();
        }
    }
    private static Key deriveKey(String secret) {
        try {
            byte[] raw;
            try {
                raw = Decoders.BASE64.decode(JwtService.Base64Padding.ensure(secret));
            } catch (Exception ex) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                raw = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            }
            return Keys.hmacShaKeyFor(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot init JWT key", e);
        }
    }
}


