package com.security.monitor.service;

import com.security.monitor.entity.Video;
import com.security.monitor.repository.VideoRepository;
import com.security.monitor.util.FFmpegUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private FFmpegUtil ffmpegUtil;

    @Autowired
    private CacheService cacheService;

    @Value("${storage.transcoded}")
    private String transcodedPath;

    @Async("videoTaskExecutor")
    public CompletableFuture<Void> transcodeVideo(Long videoId) {
        logger.info("Starting transcoding for video ID: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        video.setStatus("TRANSCODING");
        video.setTranscodingStartedAt(LocalDateTime.now());
        videoRepository.save(video);

        try {
            String inputPath = video.getOriginalPath();
            String outputDir = transcodedPath + "/" + videoId;

            // Transcode to HLS
            ffmpegUtil.transcodeToHLS(inputPath, outputDir);

            // Generate thumbnail
            String thumbnailPath = ffmpegUtil.generateThumbnail(inputPath, outputDir);

            // Get video duration
            int duration = ffmpegUtil.getVideoDuration(inputPath);

            // Update video entity
            video.setStatus("READY");
            video.setHlsPath(outputDir + "/playlist.m3u8");
            video.setThumbnailPath(thumbnailPath);
            video.setDuration(duration);
            video.setTranscodingCompletedAt(LocalDateTime.now());
            videoRepository.save(video);

            // Cache video metadata
            cacheService.set("video:" + videoId, video, 1, java.util.concurrent.TimeUnit.HOURS);

            logger.info("Transcoding completed for video ID: {}", videoId);

            // 不再自动启动帧分析，改为播放时实时分析

        } catch (Exception e) {
            logger.error("Transcoding failed for video ID: {}", videoId, e);
            video.setStatus("FAILED");
            videoRepository.save(video);
            throw new RuntimeException("Transcoding failed", e);
        }

        return CompletableFuture.completedFuture(null);
    }
}
