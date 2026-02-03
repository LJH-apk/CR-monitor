package com.security.monitor.controller;

import com.security.monitor.entity.DangerBehavior;
import com.security.monitor.repository.DangerBehaviorRepository;
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
@RequestMapping("/admin/danger-behaviors")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEVELOPER')")
public class DangerBehaviorController {

    @Autowired
    private DangerBehaviorRepository dangerBehaviorRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SystemLogService systemLogService;

    @GetMapping
    public ResponseEntity<List<DangerBehavior>> getAllBehaviors() {
        List<DangerBehavior> behaviors = dangerBehaviorRepository.findAll();
        return ResponseEntity.ok(behaviors);
    }

    @GetMapping("/active")
    public ResponseEntity<List<DangerBehavior>> getActiveBehaviors() {
        List<DangerBehavior> behaviors = dangerBehaviorRepository.findByIsActiveTrue();
        return ResponseEntity.ok(behaviors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DangerBehavior> getBehavior(@PathVariable Long id) {
        return dangerBehaviorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DangerBehavior> createBehavior(@RequestBody DangerBehavior behavior) {
        DangerBehavior saved = dangerBehaviorRepository.save(behavior);
        invalidateCache();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        systemLogService.logConfig(null, auth.getName(),
            "创建危险行为", saved.getName(),
            String.format("ID: %d, 严重级别: %d", saved.getId(), saved.getSeverityLevel()));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DangerBehavior> updateBehavior(
            @PathVariable Long id,
            @RequestBody DangerBehavior behavior) {

        return dangerBehaviorRepository.findById(id)
                .map(existing -> {
                    if (behavior.getName() != null) {
                        existing.setName(behavior.getName());
                    }
                    if (behavior.getDescription() != null) {
                        existing.setDescription(behavior.getDescription());
                    }
                    if (behavior.getSeverityLevel() != null) {
                        existing.setSeverityLevel(behavior.getSeverityLevel());
                    }
                    if (behavior.getColorCode() != null) {
                        existing.setColorCode(behavior.getColorCode());
                    }
                    if (behavior.getIsActive() != null) {
                        existing.setIsActive(behavior.getIsActive());
                    }
                    DangerBehavior updated = dangerBehaviorRepository.save(existing);
                    invalidateCache();
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    systemLogService.logConfig(null, auth.getName(),
                        "更新危险行为", existing.getName(),
                        String.format("ID: %d", id));
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBehavior(@PathVariable Long id) {
        DangerBehavior behavior = dangerBehaviorRepository.findById(id).orElse(null);
        if (behavior != null) {
            String behaviorName = behavior.getName();
            dangerBehaviorRepository.deleteById(id);
            invalidateCache();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logConfig(null, auth.getName(),
                "删除危险行为", behaviorName,
                String.format("ID: %d", id));
        }
        return ResponseEntity.ok().build();
    }

    private void invalidateCache() {
        cacheService.delete("config:danger_behaviors");
    }
}
