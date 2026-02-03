package com.security.monitor.dto;

import com.security.monitor.entity.SystemLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemLogDTO {
    private Long id;
    private String level;
    private String type;
    private String message;
    private String details;
    private Long userId;
    private String username;
    private String ipAddress;
    private String location;
    private Boolean loginSuccess;
    private String requestUri;
    private String requestMethod;
    private LocalDateTime createdAt;

    public static SystemLogDTO fromEntity(SystemLog log) {
        SystemLogDTO dto = new SystemLogDTO();
        dto.setId(log.getId());
        dto.setLevel(log.getLevel());
        dto.setType(log.getType());
        dto.setMessage(log.getMessage());
        dto.setDetails(log.getDetails());
        dto.setUserId(log.getUserId());
        dto.setUsername(log.getUsername());
        dto.setIpAddress(log.getIpAddress());
        dto.setLocation(log.getLocation());
        dto.setLoginSuccess(log.getLoginSuccess());
        dto.setRequestUri(log.getRequestUri());
        dto.setRequestMethod(log.getRequestMethod());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
