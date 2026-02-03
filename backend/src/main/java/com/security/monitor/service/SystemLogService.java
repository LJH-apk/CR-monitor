package com.security.monitor.service;

import com.security.monitor.dto.LogStatsDTO;
import com.security.monitor.dto.SystemLogDTO;
import com.security.monitor.entity.SystemLog;
import com.security.monitor.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemLogService {

    private static final Logger logger = LoggerFactory.getLogger(SystemLogService.class);

    @Autowired
    private SystemLogRepository systemLogRepository;

    @Async
    public void saveLogAsync(SystemLog log) {
        try {
            systemLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to save system log: {}", e.getMessage());
        }
    }

    public SystemLog saveLog(SystemLog log) {
        return systemLogRepository.save(log);
    }

    @Async
    public void logLogin(Long userId, String username, String ipAddress,
                         String userAgent, boolean success, String message) {
        SystemLog log = SystemLog.loginLog(userId, username, ipAddress, userAgent, success, message);
        log.setLocation(resolveLocation(ipAddress));
        saveLogAsync(log);
    }

    @Async
    public void logError(String message, String details, String requestUri, String requestMethod) {
        SystemLog log = SystemLog.errorLog(message, details, requestUri, requestMethod);
        saveLogAsync(log);
    }

    @Async
    public void logOperation(Long userId, String username, String message, String requestUri) {
        SystemLog log = SystemLog.operationLog(userId, username, message, requestUri);
        saveLogAsync(log);
    }

    @Async
    public void logLogout(Long userId, String username, String ipAddress, String userAgent, String message) {
        SystemLog log = SystemLog.logoutLog(userId, username, ipAddress, userAgent, message);
        log.setLocation(resolveLocation(ipAddress));
        saveLogAsync(log);
    }

    @Async
    public void logUpload(Long userId, String username, String message, String details) {
        SystemLog log = SystemLog.uploadLog(userId, username, message, details);
        saveLogAsync(log);
    }

    @Async
    public void logDelete(Long userId, String username, String message, String details) {
        SystemLog log = SystemLog.deleteLog(userId, username, message, details);
        saveLogAsync(log);
    }

    @Async
    public void logUserManagement(Long operatorId, String operatorName,
                                   String action, String targetUser, String details) {
        SystemLog log = SystemLog.userManagementLog(operatorId, operatorName, action, targetUser, details);
        saveLogAsync(log);
    }

    public Page<SystemLogDTO> getLogs(String level, String type, Long userId,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       String keyword, Pageable pageable) {
        Page<SystemLog> logs = systemLogRepository.findByFilters(
                level, type, userId, startTime, endTime, keyword, pageable
        );
        return logs.map(SystemLogDTO::fromEntity);
    }

    public LogStatsDTO getStats(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        LogStatsDTO stats = new LogStatsDTO();

        Map<String, Long> levelCounts = new HashMap<>();
        List<Object[]> levelResults = systemLogRepository.countByLevelSince(since);
        for (Object[] row : levelResults) {
            levelCounts.put((String) row[0], (Long) row[1]);
        }
        stats.setLevelCounts(levelCounts);

        stats.setLoginSuccessCount(0L);
        stats.setLoginFailCount(0L);
        List<Object[]> loginResults = systemLogRepository.countLoginStatsSince(since);
        for (Object[] row : loginResults) {
            Boolean success = (Boolean) row[0];
            Long count = (Long) row[1];
            if (Boolean.TRUE.equals(success)) {
                stats.setLoginSuccessCount(count);
            } else if (Boolean.FALSE.equals(success)) {
                stats.setLoginFailCount(count);
            }
        }

        // 统计登出次数
        Long logoutCount = systemLogRepository.countLogoutSince(since);
        stats.setLogoutCount(logoutCount != null ? logoutCount : 0L);

        stats.setTotalCount(levelCounts.values().stream().mapToLong(Long::longValue).sum());

        return stats;
    }

    private String resolveLocation(String ipAddress) {
        if (ipAddress == null) return null;

        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return "本地";
        }

        if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") ||
            ipAddress.startsWith("172.16.")) {
            return "内网";
        }

        return "未知";
    }
}
