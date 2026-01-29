# 安全配置指南

本文档说明如何安全地配置智能安全监控系统的后端服务。

## 环境变量配置

### 1. 创建环境变量文件

复制 `.env.example` 文件并重命名为 `.env`：

```bash
cp .env.example .env
```

**重要**: 确保 `.env` 文件已添加到 `.gitignore`，不要提交到版本控制系统！

### 2. 生成强JWT密钥

JWT密钥是系统安全的关键。使用以下命令生成强密钥：

```bash
# 生成256位（32字节）的Base64编码密钥
openssl rand -base64 64
```

将生成的密钥复制到 `.env` 文件的 `JWT_SECRET` 变量中。

### 3. 配置数据库密码

修改 `.env` 文件中的数据库密码：

```env
DB_PASSWORD=your_secure_database_password_here
```

**建议**:
- 使用至少16个字符的强密码
- 包含大小写字母、数字和特殊字符
- 不要使用默认密码（如 `root`, `admin`, `password` 等）

### 4. 配置Redis密码

如果Redis启用了密码保护，配置Redis密码：

```env
REDIS_PASSWORD=your_secure_redis_password_here
```

**配置Redis密码**:

编辑Redis配置文件（通常在 `/etc/redis/redis.conf`）：

```conf
requirepass your_secure_redis_password_here
```

重启Redis服务：

```bash
# macOS
brew services restart redis

# Linux
sudo systemctl restart redis
```

### 5. 配置存储路径

根据部署环境配置存储路径：

```env
# 开发环境（相对路径）
STORAGE_BASE_PATH=./storage

# 生产环境（绝对路径）
STORAGE_BASE_PATH=/var/app/security-monitor/storage
```

## 生产环境安全检查清单

### 必须完成的安全配置

- [ ] 已生成并配置强JWT密钥（至少256位）
- [ ] 已修改数据库默认密码
- [ ] 已配置Redis密码
- [ ] `.env` 文件已添加到 `.gitignore`
- [ ] 已禁用Spring Boot的详细错误信息（`server.error.include-message=never`）
- [ ] 已配置HTTPS（使用Nginx反向代理或Spring Boot SSL）
- [ ] 已限制CORS允许的来源（不使用 `*`）
- [ ] 已配置防火墙规则，只开放必要端口

### 推荐的安全配置

- [ ] 启用数据库SSL连接
- [ ] 配置Redis SSL/TLS
- [ ] 实施API速率限制
- [ ] 配置日志审计
- [ ] 定期备份数据库
- [ ] 配置监控和告警
- [ ] 使用密钥管理服务（如AWS Secrets Manager、HashiCorp Vault）

## 使用环境变量运行应用

### 方式1: 使用.env文件（推荐用于开发）

安装 `dotenv` 工具或使用IDE的环境变量支持。

### 方式2: 直接设置环境变量

```bash
export JWT_SECRET="your_generated_secret_key"
export DB_PASSWORD="your_database_password"
export REDIS_PASSWORD="your_redis_password"

mvn spring-boot:run
```

### 方式3: 使用系统环境变量（推荐用于生产）

在系统级别配置环境变量：

**Linux/macOS** (`/etc/environment` 或 `~/.bashrc`):
```bash
export JWT_SECRET="your_generated_secret_key"
export DB_PASSWORD="your_database_password"
```

**Docker**:
```yaml
environment:
  - JWT_SECRET=your_generated_secret_key
  - DB_PASSWORD=your_database_password
```

**Kubernetes**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  jwt-secret: <base64-encoded-secret>
  db-password: <base64-encoded-password>
```

## 安全最佳实践

### 1. 密钥轮换

定期轮换JWT密钥和数据库密码（建议每90天）。

### 2. 最小权限原则

数据库用户只授予必要的权限：

```sql
CREATE USER 'security_monitor'@'localhost' IDENTIFIED BY 'strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON security_monitor.* TO 'security_monitor'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 监控和审计

启用应用日志和数据库审计日志，监控异常访问。

### 4. 定期安全审计

- 定期检查依赖库的安全漏洞：`mvn dependency-check:check`
- 使用OWASP ZAP或Burp Suite进行渗透测试
- 审查访问日志，检测异常行为

## 故障排查

### JWT验证失败

如果遇到JWT验证失败，检查：
1. `JWT_SECRET` 是否正确配置
2. 密钥长度是否足够（至少256位）
3. 客户端和服务端使用的密钥是否一致

### 数据库连接失败

检查：
1. `DB_PASSWORD` 是否正确
2. 数据库用户是否有正确的权限
3. 数据库服务是否正在运行

### Redis连接失败

检查：
1. `REDIS_PASSWORD` 是否与Redis配置匹配
2. Redis服务是否正在运行
3. Redis是否配置了密码保护

## 联系支持

如有安全问题或疑问，请联系安全团队。
