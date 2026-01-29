package com.security.monitor.controller;

import com.security.monitor.entity.Alert;
import com.security.monitor.entity.Video;
import com.security.monitor.repository.AlertRepository;
import com.security.monitor.repository.VideoRepository;
import com.security.monitor.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 安全：验证用户是否有权访问该告警
     * 用户只能访问与自己视频相关的告警
     */
    private boolean canAccessAlert(Long alertId, Long userId) {
        return alertRepository.findById(alertId)
                .flatMap(alert -> videoRepository.findById(alert.getVideoId()))
                .map(video -> video.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * 安全：过滤出用户有权访问的告警
     */
    private List<Alert> filterUserAlerts(List<Alert> alerts, Long userId) {
        return alerts.stream()
                .filter(alert -> {
                    return videoRepository.findById(alert.getVideoId())
                            .map(video -> video.getUserId().equals(userId))
                            .orElse(false);
                })
                .collect(Collectors.toList());
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 安全：只返回用户自己视频的告警
        List<Alert> allAlerts = alertRepository.findTop100ByOrderByCreatedAtDesc();
        List<Alert> userAlerts = filterUserAlerts(allAlerts, userId);

        return ResponseEntity.ok(userAlerts);
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<Alert>> getVideoAlerts(
            @PathVariable Long videoId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 安全：验证用户是否拥有该视频
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return ResponseEntity.notFound().build();
        }

        if (!video.getUserId().equals(userId)) {
            logger.warn("User {} attempted to access alerts for video {} without permission", userId, videoId);
            return ResponseEntity.status(403).build();
        }

        List<Alert> alerts = alertRepository.findByVideoIdOrderByTimestampInVideoAsc(videoId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/unacknowledged")
    public ResponseEntity<List<Alert>> getUnacknowledgedAlerts(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 安全：只返回用户自己视频的未确认告警
        List<Alert> allAlerts = alertRepository.findByIsAcknowledgedFalseOrderByCreatedAtDesc();
        List<Alert> userAlerts = filterUserAlerts(allAlerts, userId);

        return ResponseEntity.ok(userAlerts);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Alert>> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 安全：只返回用户自己视频的最近告警
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<Alert> allAlerts = alertRepository.findRecentAlerts(startTime);
        List<Alert> userAlerts = filterUserAlerts(allAlerts, userId);

        return ResponseEntity.ok(userAlerts);
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setIsAcknowledged(true);
                    alert.setAcknowledgedBy(userId);
                    alert.setAcknowledgedAt(LocalDateTime.now());
                    return ResponseEntity.ok(alertRepository.save(alert));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 安全：验证用户是否有权删除该告警
        if (!canAccessAlert(id, userId)) {
            logger.warn("User {} attempted to delete alert {} without permission", userId, id);
            return ResponseEntity.status(403).build();
        }

        alertRepository.deleteById(id);
        logger.info("Alert {} deleted by user {}", id, userId);
        return ResponseEntity.ok().build();
    }
}
