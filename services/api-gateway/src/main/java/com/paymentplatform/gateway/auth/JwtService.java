package com.paymentplatform.gateway.auth;

import com.paymentplatform.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private final GatewaySecurityProperties properties;
    private final SecretKey secretKey;

    public JwtService(GatewaySecurityProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthResponse issueToken(String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getJwt().getExpirationMinutes(), ChronoUnit.MINUTES);

        String token = Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claims(Map.of("roles", roles))
                .signWith(secretKey)
                .compact();

        return new AuthResponse(token, "Bearer", expiresAt, username, roles);
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedUser(
                claims.getSubject(),
                (List<String>) claims.get("roles", List.class)
        );
    }
}
