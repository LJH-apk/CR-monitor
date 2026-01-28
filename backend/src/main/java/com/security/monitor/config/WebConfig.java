package com.security.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure static resource access for storage directory
        registry.addResourceHandler("/storage/**")
                .addResourceLocations("file:/Users/liujiahang/Page/backend/storage/");
    }
}
