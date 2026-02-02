# Linux 服务器部署指南

本指南详细说明如何在 Linux 服务器（Ubuntu 20.04/22.04 或 CentOS 7/8）上部署智能安全监控系统。

---

## 📋 目录

- [服务器要求](#服务器要求)
- [环境准备](#环境准备)
- [依赖安装](#依赖安装)
- [数据库配置](#数据库配置)
- [项目部署](#项目部署)
- [Nginx 配置](#nginx-配置)
- [系统服务配置](#系统服务配置)
- [安全配置](#安全配置)
- [监控和日志](#监控和日志)
- [常见问题](#常见问题)

---

## 服务器要求

### 最低配置
- **CPU**: 2 核心
- **内存**: 4GB RAM
- **磁盘**: 50GB 可用空间
- **操作系统**: Ubuntu 20.04+ 或 CentOS 7+

### 推荐配置
- **CPU**: 4 核心或以上
- **内存**: 8GB RAM 或以上
- **磁盘**: 100GB SSD
- **带宽**: 10Mbps 或以上

### 端口要求
- `80`: HTTP (Nginx)
- `443`: HTTPS (Nginx)
- `8080`: 后端服务（内部）
- `5001`: AI 服务（内部）
- `3306`: MySQL（内部）
- `6379`: Redis（内部）

---

## 环境准备

### 1. 更新系统

```bash
# Ubuntu/Debian
sudo apt update && sudo apt upgrade -y

# CentOS/RHEL
sudo yum update -y
```

### 2. 创建部署用户

```bash
# 创建专用用户
sudo useradd -m -s /bin/bash secmonitor

# 设置密码
sudo passwd secmonitor

# 添加到 sudo 组（可选）
sudo usermod -aG sudo secmonitor

# 切换到部署用户
su - secmonitor
```

### 3. 创建项目目录

```bash
# 创建应用目录
sudo mkdir -p /opt/security-monitor
sudo chown -R secmonitor:secmonitor /opt/security-monitor

# 创建数据目录
sudo mkdir -p /data/security-monitor/{uploads,transcoded,thumbnails,training-samples}
sudo chown -R secmonitor:secmonitor /data/security-monitor

# 创建日志目录
sudo mkdir -p /var/log/security-monitor
sudo chown -R secmonitor:secmonitor /var/log/security-monitor
```

---

## 依赖安装

### 1. 安装 Java 17

```bash
# Ubuntu/Debian
sudo apt install -y openjdk-17-jdk

# CentOS/RHEL 8
sudo dnf install -y java-17-openjdk java-17-openjdk-devel

# 验证安装
java -version
```

### 2. 安装 Maven

```bash
# Ubuntu/Debian
sudo apt install -y maven

# CentOS/RHEL
sudo yum install -y maven

# 验证安装
mvn -version
```

### 3. 安装 Node.js 18+

```bash
# 使用 NodeSource 仓库
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 或使用 nvm（推荐）
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install 18
nvm use 18

# 验证安装
node -v
npm -v
```

### 4. 安装 MySQL 8.0

```bash
# Ubuntu/Debian
sudo apt install -y mysql-server mysql-client

# CentOS/RHEL 8
sudo dnf install -y mysql-server

# 启动 MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

### 5. 安装 Redis 7.0

```bash
# Ubuntu/Debian
sudo apt install -y redis-server

# CentOS/RHEL 8
sudo dnf install -y redis

# 启动 Redis
sudo systemctl start redis
sudo systemctl enable redis

# 验证连接
redis-cli ping  # 应返回 PONG
```

### 6. 安装 FFmpeg

```bash
# Ubuntu/Debian
sudo apt install -y ffmpeg

# CentOS/RHEL 8 (需要 EPEL 和 RPM Fusion)
sudo dnf install -y epel-release
sudo dnf install -y --nogpgcheck https://download1.rpmfusion.org/free/el/rpmfusion-free-release-8.noarch.rpm
sudo dnf install -y ffmpeg

# 验证安装
ffmpeg -version
```

### 7. 安装 Python 3.10+ (AI 服务)

```bash
# Ubuntu 22.04 自带 Python 3.10
sudo apt install -y python3 python3-pip python3-venv

# Ubuntu 20.04 需要添加 PPA
sudo add-apt-repository ppa:deadsnakes/ppa
sudo apt update
sudo apt install -y python3.10 python3.10-venv python3.10-dev

# 验证安装
python3 --version
```

### 8. 安装 Nginx

```bash
# Ubuntu/Debian
sudo apt install -y nginx

# CentOS/RHEL
sudo yum install -y nginx

# 启动 Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

## 数据库配置

### 1. 配置 MySQL

```bash
# 登录 MySQL
sudo mysql -u root -p

# 创建数据库
CREATE DATABASE security_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户
CREATE USER 'secmonitor'@'localhost' IDENTIFIED BY 'your_strong_password';

# 授予权限
GRANT ALL PRIVILEGES ON security_monitor.* TO 'secmonitor'@'localhost';
FLUSH PRIVILEGES;

# 退出
EXIT;
```

### 2. 配置 Redis

```bash
# 编辑 Redis 配置
sudo vim /etc/redis/redis.conf

# 修改以下配置
bind 127.0.0.1
protected-mode yes
maxmemory 512mb
maxmemory-policy allkeys-lru

# 重启 Redis
sudo systemctl restart redis
```

---

## 项目部署

### 1. 上传项目代码

```bash
# 方式一：使用 Git
cd /opt/security-monitor
git clone <your-repository-url> .

# 方式二：使用 SCP 上传
# 在本地执行
scp -r /path/to/Page secmonitor@your-server:/opt/security-monitor/
```

### 2. 部署后端服务

```bash
cd /opt/security-monitor/backend

# 修改配置文件
vim src/main/resources/application.yml
```

修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/security_monitor?useSSL=false&serverTimezone=Asia/Shanghai
    username: secmonitor
    password: your_strong_password

  redis:
    host: localhost
    port: 6379

server:
  port: 8080

# 文件存储路径
file:
  upload-dir: /data/security-monitor/uploads
  transcoded-dir: /data/security-monitor/transcoded
  thumbnail-dir: /data/security-monitor/thumbnails
  training-samples-dir: /data/security-monitor/training-samples

# AI 服务配置
ai-service:
  base-url: http://localhost:5001
  enabled: true
```

编译并打包：

```bash
# 编译项目
mvn clean package -DskipTests

# 生成的 JAR 文件位于
ls -lh target/security-monitor-*.jar
```

### 3. 部署前端应用

```bash
cd /opt/security-monitor/frontend

# 安装依赖
npm install

# 修改生产环境配置（如需要）
vim .env.production
```

创建 `.env.production` 文件：

```env
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=ws://your-domain.com/api/ws
```

构建生产版本：

```bash
# 构建
npm run build

# 构建产物位于 dist/ 目录
ls -lh dist/
```

### 4. 部署 AI 服务

```bash
cd /opt/security-monitor/ai-service

# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 安装依赖
pip install --upgrade pip
pip install -r requirements.txt

# 修改配置
vim config.py
```

修改配置：

```python
# Flask 配置
FLASK_HOST = '127.0.0.1'
FLASK_PORT = 5001
FLASK_DEBUG = False

# 模型路径
YOLO_MODEL_PATH = '/opt/security-monitor/ai-service/yolo26n.pt'

# Qwen API 配置（如使用）
QWEN_API_KEY = 'your_api_key'
```

---

## Nginx 配置

### 1. 创建 Nginx 配置文件

```bash
sudo vim /etc/nginx/sites-available/security-monitor
```

添加以下配置：

```nginx
# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 主配置
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 证书配置（使用 Let's Encrypt）
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 日志配置
    access_log /var/log/nginx/security-monitor-access.log;
    error_log /var/log/nginx/security-monitor-error.log;

    # 前端静态文件
    location / {
        root /opt/security-monitor/frontend/dist;
        try_files $uri $uri/ /index.html;

        # 缓存配置
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # 文件上传大小限制
        client_max_body_size 500M;
    }

    # WebSocket 代理
    location /api/ws/ {
        proxy_pass http://localhost:8080/api/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # WebSocket 超时配置
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }

    # 视频文件代理
    location /storage/ {
        alias /data/security-monitor/;

        # 视频文件缓存
        location ~* \.(m3u8|ts)$ {
            add_header Cache-Control "no-cache";
        }

        location ~* \.(mp4|jpg|png)$ {
            expires 7d;
            add_header Cache-Control "public";
        }
    }
}
```

### 2. 启用配置

```bash
# 创建软链接
sudo ln -s /etc/nginx/sites-available/security-monitor /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

### 3. 配置 SSL 证书（使用 Let's Encrypt）

```bash
# 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

---

## 系统服务配置

### 1. 创建后端服务

```bash
sudo vim /etc/systemd/system/security-monitor-backend.service
```

添加以下内容：

```ini
[Unit]
Description=Security Monitor Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=secmonitor
Group=secmonitor
WorkingDirectory=/opt/security-monitor/backend
ExecStart=/usr/bin/java -jar \
    -Xms512m -Xmx2g \
    -Dspring.profiles.active=prod \
    /opt/security-monitor/backend/target/security-monitor-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/security-monitor/backend.log
StandardError=append:/var/log/security-monitor/backend-error.log

[Install]
WantedBy=multi-user.target
```

### 2. 创建 AI 服务

```bash
sudo vim /etc/systemd/system/security-monitor-ai.service
```

添加以下内容：

```ini
[Unit]
Description=Security Monitor AI Service
After=network.target

[Service]
Type=simple
User=secmonitor
Group=secmonitor
WorkingDirectory=/opt/security-monitor/ai-service
Environment="PATH=/opt/security-monitor/ai-service/venv/bin"
ExecStart=/opt/security-monitor/ai-service/venv/bin/python app.py
Restart=always
RestartSec=10
StandardOutput=append:/var/log/security-monitor/ai-service.log
StandardError=append:/var/log/security-monitor/ai-service-error.log

[Install]
WantedBy=multi-user.target
```

### 3. 启动服务

```bash
# 重新加载 systemd
sudo systemctl daemon-reload

# 启动后端服务
sudo systemctl start security-monitor-backend
sudo systemctl enable security-monitor-backend

# 启动 AI 服务
sudo systemctl start security-monitor-ai
sudo systemctl enable security-monitor-ai

# 查看服务状态
sudo systemctl status security-monitor-backend
sudo systemctl status security-monitor-ai

# 查看日志
sudo journalctl -u security-monitor-backend -f
sudo journalctl -u security-monitor-ai -f
```

---

## 安全配置

### 1. 配置防火墙

```bash
# Ubuntu (UFW)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# CentOS (firewalld)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload
```

### 2. 配置 MySQL 安全

```bash
# 只允许本地连接
sudo vim /etc/mysql/mysql.conf.d/mysqld.cnf

# 添加或修改
bind-address = 127.0.0.1

# 重启 MySQL
sudo systemctl restart mysql
```

### 3. 配置 Redis 安全

```bash
# 编辑配置
sudo vim /etc/redis/redis.conf

# 设置密码
requirepass your_redis_password

# 重启 Redis
sudo systemctl restart redis
```

更新后端配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
```

### 4. 配置文件权限

```bash
# 设置配置文件权限
chmod 600 /opt/security-monitor/backend/src/main/resources/application.yml
chmod 600 /opt/security-monitor/ai-service/config.py

# 设置数据目录权限
chmod 755 /data/security-monitor
chmod 755 /data/security-monitor/*
```

---

## 监控和日志

### 1. 日志管理

```bash
# 配置日志轮转
sudo vim /etc/logrotate.d/security-monitor
```

添加以下内容：

```
/var/log/security-monitor/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 secmonitor secmonitor
    sharedscripts
    postrotate
        systemctl reload security-monitor-backend > /dev/null 2>&1 || true
        systemctl reload security-monitor-ai > /dev/null 2>&1 || true
    endscript
}
```

### 2. 系统监控

```bash
# 安装监控工具
sudo apt install -y htop iotop nethogs

# 查看系统资源
htop

# 查看磁盘使用
df -h

# 查看服务状态
sudo systemctl status security-monitor-backend
sudo systemctl status security-monitor-ai
```

### 3. 应用监控

```bash
# 查看后端日志
tail -f /var/log/security-monitor/backend.log

# 查看 AI 服务日志
tail -f /var/log/security-monitor/ai-service.log

# 查看 Nginx 日志
tail -f /var/log/nginx/security-monitor-access.log
tail -f /var/log/nginx/security-monitor-error.log
```

---

## 常见问题

### Q1: 服务启动失败

```bash
# 查看详细错误信息
sudo journalctl -u security-monitor-backend -n 100 --no-pager

# 检查端口占用
sudo netstat -tlnp | grep 8080

# 检查 Java 进程
ps aux | grep java
```

### Q2: 数据库连接失败

```bash
# 测试数据库连接
mysql -u secmonitor -p security_monitor

# 检查 MySQL 状态
sudo systemctl status mysql

# 查看 MySQL 日志
sudo tail -f /var/log/mysql/error.log
```

### Q3: Redis 连接失败

```bash
# 测试 Redis 连接
redis-cli -a your_redis_password ping

# 检查 Redis 状态
sudo systemctl status redis

# 查看 Redis 日志
sudo tail -f /var/log/redis/redis-server.log
```

### Q4: 视频转码失败

```bash
# 检查 FFmpeg 安装
ffmpeg -version

# 检查存储目录权限
ls -la /data/security-monitor/

# 手动测试转码
ffmpeg -i /data/security-monitor/uploads/test.mp4 \
    -codec: copy -start_number 0 -hls_time 10 \
    -hls_list_size 0 -f hls /tmp/test.m3u8
```

### Q5: Nginx 502 错误

```bash
# 检查后端服务是否运行
sudo systemctl status security-monitor-backend

# 检查端口监听
sudo netstat -tlnp | grep 8080

# 查看 Nginx 错误日志
sudo tail -f /var/log/nginx/security-monitor-error.log

# 测试后端连接
curl http://localhost:8080/api/health
```

### Q6: 磁盘空间不足

```bash
# 查看磁盘使用
df -h

# 查找大文件
du -sh /data/security-monitor/* | sort -h

# 清理旧视频（保留最近 30 天）
find /data/security-monitor/uploads -type f -mtime +30 -delete
find /data/security-monitor/transcoded -type d -mtime +30 -exec rm -rf {} +
```

---

## 备份和恢复

### 1. 数据库备份

```bash
# 创建备份脚本
vim /opt/security-monitor/scripts/backup-db.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/backup/security-monitor/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

mysqldump -u secmonitor -p'your_password' security_monitor \
    | gzip > $BACKUP_DIR/security_monitor_$DATE.sql.gz

# 保留最近 7 天的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

```bash
# 设置定时任务
crontab -e

# 每天凌晨 2 点备份
0 2 * * * /opt/security-monitor/scripts/backup-db.sh
```

### 2. 文件备份

```bash
# 备份上传文件
rsync -avz /data/security-monitor/ /backup/security-monitor/files/
```

---

## 性能优化

### 1. JVM 优化

修改服务配置：

```ini
ExecStart=/usr/bin/java -jar \
    -Xms1g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/var/log/security-monitor/heap-dump.hprof \
    /opt/security-monitor/backend/target/security-monitor-0.0.1-SNAPSHOT.jar
```

### 2. MySQL 优化

```bash
sudo vim /etc/mysql/mysql.conf.d/mysqld.cnf
```

```ini
[mysqld]
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
max_connections = 200
query_cache_size = 64M
```

### 3. Redis 优化

```bash
sudo vim /etc/redis/redis.conf
```

```
maxmemory 1gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

---

## 更新部署

### 1. 更新后端

```bash
cd /opt/security-monitor/backend

# 拉取最新代码
git pull

# 重新编译
mvn clean package -DskipTests

# 重启服务
sudo systemctl restart security-monitor-backend
```

### 2. 更新前端

```bash
cd /opt/security-monitor/frontend

# 拉取最新代码
git pull

# 重新构建
npm install
npm run build

# Nginx 会自动使用新的静态文件
```

### 3. 更新 AI 服务

```bash
cd /opt/security-monitor/ai-service

# 拉取最新代码
git pull

# 更新依赖
source venv/bin/activate
pip install -r requirements.txt

# 重启服务
sudo systemctl restart security-monitor-ai
```

---

## 总结

完成以上步骤后，系统应该已经成功部署并运行。访问 `https://your-domain.com` 即可使用系统。

### 快速检查清单

- [ ] 所有依赖已安装
- [ ] 数据库已创建并配置
- [ ] Redis 已启动
- [ ] 后端服务运行正常
- [ ] AI 服务运行正常
- [ ] Nginx 配置正确
- [ ] SSL 证书已配置
- [ ] 防火墙规则已设置
- [ ] 日志轮转已配置
- [ ] 备份脚本已设置

### 有用的命令

```bash
# 查看所有服务状态
sudo systemctl status security-monitor-backend security-monitor-ai nginx mysql redis

# 重启所有服务
sudo systemctl restart security-monitor-backend security-monitor-ai nginx

# 查看实时日志
tail -f /var/log/security-monitor/*.log

# 检查磁盘空间
df -h /data/security-monitor
```

---

**部署完成！如有问题，请参考常见问题部分或查看日志文件。**
