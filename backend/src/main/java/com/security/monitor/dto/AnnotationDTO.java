package com.security.monitor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("xMin")
    private Integer xMin;

    @JsonProperty("yMin")
    private Integer yMin;

    @JsonProperty("xMax")
    private Integer xMax;

    @JsonProperty("yMax")
    private Integer yMax;

    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
