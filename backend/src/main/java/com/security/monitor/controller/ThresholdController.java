package com.security.monitor.controller;

import com.security.monitor.entity.AlertThreshold;
import com.security.monitor.repository.AlertThresholdRepository;
import com.security.monitor.service.CacheService;
import com.security.monitor.service.SystemLogService;
import com.security.monitor.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/thresholds")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEVELOPER')")
public class ThresholdController {

    @Autowired
    private AlertThresholdRepository thresholdRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SystemLogService systemLogService;

    @GetMapping
    public ResponseEntity<List<AlertThreshold>> getAllThresholds() {
        List<AlertThreshold> thresholds = thresholdRepository.findAll();
        return ResponseEntity.ok(thresholds);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertThreshold> getThreshold(@PathVariable Long id) {
        return thresholdRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/behavior/{behaviorId}")
    public ResponseEntity<AlertThreshold> getThresholdByBehavior(@PathVariable Long behaviorId) {
        return thresholdRepository.findByDangerBehaviorId(behaviorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AlertThreshold> createThreshold(@RequestBody AlertThreshold threshold,
                                                           HttpServletRequest request) {
        AlertThreshold saved = thresholdRepository.save(threshold);
        invalidateCache(threshold.getDangerBehaviorId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ipAddress = RequestUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        systemLogService.logConfig(null, auth.getName(),
            "创建告警阈值", "行为ID:" + saved.getDangerBehaviorId(),
            String.format("阈值ID: %d, 置信度: %.2f, 时间窗口: %ds, 最大告警数: %d, 状态: %s",
                saved.getId(), saved.getConfidenceThreshold(), saved.getTimeWindowSeconds(),
                saved.getMaxAlertsPerWindow(), saved.getIsActive() ? "启用" : "禁用"),
            ipAddress, userAgent);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertThreshold> updateThreshold(
            @PathVariable Long id,
            @RequestBody AlertThreshold threshold,
            HttpServletRequest request) {

        String ipAddress = RequestUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        return thresholdRepository.findById(id)
                .map(existing -> {
                    StringBuilder changes = new StringBuilder();
                    if (threshold.getConfidenceThreshold() != null &&
                        !threshold.getConfidenceThreshold().equals(existing.getConfidenceThreshold())) {
                        changes.append(String.format("置信度: %.2f -> %.2f; ",
                            existing.getConfidenceThreshold(), threshold.getConfidenceThreshold()));
                        existing.setConfidenceThreshold(threshold.getConfidenceThreshold());
                    }
                    if (threshold.getTimeWindowSeconds() != null &&
                        !threshold.getTimeWindowSeconds().equals(existing.getTimeWindowSeconds())) {
                        changes.append(String.format("时间窗口: %ds -> %ds; ",
                            existing.getTimeWindowSeconds(), threshold.getTimeWindowSeconds()));
                        existing.setTimeWindowSeconds(threshold.getTimeWindowSeconds());
                    }
                    if (threshold.getMaxAlertsPerWindow() != null &&
                        !threshold.getMaxAlertsPerWindow().equals(existing.getMaxAlertsPerWindow())) {
                        changes.append(String.format("最大告警数: %d -> %d; ",
                            existing.getMaxAlertsPerWindow(), threshold.getMaxAlertsPerWindow()));
                        existing.setMaxAlertsPerWindow(threshold.getMaxAlertsPerWindow());
                    }
                    if (threshold.getIsActive() != null &&
                        !threshold.getIsActive().equals(existing.getIsActive())) {
                        changes.append(String.format("状态: %s -> %s; ",
                            existing.getIsActive() ? "启用" : "禁用", threshold.getIsActive() ? "启用" : "禁用"));
                        existing.setIsActive(threshold.getIsActive());
                    }
                    AlertThreshold updated = thresholdRepository.save(existing);
                    invalidateCache(existing.getDangerBehaviorId());
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    systemLogService.logConfig(null, auth.getName(),
                        "更新告警阈值", "阈值ID:" + id,
                        String.format("行为ID: %d, 变更: %s", existing.getDangerBehaviorId(),
                            changes.length() > 0 ? changes.toString() : "无字段变更"),
                        ipAddress, userAgent);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThreshold(@PathVariable Long id, HttpServletRequest request) {
        thresholdRepository.findById(id).ifPresent(threshold -> {
            Long behaviorId = threshold.getDangerBehaviorId();
            String ipAddress = RequestUtil.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            thresholdRepository.deleteById(id);
            invalidateCache(behaviorId);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logConfig(null, auth.getName(),
                "删除告警阈值", "阈值ID:" + id,
                String.format("行为ID: %d, 置信度: %.2f, 时间窗口: %ds",
                    behaviorId, threshold.getConfidenceThreshold(), threshold.getTimeWindowSeconds()),
                ipAddress, userAgent);
        });
        return ResponseEntity.ok().build();
    }

    private void invalidateCache(Long behaviorId) {
        cacheService.delete("config:thresholds:" + behaviorId);
    }

}
