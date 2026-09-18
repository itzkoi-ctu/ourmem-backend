package koi.ourmemory.security;

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
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UUID userId, UUID sessionId) {
        return generateToken(userId, sessionId, new Date(System.currentTimeMillis() + accessTokenExpiration), "ACCESS");
    }

    public String generateRefreshToken(UUID userId, UUID sessionId, java.time.Instant expiresAt) {
        return generateToken(userId, sessionId, Date.from(expiresAt), "REFRESH");
    }

    public java.time.Instant newSessionExpiry() {
        return java.time.Instant.now().plusMillis(refreshTokenExpiration);
    }

    private String generateToken(UUID userId, UUID sessionId, Date expiryDate, String type) {
        Date now = new Date();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .id(UUID.randomUUID().toString())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public UUID getSessionIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).get("sid", String.class));
    }

    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get("type", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected invalid or expired JWT");
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
