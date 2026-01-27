package com.security.monitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_thresholds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "danger_behavior_id", nullable = false)
    private Long dangerBehaviorId;

    @Column(name = "confidence_threshold", precision = 5, scale = 2)
    private BigDecimal confidenceThreshold = BigDecimal.valueOf(0.70);

    @Column(name = "time_window_seconds")
    private Integer timeWindowSeconds = 60;

    @Column(name = "max_alerts_per_window")
    private Integer maxAlertsPerWindow = 10;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
