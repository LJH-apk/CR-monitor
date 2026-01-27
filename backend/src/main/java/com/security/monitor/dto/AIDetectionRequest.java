package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIDetectionRequest {
    private String image;  // Base64 encoded image
    private Double confThreshold;  // YOLO confidence threshold
    private Double fineThreshold;  // Fine detection threshold
}
