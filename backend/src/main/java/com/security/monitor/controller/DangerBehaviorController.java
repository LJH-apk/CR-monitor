package com.security.monitor.controller;

import com.security.monitor.entity.DangerBehavior;
import com.security.monitor.repository.DangerBehaviorRepository;
import com.security.monitor.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DangerBehavior> updateBehavior(
            @PathVariable Long id,
            @RequestBody DangerBehavior behavior) {

        return dangerBehaviorRepository.findById(id)
                .map(existing -> {
                    existing.setName(behavior.getName());
                    existing.setDescription(behavior.getDescription());
                    existing.setSeverityLevel(behavior.getSeverityLevel());
                    existing.setColorCode(behavior.getColorCode());
                    existing.setIsActive(behavior.getIsActive());
                    DangerBehavior updated = dangerBehaviorRepository.save(existing);
                    invalidateCache();
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBehavior(@PathVariable Long id) {
        dangerBehaviorRepository.deleteById(id);
        invalidateCache();
        return ResponseEntity.ok().build();
    }

    private void invalidateCache() {
        cacheService.delete("config:danger_behaviors");
    }
}
