# 快速启动指南

## 🚀 一键启动智能安全监控系统

### 前置检查

#### macOS / Linux

```bash
# 1. 确认Docker已安装
docker --version
docker compose version  # 或 docker-compose --version

# 2. 确认关键文件存在
ls -lh ai-service/yolo11n.pt  # 应显示 5.4MB
du -sh storage                 # 应显示 3.1GB
```

#### Windows (PowerShell)

```powershell
# 1. 确认Docker已安装
docker --version
docker compose version

# 2. 确认关键文件存在
Get-Item ai-service\yolo11n.pt | Select-Object Length
Get-ChildItem storage -Recurse | Measure-Object -Property Length -Sum
```

---

## 🔧 修复 macOS Docker 命令问题

如果遇到 `sudo: docker: command not found` 错误，说明Docker符号链接损坏，请执行以下命令修复：

```bash
# 1. 删除旧的错误链接
sudo rm /usr/local/bin/docker /usr/local/bin/docker-compose /usr/local/bin/com.docker.cli

# 2. 创建正确的链接
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker-compose

# 3. 验证修复
docker --version
docker compose version
```

**或者直接使用完整路径：**

```bash
/Applications/Docker.app/Contents/Resources/bin/docker compose up -d
```

---

## 📋 启动步骤

### macOS / Linux

```bash
# 1. 进入项目目录
cd /Users/liujiahang/Page

# 2. 复制并配置环境变量
cp .env.example .env

# 编辑.env文件（重要！）
vim .env
# 必须修改：
# - DASHSCOPE_API_KEY（通义千问API密钥）
# 生产环境建议修改：
# - DB_ROOT_PASSWORD
# - DB_PASSWORD
# - JWT_SECRET（使用: openssl rand -base64 64）

# 3. 启动所有服务（使用80端口需要sudo）
sudo docker compose up -d
# 或使用旧版命令
sudo docker-compose up -d

# 如果docker命令不可用，使用完整路径：
sudo /Applications/Docker.app/Contents/Resources/bin/docker compose up -d

# 4. 查看启动状态
docker compose ps

# 5. 查看日志
docker compose logs -f
```

### Windows (PowerShell 管理员模式)

```powershell
# 1. 进入项目目录
cd C:\path\to\Page

# 2. 复制并配置环境变量
Copy-Item .env.example .env

# 编辑.env文件（重要！）
notepad .env
# 必须修改：
# - DASHSCOPE_API_KEY（通义千问API密钥）
# 生产环境建议修改：
# - DB_ROOT_PASSWORD
# - DB_PASSWORD
# - JWT_SECRET（使用: openssl rand -base64 64）

# 3. 启动所有服务
docker compose up -d

# 4. 查看启动状态
docker compose ps

# 5. 查看日志
docker compose logs -f
```

### Windows (CMD 管理员模式)

```cmd
REM 1. 进入项目目录
cd C:\path\to\Page

REM 2. 复制并配置环境变量
copy .env.example .env

REM 编辑.env文件
notepad .env

REM 3. 启动所有服务
docker compose up -d

REM 4. 查看启动状态
docker compose ps

REM 5. 查看日志
docker compose logs -f
```

### 访问系统

#### 所有平台

- **前端**: http://localhost
- **后端API**: http://localhost:8080/api
- **AI服务**: http://localhost:5001/api/health
- **默认账号**: admin / admin123

### 健康检查

#### macOS / Linux

```bash
# 检查所有服务状态
docker compose ps

# 检查Backend
curl http://localhost:8080/api/actuator/health

# 检查AI Service
curl http://localhost:5001/api/health

# 检查Frontend
curl http://localhost/
```

#### Windows (PowerShell)

```powershell
# 检查所有服务状态
docker compose ps

# 检查Backend
Invoke-WebRequest -Uri http://localhost:8080/api/actuator/health

# 检查AI Service
Invoke-WebRequest -Uri http://localhost:5001/api/health

# 检查Frontend
Invoke-WebRequest -Uri http://localhost/
```

### 常用命令

#### macOS / Linux

```bash
# 停止服务
docker compose down

# 重启服务
docker compose restart

# 查看日志
docker compose logs -f backend
docker compose logs -f ai-service

# 进入容器
docker compose exec backend bash
docker compose exec mysql mysql -u root -p
```

#### Windows (PowerShell)

```powershell
# 停止服务
docker compose down

# 重启服务
docker compose restart

# 查看日志
docker compose logs -f backend
docker compose logs -f ai-service

# 进入容器
docker compose exec backend bash
docker compose exec mysql mysql -u root -p
```

### 故障排查

如果遇到问题，请查看详细文档：
- 完整部署文档: `docs/DOCKER_DEPLOYMENT.md`
- 项目说明: `CLAUDE.md`

### 注意事项

#### macOS / Linux

1. **80端口权限**: 使用80端口需要sudo权限
2. **Docker命令问题**: 如遇到`command not found`，参考上方"修复 macOS Docker 命令问题"章节
3. **存储空间**: 确保至少有150GB可用空间
4. **API密钥**: 必须配置有效的通义千问API密钥
5. **生产环境**: 务必修改所有默认密码和JWT密钥

#### Windows

1. **管理员权限**: 必须以管理员身份运行PowerShell或CMD
2. **Docker Desktop**: 确保Docker Desktop正在运行
3. **WSL2**: 建议使用WSL2后端以获得更好的性能
4. **防火墙**: 可能需要允许Docker通过Windows防火墙
5. **路径分隔符**: Windows使用反斜杠`\`，注意修改路径
6. **存储空间**: 确保至少有150GB可用空间
7. **API密钥**: 必须配置有效的通义千问API密钥
8. **生产环境**: 务必修改所有默认密码和JWT密钥

---

## 🐛 常见问题排查

### macOS: sudo: docker: command not found

**原因**: Docker符号链接损坏或指向错误路径

**解决方案**:
```bash
# 方案1: 修复符号链接（推荐）
sudo rm /usr/local/bin/docker /usr/local/bin/docker-compose
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker-compose

# 方案2: 使用完整路径
sudo /Applications/Docker.app/Contents/Resources/bin/docker compose up -d

# 方案3: 添加到PATH（永久解决）
echo 'export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Windows: docker: command not found

**原因**: Docker Desktop未启动或未安装

**解决方案**:
1. 启动Docker Desktop应用
2. 等待Docker完全启动（系统托盘图标变为绿色）
3. 重新打开PowerShell/CMD
4. 如果仍然失败，重新安装Docker Desktop

### 端口被占用

**macOS / Linux**:
```bash
# 查看80端口占用
sudo lsof -i :80

# 查看8080端口占用
sudo lsof -i :8080

# 停止占用进程
sudo kill -9 <PID>
```

**Windows**:
```powershell
# 查看80端口占用
netstat -ano | findstr :80

# 停止占用进程
taskkill /PID <PID> /F
```

### 容器启动失败

**所有平台**:
```bash
# 查看详细日志
docker compose logs <service_name>

# 重新构建镜像
docker compose build --no-cache

# 完全清理后重启
docker compose down -v
docker compose up -d
```

---

## 📦 已创建的文件清单

### Docker配置文件
- ✅ `docker-compose.yml` - 主编排文件（5个服务）
- ✅ `backend/Dockerfile` - Backend多阶段构建
- ✅ `frontend/Dockerfile` - Frontend多阶段构建
- ✅ `frontend/nginx.conf` - Nginx配置
- ✅ `ai-service/Dockerfile` - AI Service构建

### 环境配置
- ✅ `.env.example` - 环境变量模板
- ✅ `.dockerignore` - 根目录忽略文件
- ✅ `backend/.dockerignore` - Backend忽略文件
- ✅ `frontend/.dockerignore` - Frontend忽略文件
- ✅ `ai-service/.dockerignore` - AI Service忽略文件

### 辅助文件
- ✅ `scripts/init-db.sh` - 数据库初始化脚本
- ✅ `docs/DOCKER_DEPLOYMENT.md` - 完整部署文档

### 验证结果
- ✅ YOLO模型文件存在 (5.4MB)
- ✅ 存储目录存在 (3.1GB)
- ✅ 所有Dockerfile配置正确
- ✅ 5个健康检查配置完成
- ✅ 初始化脚本可执行

---

## 🎯 下一步

1. 配置 `.env` 文件中的API密钥
2. 运行 `sudo docker compose up -d`
3. 访问 http://localhost
4. 使用 admin/admin123 登录

**开箱即用！** 🎉
