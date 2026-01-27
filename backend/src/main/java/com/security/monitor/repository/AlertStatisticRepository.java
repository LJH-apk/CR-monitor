package com.security.monitor.repository;

import com.security.monitor.entity.AlertStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertStatisticRepository extends JpaRepository<AlertStatistic, Long> {

    Optional<AlertStatistic> findByDateAndHourAndDangerBehaviorId(LocalDate date, Integer hour, Long dangerBehaviorId);

    @Query("SELECT a FROM AlertStatistic a WHERE a.date >= :startDate ORDER BY a.date DESC, a.hour DESC")
    List<AlertStatistic> findRecentStatistics(@Param("startDate") LocalDate startDate);

    List<AlertStatistic> findByDateBetweenOrderByDateAscHourAsc(LocalDate startDate, LocalDate endDate);
}
