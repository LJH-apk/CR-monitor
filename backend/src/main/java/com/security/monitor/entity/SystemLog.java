package com.security.monitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String level = "INFO";

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_uri", length = 255)
    private String requestUri;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(length = 100)
    private String location;

    @Column(name = "login_success")
    private Boolean loginSuccess;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static SystemLog loginLog(Long userId, String username, String ipAddress,
                                      String userAgent, boolean success, String message) {
        SystemLog log = new SystemLog();
        log.setLevel(success ? "INFO" : "WARN");
        log.setType("LOGIN");
        log.setUserId(userId);
        log.setUsername(username);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setLoginSuccess(success);
        log.setMessage(message);
        return log;
    }

    public static SystemLog errorLog(String message, String details,
                                      String requestUri, String requestMethod) {
        SystemLog log = new SystemLog();
        log.setLevel("ERROR");
        log.setType("ERROR");
        log.setMessage(message);
        log.setDetails(details);
        log.setRequestUri(requestUri);
        log.setRequestMethod(requestMethod);
        return log;
    }

    public static SystemLog operationLog(Long userId, String username,
                                          String message, String requestUri) {
        SystemLog log = new SystemLog();
        log.setLevel("INFO");
        log.setType("OPERATION");
        log.setUserId(userId);
        log.setUsername(username);
        log.setMessage(message);
        log.setRequestUri(requestUri);
        return log;
    }

    public static SystemLog logoutLog(Long userId, String username, String ipAddress,
                                       String userAgent, String message) {
        SystemLog log = new SystemLog();
        log.setLevel("INFO");
        log.setType("LOGOUT");
        log.setUserId(userId);
        log.setUsername(username);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setMessage(message);
        return log;
    }
}
