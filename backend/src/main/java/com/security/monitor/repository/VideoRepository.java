package com.security.monitor.repository;

import com.security.monitor.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByUserIdOrderByUploadTimeDesc(Long userId);

    List<Video> findByStatus(String status);
}
