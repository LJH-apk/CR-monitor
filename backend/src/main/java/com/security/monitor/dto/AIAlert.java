package com.security.monitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIAlert {
    private String category;  // suspicious_behavior, safety_hazard, security_risk, emergency
    private String keyword;
    private String severity;  // high, medium, low

    @JsonProperty("combined_score")
    private Double combinedScore;

    @JsonProperty("detection_confidence")
    private Double detectionConfidence;

    @JsonProperty("keyword_weight")
    private Double keywordWeight;
}
