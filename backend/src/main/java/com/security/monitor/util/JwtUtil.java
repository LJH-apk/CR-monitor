package com.security.monitor.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 安全：验证JWT密钥强度
     */
    @PostConstruct
    public void validateSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            logger.error("JWT secret key is too short! Must be at least 256 bits (32 bytes)");
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes) for HS256 algorithm");
        }
        logger.info("JWT secret key validated successfully");
    }

    /**
     * 安全：使用UTF-8编码生成签名密钥
     */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT令牌
     * 安全：添加JWT ID (jti) 防止重放攻击
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setId(UUID.randomUUID().toString())  // 安全：添加唯一ID
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 安全：提取JWT声明，包含签名验证
     * @throws JwtException 如果令牌无效、过期或签名不匹配
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage());
            throw e;
        }
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * 安全：验证JWT令牌的有效性
     * 包括签名验证、过期时间验证和用户名匹配验证
     */
    public boolean validateToken(String token, String username) {
        try {
            Claims claims = extractClaims(token);
            String tokenUsername = claims.getSubject();
            Date expiration = claims.getExpiration();

            // 验证用户名匹配
            if (!tokenUsername.equals(username)) {
                logger.warn("Token username mismatch: expected {}, got {}", username, tokenUsername);
                return false;
            }

            // 验证令牌未过期
            if (expiration.before(new Date())) {
                logger.warn("Token expired for user: {}", username);
                return false;
            }

            // 验证必需的声明存在
            if (claims.get("userId") == null || claims.get("role") == null) {
                logger.warn("Token missing required claims for user: {}", username);
                return false;
            }

            return true;
        } catch (JwtException e) {
            logger.error("Token validation failed for user {}: {}", username, e.getMessage());
            return false;
        }
    }
}
