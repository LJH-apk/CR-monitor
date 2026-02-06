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
import com.security.monitor.util.AlertUtil;
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
import java.util.ArrayList;
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

    // 连续帧检测所需的帧数（默认3帧）
    @Value("${frame-analysis.consecutive-frames:3}")
    private int consecutiveFramesRequired;

    // 检测周期帧数（默认10帧为一个周期）
    @Value("${frame-analysis.cycle-frames:10}")
    private int cycleFrames;

    // 关键词到危险行为的映射
    private Map<String, DangerBehavior> keywordToBehaviorMap = new HashMap<>();

    // 连续帧检测缓存：videoId -> (keyword -> 连续检测次数)
    private Map<Long, Map<String, Integer>> consecutiveDetectionCount = new ConcurrentHashMap<>();

    // 连续帧检测的详细信息缓存：videoId -> (keyword -> 最近的检测结果列表)
    private Map<Long, Map<String, List<PendingAlert>>> pendingAlerts = new ConcurrentHashMap<>();

    /**
     * 待确认的预警信息
     */
    private static class PendingAlert {
        ExtractedFrame frame;
        AIDetectionResult result;
        AIAlert aiAlert;

        PendingAlert(ExtractedFrame frame, AIDetectionResult result, AIAlert aiAlert) {
            this.frame = frame;
            this.result = result;
            this.aiAlert = aiAlert;
        }
    }

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

            if (frames.isEmpty()) {
                logger.warn("未提取到任何帧，跳过分析");
                return CompletableFuture.completedFuture(null);
            }

            // 2. 初始化关键词映射
            initializeKeywordMapping();

            // 3. 初始化连续帧检测缓存
            consecutiveDetectionCount.put(videoId, new ConcurrentHashMap<>());
            pendingAlerts.put(videoId, new ConcurrentHashMap<>());

            // 4. 对每一帧进行AI检测（帧已经按间隔提取，无需再做时间间隔控制）
            int processedFrames = 0;
            int cycleFrameCount = 0;  // 当前周期内已处理的帧数
            boolean cycleHasAlert = false;  // 当前周期内是否有预警

            for (ExtractedFrame frame : frames) {
                try {
                    // 调用AI服务
                    AIDetectionResponse response = aiServiceClient.detect(frame.getBase64Image());

                    // 处理检测结果
                    if (response != null && response.getData() != null) {
                        boolean hasAlert = processDetectionResultsWithConsecutive(video, frame, response);
                        if (hasAlert) {
                            cycleHasAlert = true;
                        }
                    }

                    processedFrames++;
                    cycleFrameCount++;

                    // 检查是否完成一个检测周期
                    if (cycleFrameCount >= cycleFrames) {
                        if (!cycleHasAlert) {
                            // 周期内无异常，推送绿色状态
                            alertPushService.pushNormalStatus(videoId, cycleFrameCount, video.getUserId());
                            logger.info("检测周期完成，无异常: videoId={}, frames={}", videoId, cycleFrameCount);
                        }
                        // 重置周期计数
                        cycleFrameCount = 0;
                        cycleHasAlert = false;
                        // 清理连续检测计数（新周期开始）
                        consecutiveDetectionCount.get(videoId).clear();
                        pendingAlerts.get(videoId).clear();
                    }

                    // 更新进度
                    updateAnalysisProgress(videoId, processedFrames, frames.size());

                    logger.info("已分析帧 {}/{}", processedFrames, frames.size());

                } catch (Exception e) {
                    logger.error("帧分析失败: {}", frame.getFileName(), e);
                }
            }

            // 处理最后一个不完整的周期
            if (cycleFrameCount > 0 && !cycleHasAlert) {
                alertPushService.pushNormalStatus(videoId, cycleFrameCount, video.getUserId());
                logger.info("最后检测周期完成，无异常: videoId={}, frames={}", videoId, cycleFrameCount);
            }

            // 清理缓存
            consecutiveDetectionCount.remove(videoId);
            pendingAlerts.remove(videoId);

            logger.info("视频分析完成: {}, 处理了 {} 帧", videoId, processedFrames);

        } catch (Exception e) {
            logger.error("视频分析失败: {}", videoId, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 处理AI检测结果（连续帧检测逻辑）
     * @return 是否产生了预警
     */
    private boolean processDetectionResultsWithConsecutive(Video video, ExtractedFrame frame, AIDetectionResponse response) {
        logger.info("处理AI检测结果，共 {} 个检测对象", response.getData().size());

        boolean hasAlert = false;
        Map<String, Integer> videoConsecutiveCount = consecutiveDetectionCount.get(video.getId());
        Map<String, List<PendingAlert>> videoPendingAlerts = pendingAlerts.get(video.getId());

        // 记录本帧检测到的关键词
        List<String> detectedKeywords = new ArrayList<>();

        for (AIDetectionResult result : response.getData()) {
            logger.debug("检测对象: detection={}, analysis={}",
                result.getDetection(),
                result.getAnalysis() != null ? result.getAnalysis().getAnalysis() : "null");

            // 检查是否有预警
            if (result.getAlerts() != null && !result.getAlerts().isEmpty()) {
                for (AIAlert aiAlert : result.getAlerts()) {
                    String keyword = aiAlert.getKeyword();
                    detectedKeywords.add(keyword);

                    // 增加连续检测计数
                    int count = videoConsecutiveCount.getOrDefault(keyword, 0) + 1;
                    videoConsecutiveCount.put(keyword, count);

                    // 保存待确认的预警信息
                    videoPendingAlerts.computeIfAbsent(keyword, k -> new ArrayList<>())
                            .add(new PendingAlert(frame, result, aiAlert));

                    logger.info("检测到关键词: {}, 连续次数: {}/{}", keyword, count, consecutiveFramesRequired);

                    // 检查是否达到连续帧阈值
                    if (count >= consecutiveFramesRequired) {
                        logger.info("连续 {} 帧检测到相同异常: {}", consecutiveFramesRequired, keyword);

                        // 使用最新的检测结果创建预警
                        boolean alertCreated = createAndPushAlert(video, frame, result, aiAlert);
                        if (alertCreated) {
                            hasAlert = true;
                        }

                        // 重置该关键词的计数，避免重复报警
                        videoConsecutiveCount.put(keyword, 0);
                        videoPendingAlerts.get(keyword).clear();
                    }
                }
            }
        }

        // 清理未在本帧检测到的关键词的连续计数（连续性中断）
        List<String> keysToReset = new ArrayList<>();
        for (String keyword : videoConsecutiveCount.keySet()) {
            if (!detectedKeywords.contains(keyword)) {
                keysToReset.add(keyword);
            }
        }
        for (String keyword : keysToReset) {
            if (videoConsecutiveCount.get(keyword) > 0) {
                logger.debug("关键词 {} 连续性中断，重置计数", keyword);
                videoConsecutiveCount.put(keyword, 0);
                if (videoPendingAlerts.containsKey(keyword)) {
                    videoPendingAlerts.get(keyword).clear();
                }
            }
        }

        return hasAlert;
    }

    /**
     * 创建并推送预警
     */
    private boolean createAndPushAlert(Video video, ExtractedFrame frame, AIDetectionResult result, AIAlert aiAlert) {
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
                            return true;
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
        return false;
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
            keywordToBehaviorMap = AlertUtil.buildKeywordBehaviorMap(behaviors);
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
     * 映射严重等级
     */
    private Integer mapSeverity(String aiSeverity) {
        return AlertUtil.mapSeverity(aiSeverity);
    }

    /**
     * 构建预警描述
     */
    private String buildDescription(AIDetectionResult result, AIAlert aiAlert) {
        return AlertUtil.buildDescription(result, aiAlert);
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
