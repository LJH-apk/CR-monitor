package com.security.monitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "status", length = 20)
    private String status = "UPLOADING";

    @Column(name = "original_path", length = 500)
    private String originalPath;

    @Column(name = "hls_path", length = 500)
    private String hlsPath;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "transcoding_started_at")
    private LocalDateTime transcodingStartedAt;

    @Column(name = "transcoding_completed_at")
    private LocalDateTime transcodingCompletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
