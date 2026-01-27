package com.security.monitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIDetectionResult {
    private DetectionInfo detection;
    private AnalysisInfo analysis;
    private List<AIAlert> alerts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetectionInfo {
        private List<Double> bbox;  // [x1, y1, x2, y2]
        private Double confidence;

        @JsonProperty("class_name")
        private String className;

        private String type;
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysisInfo {
        private String analysis;
        private Map<String, Object> metadata;
    }
}
