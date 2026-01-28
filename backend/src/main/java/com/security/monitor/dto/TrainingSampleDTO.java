package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSampleDTO {
    private Long id;
    private Long userId;
    private String username;
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    private Integer imageWidth;
    private Integer imageHeight;
    private String status;
    private LocalDateTime uploadTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AnnotationDTO> annotations;
}
