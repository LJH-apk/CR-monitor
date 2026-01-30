#!/bin/bash
# Docker命令修复脚本 - macOS

echo "🔧 开始修复Docker命令链接..."
echo ""

# 检查Docker Desktop是否安装
if [ ! -d "/Applications/Docker.app" ]; then
    echo "❌ 错误: Docker Desktop未安装在 /Applications/Docker.app"
    echo "请先安装Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# 检查Docker二进制文件是否存在
if [ ! -f "/Applications/Docker.app/Contents/Resources/bin/docker" ]; then
    echo "❌ 错误: Docker二进制文件不存在"
    exit 1
fi

echo "✓ Docker Desktop已安装"
echo ""

# 删除旧的错误链接
echo "1. 删除旧的符号链接..."
sudo rm -f /usr/local/bin/docker \
           /usr/local/bin/docker-compose \
           /usr/local/bin/com.docker.cli \
           /usr/local/bin/docker-credential-desktop \
           /usr/local/bin/docker-credential-ecr-login \
           /usr/local/bin/docker-credential-osxkeychain 2>/dev/null
echo "✓ 旧链接已删除"
echo ""

# 创建新的正确链接
echo "2. 创建新的符号链接..."
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker-compose
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker-credential-desktop /usr/local/bin/docker-credential-desktop
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker-credential-ecr-login /usr/local/bin/docker-credential-ecr-login
sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker-credential-osxkeychain /usr/local/bin/docker-credential-osxkeychain
echo "✓ 新链接已创建"
echo ""

# 验证修复
echo "3. 验证Docker命令..."
if docker --version > /dev/null 2>&1; then
    echo "✓ Docker命令可用: $(docker --version)"
else
    echo "❌ Docker命令仍然不可用"
    exit 1
fi

if docker compose version > /dev/null 2>&1; then
    echo "✓ Docker Compose可用: $(docker compose version)"
else
    echo "⚠️  Docker Compose命令不可用，但docker命令可用"
fi

echo ""
echo "🎉 Docker命令修复完成！"
echo ""
echo "现在可以使用以下命令启动项目："
echo "  cd /Users/liujiahang/Page"
echo "  sudo docker compose up -d"
