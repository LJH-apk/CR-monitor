package com.security.monitor.config;

import com.security.monitor.websocket.AlertWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AlertWebSocketHandler alertWebSocketHandler;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 安全：限制允许的来源，不使用通配符
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
