package com.security.monitor.controller;

import com.security.monitor.dto.DashboardStatsDTO;
import com.security.monitor.entity.Alert;
import com.security.monitor.entity.AlertStatistic;
import com.security.monitor.entity.DangerBehavior;
import com.security.monitor.repository.AlertRepository;
import com.security.monitor.repository.AlertStatisticRepository;
import com.security.monitor.repository.DangerBehaviorRepository;
import com.security.monitor.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private DangerBehaviorRepository dangerBehaviorRepository;

    @Autowired
    private AlertStatisticRepository alertStatisticRepository;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);

        // Total alerts
        long totalAlerts = alertRepository.count();

        // Unacknowledged alerts
        long unacknowledgedAlerts = alertRepository.findByIsAcknowledgedFalseOrderByCreatedAtDesc().size();

        // Total videos
        long totalVideos = videoRepository.count();

        // Recent alerts for analysis
        List<Alert> recentAlerts = alertRepository.findRecentAlerts(startTime);

        // Alerts by behavior
        Map<String, Long> alertsByBehavior = new HashMap<>();
        Map<Long, String> behaviorNames = dangerBehaviorRepository.findAll().stream()
                .collect(Collectors.toMap(DangerBehavior::getId, DangerBehavior::getName));

        for (Alert alert : recentAlerts) {
            String behaviorName = behaviorNames.getOrDefault(alert.getDangerBehaviorId(), "Unknown");
            alertsByBehavior.put(behaviorName, alertsByBehavior.getOrDefault(behaviorName, 0L) + 1);
        }

        // Alerts by severity
        Map<String, Long> alertsBySeverity = recentAlerts.stream()
                .collect(Collectors.groupingBy(
                        alert -> "Level " + alert.getSeverityLevel(),
                        Collectors.counting()
                ));

        // Alerts by hour (last 24 hours)
        List<DashboardStatsDTO.HourlyAlertCount> alertsByHour = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<AlertStatistic> statistics = alertStatisticRepository.findRecentStatistics(today.minusDays(1));

        Map<Integer, Long> hourlyMap = statistics.stream()
                .collect(Collectors.groupingBy(
                        AlertStatistic::getHour,
                        Collectors.summingLong(AlertStatistic::getAlertCount)
                ));

        for (int i = 0; i < 24; i++) {
            alertsByHour.add(new DashboardStatsDTO.HourlyAlertCount(
                    String.format("%02d:00", i),
                    hourlyMap.getOrDefault(i, 0L)
            ));
        }

        DashboardStatsDTO stats = new DashboardStatsDTO(
                totalAlerts,
                unacknowledgedAlerts,
                totalVideos,
                alertsByBehavior,
                alertsBySeverity,
                alertsByHour
        );

        return ResponseEntity.ok(stats);
    }
}
