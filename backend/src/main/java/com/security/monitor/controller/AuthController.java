package com.security.monitor.controller;

import com.security.monitor.dto.LoginRequest;
import com.security.monitor.dto.LoginResponse;
import com.security.monitor.service.AuthService;
import com.security.monitor.service.SystemLogService;
import com.security.monitor.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            logger.info("Login attempt for username: {}", request.getUsername());
            LoginResponse response = authService.login(request);
            logger.info("Login successful for username: {}", request.getUsername());

            systemLogService.logLogin(
                    response.getUserId(),
                    response.getUsername(),
                    ipAddress,
                    userAgent,
                    true,
                    "用户登录成功"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for username: {}", request.getUsername(), e);

            systemLogService.logLogin(
                    null,
                    request.getUsername(),
                    ipAddress,
                    userAgent,
                    false,
                    "登录失败: " + e.getMessage()
            );

            return ResponseEntity.badRequest().build();
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 从 Authorization header 获取用户信息
        String authHeader = httpRequest.getHeader("Authorization");
        Long userId = null;
        String username = "未知用户";

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                userId = jwtUtil.extractUserId(token);
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                logger.warn("Failed to extract user info from token: {}", e.getMessage());
            }
        }

        systemLogService.logLogout(userId, username, ipAddress, userAgent, "用户登出");
        logger.info("User logged out: {}", username);

        return ResponseEntity.ok().build();
    }
}
