# 智能安全监控系统 - 后端

基于 Spring Boot 3.2.1 的视频监控后端服务。

## 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+
- FFmpeg 5.0+

## 快速开始

1. 创建数据库
```sql
CREATE DATABASE security_monitor;
```

2. 修改配置 `src/main/resources/application.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/security_monitor
    username: root
    password: your_password
```

3. 启动服务
```bash
mvn spring-boot:run
```

服务地址: `http://localhost:8080/api`

## 默认账号

- 用户名: admin
- 密码: admin123

## 主要功能

- JWT 用户认证
- 视频上传与 HLS 转码
- AI 危险行为检测
- WebSocket 实时预警推送
- 危险行为与阈值配置
