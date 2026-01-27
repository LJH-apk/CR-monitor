package com.security.monitor.service;

import com.security.monitor.dto.AIDetectionResponse;
import com.security.monitor.dto.AIDetectionResult;
import com.security.monitor.dto.AIAlert;
import com.security.monitor.dto.ExtractedFrame;
import com.security.monitor.entity.Alert;
import com.security.monitor.entity.AlertStatistic;
import com.security.monitor.entity.AlertThreshold;
import com.security.monitor.entity.DangerBehavior;
import com.security.monitor.entity.Video;
import com.security.monitor.repository.AlertRepository;
import com.security.monitor.repository.AlertStatisticRepository;
import com.security.monitor.repository.AlertThresholdRepository;
import com.security.monitor.repository.DangerBehaviorRepository;
import com.security.monitor.repository.VideoRepository;
import com.security.monitor.util.FrameExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class FrameAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(FrameAnalysisService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private DangerBehaviorRepository dangerBehaviorRepository;

    @Autowired
    private AlertThresholdRepository alertThresholdRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertStatisticRepository alertStatisticRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private AlertPushService alertPushService;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Autowired
    private FrameExtractor frameExtractor;

    @Value("${frame-extraction.min-interval-seconds}")
    private int minIntervalSeconds;

    // 记录每个视频的最后分析时间
    private Map<Long, Long> lastAnalysisTime = new ConcurrentHashMap<>();

    // 关键词到危险行为的映射
    private Map<String, DangerBehavior> keywordToBehaviorMap = new HashMap<>();

    @Async("analysisTaskExecutor")
    public CompletableFuture<Void> analyzeVideo(Long videoId) {
        logger.info("开始AI分析视频: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("视频不存在"));

        // 检查AI服务是否可用
        if (!aiServiceClient.isHealthy()) {
            logger.error("AI服务不可用，跳过分析");
            return CompletableFuture.completedFuture(null);
        }

        try {
            // 1. 提取视频帧
            logger.info("开始提取视频帧: {}", video.getOriginalPath());
            List<ExtractedFrame> frames = frameExtractor.extractFrames(video.getOriginalPath());
            logger.info("成功提取 {} 帧", frames.size());

            // 2. 初始化关键词映射
            initializeKeywordMapping();

            // 3. 对每一帧进行AI检测
            int processedFrames = 0;
            for (ExtractedFrame frame : frames) {
                // 检查时间间隔
                if (!shouldAnalyze(videoId)) {
                    continue;
                }

                try {
                    // 调用AI服务
                    AIDetectionResponse response = aiServiceClient.detect(frame.getBase64Image());

                    // 处理检测结果
                    if (response != null && response.getData() != null) {
                        processDetectionResults(video, frame, response);
                    }

                    // 更新最后分析时间
                    lastAnalysisTime.put(videoId, System.currentTimeMillis());
                    processedFrames++;

                    // 更新进度
                    if (processedFrames % 5 == 0) {
                        updateAnalysisProgress(videoId, processedFrames, frames.size());
                    }

                } catch (Exception e) {
                    logger.error("帧分析失败: {}", frame.getFileName(), e);
                }
            }

            logger.info("视频分析完成: {}, 处理了 {} 帧", videoId, processedFrames);

        } catch (Exception e) {
            logger.error("视频分析失败: {}", videoId, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 处理AI检测结果
     */
    private void processDetectionResults(Video video, ExtractedFrame frame, AIDetectionResponse response) {
        logger.info("处理AI检测结果，共 {} 个检测对象", response.getData().size());

        for (AIDetectionResult result : response.getData()) {
            logger.debug("检测对象: detection={}, analysis={}",
                result.getDetection(),
                result.getAnalysis() != null ? result.getAnalysis().getAnalysis() : "null");

            // 检查是否有预警
            if (result.getAlerts() != null && !result.getAlerts().isEmpty()) {
                logger.info("检测到 {} 个预警", result.getAlerts().size());

                for (AIAlert aiAlert : result.getAlerts()) {
                    logger.info("处理预警: keyword={}, severity={}, score={}",
                        aiAlert.getKeyword(), aiAlert.getSeverity(), aiAlert.getCombinedScore());

                    try {
                        // 创建预警记录
                        Alert alert = createAlertFromAI(video, frame, result, aiAlert);
                        logger.info("创建预警对象: dangerBehaviorId={}, confidence={}",
                            alert.getDangerBehaviorId(), alert.getConfidence());

                        // 检查阈值和限流
                        if (alert.getDangerBehaviorId() != null) {
                            AlertThreshold threshold = alertThresholdRepository
                                    .findByDangerBehaviorId(alert.getDangerBehaviorId())
                                    .orElse(null);

                            logger.info("查找阈值配置: dangerBehaviorId={}, threshold={}",
                                alert.getDangerBehaviorId(), threshold);

                            if (threshold != null && threshold.getIsActive()) {
                                logger.info("阈值检查: confidence={}, threshold={}",
                                    alert.getConfidence().doubleValue(),
                                    threshold.getConfidenceThreshold().doubleValue());

                                if (alert.getConfidence().doubleValue() >= threshold.getConfidenceThreshold().doubleValue()) {
                                    if (canCreateAlert(alert.getDangerBehaviorId(), threshold)) {
                                        // 保存预警
                                        alert = alertRepository.save(alert);

                                        logger.info("生成预警: {} - {} (置信度: {})",
                                                aiAlert.getKeyword(),
                                                aiAlert.getSeverity(),
                                                aiAlert.getCombinedScore());

                                        // 推送WebSocket预警
                                        alertPushService.pushAlert(alert, video.getUserId());

                                        // 更新统计
                                        updateStatistics(alert);
                                    } else {
                                        logger.warn("预警被限流: dangerBehaviorId={}", alert.getDangerBehaviorId());
                                    }
                                } else {
                                    logger.warn("预警置信度不足: confidence={} < threshold={}",
                                        alert.getConfidence().doubleValue(),
                                        threshold.getConfidenceThreshold().doubleValue());
                                }
                            } else {
                                logger.warn("阈值配置无效或未激活: threshold={}", threshold);
                            }
                        } else {
                            // 没有匹配到危险行为，仍然保存预警但不推送
                            alert = alertRepository.save(alert);
                            logger.info("生成未分类预警: {}", aiAlert.getKeyword());
                        }

                    } catch (Exception e) {
                        logger.error("创建预警失败", e);
                    }
                }
            } else {
                logger.debug("该检测对象没有预警");
            }
        }
    }

    /**
     * 从AI检测结果创建预警
     */
    private Alert createAlertFromAI(Video video, ExtractedFrame frame,
                                    AIDetectionResult result, AIAlert aiAlert) {
        Alert alert = new Alert();
        alert.setVideoId(video.getId());
        alert.setTimestampInVideo((int) (frame.getTimestampMs() / 1000));  // 转换为秒
        alert.setConfidence(BigDecimal.valueOf(aiAlert.getCombinedScore()));
        alert.setSeverityLevel(mapSeverity(aiAlert.getSeverity()));

        // 构建描述
        String description = buildDescription(result, aiAlert);
        alert.setDescription(description);

        // 根据关键词匹配危险行为
        DangerBehavior behavior = findMatchingBehavior(aiAlert.getKeyword());
        if (behavior != null) {
            alert.setDangerBehaviorId(behavior.getId());
        }

        alert.setIsAcknowledged(false);

        return alert;
    }

    /**
     * 初始化关键词到危险行为的映射
     */
    private void initializeKeywordMapping() {
        if (keywordToBehaviorMap.isEmpty()) {
            List<DangerBehavior> behaviors = dangerBehaviorRepository.findByIsActiveTrue();

            // 简单的关键词映射（实际应该从数据库配置）
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

            logger.info("初始化关键词映射，共 {} 个关键词", keywordToBehaviorMap.size());
        }
    }

    /**
     * 根据关键词查找匹配的危险行为
     */
    private DangerBehavior findMatchingBehavior(String keyword) {
        return keywordToBehaviorMap.get(keyword);
    }

    /**
     * 映射严重等级（返回Integer：1=LOW, 2=MEDIUM, 3=HIGH）
     */
    private Integer mapSeverity(String aiSeverity) {
        switch (aiSeverity.toLowerCase()) {
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            default:
                return 2;  // 默认为MEDIUM
        }
    }

    /**
     * 构建预警描述
     */
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

    /**
     * 检查是否应该进行分析（时间间隔控制）
     */
    private boolean shouldAnalyze(Long videoId) {
        Long lastTime = lastAnalysisTime.get(videoId);
        if (lastTime == null) {
            return true;
        }

        long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
        return elapsed >= minIntervalSeconds;
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

    private void updateStatistics(Alert alert) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();

        AlertStatistic statistic = alertStatisticRepository
                .findByDateAndHourAndDangerBehaviorId(date, hour, alert.getDangerBehaviorId())
                .orElse(new AlertStatistic());

        if (statistic.getId() == null) {
            statistic.setDate(date);
            statistic.setHour(hour);
            statistic.setDangerBehaviorId(alert.getDangerBehaviorId());
            statistic.setAlertCount(1);
            statistic.setAvgConfidence(alert.getConfidence());
        } else {
            int newCount = statistic.getAlertCount() + 1;
            BigDecimal newAvg = statistic.getAvgConfidence()
                    .multiply(BigDecimal.valueOf(statistic.getAlertCount()))
                    .add(alert.getConfidence())
                    .divide(BigDecimal.valueOf(newCount), 2, BigDecimal.ROUND_HALF_UP);

            statistic.setAlertCount(newCount);
            statistic.setAvgConfidence(newAvg);
        }

        alertStatisticRepository.save(statistic);
    }

    private void updateAnalysisProgress(Long videoId, int current, int total) {
        int progress = (int) ((current / (double) total) * 100);
        cacheService.set("transcoding:status:" + videoId,
                String.format("{\"progress\": %d, \"status\": \"analyzing\"}", progress),
                2, TimeUnit.HOURS);
    }
}
