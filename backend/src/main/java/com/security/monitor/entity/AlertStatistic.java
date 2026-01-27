package com.security.monitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_statistics",
       uniqueConstraints = @UniqueConstraint(name = "unique_stat", columnNames = {"date", "hour", "danger_behavior_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer hour;

    @Column(name = "danger_behavior_id", nullable = false)
    private Long dangerBehaviorId;

    @Column(name = "alert_count")
    private Integer alertCount = 0;

    @Column(name = "avg_confidence", precision = 5, scale = 2)
    private BigDecimal avgConfidence;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
