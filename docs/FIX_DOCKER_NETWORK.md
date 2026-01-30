# Docker镜像拉取失败解决方案

## 问题描述

错误信息：`Error response from daemon: Get "https://registry-1.docker.io/v2/": context deadline exceeded`

这是因为无法访问Docker Hub官方镜像源（网络问题或被墙）。

---

## 解决方案

### 方案1：配置国内镜像源（推荐）

#### 步骤1：创建或编辑Docker配置文件

```bash
# 创建配置目录（如果不存在）
mkdir -p ~/.docker

# 编辑配置文件
vim ~/.docker/daemon.json
```

#### 步骤2：添加以下内容

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

#### 步骤3：重启Docker Desktop

1. 打开Docker Desktop应用
2. 点击右上角设置图标 → Quit Docker Desktop
3. 重新启动Docker Desktop
4. 等待Docker完全启动（托盘图标变绿）

#### 步骤4：验证配置

```bash
docker info | grep -A 5 "Registry Mirrors"
```

#### 步骤5：重新拉取镜像

```bash
cd /Users/liujiahang/Page
sudo docker compose pull
sudo docker compose up -d
```

---

### 方案2：使用Docker Desktop图形界面配置

1. 打开Docker Desktop
2. 点击右上角 **设置图标（齿轮）**
3. 选择 **Docker Engine**
4. 在JSON配置中添加：

```json
{
  "builder": {
    "gc": {
      "defaultKeepStorage": "20GB",
      "enabled": true
    }
  },
  "experimental": false,
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

5. 点击 **Apply & Restart**

---

### 方案3：使用阿里云镜像加速器（需要注册）

1. 访问：https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors
2. 登录阿里云账号（免费注册）
3. 获取专属加速器地址（类似：`https://xxxxx.mirror.aliyuncs.com`）
4. 配置到 `daemon.json`：

```json
{
  "registry-mirrors": [
    "https://xxxxx.mirror.aliyuncs.com"
  ]
}
```

---

### 方案4：手动下载镜像（临时方案）

如果镜像源都无法访问，可以尝试：

```bash
# 使用代理（如果有）
export HTTP_PROXY=http://127.0.0.1:7890
export HTTPS_PROXY=http://127.0.0.1:7890

# 拉取镜像
docker pull mysql:8.0
docker pull redis:7.0-alpine
docker pull eclipse-temurin:17-jre-alpine
docker pull maven:3.9-eclipse-temurin-17-alpine
docker pull node:18-alpine
docker pull nginx:1.25-alpine
docker pull python:3.11-slim

# 取消代理
unset HTTP_PROXY
unset HTTPS_PROXY
```

---

### 方案5：修改docker-compose.yml使用国内镜像

如果以上方案都不行，可以修改镜像源：

```yaml
services:
  mysql:
    image: registry.cn-hangzhou.aliyuncs.com/library/mysql:8.0

  redis:
    image: registry.cn-hangzhou.aliyuncs.com/library/redis:7.0-alpine
```

---

## 推荐配置（综合方案）

### 完整的 daemon.json 配置

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ],
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 5,
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

---

## 验证步骤

### 1. 检查Docker是否正常运行

```bash
docker info
```

### 2. 测试镜像拉取

```bash
docker pull hello-world
```

### 3. 查看配置的镜像源

```bash
docker info | grep -A 10 "Registry Mirrors"
```

### 4. 重新启动项目

```bash
cd /Users/liujiahang/Page
sudo docker compose pull
sudo docker compose up -d
```

---

## 常见问题

### Q1: 配置后仍然超时？

**A**: 尝试以下步骤：
1. 完全退出Docker Desktop
2. 删除 `~/.docker/daemon.json`
3. 重启Docker Desktop
4. 使用Docker Desktop图形界面配置镜像源
5. 重启Docker Desktop

### Q2: 镜像源不可用？

**A**: 国内镜像源可能会变化，尝试以下备用源：
- 中科大：`https://docker.mirrors.ustc.edu.cn`
- 网易：`https://hub-mirror.c.163.com`
- 百度：`https://mirror.baidubce.com`
- 腾讯：`https://mirror.ccs.tencentyun.com`

### Q3: 需要代理？

**A**: 在Docker Desktop设置中配置代理：
1. Settings → Resources → Proxies
2. 启用 Manual proxy configuration
3. 输入代理地址（如：`http://127.0.0.1:7890`）

---

## 快速修复命令

```bash
# 1. 配置镜像源
cat > ~/.docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
EOF

# 2. 重启Docker Desktop（手动操作）
# 打开Docker Desktop → 右上角设置 → Quit Docker Desktop → 重新启动

# 3. 等待Docker启动完成后，验证配置
docker info | grep -A 5 "Registry Mirrors"

# 4. 重新拉取镜像
cd /Users/liujiahang/Page
sudo docker compose pull
sudo docker compose up -d
```

---

## 推荐操作流程

1. **配置镜像源**（使用Docker Desktop图形界面最简单）
2. **重启Docker Desktop**
3. **验证配置生效**
4. **重新拉取镜像**

配置完成后，镜像拉取速度会显著提升！
