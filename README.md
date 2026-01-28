# 智能安全监控系统

<div align="center">

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue-3-green.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)

一个基于AI的全栈视频监控平台，具备实时危险检测和智能预警功能

[功能特性](#功能特性) • [快速开始](#快速开始) • [技术栈](#技术栈) • [系统架构](#系统架构) • [使用指南](#使用指南)

</div>

---

## 📋 目录

- [项目简介](#-项目简介)
- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [环境要求](#-环境要求)
- [快速开始](#-快速开始)
- [项目结构](#-项目结构)
- [配置说明](#-配置说明)
- [使用指南](#-使用指南)
- [API文档](#-api文档)
- [常见问题](#-常见问题)
- [开发路线图](#-开发路线图)
- [贡献指南](#-贡献指南)
- [许可证](#-许可证)

---

## 🎯 项目简介

智能安全监控系统是一个现代化的视频监控解决方案，集成了AI驱动的危险行为检测功能。系统能够实时分析视频内容，自动识别潜在的安全威胁（如打架、摔倒、非法入侵等），并通过WebSocket实时推送预警信息。

### 核心亮点

- 🤖 **AI智能分析**：集成先进的AI模型进行视频内容分析
- ⚡ **实时预警**：基于WebSocket的毫秒级预警推送
- 🎬 **视频转码**：自动将上传视频转换为HLS流媒体格式
- 📊 **数据可视化**：直观的图表展示预警统计数据
- 🔐 **安全认证**：基于JWT的用户认证和权限管理
- 🎨 **现代UI**：响应式设计，支持多种设备访问

---

## ✨ 功能特性

### 视频管理
- ✅ 视频上传（支持最大500MB）
- ✅ 自动HLS转码
- ✅ 缩略图生成
- ✅ 视频列表管理
- ✅ 视频播放控制

### AI分析
- ✅ 实时帧提取和分析
- ✅ 多种危险行为识别（打架、摔倒、入侵、吸烟、火灾等）
- ✅ 置信度评分
- ✅ 分析进度实时显示
- ✅ 智能限流机制

### 预警系统
- ✅ 实时预警推送
- ✅ 预警等级分类
- ✅ 预警确认机制
- ✅ 历史预警查询
- ✅ 预警统计分析

### 管理后台
- ✅ 危险行为配置
- ✅ 预警阈值设置
- ✅ 用户权限管理
- ✅ 系统监控面板

### 权限系统
- ✅ 三级权限体系（普通用户/管理员/超级管理员）
- ✅ 基于角色的访问控制（RBAC）
- ✅ 用户账户管理（创建、编辑、删除）
- ✅ 角色权限分配

### 训练样本管理
- ✅ 图片样本上传（支持JPG/PNG格式）
- ✅ 矩形边界框标注工具
- ✅ 多标注框支持
- ✅ 样本状态管理（待标注/已标注/已审核/已拒绝）
- ✅ COCO JSON格式导出（用于YOLO增量学习）
- ✅ 样本审核流程

---

## 🛠 技术栈

### 后端技术
- **框架**: Spring Boot 3.2.1
- **语言**: Java 18
- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.0+
- **ORM**: Spring Data JPA
- **安全**: Spring Security + JWT
- **WebSocket**: Spring WebSocket
- **视频处理**: FFmpeg 5.0+

### 前端技术
- **框架**: Vue 3
- **语言**: TypeScript 5
- **构建工具**: Vite
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP客户端**: Axios
- **视频播放**: Video.js
- **图表**: ECharts

### 开发工具
- **构建**: Maven 3.6+
- **包管理**: npm / yarn
- **版本控制**: Git
- **IDE**: IntelliJ IDEA / VS Code

---

## 🏗 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         前端层 (Vue 3)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Dashboard│  │  Admin   │  │  Video   │  │  Alert   │   │
│  │   页面   │  │   管理   │  │   播放   │  │   面板   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↕ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                      后端层 (Spring Boot)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │Controller│  │ Service  │  │Repository│  │  Entity  │   │
│  │   层     │→ │   层     │→ │   层     │→ │   层     │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│                         数据层                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  MySQL   │  │  Redis   │  │ FFmpeg   │  │AI Service│   │
│  │  数据库  │  │  缓存    │  │ 转码引擎 │  │  AI分析  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 视频处理流程

```
上传视频 → 存储到本地 → 异步转码(FFmpeg) → 生成HLS → AI分析 → 生成预警 → WebSocket推送
```

---

## 📦 环境要求

### 必需环境
- **Java**: 18 或更高版本
- **Node.js**: 18 或更高版本
- **Maven**: 3.6 或更高版本
- **MySQL**: 8.0 或更高版本
- **Redis**: 7.0 或更高版本
- **FFmpeg**: 5.0 或更高版本

### 推荐配置
- **CPU**: 4核心或以上
- **内存**: 8GB或以上
- **磁盘**: 50GB可用空间（用于视频存储）

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd Page
```

### 2. 配置数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE security_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 退出MySQL
exit
```

### 3. 配置Redis

```bash
# macOS
brew services start redis

# Linux
sudo systemctl start redis

# 验证Redis连接
redis-cli ping  # 应返回 PONG
```

### 4. 安装FFmpeg

```bash
# macOS
brew install ffmpeg

# Ubuntu/Debian
sudo apt-get install ffmpeg

# 验证安装
ffmpeg -version
```

### 5. 配置后端

```bash
cd backend

# 修改配置文件（如需要）
# vim src/main/resources/application.yml

# 编译项目
mvn clean install -DskipTests

# 运行后端
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

### 6. 配置前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端将在 `http://localhost:5173` 启动

### 7. 访问系统

打开浏览器访问：`http://localhost:5173`

**默认账号**：

| 角色 | 用户名 | 密码 | 权限说明 |
|------|--------|------|----------|
| 超级管理员 | admin | admin123 | 所有权限，包括用户管理 |
| 管理员 | admin1 | admin123 | 视频管理、配置管理、样本审核 |
| 普通用户 | user1 | admin123 | 查看监控、上传样本 |

---

## 📁 项目结构

```
Page/
├── backend/                    # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/security/monitor/
│   │   │   │       ├── config/          # 配置类
│   │   │   │       ├── controller/      # 控制器
│   │   │   │       ├── dto/             # 数据传输对象
│   │   │   │       ├── entity/          # 实体类
│   │   │   │       ├── repository/      # 数据访问层
│   │   │   │       ├── service/         # 业务逻辑层
│   │   │   │       └── util/            # 工具类
│   │   │   └── resources/
│   │   │       ├── application.yml      # 应用配置
│   │   │       └── db/migration/        # 数据库迁移脚本
│   │   └── test/                        # 测试代码
│   ├── storage/                         # 文件存储目录
│   │   ├── uploads/                     # 上传的原始视频
│   │   ├── transcoded/                  # 转码后的HLS文件
│   │   └── thumbnails/                  # 视频缩略图
│   └── pom.xml                          # Maven配置
│
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                         # API接口
│   │   ├── assets/                      # 静态资源
│   │   ├── components/                  # 组件
│   │   │   ├── AlertChart.vue          # 预警图表
│   │   │   ├── AlertPanel.vue          # 预警面板
│   │   │   └── VideoPlayer.vue         # 视频播放器
│   │   ├── composables/                 # 组合式函数
│   │   ├── router/                      # 路由配置
│   │   ├── store/                       # 状态管理
│   │   │   └── modules/
│   │   │       ├── alert.ts            # 预警状态
│   │   │       ├── auth.ts             # 认证状态
│   │   │       ├── config.ts           # 配置状态
│   │   │       └── video.ts            # 视频状态
│   │   ├── types/                       # TypeScript类型定义
│   │   ├── views/                       # 页面视图
│   │   │   ├── Dashboard.vue           # 主控制台
│   │   │   ├── Login.vue               # 登录页
│   │   │   └── Admin/                  # 管理页面
│   │   ├── App.vue                      # 根组件
│   │   └── main.ts                      # 入口文件
│   ├── package.json                     # npm配置
│   └── vite.config.ts                   # Vite配置
│
├── CLAUDE.md                   # AI助手指南
└── README.md                   # 项目说明文档
```

---

## ⚙️ 配置说明

### 后端配置 (application.yml)

```yaml
# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /api

# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/security_monitor
    username: root
    password: your_password

# Redis配置
  data:
    redis:
      host: localhost
      port: 6379

# JWT配置
jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时

# 文件上传配置
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

# 存储路径配置
storage:
  uploads: ./storage/uploads
  transcoded: ./storage/transcoded
  thumbnails: ./storage/thumbnails
```

### 前端配置 (vite.config.ts)

```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

---

## 📖 使用指南

### 1. 登录系统

使用默认账号登录系统：
- 用户名：`admin`
- 密码：`admin123`

### 2. 上传视频

1. 点击右上角"管理后台"按钮
2. 进入"视频管理"页面
3. 点击"上传视频"按钮
4. 选择视频文件（支持MP4格式，最大500MB）
5. 等待上传和转码完成

### 3. 查看监控

1. 返回主控制台（Dashboard）
2. 在视频选择下拉框中选择已上传的视频
3. 等待AI分析完成（查看分析进度卡片）
4. 分析完成后视频自动播放
5. 右侧实时预警面板显示检测到的异常行为

### 4. 管理预警

1. 在预警面板中查看实时预警
2. 点击"确认"按钮确认预警
3. 查看预警统计图表
4. 在管理后台配置预警阈值

### 5. 配置系统

#### 配置危险行为
1. 进入"管理后台" → "危险行为管理"
2. 添加或编辑危险行为类型
3. 设置行为名称、描述和严重等级

#### 配置预警阈值
1. 进入"管理后台" → "预警阈值配置"
2. 为每种危险行为设置：
   - 置信度阈值（0-1）
   - 时间窗口（秒）
   - 最大预警数量

### 6. 权限系统使用

#### 用户角色说明
- **普通用户（USER）**：可以查看监控、确认预警、上传训练样本
- **管理员（ADMIN）**：拥有普通用户权限，还可以上传视频、配置系统、审核样本
- **超级管理员（SUPER_ADMIN）**：拥有所有权限，包括用户账户管理

#### 管理用户账户（仅超级管理员）
1. 使用超级管理员账号登录（admin/admin123）
2. 进入"管理后台" → "用户管理"
3. 可以执行以下操作：
   - 创建新用户账户
   - 编辑用户信息
   - 修改用户角色
   - 删除用户账户

### 7. 训练样本上传

#### 上传样本图片
1. 点击顶部导航栏的"样本上传"按钮
2. 选择或拖拽图片文件（支持JPG/PNG，最大10MB）
3. 预览图片信息
4. 点击"上传并标注"按钮

#### 标注样本
1. 上传成功后自动跳转到标注页面
2. 使用鼠标拖拽绘制矩形边界框
3. 为每个边界框选择对应的危险行为类型
4. 可选填写备注信息
5. 点击"添加到列表"保存当前标注
6. 完成所有标注后点击"保存标注"

#### 查看和管理样本
1. 进入"样本列表"页面
2. 使用状态筛选器查看不同状态的样本
3. 普通用户只能看到自己上传的样本
4. 管理员可以查看所有样本并进行审核

#### 导出训练数据（仅管理员）
1. 进入"样本列表"页面
2. 点击"导出COCO JSON格式"按钮
3. 系统将导出所有已审核通过的样本
4. 下载的JSON文件可直接用于YOLO模型训练

---

## 🔌 API文档

### 认证接口

#### 登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### 视频接口

#### 上传视频
```http
POST /api/videos/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <video-file>
```

#### 获取视频列表
```http
GET /api/videos
Authorization: Bearer <token>
```

#### 获取分析进度
```http
GET /api/videos/{id}/analysis-progress
Authorization: Bearer <token>
```

### 预警接口

#### 获取视频预警
```http
GET /api/alerts/video/{videoId}
Authorization: Bearer <token>
```

#### 确认预警
```http
PUT /api/alerts/{id}/acknowledge
Authorization: Bearer <token>
```

### WebSocket接口

#### 连接预警推送
```
ws://localhost:8080/api/ws/alerts?token=<jwt-token>
```

### 用户管理接口（仅超级管理员）

#### 获取用户列表
```http
GET /api/admin/users?page=0&size=10
Authorization: Bearer <token>
```

#### 创建用户
```http
POST /api/admin/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "role": "USER"
}
```

#### 更新用户角色
```http
PUT /api/admin/users/{id}/role
Authorization: Bearer <token>
Content-Type: application/json

{
  "role": "ADMIN"
}
```

#### 删除用户
```http
DELETE /api/admin/users/{id}
Authorization: Bearer <token>
```

### 训练样本接口

#### 上传样本图片
```http
POST /api/samples/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <image-file>
```

#### 获取样本列表
```http
GET /api/samples?status=PENDING
Authorization: Bearer <token>
```

#### 保存标注
```http
POST /api/samples/{id}/annotations
Authorization: Bearer <token>
Content-Type: application/json

{
  "dangerBehaviorId": 1,
  "xMin": 100,
  "yMin": 150,
  "xMax": 300,
  "yMax": 400,
  "notes": "可选备注"
}
```

#### 更新样本状态（仅管理员）
```http
PUT /api/samples/{id}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "APPROVED"
}
```

#### 导出COCO格式（仅管理员）
```http
GET /api/samples/export/coco
Authorization: Bearer <token>
```

#### 删除样本
```http
DELETE /api/samples/{id}
Authorization: Bearer <token>
```

---

## ❓ 常见问题

### Q1: 视频上传后无法播放？
**A**: 请检查：
1. FFmpeg是否正确安装
2. 转码是否完成（查看视频状态）
3. 存储目录权限是否正确

### Q2: AI分析进度一直为0？
**A**: 请检查：
1. AI服务是否正常运行
2. 视频是否转码完成
3. 查看后端日志是否有错误

### Q3: WebSocket连接失败？
**A**: 请检查：
1. JWT token是否有效
2. 后端WebSocket配置是否正确
3. 防火墙是否阻止了WebSocket连接

### Q4: 编译时提示Java版本错误？
**A**: 确保使用Java 17：
```bash
# 设置JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 验证版本
java -version
```

### Q5: 前端无法连接后端？
**A**: 请检查：
1. 后端是否在8080端口运行
2. Vite代理配置是否正确
3. CORS配置是否启用

---

## 🗺 开发路线图

### ~~v0.1.0 (老版本)~~
- ✅ 基础视频上传和播放
- ✅ AI危险行为检测
- ✅ 实时预警推送
- ✅ 管理后台

### v0.1.2 (目前版本)
- ✅ 样本图片上传标注（施工中）
- ✅ 三级用户权限设置
- ✅ 样本审核管理


### v0.2.0 (计划中)
- ⏳ 多摄像头支持
- ⏳ 危险行为管理
- ⏳ 实时视频流分析
- ⏳ 移动端适配
- ⏳ 预警邮件通知

### v1.0.0 (未来)
- 📋 更精细的物体识别
- 📋 失物招领功能

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- Java: 遵循阿里巴巴Java开发手册
- TypeScript: 使用ESLint和Prettier
- 提交信息: 使用语义化提交规范

---

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

---

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件至：[Liu18701059325@qq.com]
- 项目主页：[项目链接]

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个星标！**

Made with ❤️ by [Liu Jiahang]

</div>
