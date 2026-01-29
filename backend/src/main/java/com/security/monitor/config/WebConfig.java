package com.security.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 *
 * 安全说明：
 * - 已移除不安全的静态资源映射 /storage/**
 * - 文件访问现在通过 FileController 进行，需要认证和授权
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 安全：移除了不安全的静态资源映射
    // 文件访问现在通过 FileController 进行，需要认证和授权
}
