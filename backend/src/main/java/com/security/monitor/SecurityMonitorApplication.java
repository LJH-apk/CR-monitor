package com.security.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SecurityMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityMonitorApplication.class, args);
    }
}
