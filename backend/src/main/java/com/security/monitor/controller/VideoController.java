package com.security.monitor.controller;

import com.security.monitor.dto.VideoUploadResponse;
import com.security.monitor.entity.Video;
import com.security.monitor.repository.VideoRepository;
import com.security.monitor.service.VideoProcessingService;
import com.security.monitor.util.JwtUtil;
import com.security.monitor.util.RequestUtil;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    // 安全：允许的视频文件扩展名
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
            ".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv", ".webm"
    );

    // 安全：允许的MIME类型
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "video/mp4", "video/x-msvideo", "video/quicktime",
            "video/x-matroska", "video/x-flv", "video/x-ms-wmv", "video/webm"
    );

    // 安全：文件名只允许字母、数字、下划线、连字符和点
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$");

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.security.monitor.service.CacheService cacheService;

    @Autowired
    private com.security.monitor.repository.AlertRepository alertRepository;

    @Autowired
    private com.security.monitor.service.SystemLogService systemLogService;

    @Autowired
    private com.security.monitor.repository.UserRepository userRepository;

    @Value("${storage.uploads}")
    private String uploadsPath;

    /**
     * 安全：验证文件名，防止路径遍历攻击
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 移除路径分隔符
        filename = filename.replace("/", "").replace("\\", "").replace("..", "");

        // 只保留文件名部分（去除路径）
        int lastSeparator = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (lastSeparator >= 0) {
            filename = filename.substring(lastSeparator + 1);
        }

        // 验证文件名格式
        if (!SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IllegalArgumentException("文件名包含非法字符");
        }

        return filename;
    }

    /**
     * 安全：验证文件类型
     */
    private void validateVideoFile(MultipartFile file) {
        // 验证文件不为空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 验证文件大小（最大500MB）
        long maxSize = 500 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小超过限制（最大500MB）");
        }

        // 验证MIME类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型，只允许视频文件");
        }

        // 验证文件扩展名
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名");
        }
    }

    /**
     * 安全：验证用户是否有权访问该视频
     */
    private boolean canAccessVideo(Long videoId, Long userId) {
        return videoRepository.findById(videoId)
                .map(video -> video.getUserId().equals(userId))
                .orElse(false);
    }

    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader,
            jakarta.servlet.http.HttpServletRequest request) {

        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            String ipAddress = RequestUtil.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 安全：验证文件类型和大小
            validateVideoFile(file);

            // 安全：清理文件名，防止路径遍历
            String originalFilename = sanitizeFilename(file.getOriginalFilename());

            // Create user directory
            String userDir = uploadsPath + "/" + userId;
            File directory = new File(userDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 安全：使用UUID生成文件名，只保留扩展名
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String storedFilename = UUID.randomUUID().toString() + extension;
            String filePath = userDir + "/" + storedFilename;

            // 安全：验证最终路径不包含路径遍历
            Path normalizedPath = Paths.get(filePath).normalize();
            if (!normalizedPath.startsWith(Paths.get(uploadsPath).normalize())) {
                throw new SecurityException("检测到路径遍历攻击");
            }

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

            // 记录上传日志
            String username = userRepository.findById(userId).map(u -> u.getUsername()).orElse("unknown");
            systemLogService.logUpload(userId, username,
                    "上传视频: " + originalFilename,
                    String.format("视频ID: %d, 文件大小: %.2fMB", video.getId(), file.getSize() / 1024.0 / 1024.0),
                    ipAddress, userAgent);

            logger.info("Video uploaded successfully: {}", video.getId());

            // Start transcoding asynchronously
            videoProcessingService.transcodeVideo(video.getId());

            return ResponseEntity.ok(new VideoUploadResponse(
                    video.getId(),
                    originalFilename,
                    "UPLOADING",
                    "Video uploaded successfully and transcoding started"
            ));

        } catch (IllegalArgumentException | SecurityException e) {
            logger.warn("Invalid upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new VideoUploadResponse(
                    null, null, "FAILED", e.getMessage()
            ));
        } catch (IOException e) {
            logger.error("Failed to upload video", e);
            return ResponseEntity.badRequest().body(new VideoUploadResponse(
                    null, null, "FAILED", "文件上传失败，请重试"
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
    public ResponseEntity<Video> getVideo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 安全：验证用户是否有权访问该视频
        if (!canAccessVideo(id, userId)) {
            logger.warn("User {} attempted to access video {} without permission", userId, id);
            return ResponseEntity.status(403).build();
        }

        return videoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            jakarta.servlet.http.HttpServletRequest request) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        String ipAddress = RequestUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // 安全：验证用户是否拥有该视频
        if (!canAccessVideo(id, userId)) {
            logger.warn("User {} attempted to delete video {} without permission", userId, id);
            return ResponseEntity.status(403).build();
        }

        // 删除视频文件和数据库记录
        videoRepository.findById(id).ifPresent(video -> {
            try {
                // 记录删除日志
                String username = userRepository.findById(userId).map(u -> u.getUsername()).orElse("unknown");
                systemLogService.logDelete(userId, username,
                        "删除视频: " + video.getOriginalFilename(),
                        String.format("视频ID: %d, 原始文件: %s", video.getId(), video.getOriginalPath()),
                        ipAddress, userAgent);

                // 删除原始文件
                File originalFile = new File(video.getOriginalPath());
                if (originalFile.exists()) {
                    originalFile.delete();
                }

                // 删除转码文件目录
                if (video.getHlsPath() != null) {
                    File transcodedDir = new File(video.getHlsPath()).getParentFile();
                    if (transcodedDir != null && transcodedDir.exists()) {
                        deleteDirectory(transcodedDir);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to delete video files for video {}", id, e);
            }
        });

        videoRepository.deleteById(id);
        logger.info("Video {} deleted by user {}", id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
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

    /**
     * 获取视频的检测摘要
     */
    @GetMapping("/{id}/detection-summary")
    public ResponseEntity<java.util.Map<String, Object>> getDetectionSummary(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 验证用户是否有权访问该视频
        if (!canAccessVideo(id, userId)) {
            return ResponseEntity.status(403).build();
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        // 获取该视频的预警数量
        long alertCount = alertRepository.countByVideoId(id);
        response.put("totalAlerts", alertCount);

        // 获取分析状态
        Object progressDataObj = cacheService.get("transcoding:status:" + id);
        boolean isAnalyzing = false;
        int progress = 100;

        if (progressDataObj != null) {
            String progressData = progressDataObj.toString();
            if (progressData.contains("progress")) {
                try {
                    int progressStart = progressData.indexOf("\"progress\":") + 11;
                    int progressEnd = progressData.indexOf(",", progressStart);
                    if (progressEnd == -1) progressEnd = progressData.indexOf("}", progressStart);
                    progress = Integer.parseInt(progressData.substring(progressStart, progressEnd).trim());
                    isAnalyzing = progress < 100;
                } catch (Exception e) {
                    logger.error("Failed to parse progress data", e);
                }
            }
        }

        response.put("isAnalyzing", isAnalyzing);
        response.put("progress", progress);
        response.put("hasAlerts", alertCount > 0);

        // 如果分析完成且无预警，返回正常状态
        if (!isAnalyzing && alertCount == 0) {
            response.put("status", "normal");
            response.put("message", "视频分析完成，未检测到异常");
        } else if (!isAnalyzing && alertCount > 0) {
            response.put("status", "alert");
            response.put("message", String.format("检测到 %d 个异常", alertCount));
        } else {
            response.put("status", "analyzing");
            response.put("message", "正在分析中...");
        }

        return ResponseEntity.ok(response);
    }
}
