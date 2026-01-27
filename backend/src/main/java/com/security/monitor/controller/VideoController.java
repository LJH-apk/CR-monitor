package com.security.monitor.controller;

import com.security.monitor.dto.VideoUploadResponse;
import com.security.monitor.entity.Video;
import com.security.monitor.repository.VideoRepository;
import com.security.monitor.service.VideoProcessingService;
import com.security.monitor.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.security.monitor.service.CacheService cacheService;

    @Value("${storage.uploads}")
    private String uploadsPath;

    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);

            // Create user directory
            String userDir = uploadsPath + "/" + userId;
            File directory = new File(userDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            String filePath = userDir + "/" + storedFilename;

            // Save file
            Path path = Paths.get(filePath);
            Files.write(path, file.getBytes());

            // Create video entity
            Video video = new Video();
            video.setUserId(userId);
            video.setOriginalFilename(originalFilename);
            video.setStoredFilename(storedFilename);
            video.setFileSize(file.getSize());
            video.setStatus("UPLOADING");
            video.setOriginalPath(filePath);
            video.setUploadTime(LocalDateTime.now());

            video = videoRepository.save(video);

            logger.info("Video uploaded successfully: {}", video.getId());

            // Start transcoding asynchronously
            videoProcessingService.transcodeVideo(video.getId());

            return ResponseEntity.ok(new VideoUploadResponse(
                    video.getId(),
                    originalFilename,
                    "UPLOADING",
                    "Video uploaded successfully and transcoding started"
            ));

        } catch (IOException e) {
            logger.error("Failed to upload video", e);
            return ResponseEntity.badRequest().body(new VideoUploadResponse(
                    null, null, "FAILED", "Failed to upload video: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<Video>> getUserVideos(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        List<Video> videos = videoRepository.findByUserIdOrderByUploadTimeDesc(userId);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Video> getVideo(@PathVariable Long id) {
        return videoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        videoRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/analysis-progress")
    public ResponseEntity<java.util.Map<String, Object>> getAnalysisProgress(@PathVariable Long id) {
        try {
            Object progressDataObj = cacheService.get("transcoding:status:" + id);
            String progressData = progressDataObj != null ? progressDataObj.toString() : null;

            java.util.Map<String, Object> response = new java.util.HashMap<>();

            if (progressData != null && !progressData.isEmpty()) {
                // 解析JSON字符串
                if (progressData.contains("progress")) {
                    // 简单的JSON解析
                    int progress = 0;
                    String status = "analyzing";

                    try {
                        // 提取progress值
                        int progressStart = progressData.indexOf("\"progress\":") + 11;
                        int progressEnd = progressData.indexOf(",", progressStart);
                        if (progressEnd == -1) progressEnd = progressData.indexOf("}", progressStart);
                        progress = Integer.parseInt(progressData.substring(progressStart, progressEnd).trim());

                        // 提取status值
                        int statusStart = progressData.indexOf("\"status\":") + 10;
                        int statusEnd = progressData.indexOf("\"", statusStart + 1);
                        status = progressData.substring(statusStart, statusEnd);
                    } catch (Exception e) {
                        logger.error("Failed to parse progress data", e);
                    }

                    response.put("progress", progress);
                    response.put("status", status);
                    response.put("analyzing", progress < 100);
                } else {
                    response.put("progress", 0);
                    response.put("status", "pending");
                    response.put("analyzing", false);
                }
            } else {
                // 没有进度数据，检查视频状态
                Video video = videoRepository.findById(id).orElse(null);
                if (video != null && "READY".equals(video.getStatus())) {
                    response.put("progress", 100);
                    response.put("status", "completed");
                    response.put("analyzing", false);
                } else {
                    response.put("progress", 0);
                    response.put("status", "pending");
                    response.put("analyzing", false);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get analysis progress", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
