package com.security.monitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "danger_behavior_id", nullable = false)
    private Long dangerBehaviorId;

    @Column(name = "timestamp_in_video", nullable = false)
    private Integer timestampInVideo;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "severity_level", nullable = false)
    private Integer severityLevel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "frame_snapshot_path", length = 500)
    private String frameSnapshotPath;

    @Column(name = "is_acknowledged")
    private Boolean isAcknowledged = false;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
