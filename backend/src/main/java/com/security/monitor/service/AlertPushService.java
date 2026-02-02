package com.security.monitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.monitor.dto.DetectionStatusMessage;
import com.security.monitor.entity.Alert;
import com.security.monitor.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

@Service
public class AlertPushService {

    private static final Logger logger = LoggerFactory.getLogger(AlertPushService.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    public void pushAlert(Alert alert, Long userId) {
        try {
            DetectionStatusMessage message = DetectionStatusMessage.alert(alert.getVideoId(), alert);
            String json = objectMapper.writeValueAsString(message);
            sendMessageToUser(userId, json);
            logger.info("Alert pushed to user {}: {}", userId, alert.getId());
        } catch (Exception e) {
            logger.error("Failed to push alert to user: {}", userId, e);
        }
    }

    /**
     * 推送无异常状态消息
     */
    public void pushNormalStatus(Long videoId, int analyzedFrames, Long userId) {
        try {
            DetectionStatusMessage message = DetectionStatusMessage.normal(videoId, analyzedFrames);
            String json = objectMapper.writeValueAsString(message);
            sendMessageToUser(userId, json);
            logger.info("Normal status pushed to user {}: videoId={}, frames={}", userId, videoId, analyzedFrames);
        } catch (Exception e) {
            logger.error("Failed to push normal status to user: {}", userId, e);
        }
    }

    /**
     * 发送消息给指定用户
     */
    private void sendMessageToUser(Long userId, String message) {
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
}
