package com.security.monitor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component
public class FFmpegUtil {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegUtil.class);

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${ffmpeg.hls-time}")
    private Integer hlsTime;

    @Value("${ffmpeg.preset}")
    private String preset;

    public void transcodeToHLS(String inputPath, String outputDir) throws Exception {
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String outputPath = outputDir + "/playlist.m3u8";

        String command = String.format(
                "%s -i \"%s\" -codec: copy -start_number 0 -hls_time %d -hls_list_size 0 -f hls \"%s\"",
                ffmpegPath, inputPath, hlsTime, outputPath
        );

        logger.info("Executing FFmpeg command: {}", command);

        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
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
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String thumbnailPath = outputDir + "/thumbnail.jpg";

        String command = String.format(
                "%s -i \"%s\" -ss 00:00:01 -vframes 1 \"%s\"",
                ffmpegPath, inputPath, thumbnailPath
        );

        logger.info("Generating thumbnail: {}", command);

        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
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
        String command = String.format(
                "%s -i \"%s\" 2>&1 | grep Duration | awk '{print $2}' | tr -d ,",
                ffmpegPath, inputPath
        );

        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String duration = reader.readLine();
            if (duration != null && !duration.isEmpty()) {
                String[] parts = duration.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = (int) Double.parseDouble(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            }
        }

        return 0;
    }
}
