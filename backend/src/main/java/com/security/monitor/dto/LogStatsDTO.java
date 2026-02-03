package com.security.monitor.dto;

import lombok.Data;
import java.util.Map;

@Data
public class LogStatsDTO {
    private Map<String, Long> levelCounts;
    private Long loginSuccessCount;
    private Long loginFailCount;
    private Long totalCount;
}
