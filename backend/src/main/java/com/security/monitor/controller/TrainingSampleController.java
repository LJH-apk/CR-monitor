package com.security.monitor.controller;

import com.security.monitor.dto.*;
import com.security.monitor.service.TrainingSampleService;
import com.security.monitor.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/samples")
public class TrainingSampleController {

    @Autowired
    private TrainingSampleService sampleService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 上传图片样本（所有认证用户）
     */
    @PostMapping("/upload")
    public ResponseEntity<SampleUploadResponse> uploadSample(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            SampleUploadResponse response = sampleService.uploadSample(file, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Upload sample error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取样本列表
     */
    @GetMapping
    public ResponseEntity<List<TrainingSampleDTO>> getSamples(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            String userRole = extractUserRole(request);
            List<TrainingSampleDTO> samples = sampleService.getSamples(userId, userRole, status);
            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取样本详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<TrainingSampleDTO> getSampleById(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            String userRole = extractUserRole(request);
            return sampleService.getSampleById(id, userId, userRole)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 保存标注数据
     */
    @PostMapping("/{id}/annotations")
    public ResponseEntity<AnnotationDTO> saveAnnotation(
            @PathVariable Long id,
            @RequestBody AnnotationDTO annotationDTO,
            HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            String userRole = extractUserRole(request);
            AnnotationDTO saved = sampleService.saveAnnotation(id, annotationDTO, userId, userRole);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除样本
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSample(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            String userRole = extractUserRole(request);
            sampleService.deleteSample(id, userId, userRole);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新样本状态（仅管理员）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TrainingSampleDTO> updateSampleStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || !status.matches("PENDING|ANNOTATED|APPROVED|REJECTED")) {
                return ResponseEntity.badRequest().build();
            }
            TrainingSampleDTO updated = sampleService.updateSampleStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 导出COCO JSON格式（仅管理员）
     */
    @GetMapping("/export/coco")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CocoExportDTO> exportCoco() {
        try {
            CocoExportDTO cocoData = sampleService.exportCoco();
            return ResponseEntity.ok(cocoData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 从请求中提取用户ID
     */
    private Long extractUserId(HttpServletRequest request) {
        String token = extractToken(request);
        return jwtUtil.extractUserId(token);
    }

    /**
     * 从请求中提取用户角色
     */
    private String extractUserRole(HttpServletRequest request) {
        String token = extractToken(request);
        return jwtUtil.extractRole(token);
    }

    /**
     * 从请求头中提取JWT token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("未找到认证令牌");
    }
}
