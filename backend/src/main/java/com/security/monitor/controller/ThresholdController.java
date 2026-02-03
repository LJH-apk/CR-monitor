package com.security.monitor.controller;

import com.security.monitor.entity.AlertThreshold;
import com.security.monitor.repository.AlertThresholdRepository;
import com.security.monitor.service.CacheService;
import com.security.monitor.service.SystemLogService;
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
    public ResponseEntity<AlertThreshold> createThreshold(@RequestBody AlertThreshold threshold) {
        AlertThreshold saved = thresholdRepository.save(threshold);
        invalidateCache(threshold.getDangerBehaviorId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        systemLogService.logConfig(null, auth.getName(),
            "创建告警阈值", "行为ID:" + saved.getDangerBehaviorId(),
            String.format("阈值ID: %d, 置信度: %.2f", saved.getId(), saved.getConfidenceThreshold()));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertThreshold> updateThreshold(
            @PathVariable Long id,
            @RequestBody AlertThreshold threshold) {

        return thresholdRepository.findById(id)
                .map(existing -> {
                    if (threshold.getConfidenceThreshold() != null) {
                        existing.setConfidenceThreshold(threshold.getConfidenceThreshold());
                    }
                    if (threshold.getTimeWindowSeconds() != null) {
                        existing.setTimeWindowSeconds(threshold.getTimeWindowSeconds());
                    }
                    if (threshold.getMaxAlertsPerWindow() != null) {
                        existing.setMaxAlertsPerWindow(threshold.getMaxAlertsPerWindow());
                    }
                    if (threshold.getIsActive() != null) {
                        existing.setIsActive(threshold.getIsActive());
                    }
                    AlertThreshold updated = thresholdRepository.save(existing);
                    invalidateCache(existing.getDangerBehaviorId());
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    systemLogService.logConfig(null, auth.getName(),
                        "更新告警阈值", "阈值ID:" + id,
                        String.format("置信度: %.2f, 时间窗口: %ds",
                            existing.getConfidenceThreshold(), existing.getTimeWindowSeconds()));
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThreshold(@PathVariable Long id) {
        thresholdRepository.findById(id).ifPresent(threshold -> {
            Long behaviorId = threshold.getDangerBehaviorId();
            thresholdRepository.deleteById(id);
            invalidateCache(behaviorId);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logConfig(null, auth.getName(),
                "删除告警阈值", "阈值ID:" + id,
                String.format("行为ID: %d", behaviorId));
        });
        return ResponseEntity.ok().build();
    }

    private void invalidateCache(Long behaviorId) {
        cacheService.delete("config:thresholds:" + behaviorId);
    }
}
