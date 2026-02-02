package com.security.monitor.service;

import com.security.monitor.dto.AIDetectionResponse;
import com.security.monitor.dto.AIDetectionResult;
import com.security.monitor.dto.AIAlert;
import com.security.monitor.entity.Alert;
import com.security.monitor.entity.AlertThreshold;
import com.security.monitor.entity.DangerBehavior;
import com.security.monitor.entity.Video;
import com.security.monitor.repository.AlertRepository;
import com.security.monitor.repository.AlertThresholdRepository;
import com.security.monitor.repository.DangerBehaviorRepository;
import com.security.monitor.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 实时视频分析服务 - 根据播放进度实时分析视频帧
 */
@Service
public class RealtimeAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeAnalysisService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private DangerBehaviorRepository dangerBehaviorRepository;

    @Autowired
    private AlertThresholdRepository alertThresholdRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private AlertPushService alertPushService;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    // 连续帧检测所需的帧数
    @Value("${frame-analysis.consecutive-frames:3}")
    private int consecutiveFramesRequired;

    // 检测周期帧数
    @Value("${frame-analysis.cycle-frames:10}")
    private int cycleFrames;

    // 分析间隔（秒）- 每隔多少秒分析一次
    @Value("${frame-analysis.realtime-interval:2}")
    private int realtimeInterval;

    // 关键词到危险行为的映射
    private Map<String, DangerBehavior> keywordToBehaviorMap = new HashMap<>();

    // 每个用户-视频的分析状态：userId_videoId -> AnalysisState
    private Map<String, AnalysisState> analysisStates = new ConcurrentHashMap<>();

    /**
     * 分析状态
     */
    private static class AnalysisState {
        long lastAnalyzedTime = -1;  // 上次分析的视频时间（秒）
        int cycleFrameCount = 0;     // 当前周期内已分析的帧数
        boolean cycleHasAlert = false;  // 当前周期内是否有预警
        Map<String, Integer> consecutiveCount = new ConcurrentHashMap<>();  // 连续检测计数
        Map<String, List<AIAlert>> pendingAlerts = new ConcurrentHashMap<>();  // 待确认预警
    }

    /**
     * 根据播放进度实时分析
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param currentTime 当前播放时间（秒）
     */
    @Async("analysisTaskExecutor")
    public void analyzeAtPlaybackTime(Long userId, Long videoId, double currentTime) {
        String stateKey = userId + "_" + videoId;
        AnalysisState state = analysisStates.computeIfAbsent(stateKey, k -> new AnalysisState());

        // 检查是否需要分析（每隔 realtimeInterval 秒分析一次）
        long currentSecond = (long) currentTime;
        if (currentSecond <= state.lastAnalyzedTime) {
            return;  // 已经分析过这个时间点
        }

        // 检查间隔
        if (state.lastAnalyzedTime >= 0 && currentSecond - state.lastAnalyzedTime < realtimeInterval) {
            return;  // 间隔太短
        }

        state.lastAnalyzedTime = currentSecond;

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || !"READY".equals(video.getStatus())) {
            return;
        }

        // 检查AI服务是否可用
        if (!aiServiceClient.isHealthy()) {
            logger.warn("AI服务不可用，跳过实时分析");
            return;
        }

        try {
            // 初始化关键词映射
            initializeKeywordMapping();

            // 提取当前时间点的帧
            String base64Frame = extractFrameAtTime(video.getOriginalPath(), currentTime);
            if (base64Frame == null) {
                return;
            }

            // 调用AI服务
            AIDetectionResponse response = aiServiceClient.detect(base64Frame);

            if (response != null && response.getData() != null) {
                boolean hasAlert = processDetectionResults(video, state, currentTime, response);

                state.cycleFrameCount++;

                // 检查是否完成一个检测周期
                if (state.cycleFrameCount >= cycleFrames) {
                    if (!state.cycleHasAlert) {
                        // 周期内无异常，推送绿色状态
                        alertPushService.pushNormalStatus(videoId, state.cycleFrameCount, userId);
                        logger.info("实时检测周期完成，无异常: videoId={}, frames={}", videoId, state.cycleFrameCount);
                    }
                    // 重置周期
                    state.cycleFrameCount = 0;
                    state.cycleHasAlert = false;
                    state.consecutiveCount.clear();
                    state.pendingAlerts.clear();
                }

                if (hasAlert) {
                    state.cycleHasAlert = true;
                }
            }

            logger.debug("实时分析完成: videoId={}, time={}s", videoId, currentSecond);

        } catch (Exception e) {
            logger.error("实时分析失败: videoId={}, time={}", videoId, currentTime, e);
        }
    }

    /**
     * 提取指定时间点的帧
     */
    private String extractFrameAtTime(String videoPath, double timeSeconds) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("frame_", ".jpg");
            String timeStr = String.format("%.2f", timeSeconds);

            String command = String.format(
                    "%s -ss %s -i \"%s\" -vframes 1 -q:v 2 -y \"%s\"",
                    ffmpegPath, timeStr, videoPath, tempFile.getAbsolutePath()
            );

            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // 消费输出
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || !tempFile.exists() || tempFile.length() == 0) {
                return null;
            }

            byte[] imageBytes = Files.readAllBytes(tempFile.toPath());
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (Exception e) {
            logger.error("提取帧失败: time={}", timeSeconds, e);
            return null;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 处理检测结果（连续帧检测逻辑）
     */
    private boolean processDetectionResults(Video video, AnalysisState state, double currentTime, AIDetectionResponse response) {
        boolean hasAlert = false;
        List<String> detectedKeywords = new ArrayList<>();

        for (AIDetectionResult result : response.getData()) {
            if (result.getAlerts() != null && !result.getAlerts().isEmpty()) {
                for (AIAlert aiAlert : result.getAlerts()) {
                    String keyword = aiAlert.getKeyword();
                    detectedKeywords.add(keyword);

                    // 增加连续检测计数
                    int count = state.consecutiveCount.getOrDefault(keyword, 0) + 1;
                    state.consecutiveCount.put(keyword, count);

                    // 保存待确认的预警
                    state.pendingAlerts.computeIfAbsent(keyword, k -> new ArrayList<>()).add(aiAlert);

                    logger.info("实时检测到关键词: {}, 连续次数: {}/{}", keyword, count, consecutiveFramesRequired);

                    // 检查是否达到连续帧阈值
                    if (count >= consecutiveFramesRequired) {
                        logger.info("连续 {} 帧检测到相同异常: {}", consecutiveFramesRequired, keyword);

                        boolean alertCreated = createAndPushAlert(video, currentTime, result, aiAlert);
                        if (alertCreated) {
                            hasAlert = true;
                        }

                        // 重置计数
                        state.consecutiveCount.put(keyword, 0);
                        state.pendingAlerts.get(keyword).clear();
                    }
                }
            }
        }

        // 清理未检测到的关键词的连续计数
        for (String keyword : new ArrayList<>(state.consecutiveCount.keySet())) {
            if (!detectedKeywords.contains(keyword) && state.consecutiveCount.get(keyword) > 0) {
                state.consecutiveCount.put(keyword, 0);
                if (state.pendingAlerts.containsKey(keyword)) {
                    state.pendingAlerts.get(keyword).clear();
                }
            }
        }

        return hasAlert;
    }

    /**
     * 创建并推送预警
     */
    private boolean createAndPushAlert(Video video, double currentTime, AIDetectionResult result, AIAlert aiAlert) {
        try {
            Alert alert = new Alert();
            alert.setVideoId(video.getId());
            alert.setTimestampInVideo((int) currentTime);
            alert.setConfidence(BigDecimal.valueOf(aiAlert.getCombinedScore()));
            alert.setSeverityLevel(mapSeverity(aiAlert.getSeverity()));
            alert.setDescription(buildDescription(result, aiAlert));
            alert.setIsAcknowledged(false);

            // 匹配危险行为
            DangerBehavior behavior = keywordToBehaviorMap.get(aiAlert.getKeyword());
            if (behavior != null) {
                alert.setDangerBehaviorId(behavior.getId());
            }

            // 检查阈值
            if (alert.getDangerBehaviorId() != null) {
                AlertThreshold threshold = alertThresholdRepository
                        .findByDangerBehaviorId(alert.getDangerBehaviorId())
                        .orElse(null);

                if (threshold != null && threshold.getIsActive()) {
                    if (alert.getConfidence().doubleValue() >= threshold.getConfidenceThreshold().doubleValue()) {
                        if (canCreateAlert(alert.getDangerBehaviorId(), threshold)) {
                            alert = alertRepository.save(alert);
                            alertPushService.pushAlert(alert, video.getUserId());
                            logger.info("实时生成预警: {} (置信度: {})", aiAlert.getKeyword(), aiAlert.getCombinedScore());
                            return true;
                        }
                    }
                }
            } else {
                // 未分类预警
                alert = alertRepository.save(alert);
                logger.info("实时生成未分类预警: {}", aiAlert.getKeyword());
            }

        } catch (Exception e) {
            logger.error("创建预警失败", e);
        }
        return false;
    }

    /**
     * 重置用户-视频的分析状态（视频切换或停止播放时调用）
     */
    public void resetAnalysisState(Long userId, Long videoId) {
        String stateKey = userId + "_" + videoId;
        analysisStates.remove(stateKey);
        logger.info("重置分析状态: userId={}, videoId={}", userId, videoId);
    }

    /**
     * 初始化关键词映射
     */
    private void initializeKeywordMapping() {
        if (keywordToBehaviorMap.isEmpty()) {
            List<DangerBehavior> behaviors = dangerBehaviorRepository.findByIsActiveTrue();

            Map<String, String> keywordMap = new HashMap<>();
            keywordMap.put("打架", "Fighting");
            keywordMap.put("斗殴", "Fighting");
            keywordMap.put("争执", "Fighting");
            keywordMap.put("推搡", "Fighting");
            keywordMap.put("拉扯", "Fighting");
            keywordMap.put("冲突", "Fighting");
            keywordMap.put("摔倒", "Falling");
            keywordMap.put("晕倒", "Falling");
            keywordMap.put("非法进入", "Intrusion");
            keywordMap.put("翻越", "Intrusion");
            keywordMap.put("吸烟", "Smoking");
            keywordMap.put("烟火", "Fire");
            keywordMap.put("明火", "Fire");

            for (DangerBehavior behavior : behaviors) {
                for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
                    if (behavior.getName().equalsIgnoreCase(entry.getValue())) {
                        keywordToBehaviorMap.put(entry.getKey(), behavior);
                    }
                }
            }
        }
    }

    private Integer mapSeverity(String aiSeverity) {
        switch (aiSeverity.toLowerCase()) {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 2;
        }
    }

    private String buildDescription(AIDetectionResult result, AIAlert aiAlert) {
        StringBuilder desc = new StringBuilder();
        desc.append("检测到异常行为: ").append(aiAlert.getKeyword());

        if (result.getAnalysis() != null && result.getAnalysis().getAnalysis() != null) {
            String analysis = result.getAnalysis().getAnalysis();
            if (analysis.length() > 100) {
                analysis = analysis.substring(0, 100) + "...";
            }
            desc.append("\n分析: ").append(analysis);
        }

        desc.append("\n类别: ").append(aiAlert.getCategory());
        desc.append("\n严重等级: ").append(aiAlert.getSeverity());
        desc.append(String.format("\n置信度: %.2f%%", aiAlert.getCombinedScore() * 100));

        return desc.toString();
    }

    private boolean canCreateAlert(Long behaviorId, AlertThreshold threshold) {
        String rateKey = "alert:rate:" + behaviorId + ":" + System.currentTimeMillis() / 1000 / threshold.getTimeWindowSeconds();
        Long count = cacheService.getCounter(rateKey);

        if (count >= threshold.getMaxAlertsPerWindow()) {
            return false;
        }

        cacheService.increment(rateKey);
        cacheService.expire(rateKey, threshold.getTimeWindowSeconds(), TimeUnit.SECONDS);
        return true;
    }
}
