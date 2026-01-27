package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadResponse {
    private Long videoId;
    private String filename;
    private String status;
    private String message;
}
