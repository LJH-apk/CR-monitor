package com.security.monitor.repository;

import com.security.monitor.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    Page<SystemLog> findByLevel(String level, Pageable pageable);

    Page<SystemLog> findByType(String type, Pageable pageable);

    Page<SystemLog> findByLevelAndType(String level, String type, Pageable pageable);

    Page<SystemLog> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT l FROM SystemLog l WHERE " +
           "(:level IS NULL OR l.level = :level) AND " +
           "(:type IS NULL OR l.type = :type) AND " +
           "(:userId IS NULL OR l.userId = :userId) AND " +
           "(:startTime IS NULL OR l.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR l.createdAt <= :endTime) AND " +
           "(:keyword IS NULL OR l.message LIKE %:keyword% OR l.username LIKE %:keyword%)")
    Page<SystemLog> findByFilters(
            @Param("level") String level,
            @Param("type") String type,
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword,
            Pageable pageable);

    List<SystemLog> findTop10ByTypeOrderByCreatedAtDesc(String type);

    @Query("SELECT l.level, COUNT(l) FROM SystemLog l " +
           "WHERE l.createdAt >= :since GROUP BY l.level")
    List<Object[]> countByLevelSince(@Param("since") LocalDateTime since);

    @Query("SELECT l.loginSuccess, COUNT(l) FROM SystemLog l " +
           "WHERE l.type = 'LOGIN' AND l.createdAt >= :since GROUP BY l.loginSuccess")
    List<Object[]> countLoginStatsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM SystemLog l " +
           "WHERE l.type = 'LOGOUT' AND l.createdAt >= :since")
    Long countLogoutSince(@Param("since") LocalDateTime since);
}
