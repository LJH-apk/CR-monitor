package com.security.monitor.repository;

import com.security.monitor.entity.AlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, Long> {

    Optional<AlertThreshold> findByDangerBehaviorId(Long dangerBehaviorId);
}
