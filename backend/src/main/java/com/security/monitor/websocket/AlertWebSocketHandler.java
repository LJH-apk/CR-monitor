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

    private Long extractUserId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null) {
                String query = uri.getQuery();
                if (query != null && query.contains("token=")) {
                    String token = query.split("token=")[1].split("&")[0];
                    return jwtUtil.extractUserId(token);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to extract user ID from WebSocket session", e);
        }
        return null;
    }
}
