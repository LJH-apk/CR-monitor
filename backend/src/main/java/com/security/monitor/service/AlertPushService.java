package com.security.monitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.monitor.entity.Alert;
import com.security.monitor.websocket.AlertWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlertPushService {

    private static final Logger logger = LoggerFactory.getLogger(AlertPushService.class);

    @Autowired
    private AlertWebSocketHandler webSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    public void pushAlert(Alert alert, Long userId) {
        try {
            String message = objectMapper.writeValueAsString(alert);
            webSocketHandler.sendAlertToUser(userId, message);
            logger.info("Alert pushed to user {}: {}", userId, alert.getId());
        } catch (Exception e) {
            logger.error("Failed to push alert to user: {}", userId, e);
        }
    }
}
