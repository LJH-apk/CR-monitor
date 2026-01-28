package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SampleUploadResponse {
    private Long sampleId;
    private String filename;
    private String status;
    private String message;
    private Integer imageWidth;
    private Integer imageHeight;
}
