# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

请始终使用简体中文与我对话，并在回答时保持专业、简洁

请遵循以下版本号规则，并在完成对应任务后修改前端显示、后端项目中的版本号。版本号规则如下：1.版本号采用三位版本号，如：1.5.3；2.每当完成一个子功能时，版本第二位加一，且第三位重置归零，如1.5.3 -> 1.6.0；3.当修复子功能的bug以及错误时，仅在版本号第三位加一，如：1.5.3 -> 1.5.4。其中子功能为多路监控这类牵扯多文件创建与修改的复杂功能，子功能bug为一些仅在单个或几个文件中修改的逻辑和处理问题。

## Project Overview

**Intelligent Security Monitoring System** (v1.5.5) - A full-stack video surveillance platform with AI-powered danger detection and real-time alerting.

This is a monorepo containing:
- **Backend**: Spring Boot 3.2.1 REST API (Java 17)
- **Frontend**: Vue 3 + TypeScript SPA
- **AI Service**: Python Flask + YOLO11 + Qwen-VL
- **Storage**: Shared file storage for video uploads and HLS transcoding

## Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- FFmpeg 5.0+ (for video transcoding)
- Python 3.10+ (for AI service)

## Common Commands

### Backend (from `/backend` directory)

```bash
mvn spring-boot:run          # Run in development mode
mvn clean package            # Build JAR
mvn test                     # Run tests
mvn test -Dtest=VideoProcessingServiceTest  # Run specific test
```

### Frontend (from `/frontend` directory)

```bash
npm install                  # Install dependencies
npm run dev                  # Run development server (port 5173)
npm run build                # Build for production
vue-tsc -b                   # Type check
```

### AI Service (from `/ai-service` directory)

```bash
pip install -r requirements.txt  # Install dependencies
python app.py                    # Run service (port 5001)
```

### Database & Redis

```bash
mysql -u root -p -e "CREATE DATABASE security_monitor;"  # Create database
# Flyway migrations run automatically on backend startup

brew services start redis    # Start Redis (macOS)
redis-cli ping               # Verify Redis connection
```

## Architecture

### Backend Architecture

**Layered Structure**: Controller → Service → Repository → Entity

**Key Services**:
- `VideoProcessingService`: Async video transcoding via FFmpeg
- `FrameAnalysisService` / `RealtimeAnalysisService`: AI detection orchestration
- `AlertPushService`: WebSocket alert push with `DetectionStatusMessage` (alert/normal types)
- `SystemLogService`: Audit logging for uploads, deletions, user management
- `CacheService`: Redis operations

**Video Processing Flow**:
1. Upload → `storage/uploads/{userId}/{uuid}_{filename}.mp4`
2. Async transcoding → FFmpeg converts to HLS
3. Output → `storage/transcoded/{videoId}/playlist.m3u8`
4. Frame analysis → AI detection via `/api/detect`
5. Alert/Normal status → WebSocket push

### Frontend Architecture

**State Management**: Pinia stores in `store/modules/`
- `auth.ts`, `video.ts`, `alert.ts`, `config.ts`

**Key Views**:
- `/dashboard`: Single video monitoring with real-time detection
- `/multi-monitor`: 2x3 grid multi-video monitoring
- `/admin/*`: Admin management pages
- `/developer/logs`: System logs (DEVELOPER role only)

**Key Composables**:
- `useWebSocket.ts`: WebSocket connection and message handling
- `useMultiVideoState.ts`: Multi-video state management for grid view

**Detection Status Flow**:
- `VideoPlayer.vue` receives `detectionStatus` prop
- Shows status indicator: "实时检测中" / "画面正常" / "检测到异常"
- Alert overlay pauses video for 3 seconds

### AI Service Architecture

**Components**:
- `YOLODetector`: Object detection with hot-swappable model versions
- `QwenAnalyzer`: Multi-modal analysis via DashScope API
- `ModelManager`: Version management for incremental training
- `YOLOTrainer`: Training pipeline with COCO format support

**Endpoints**:
- `GET /api/health`: Service health check
- `POST /api/detect`: Frame detection (base64 image)
- `GET /api/model/versions`: List model versions
- `POST /api/training/start`: Start incremental training
- `POST /api/model/swap`: Hot-swap model version

### API Structure

Base URL: `http://localhost:8080/api`

- `/auth/*`: Login/logout
- `/videos/*`: Upload, list, get, delete
- `/alerts/*`: Alert history, acknowledgment
- `/admin/users/*`: User CRUD (SUPER_ADMIN/DEVELOPER)
- `/admin/danger-behaviors/*`, `/admin/thresholds/*`: Config management
- `/system-logs`: Audit logs (DEVELOPER only)
- WebSocket: `ws://localhost:8080/api/ws/alerts?token={jwt}`

### Database Schema

Tables managed by Flyway (`db/migration/`):
- `users`: Accounts with roles (USER/ADMIN/SUPER_ADMIN/DEVELOPER)
- `videos`: Video metadata and status
- `danger_behaviors`, `alert_thresholds`: Detection config
- `alerts`, `alert_statistics`: Alert data
- `system_logs`: Audit trail
- `training_samples`: AI training data with annotations

### Redis Keys

- `video:{id}`, `video:list:{userId}`: Video cache
- `config:danger_behaviors`, `config:thresholds:{id}`: Config cache
- `transcoding:status:{videoId}`: Transcoding progress
- `ws:sessions:{userId}`: WebSocket sessions

## Configuration

### Backend (`application.yml`)

- Server: Port 8080, context path `/api`
- Storage: `./storage/{uploads,transcoded,thumbnails,training-samples}`
- AI Service: `http://localhost:5001`
- Frame analysis: `consecutive-frames: 3`, `cycle-frames: 5`, `realtime-interval: 2`

### Frontend (`vite.config.ts`)

- Dev server: Port 5173
- API proxy: `/api` → `http://localhost:8080`

### AI Service (`config.py`)

- YOLO model path, DashScope API key
- Backend URL for sample fetching

## Important Implementation Details

### Role-Based Access

- `USER`: View monitoring, upload samples
- `ADMIN`: + Video management, config, sample review
- `SUPER_ADMIN`: + User management
- `DEVELOPER`: + System logs, all debug features

### WebSocket Message Types

```typescript
// Alert message
{ type: 'alert', videoId: number, alertData: Alert }

// Normal status (detection cycle complete, no anomaly)
{ type: 'normal', videoId: number, analyzedFrames: number, message: string }
```

### System Logging

Logged operations (via `SystemLogService`):
- LOGIN/LOGOUT: User authentication
- UPLOAD/DELETE: Video operations
- USER_MGMT: User create/update/delete/role change
- ERROR: System errors

## Default Credentials

| Role | Username | Password |
|------|----------|----------|
| DEVELOPER | developer | admin123 |
| SUPER_ADMIN | admin | admin123 |
| ADMIN | admin1 | admin123 |
| USER | user1 | admin123 |

## Troubleshooting

### Backend won't start
- Check: `java -version` (17+), MySQL running, Redis running, FFmpeg installed

### Video transcoding fails
- Ensure FFmpeg in PATH, check storage permissions

### WebSocket connection fails
- JWT must be valid, use `?token={jwt}` (no "Bearer " prefix)

### AI Service issues
- Check DashScope API key in `config.py`
- Verify YOLO model file exists
