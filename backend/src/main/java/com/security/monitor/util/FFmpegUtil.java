package com.security.monitor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FFmpegUtil {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegUtil.class);

    // 安全：只允许字母、数字、下划线、连字符、点和斜杠
    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-./]+$");

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${ffmpeg.hls-time}")
    private Integer hlsTime;

    @Value("${ffmpeg.preset}")
    private String preset;

    /**
     * 验证文件路径安全性，防止路径遍历和命令注入
     */
    private void validatePath(String path) throws SecurityException {
        if (path == null || path.trim().isEmpty()) {
            throw new SecurityException("路径不能为空");
        }

        // 规范化路径
        Path normalizedPath = Paths.get(path).normalize();
        String pathStr = normalizedPath.toString();

        // 检查路径遍历
        if (pathStr.contains("..")) {
            throw new SecurityException("检测到路径遍历攻击");
        }

        // 检查危险字符
        if (pathStr.contains(";") || pathStr.contains("|") ||
            pathStr.contains("&") || pathStr.contains("`") ||
            pathStr.contains("$") || pathStr.contains("(") ||
            pathStr.contains(")") || pathStr.contains("<") ||
            pathStr.contains(">") || pathStr.contains("'") ||
            pathStr.contains("\"") || pathStr.contains("\\")) {
            throw new SecurityException("路径包含非法字符");
        }
    }

    public void transcodeToHLS(String inputPath, String outputDir) throws Exception {
        // 安全验证：验证输入路径和输出目录
        validatePath(inputPath);
        validatePath(outputDir);

        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("输入文件不存在或不是有效文件");
        }

        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String outputPath = outputDir + "/playlist.m3u8";

        // 安全：使用参数数组而不是shell命令字符串
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(inputPath);
        command.add("-codec:");
        command.add("copy");
        command.add("-start_number");
        command.add("0");
        command.add("-hls_time");
        command.add(String.valueOf(hlsTime));
        command.add("-hls_list_size");
        command.add("0");
        command.add("-f");
        command.add("hls");
        command.add(outputPath);

        logger.info("Executing FFmpeg transcoding for: {}", inputPath);

        // 直接使用参数数组，不通过shell
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("FFmpeg output: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg transcoding failed with exit code: " + exitCode);
        }

        logger.info("Transcoding completed successfully");
    }

    public String generateThumbnail(String inputPath, String outputDir) throws Exception {
        // 安全验证
        validatePath(inputPath);
        validatePath(outputDir);

        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("输入文件不存在或不是有效文件");
        }

        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String thumbnailPath = outputDir + "/thumbnail.jpg";

        // 安全：使用参数数组
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(inputPath);
        command.add("-ss");
        command.add("00:00:01");
        command.add("-vframes");
        command.add("1");
        command.add(thumbnailPath);

        logger.info("Generating thumbnail for: {}", inputPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Thumbnail generation failed with exit code: " + exitCode);
        }

        logger.info("Thumbnail generated successfully");
        return thumbnailPath;
    }

    public int getVideoDuration(String inputPath) throws Exception {
        // 安全验证
        validatePath(inputPath);

        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("输入文件不存在或不是有效文件");
        }

        // 使用ffprobe获取视频时长（更安全和可靠）
        String ffprobePath = ffmpegPath.replace("ffmpeg", "ffprobe");

        List<String> command = new ArrayList<>();
        command.add(ffprobePath);
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("format=duration");
        command.add("-of");
        command.add("default=noprint_wrappers=1:nokey=1");
        command.add(inputPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String durationStr = reader.readLine();
            if (durationStr != null && !durationStr.isEmpty()) {
                return (int) Double.parseDouble(durationStr.trim());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to get video duration with exit code: " + exitCode);
        }

        return 0;
    }
}
