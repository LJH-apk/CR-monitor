package com.security.monitor.repository;

import com.security.monitor.entity.DangerBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DangerBehaviorRepository extends JpaRepository<DangerBehavior, Long> {

    List<DangerBehavior> findByIsActiveTrue();
}
