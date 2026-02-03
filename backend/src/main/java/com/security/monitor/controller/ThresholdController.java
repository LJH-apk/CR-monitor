package com.security.monitor.controller;

import com.security.monitor.entity.AlertThreshold;
import com.security.monitor.repository.AlertThresholdRepository;
import com.security.monitor.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertThreshold> updateThreshold(
            @PathVariable Long id,
            @RequestBody AlertThreshold threshold) {

        return thresholdRepository.findById(id)
                .map(existing -> {
                    existing.setConfidenceThreshold(threshold.getConfidenceThreshold());
                    existing.setTimeWindowSeconds(threshold.getTimeWindowSeconds());
                    existing.setMaxAlertsPerWindow(threshold.getMaxAlertsPerWindow());
                    existing.setIsActive(threshold.getIsActive());
                    AlertThreshold updated = thresholdRepository.save(existing);
                    invalidateCache(existing.getDangerBehaviorId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThreshold(@PathVariable Long id) {
        thresholdRepository.findById(id).ifPresent(threshold -> {
            thresholdRepository.deleteById(id);
            invalidateCache(threshold.getDangerBehaviorId());
        });
        return ResponseEntity.ok().build();
    }

    private void invalidateCache(Long behaviorId) {
        cacheService.delete("config:thresholds:" + behaviorId);
    }
}
