package com.travelapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT issuance and validation, matching section 1.2 of the API docs: accessToken expires in 2
 * hours, refreshToken expires in 7 days. accessToken/refreshToken are distinguished by the type
 * claim field, preventing a refreshToken from being used as an accessToken.
 */
@Component
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenExpireSeconds;
    private final long refreshTokenExpireSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expire-seconds}") long accessTokenExpireSeconds,
            @Value("${app.jwt.refresh-token-expire-seconds}") long refreshTokenExpireSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
        this.refreshTokenExpireSeconds = refreshTokenExpireSeconds;
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, TOKEN_TYPE_ACCESS, accessTokenExpireSeconds);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, TOKEN_TYPE_REFRESH, refreshTokenExpireSeconds);
    }

    public long getAccessTokenExpireSeconds() {
        return accessTokenExpireSeconds;
    }

    /** Parses and validates the token, returning userId; throws JwtException if the token is invalid/expired/wrong type, converted by the caller into a business error */
    public Long parseAccessTokenUserId(String token) {
        return parseUserId(token, TOKEN_TYPE_ACCESS);
    }

    public Long parseRefreshTokenUserId(String token) {
        return parseUserId(token, TOKEN_TYPE_REFRESH);
    }

    private String generateToken(Long userId, String tokenType, long expireSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Long parseUserId(String token, String expectedTokenType) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedTokenType.equals(tokenType)) {
            throw new JwtException("Token type mismatch, expected: " + expectedTokenType + ", actual: " + tokenType);
        }
        return Long.valueOf(claims.getSubject());
    }
}
