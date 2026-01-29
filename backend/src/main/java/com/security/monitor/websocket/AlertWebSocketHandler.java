package com.security.monitor.websocket;

import com.security.monitor.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Set;

@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AlertWebSocketHandler.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            sessionManager.addSession(userId, session);
            logger.info("WebSocket connection established for user: {}", userId);
        } else {
            logger.warn("WebSocket connection rejected: invalid token");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            sessionManager.removeSession(userId, session);
            logger.info("WebSocket connection closed for user: {}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages if needed
        logger.debug("Received message: {}", message.getPayload());
    }

    public void sendAlertToUser(Long userId, String message) {
        Set<WebSocketSession> sessions = sessionManager.getUserSessions(userId);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    logger.error("Failed to send message to user: {}", userId, e);
                }
            }
        }
    }

    /**
     * 安全：从WebSocket会话中提取并验证用户ID
     */
    private Long extractUserId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) {
                logger.warn("WebSocket URI is null");
                return null;
            }

            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                logger.warn("WebSocket query string is empty");
                return null;
            }

            // 安全：使用更安全的方式解析token参数
            String token = null;
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6); // "token=".length() = 6
                    break;
                }
            }

            if (token == null || token.isEmpty()) {
                logger.warn("Token not found in WebSocket query string");
                return null;
            }

            // 安全：验证token的有效性
            Long userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);

            if (userId == null || username == null) {
                logger.warn("Invalid token: missing userId or username");
                return null;
            }

            // 安全：使用validateToken进行完整验证
            if (!jwtUtil.validateToken(token, username)) {
                logger.warn("Token validation failed for user: {}", username);
                return null;
            }

            return userId;
        } catch (Exception e) {
            logger.error("Failed to extract and validate user ID from WebSocket session", e);
            return null;
        }
    }
}
