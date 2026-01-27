package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalAlerts;
    private long unacknowledgedAlerts;
    private long totalVideos;
    private Map<String, Long> alertsByBehavior;
    private Map<String, Long> alertsBySeverity;
    private List<HourlyAlertCount> alertsByHour;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyAlertCount {
        private String hour;
        private long count;
    }
}
