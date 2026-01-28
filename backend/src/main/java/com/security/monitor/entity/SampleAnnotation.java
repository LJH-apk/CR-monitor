package com.security.monitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sample_annotations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SampleAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sample_id", nullable = false)
    private Long sampleId;

    @Column(name = "danger_behavior_id", nullable = false)
    private Long dangerBehaviorId;

    @Column(name = "x_min", nullable = false)
    private Integer xMin;

    @Column(name = "y_min", nullable = false)
    private Integer yMin;

    @Column(name = "x_max", nullable = false)
    private Integer xMax;

    @Column(name = "y_max", nullable = false)
    private Integer yMax;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
