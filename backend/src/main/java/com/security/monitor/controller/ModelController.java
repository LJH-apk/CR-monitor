package com.security.monitor.controller;

import com.security.monitor.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/admin/models")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEVELOPER')")
public class ModelController {

    @Value("${ai-service.url:http://localhost:5001}")
    private String aiServiceUrl;

    @Autowired
    private SystemLogService systemLogService;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/versions")
    public ResponseEntity<?> getVersions() {
        try {
            return restTemplate.getForEntity(aiServiceUrl + "/api/models/versions", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentModel() {
        try {
            return restTemplate.getForEntity(aiServiceUrl + "/api/models/current", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }

    @PostMapping("/swap")
    public ResponseEntity<?> swapModel(@RequestBody Map<String, String> request) {
        String version = request.get("version");
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                aiServiceUrl + "/api/models/swap", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                systemLogService.logConfig(null, auth.getName(),
                    "模型热替换", version, "切换到版本: " + version);
            }
            return response;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }

    @PostMapping("/rollback")
    public ResponseEntity<?> rollbackModel(@RequestBody(required = false) Map<String, String> request) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                aiServiceUrl + "/api/models/rollback", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String version = request != null ? request.get("version") : "上一版本";
                systemLogService.logConfig(null, auth.getName(),
                    "模型回滚", version != null ? version : "上一版本", "回滚模型版本");
            }
            return response;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }

    @GetMapping("/training/status")
    public ResponseEntity<?> getTrainingStatus() {
        try {
            return restTemplate.getForEntity(aiServiceUrl + "/api/training/status", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }

    @GetMapping("/training/history")
    public ResponseEntity<?> getTrainingHistory() {
        try {
            return restTemplate.getForEntity(aiServiceUrl + "/api/training/history", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }

    @PostMapping("/training/start")
    public ResponseEntity<?> startTraining(@RequestBody Map<String, Object> request) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                aiServiceUrl + "/api/training/start", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                systemLogService.logConfig(null, auth.getName(),
                    "启动模型训练", "增量训练",
                    String.format("轮数: %s, 批次: %s",
                        request.get("epochs"), request.get("batch_size")));
            }
            return response;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }

    @PostMapping("/training/cancel")
    public ResponseEntity<?> cancelTraining() {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                aiServiceUrl + "/api/training/cancel", null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                systemLogService.logConfig(null, auth.getName(),
                    "取消模型训练", "增量训练", "用户取消训练任务");
            }
            return response;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 500, "message", "AI服务不可用"));
        }
    }
}
