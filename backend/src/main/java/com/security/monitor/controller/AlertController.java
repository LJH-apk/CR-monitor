package com.security.monitor.controller;

import com.security.monitor.entity.Alert;
import com.security.monitor.repository.AlertRepository;
import com.security.monitor.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        List<Alert> alerts = alertRepository.findTop100ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<Alert>> getVideoAlerts(@PathVariable Long videoId) {
        List<Alert> alerts = alertRepository.findByVideoIdOrderByTimestampInVideoAsc(videoId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/unacknowledged")
    public ResponseEntity<List<Alert>> getUnacknowledgedAlerts() {
        List<Alert> alerts = alertRepository.findByIsAcknowledgedFalseOrderByCreatedAtDesc();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Alert>> getRecentAlerts(@RequestParam(defaultValue = "24") int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<Alert> alerts = alertRepository.findRecentAlerts(startTime);
        return ResponseEntity.ok(alerts);
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
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
