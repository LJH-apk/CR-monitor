package com.security.monitor.service;

import com.security.monitor.dto.AIDetectionRequest;
import com.security.monitor.dto.AIDetectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AIServiceClient {

    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${ai-service.timeout}")
    private int timeout;

    @Value("${ai-service.enabled}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public AIServiceClient(RestTemplateBuilder restTemplateBuilder,
                          @Value("${ai-service.timeout}") int timeout) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .build();
    }

    /**
     * 调用AI服务进行图像检测和分析
     */
    public AIDetectionResponse detect(String imageBase64) {
        return detect(imageBase64, 0.5, 0.6);
    }

    /**
     * 调用AI服务进行图像检测和分析（带自定义阈值）
     */
    public AIDetectionResponse detect(String imageBase64, double confThreshold, double fineThreshold) {
        if (!enabled) {
            log.warn("AI服务已禁用，跳过检测");
            return createEmptyResponse();
        }

        String url = aiServiceBaseUrl + "/api/detect";

        try {
            // 构建请求
            AIDetectionRequest request = new AIDetectionRequest(imageBase64, confThreshold, fineThreshold);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AIDetectionRequest> entity = new HttpEntity<>(request, headers);

            // 发送请求
            log.debug("调用AI服务: {}", url);
            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    url,
                    entity,
                    String.class
            );

            log.info("AI服务原始响应: {}", rawResponse.getBody());

            // 手动解析JSON
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                AIDetectionResponse result = mapper.readValue(rawResponse.getBody(), AIDetectionResponse.class);

                if (result != null && result.getCode() == 200) {
                    log.info("AI检测成功，返回 {} 个结果",
                            result.getData() != null ? result.getData().size() : 0);
                    return result;
                } else {
                    log.error("AI服务返回错误: {}", result != null ? result.getMessage() : "null");
                    throw new RuntimeException("AI服务返回错误");
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("JSON解析失败: {}", e.getMessage());
                log.error("原始响应内容: {}", rawResponse.getBody());
                throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("AI服务调用失败: {}", e.getMessage());
            log.error("完整异常信息: ", e);
            throw new RuntimeException("AI检测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        if (!enabled) {
            return false;
        }

        try {
            String url = aiServiceBaseUrl + "/api/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("AI服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取AI服务配置规则
     */
    public Map<String, Object> getAlertRules() {
        if (!enabled) {
            return new HashMap<>();
        }

        try {
            String url = aiServiceBaseUrl + "/api/config/rules";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("获取AI服务规则失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private AIDetectionResponse createEmptyResponse() {
        AIDetectionResponse response = new AIDetectionResponse();
        response.setCode(200);
        response.setMessage("AI服务已禁用");
        response.setData(new java.util.ArrayList<>());
        return response;
    }
}
