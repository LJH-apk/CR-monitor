# ============ 阶段1: Maven构建 ============
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 复制pom.xml并下载依赖（利用Docker缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============ 阶段2: 运行时镜像 ============
FROM eclipse-temurin:17-jre-alpine

# 安装FFmpeg和其他必要工具
RUN apk add --no-cache \
    ffmpeg \
    curl \
    bash \
    tzdata

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用用户（安全最佳实践）
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=builder /build/target/security-monitor-*.jar app.jar

# 创建存储目录
RUN mkdir -p /app/storage/uploads \
             /app/storage/transcoded \
             /app/storage/thumbnails \
             /app/storage/training-samples && \
    chown -R appuser:appuser /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
