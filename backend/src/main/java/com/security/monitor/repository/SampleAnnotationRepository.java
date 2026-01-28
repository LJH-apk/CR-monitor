package com.security.monitor.repository;

import com.security.monitor.entity.SampleAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SampleAnnotationRepository extends JpaRepository<SampleAnnotation, Long> {

    // 根据样本ID查询所有标注
    List<SampleAnnotation> findBySampleId(Long sampleId);

    // 根据危险行为ID查询所有标注
    List<SampleAnnotation> findByDangerBehaviorId(Long dangerBehaviorId);

    // 删除样本的所有标注
    void deleteBySampleId(Long sampleId);
}
