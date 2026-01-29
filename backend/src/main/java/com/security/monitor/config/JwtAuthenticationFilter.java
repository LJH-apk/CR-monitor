package com.security.monitor.config;

import com.security.monitor.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 安全：只处理Bearer令牌
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // 安全：提取用户信息前先验证令牌
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 安全：使用validateToken进行完整验证（包括签名、过期时间、声明）
                    if (jwtUtil.validateToken(token, username)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.debug("JWT authentication successful for user: {}", username);
                    } else {
                        logger.warn("JWT token validation failed for user: {}", username);
                        // 安全：清除可能存在的认证信息
                        SecurityContextHolder.clearContext();
                    }
                }
            } catch (JwtException e) {
                // 安全：JWT解析或验证失败，清除认证上下文
                logger.error("JWT authentication failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                // 注意：不在这里返回401，让Spring Security的异常处理器处理
            } catch (Exception e) {
                // 安全：其他异常也要清除认证上下文
                logger.error("Unexpected error during JWT authentication", e);
                SecurityContextHolder.clearContext();
            }
        }

        // 继续过滤链，让Spring Security决定是否需要认证
        filterChain.doFilter(request, response);
    }
}
