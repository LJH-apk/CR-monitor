package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationDTO {
    private Long id;
    private Long sampleId;
    private Long dangerBehaviorId;
    private String dangerBehaviorName;
    private Integer xMin;
    private Integer yMin;
    private Integer xMax;
    private Integer yMax;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
