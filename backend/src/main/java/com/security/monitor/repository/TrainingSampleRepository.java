package com.security.monitor.repository;

import com.security.monitor.entity.TrainingSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingSampleRepository extends JpaRepository<TrainingSample, Long> {

    // 根据用户ID查询样本列表
    List<TrainingSample> findByUserId(Long userId);

    // 根据状态查询样本列表
    List<TrainingSample> findByStatus(String status);

    // 根据用户ID和状态查询样本列表
    List<TrainingSample> findByUserIdAndStatus(Long userId, String status);
}
