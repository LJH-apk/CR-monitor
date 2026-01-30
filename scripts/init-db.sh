#!/bin/bash
set -e

echo "数据库初始化脚本执行中..."

# Flyway会自动处理迁移，这里只做基础检查
mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW DATABASES;"

echo "数据库初始化完成"
