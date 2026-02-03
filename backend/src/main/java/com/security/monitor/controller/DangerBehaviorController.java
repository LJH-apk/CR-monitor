package com.security.monitor.controller;

import com.security.monitor.entity.DangerBehavior;
import com.security.monitor.repository.DangerBehaviorRepository;
import com.security.monitor.service.CacheService;
import com.security.monitor.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<DangerBehavior> createBehavior(@RequestBody DangerBehavior behavior,
                                                          HttpServletRequest request) {
        DangerBehavior saved = dangerBehaviorRepository.save(behavior);
        invalidateCache();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        systemLogService.logConfig(null, auth.getName(),
            "创建危险行为", saved.getName(),
            String.format("ID: %d, 名称: %s, 严重级别: %d, 颜色: %s, 状态: %s",
                saved.getId(), saved.getName(), saved.getSeverityLevel(),
                saved.getColorCode(), saved.getIsActive() ? "启用" : "禁用"),
            ipAddress, userAgent);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DangerBehavior> updateBehavior(
            @PathVariable Long id,
            @RequestBody DangerBehavior behavior,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        return dangerBehaviorRepository.findById(id)
                .map(existing -> {
                    StringBuilder changes = new StringBuilder();
                    if (behavior.getName() != null && !behavior.getName().equals(existing.getName())) {
                        changes.append(String.format("名称: %s -> %s; ", existing.getName(), behavior.getName()));
                        existing.setName(behavior.getName());
                    }
                    if (behavior.getDescription() != null) {
                        existing.setDescription(behavior.getDescription());
                    }
                    if (behavior.getSeverityLevel() != null && !behavior.getSeverityLevel().equals(existing.getSeverityLevel())) {
                        changes.append(String.format("严重级别: %d -> %d; ", existing.getSeverityLevel(), behavior.getSeverityLevel()));
                        existing.setSeverityLevel(behavior.getSeverityLevel());
                    }
                    if (behavior.getColorCode() != null) {
                        existing.setColorCode(behavior.getColorCode());
                    }
                    if (behavior.getIsActive() != null && !behavior.getIsActive().equals(existing.getIsActive())) {
                        changes.append(String.format("状态: %s -> %s; ", existing.getIsActive() ? "启用" : "禁用", behavior.getIsActive() ? "启用" : "禁用"));
                        existing.setIsActive(behavior.getIsActive());
                    }
                    DangerBehavior updated = dangerBehaviorRepository.save(existing);
                    invalidateCache();
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    systemLogService.logConfig(null, auth.getName(),
                        "更新危险行为", existing.getName(),
                        String.format("ID: %d, 变更: %s", id, changes.length() > 0 ? changes.toString() : "无字段变更"),
                        ipAddress, userAgent);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBehavior(@PathVariable Long id, HttpServletRequest request) {
        DangerBehavior behavior = dangerBehaviorRepository.findById(id).orElse(null);
        if (behavior != null) {
            String behaviorName = behavior.getName();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            dangerBehaviorRepository.deleteById(id);
            invalidateCache();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            systemLogService.logConfig(null, auth.getName(),
                "删除危险行为", behaviorName,
                String.format("ID: %d, 名称: %s, 严重级别: %d", id, behaviorName, behavior.getSeverityLevel()),
                ipAddress, userAgent);
        }
        return ResponseEntity.ok().build();
    }

    private void invalidateCache() {
        cacheService.delete("config:danger_behaviors");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
