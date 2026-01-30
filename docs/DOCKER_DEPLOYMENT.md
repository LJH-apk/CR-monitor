# Docker部署文档

## 智能安全监控系统 - Docker部署指南

本文档提供完整的Docker部署说明，帮助您快速启动智能安全监控系统。

---

## 系统架构

本系统采用微服务架构，包含以下组件：

- **MySQL 8.0**: 主数据库
- **Redis 7.0**: 缓存和会话存储
- **Backend**: Spring Boot 3.2.1 (Java 17) + FFmpeg
- **AI Service**: Flask + YOLO11 + 通义千问
- **Frontend**: Vue 3 + Nginx

---

## 快速启动

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少4GB可用内存
- 至少150GB可用磁盘空间（用于视频存储）

### 一键启动

```bash
# 1. 进入项目目录
cd /Users/liujiahang/Page

# 2. 复制环境变量文件
cp .env.example .env

# 3. 编辑环境变量（重要：生产环境必须修改敏感信息）
vim .env
# 修改以下配置：
# - DB_ROOT_PASSWORD（数据库root密码）
# - DB_PASSWORD（应用数据库密码）
# - JWT_SECRET（使用 openssl rand -base64 64 生成）
# - DASHSCOPE_API_KEY（通义千问API密钥）

# 4. 验证关键文件
ls -lh ai-service/yolo11n.pt  # 应显示 5.4MB
du -sh storage                 # 应显示 3.1GB

# 5. 启动所有服务（需要sudo权限，因为使用80端口）
sudo docker-compose up -d

# 6. 查看服务状态
docker-compose ps

# 7. 查看启动日志
docker-compose logs -f
```

### 访问系统

- **前端界面**: http://localhost 或 http://服务器IP
- **后端API**: http://localhost:8080/api
- **AI服务健康检查**: http://localhost:5001/api/health
- **默认账号**: admin / admin123

---

## 环境变量说明

### 数据库配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| DB_ROOT_PASSWORD | rootroot | MySQL root密码（生产环境必须修改） |
| DB_NAME | security_monitor | 数据库名称 |
| DB_USERNAME | monitor | 应用数据库用户名 |
| DB_PASSWORD | monitor123 | 应用数据库密码（生产环境必须修改） |
| MYSQL_PORT | 3306 | MySQL端口 |

### Redis配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| REDIS_PASSWORD | (空) | Redis密码（生产环境建议设置） |
| REDIS_PORT | 6379 | Redis端口 |

### JWT配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| JWT_SECRET | (弱密钥) | JWT签名密钥（生产环境必须修改） |
| JWT_EXPIRATION | 86400000 | Token过期时间（毫秒，默认24小时） |

**生成强JWT密钥**:
```bash
openssl rand -base64 64
```

### AI服务配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| DASHSCOPE_API_KEY | (示例密钥) | 通义千问API密钥（必须配置有效密钥） |
| AI_SERVICE_PORT | 5001 | AI服务端口 |

### 端口配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| FRONTEND_PORT | 80 | 前端服务端口 |
| BACKEND_PORT | 8080 | 后端服务端口 |

---

## 常用运维命令

### 服务管理

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart backend

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f ai-service

# 进入容器
docker-compose exec backend bash
docker-compose exec mysql bash
```

### 数据库操作

```bash
# 连接MySQL
docker-compose exec mysql mysql -u root -p

# 查看数据库
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"

# 备份数据库
docker-compose exec mysql mysqldump -u root -p security_monitor > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker-compose exec -T mysql mysql -u root -p security_monitor < backup_20260130.sql
```

### Redis操作

```bash
# 连接Redis
docker-compose exec redis redis-cli

# 检查Redis连接
docker-compose exec redis redis-cli ping

# 查看Redis信息
docker-compose exec redis redis-cli info
```

### 容器管理

```bash
# 查看容器资源使用
docker stats

# 查看容器详细信息
docker inspect security-monitor-backend

# 清理未使用的镜像
docker system prune -a

# 查看存储卷
docker volume ls
docker volume inspect security-monitor_mysql-data
```

### 镜像管理

```bash
# 重新构建所有镜像
docker-compose build

# 重新构建单个服务镜像
docker-compose build backend

# 不使用缓存重新构建
docker-compose build --no-cache

# 查看镜像
docker images | grep security-monitor
```

---

## 健康检查

### 检查所有服务健康状态

```bash
# 查看服务状态（健康的服务显示为healthy）
docker-compose ps

# 检查Backend健康
curl http://localhost:8080/api/actuator/health

# 检查AI Service健康
curl http://localhost:5001/api/health

# 检查Frontend
curl http://localhost/

# 检查MySQL
docker-compose exec mysql mysqladmin ping -h localhost -u root -p

# 检查Redis
docker-compose exec redis redis-cli ping

# 检查FFmpeg安装
docker-compose exec backend ffmpeg -version
```

---

## 故障排查

### Backend启动失败

**症状**: Backend容器反复重启

**排查步骤**:
```bash
# 1. 查看Backend日志
docker-compose logs backend

# 2. 检查MySQL是否健康
docker-compose ps mysql

# 3. 检查数据库连接
docker-compose exec mysql mysql -u monitor -p

# 4. 检查Flyway迁移
docker-compose logs backend | grep Flyway

# 5. 检查FFmpeg
docker-compose exec backend ffmpeg -version
```

**常见问题**:
- 数据库连接失败 → 检查MySQL健康状态和密码配置
- Flyway迁移失败 → 检查数据库权限和迁移脚本
- FFmpeg未找到 → 重新构建Backend镜像

### AI Service启动失败

**症状**: AI Service容器无法启动

**排查步骤**:
```bash
# 1. 查看AI Service日志
docker-compose logs ai-service

# 2. 检查YOLO模型文件
ls -lh ai-service/yolo11n.pt

# 3. 检查Python依赖
docker-compose exec ai-service pip list

# 4. 测试健康检查
curl http://localhost:5001/api/health
```

**常见问题**:
- YOLO模型文件不存在 → 确保yolo11n.pt在ai-service目录下
- Python依赖安装失败 → 检查requirements.txt和网络连接
- 通义千问API密钥无效 → 更新.env中的DASHSCOPE_API_KEY

### 视频转码失败

**症状**: 视频上传后一直显示"转码中"

**排查步骤**:
```bash
# 1. 检查Backend日志中的FFmpeg错误
docker-compose logs backend | grep -i ffmpeg

# 2. 检查存储目录权限
docker-compose exec backend ls -la /app/storage

# 3. 手动测试FFmpeg
docker-compose exec backend ffmpeg -i /app/storage/uploads/test.mp4 -codec:copy test.m3u8

# 4. 检查磁盘空间
df -h
```

**常见问题**:
- 存储空间不足 → 清理旧视频或扩展磁盘
- 权限问题 → 检查storage目录权限
- 视频格式不支持 → 使用MP4格式

### Frontend无法访问Backend

**症状**: 前端页面加载但API请求失败

**排查步骤**:
```bash
# 1. 检查Frontend日志
docker-compose logs frontend

# 2. 检查Nginx配置
docker-compose exec frontend cat /etc/nginx/conf.d/default.conf

# 3. 测试Backend连接
docker-compose exec frontend curl http://backend:8080/api/actuator/health

# 4. 检查网络
docker network inspect security-monitor_security-monitor-network
```

**常见问题**:
- Backend未启动 → 启动Backend服务
- 网络配置错误 → 检查docker-compose.yml中的网络配置
- CORS配置错误 → 检查Backend的CORS_ALLOWED_ORIGINS

### WebSocket连接失败

**症状**: 实时告警不显示

**排查步骤**:
```bash
# 1. 检查浏览器控制台WebSocket错误
# 2. 检查Nginx WebSocket配置
docker-compose exec frontend cat /etc/nginx/conf.d/default.conf | grep -A 5 "Upgrade"

# 3. 检查Backend WebSocket端点
docker-compose logs backend | grep -i websocket
```

---

## 数据持久化

### 存储卷说明

| 卷名 | 挂载点 | 用途 | 备份建议 |
|------|--------|------|----------|
| mysql-data | /var/lib/mysql | MySQL数据 | 每日备份 |
| redis-data | /data | Redis持久化 | 可选备份 |
| ./storage | /app/storage | 视频文件 | 定期归档 |

### 备份策略

**MySQL数据备份**:
```bash
# 手动备份
docker-compose exec mysql mysqldump -u root -p security_monitor > backup_$(date +%Y%m%d).sql

# 定时备份（添加到crontab）
0 2 * * * cd /Users/liujiahang/Page && docker-compose exec -T mysql mysqldump -u root -prootroot security_monitor > /backup/mysql_$(date +\%Y\%m\%d).sql
```

**视频文件备份**:
```bash
# 压缩备份storage目录
tar -czf storage_backup_$(date +%Y%m%d).tar.gz storage/

# 同步到远程服务器
rsync -avz storage/ user@backup-server:/backup/storage/
```

---

## 性能优化

### 资源限制

编辑`docker-compose.yml`添加资源限制：

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
```

### 日志管理

限制日志文件大小：

```yaml
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

## 安全加固

### 生产环境检查清单

- [ ] 修改所有默认密码（数据库、Redis）
- [ ] 使用强JWT密钥（`openssl rand -base64 64`）
- [ ] 配置有效的通义千问API密钥
- [ ] 限制端口暴露（仅80/443）
- [ ] 配置HTTPS（使用Let's Encrypt）
- [ ] 设置防火墙规则
- [ ] 定期更新Docker镜像
- [ ] 配置日志监控和告警
- [ ] 实施数据备份策略

### HTTPS配置（可选）

使用Let's Encrypt配置HTTPS：

```bash
# 安装certbot
sudo apt-get install certbot

# 获取证书
sudo certbot certonly --standalone -d yourdomain.com

# 修改docker-compose.yml，添加证书挂载
volumes:
  - /etc/letsencrypt:/etc/letsencrypt:ro
```

---

## 监控和日志

### 查看实时日志

```bash
# 所有服务
docker-compose logs -f

# 特定服务
docker-compose logs -f backend
docker-compose logs -f ai-service

# 最近100行
docker-compose logs --tail=100 backend
```

### 资源监控

```bash
# 实时资源使用
docker stats

# 磁盘使用
df -h
du -sh storage/
```

---

## 更新和维护

### 更新应用代码

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建镜像
docker-compose build

# 3. 重启服务
docker-compose down
docker-compose up -d
```

### 更新Docker镜像

```bash
# 拉取最新基础镜像
docker-compose pull

# 重新构建
docker-compose build --no-cache

# 重启
docker-compose up -d
```

---

## 完全清理

**警告**: 以下操作会删除所有数据，请谨慎操作！

```bash
# 停止并删除所有容器、网络、卷
docker-compose down -v

# 删除所有镜像
docker rmi $(docker images | grep security-monitor | awk '{print $3}')

# 清理系统
docker system prune -a --volumes
```

---

## 技术支持

如遇到问题，请提供以下信息：

1. 系统环境：`docker --version` 和 `docker-compose --version`
2. 服务状态：`docker-compose ps`
3. 错误日志：`docker-compose logs [service_name]`
4. 环境变量配置（隐藏敏感信息）

---

## 附录

### 端口映射表

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|----------|------------|------|
| Frontend | 80 | 80 | Web界面 |
| Backend | 8080 | 8080 | REST API |
| AI Service | 5001 | 5001 | AI检测服务 |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |

### 服务依赖关系

```
MySQL (健康检查) ──┐
                   ├──> Backend ──> Frontend
Redis (健康检查) ──┘

AI Service (独立启动)
```

### 存储目录结构

```
storage/
├── uploads/          # 原始上传视频
├── transcoded/       # HLS转码输出
│   └── {videoId}/
│       ├── playlist.m3u8
│       └── segment*.ts
├── thumbnails/       # 视频缩略图（自动创建）
└── training-samples/ # AI训练样本
```

---

**文档版本**: 1.0
**最后更新**: 2026-01-30
