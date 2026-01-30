# Docker命令修复说明

## 问题描述

您遇到的 `sudo: docker: command not found` 错误是因为Docker的符号链接指向了错误的路径：
- 旧路径（已不存在）: `/Applications/Deveop/Docker.app`
- 正确路径: `/Applications/Docker.app`

## 解决方案

### 方案1: 使用自动修复脚本（推荐）

```bash
# 运行修复脚本
bash scripts/fix-docker-macos.sh
```

脚本会自动：
1. 删除旧的错误链接
2. 创建新的正确链接
3. 验证Docker命令可用

### 方案2: 手动修复

```bash
# 1. 删除旧链接
sudo rm /usr/local/bin/docker
sudo rm /usr/local/bin/docker-compose
sudo rm /usr/local/bin/com.docker.cli

# 2. 创建新链接
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker-compose

# 3. 验证
docker --version
docker compose version
```

### 方案3: 使用完整路径（临时方案）

如果不想修改系统链接，可以直接使用完整路径：

```bash
# 启动服务
sudo /Applications/Docker.app/Contents/Resources/bin/docker compose up -d

# 查看状态
/Applications/Docker.app/Contents/Resources/bin/docker compose ps

# 查看日志
/Applications/Docker.app/Contents/Resources/bin/docker compose logs -f
```

### 方案4: 添加到PATH（永久解决）

```bash
# 添加到shell配置文件
echo 'export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"' >> ~/.zshrc

# 重新加载配置
source ~/.zshrc

# 验证
docker --version
```

## 验证修复

修复后，运行以下命令验证：

```bash
# 检查docker命令
docker --version
# 应输出: Docker version 29.1.5, build 0e6fee6

# 检查docker compose
docker compose version
# 应输出: Docker Compose version...

# 检查链接
ls -la /usr/local/bin/docker
# 应指向: /Applications/Docker.app/Contents/Resources/bin/docker
```

## 启动项目

修复完成后，即可启动项目：

```bash
cd /Users/liujiahang/Page
sudo docker compose up -d
```

## 注意事项

1. **需要sudo权限**: 修改 `/usr/local/bin` 需要管理员权限
2. **Docker Desktop必须运行**: 确保Docker Desktop应用正在运行
3. **重启终端**: 修复后建议重启终端或重新加载shell配置

## 技术细节

**当前链接状态**:
```
/usr/local/bin/docker -> /Applications/Deveop/Docker.app/... (错误，路径不存在)
```

**修复后状态**:
```
/usr/local/bin/docker -> /Applications/Docker.app/Contents/Resources/bin/docker (正确)
```

**Docker实际位置**:
```
/Applications/Docker.app/Contents/Resources/bin/docker
```
