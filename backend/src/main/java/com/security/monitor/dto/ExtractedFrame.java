package com.security.monitor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedFrame {
    private String fileName;
    private String base64Image;
    private Long timestampMs;  // Timestamp in video (milliseconds)
}
