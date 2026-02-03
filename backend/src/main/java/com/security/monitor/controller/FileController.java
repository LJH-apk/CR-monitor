package com.security.monitor.controller;

import com.security.monitor.entity.TrainingSample;
import com.security.monitor.entity.Video;
import com.security.monitor.repository.TrainingSampleRepository;
import com.security.monitor.repository.VideoRepository;
import com.security.monitor.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问控制器
 * 安全：所有文件访问都需要认证和授权
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private TrainingSampleRepository trainingSampleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${storage.base-path}")
    private String storageBasePath;

    /**
     * 安全：访问视频文件（需要验证用户权限）
     */
    @GetMapping("/videos/{videoId}/{filename}")
    public ResponseEntity<Resource> getVideoFile(
            @PathVariable Long videoId,
            @PathVariable String filename,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        // 安全：验证用户是否有权访问该视频
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            logger.warn("Video not found: {}", videoId);
            return ResponseEntity.notFound().build();
        }

        // 管理员、超级管理员、开发者可以访问所有视频，普通用户只能访问自己的视频
        boolean isAdmin = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "DEVELOPER".equals(role);
        if (!isAdmin && !video.getUserId().equals(userId)) {
            logger.warn("User {} attempted to access video {} without permission", userId, videoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 安全：验证文件路径，防止路径遍历
        try {
            Path basePath = Paths.get(storageBasePath).normalize();
            Path filePath = basePath.resolve("transcoded/" + videoId + "/" + filename).normalize();

            // 确保文件路径在存储目录内
            if (!filePath.startsWith(basePath)) {
                logger.warn("Path traversal attempt detected: {}", filename);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                logger.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            // 设置正确的Content-Type
            MediaType mediaType = getMediaType(filename);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error accessing file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 安全：访问缩略图（需要验证用户权限）
     */
    @GetMapping("/thumbnails/{videoId}/thumbnail.jpg")
    public ResponseEntity<Resource> getThumbnail(
            @PathVariable Long videoId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        // 安全：验证用户是否有权访问该视频
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            return ResponseEntity.notFound().build();
        }

        // 管理员、超级管理员、开发者可以访问所有视频，普通用户只能访问自己的视频
        boolean isAdmin = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "DEVELOPER".equals(role);
        if (!isAdmin && !video.getUserId().equals(userId)) {
            logger.warn("User {} attempted to access thumbnail for video {} without permission", userId, videoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path basePath = Paths.get(storageBasePath).normalize();
            Path filePath = basePath.resolve("transcoded/" + videoId + "/thumbnail.jpg").normalize();

            if (!filePath.startsWith(basePath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error accessing thumbnail for video: {}", videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 安全：访问训练样本图片（需要认证，但所有认证用户都可访问）
     */
    @GetMapping("/samples/{sampleId}")
    public ResponseEntity<Resource> getTrainingSample(
            @PathVariable Long sampleId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        // 安全：验证样本是否存在
        TrainingSample sample = trainingSampleRepository.findById(sampleId).orElse(null);
        if (sample == null) {
            logger.warn("Training sample not found: {}", sampleId);
            return ResponseEntity.notFound().build();
        }

        // 安全：验证文件路径，防止路径遍历
        try {
            Path basePath = Paths.get(storageBasePath).normalize();
            Path filePath = basePath.resolve("training-samples/" + sample.getUserId() + "/" + sample.getStoredFilename()).normalize();

            // 确保文件路径在存储目录内
            if (!filePath.startsWith(basePath)) {
                logger.warn("Path traversal attempt detected for sample: {}", sampleId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                logger.warn("Sample file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            // 根据文件扩展名设置Content-Type
            MediaType mediaType = getMediaType(sample.getStoredFilename());

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sample.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error accessing training sample: {}", sampleId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件扩展名确定MediaType
     */
    private MediaType getMediaType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        switch (extension) {
            case "m3u8":
                return MediaType.parseMediaType("application/vnd.apple.mpegurl");
            case "ts":
                return MediaType.parseMediaType("video/mp2t");
            case "mp4":
                return MediaType.parseMediaType("video/mp4");
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
