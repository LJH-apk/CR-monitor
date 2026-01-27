package com.security.monitor.repository;

import com.security.monitor.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByVideoIdOrderByTimestampInVideoAsc(Long videoId);

    List<Alert> findByIsAcknowledgedFalseOrderByCreatedAtDesc();

    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :startTime ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("startTime") LocalDateTime startTime);

    List<Alert> findTop100ByOrderByCreatedAtDesc();
}
