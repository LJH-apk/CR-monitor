package com.security.monitor.service;

import com.security.monitor.dto.*;
import com.security.monitor.entity.DangerBehavior;
import com.security.monitor.entity.SampleAnnotation;
import com.security.monitor.entity.TrainingSample;
import com.security.monitor.entity.User;
import com.security.monitor.repository.DangerBehaviorRepository;
import com.security.monitor.repository.SampleAnnotationRepository;
import com.security.monitor.repository.TrainingSampleRepository;
import com.security.monitor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainingSampleService {

    @Autowired
    private TrainingSampleRepository sampleRepository;

    @Autowired
    private SampleAnnotationRepository annotationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DangerBehaviorRepository dangerBehaviorRepository;

    @Value("${storage.training-samples:./storage/training-samples}")
    private String trainingSamplesPath;

    /**
     * 上传图片样本
     */
    @Transactional
    public SampleUploadResponse uploadSample(MultipartFile file, Long userId) throws IOException {
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只能上传图片文件");
        }

        // 验证文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("图片大小不能超过10MB");
        }

        // 创建用户目录
        Path userDir = Paths.get(trainingSamplesPath, userId.toString());
        Files.createDirectories(userDir);

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = userDir.resolve(storedFilename);

        // 先保存文件
        file.transferTo(filePath.toFile());

        // 读取已保存的文件获取尺寸
        BufferedImage image = ImageIO.read(filePath.toFile());
        if (image == null) {
            // 删除无效文件
            Files.deleteIfExists(filePath);
            throw new RuntimeException("无法读取图片文件");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // 验证图片尺寸
        if (width > 4096 || height > 4096) {
            // 删除超限文件
            Files.deleteIfExists(filePath);
            throw new RuntimeException("图片尺寸不能超过4096x4096");
        }

        // 创建样本记录
        TrainingSample sample = new TrainingSample();
        sample.setUserId(userId);
        sample.setOriginalFilename(originalFilename);
        sample.setStoredFilename(storedFilename);
        sample.setFilePath(filePath.toString());
        sample.setFileSize(file.getSize());
        sample.setImageWidth(width);
        sample.setImageHeight(height);
        sample.setStatus("PENDING");
        sample.setUploadTime(LocalDateTime.now());

        TrainingSample savedSample = sampleRepository.save(sample);

        return new SampleUploadResponse(
                savedSample.getId(),
                originalFilename,
                "PENDING",
                "上传成功",
                width,
                height
        );
    }

    /**
     * 获取样本列表（根据用户角色）
     */
    public List<TrainingSampleDTO> getSamples(Long userId, String userRole, String status) {
        List<TrainingSample> samples;

        if ("ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole)) {
            // 管理员可以看到所有样本
            if (status != null && !status.isEmpty()) {
                samples = sampleRepository.findByStatus(status);
            } else {
                samples = sampleRepository.findAll();
            }
        } else {
            // 普通用户只能看到自己的样本
            if (status != null && !status.isEmpty()) {
                samples = sampleRepository.findByUserIdAndStatus(userId, status);
            } else {
                samples = sampleRepository.findByUserId(userId);
            }
        }

        return samples.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取样本详情
     */
    public Optional<TrainingSampleDTO> getSampleById(Long id, Long userId, String userRole) {
        Optional<TrainingSample> sampleOpt = sampleRepository.findById(id);

        if (sampleOpt.isEmpty()) {
            return Optional.empty();
        }

        TrainingSample sample = sampleOpt.get();

        // 权限检查：普通用户只能查看自己的样本
        if (!"ADMIN".equals(userRole) && !"SUPER_ADMIN".equals(userRole)) {
            if (!sample.getUserId().equals(userId)) {
                return Optional.empty();
            }
        }

        return Optional.of(convertToDTO(sample));
    }

    /**
     * 保存标注数据
     */
    @Transactional
    public AnnotationDTO saveAnnotation(Long sampleId, AnnotationDTO annotationDTO, Long userId, String userRole) {
        // 验证样本是否存在
        TrainingSample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new RuntimeException("样本不存在"));

        // 权限检查
        if (!"ADMIN".equals(userRole) && !"SUPER_ADMIN".equals(userRole)) {
            if (!sample.getUserId().equals(userId)) {
                throw new RuntimeException("无权限操作此样本");
            }
        }

        // 验证坐标范围
        if (annotationDTO.getXMin() < 0 || annotationDTO.getYMin() < 0 ||
            annotationDTO.getXMax() > sample.getImageWidth() ||
            annotationDTO.getYMax() > sample.getImageHeight() ||
            annotationDTO.getXMin() >= annotationDTO.getXMax() ||
            annotationDTO.getYMin() >= annotationDTO.getYMax()) {
            throw new RuntimeException("标注坐标无效");
        }

        SampleAnnotation annotation = new SampleAnnotation();
        annotation.setSampleId(sampleId);
        annotation.setDangerBehaviorId(annotationDTO.getDangerBehaviorId());
        annotation.setXMin(annotationDTO.getXMin());
        annotation.setYMin(annotationDTO.getYMin());
        annotation.setXMax(annotationDTO.getXMax());
        annotation.setYMax(annotationDTO.getYMax());
        annotation.setNotes(annotationDTO.getNotes());

        SampleAnnotation saved = annotationRepository.save(annotation);

        // 更新样本状态为已标注
        sample.setStatus("ANNOTATED");
        sampleRepository.save(sample);

        return convertAnnotationToDTO(saved);
    }

    /**
     * 删除样本
     */
    @Transactional
    public void deleteSample(Long id, Long userId, String userRole) {
        TrainingSample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("样本不存在"));

        // 权限检查
        if (!"ADMIN".equals(userRole) && !"SUPER_ADMIN".equals(userRole)) {
            if (!sample.getUserId().equals(userId)) {
                throw new RuntimeException("无权限删除此样本");
            }
        }

        // 删除文件
        try {
            Files.deleteIfExists(Paths.get(sample.getFilePath()));
        } catch (IOException e) {
            // 忽略文件删除错误
        }

        // 删除数据库记录（级联删除标注）
        sampleRepository.deleteById(id);
    }

    /**
     * 更新样本状态（仅管理员）
     */
    @Transactional
    public TrainingSampleDTO updateSampleStatus(Long id, String status) {
        TrainingSample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("样本不存在"));

        sample.setStatus(status);
        TrainingSample updated = sampleRepository.save(sample);

        return convertToDTO(updated);
    }

    /**
     * 导出COCO JSON格式
     */
    public CocoExportDTO exportCoco() {
        List<TrainingSample> samples = sampleRepository.findByStatus("APPROVED");

        List<CocoExportDTO.CocoImage> images = new ArrayList<>();
        List<CocoExportDTO.CocoAnnotation> annotations = new ArrayList<>();
        Map<Long, CocoExportDTO.CocoCategory> categoryMap = new HashMap<>();

        long annotationId = 1;

        for (TrainingSample sample : samples) {
            // 添加图片信息
            images.add(new CocoExportDTO.CocoImage(
                    sample.getId(),
                    sample.getOriginalFilename(),
                    sample.getImageWidth(),
                    sample.getImageHeight()
            ));

            // 获取该样本的所有标注
            List<SampleAnnotation> sampleAnnotations = annotationRepository.findBySampleId(sample.getId());

            for (SampleAnnotation ann : sampleAnnotations) {
                // 添加类别信息
                if (!categoryMap.containsKey(ann.getDangerBehaviorId())) {
                    Optional<DangerBehavior> behavior = dangerBehaviorRepository.findById(ann.getDangerBehaviorId());
                    behavior.ifPresent(b -> categoryMap.put(b.getId(),
                            new CocoExportDTO.CocoCategory(b.getId(), b.getName())));
                }

                // 转换为COCO格式的bbox [x, y, width, height]
                double x = ann.getXMin();
                double y = ann.getYMin();
                double width = ann.getXMax() - ann.getXMin();
                double height = ann.getYMax() - ann.getYMin();
                double area = width * height;

                annotations.add(new CocoExportDTO.CocoAnnotation(
                        annotationId++,
                        sample.getId(),
                        ann.getDangerBehaviorId(),
                        Arrays.asList(x, y, width, height),
                        area
                ));
            }
        }

        List<CocoExportDTO.CocoCategory> categories = new ArrayList<>(categoryMap.values());

        return new CocoExportDTO(images, annotations, categories);
    }

    /**
     * 转换为DTO
     */
    private TrainingSampleDTO convertToDTO(TrainingSample sample) {
        TrainingSampleDTO dto = new TrainingSampleDTO();
        dto.setId(sample.getId());
        dto.setUserId(sample.getUserId());

        // 获取用户名
        userRepository.findById(sample.getUserId())
                .ifPresent(user -> dto.setUsername(user.getUsername()));

        dto.setOriginalFilename(sample.getOriginalFilename());
        dto.setStoredFilename(sample.getStoredFilename());
        dto.setFilePath(sample.getFilePath());
        dto.setFileSize(sample.getFileSize());
        dto.setImageWidth(sample.getImageWidth());
        dto.setImageHeight(sample.getImageHeight());
        dto.setStatus(sample.getStatus());
        dto.setUploadTime(sample.getUploadTime());
        dto.setCreatedAt(sample.getCreatedAt());
        dto.setUpdatedAt(sample.getUpdatedAt());

        // 获取标注列表
        List<SampleAnnotation> annotations = annotationRepository.findBySampleId(sample.getId());
        dto.setAnnotations(annotations.stream()
                .map(this::convertAnnotationToDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    /**
     * 转换标注为DTO
     */
    private AnnotationDTO convertAnnotationToDTO(SampleAnnotation annotation) {
        AnnotationDTO dto = new AnnotationDTO();
        dto.setId(annotation.getId());
        dto.setSampleId(annotation.getSampleId());
        dto.setDangerBehaviorId(annotation.getDangerBehaviorId());

        // 获取危险行为名称
        dangerBehaviorRepository.findById(annotation.getDangerBehaviorId())
                .ifPresent(behavior -> dto.setDangerBehaviorName(behavior.getName()));

        dto.setXMin(annotation.getXMin());
        dto.setYMin(annotation.getYMin());
        dto.setXMax(annotation.getXMax());
        dto.setYMax(annotation.getYMax());
        dto.setNotes(annotation.getNotes());
        dto.setCreatedAt(annotation.getCreatedAt());
        dto.setUpdatedAt(annotation.getUpdatedAt());

        return dto;
    }
}
