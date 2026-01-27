package com.security.monitor.util;

import com.security.monitor.dto.ExtractedFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class FrameExtractor {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${frame-extraction.interval}")
    private int frameInterval;

    /**
     * 从视频中提取关键帧
     * @param videoPath 视频文件路径
     * @return 提取的帧列表
     */
    public List<ExtractedFrame> extractFrames(String videoPath) throws Exception {
        List<ExtractedFrame> frames = new ArrayList<>();

        // 创建临时目录
        String tempDir = System.getProperty("java.io.tmpdir") + "/frames_" + System.currentTimeMillis();
        File tempDirectory = new File(tempDir);
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs();
        }

        try {
            // 使用FFmpeg提取关键帧
            String outputPattern = tempDir + "/frame_%04d.jpg";
            String command = String.format(
                    "%s -i \"%s\" -vf \"select='not(mod(n\\\\,%d))'\" -vsync vfr \"%s\"",
                    ffmpegPath, videoPath, frameInterval, outputPattern
            );

            log.info("提取视频帧: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg output: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("帧提取失败，退出码: " + exitCode);
            }

            // 读取提取的帧文件
            File[] frameFiles = tempDirectory.listFiles((dir, name) -> name.startsWith("frame_") && name.endsWith(".jpg"));
            if (frameFiles != null) {
                log.info("成功提取 {} 帧", frameFiles.length);

                for (File file : frameFiles) {
                    // 读取图像文件
                    byte[] imageBytes = Files.readAllBytes(file.toPath());
                    String base64 = Base64.getEncoder().encodeToString(imageBytes);

                    // 计算时间戳（基于文件名中的帧序号）
                    long timestamp = calculateTimestamp(file.getName());

                    frames.add(new ExtractedFrame(file.getName(), base64, timestamp));
                }
            }

        } finally {
            // 清理临时文件
            cleanupTempDirectory(tempDirectory);
        }

        return frames;
    }

    /**
     * 根据帧文件名计算时间戳
     * @param fileName 文件名，如 frame_0001.jpg
     * @return 时间戳（毫秒）
     */
    private long calculateTimestamp(String fileName) {
        try {
            // 提取帧序号
            String numberPart = fileName.replace("frame_", "").replace(".jpg", "");
            int frameNumber = Integer.parseInt(numberPart);

            // 假设视频帧率为30fps，每帧约33.33ms
            // 实际应该从视频元数据中获取帧率
            return (long) (frameNumber * frameInterval * 33.33);
        } catch (Exception e) {
            log.warn("无法计算时间戳: {}", fileName);
            return 0L;
        }
    }

    /**
     * 清理临时目录
     */
    private void cleanupTempDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            directory.delete();
            log.debug("已清理临时目录: {}", directory.getPath());
        }
    }
}
