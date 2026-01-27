# CLAUDE.md

请始终使用简体中文与我对话，并在回答时保持专业、简洁

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Intelligent Security Monitoring System** - A full-stack video surveillance platform with simulated AI-powered danger detection and real-time alerting.

This is a monorepo containing:
- **Backend**: Spring Boot 3.2.1 REST API (Java 17)
- **Frontend**: Vue 3 + TypeScript SPA
- **Storage**: Shared file storage for video uploads and HLS transcoding

## Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- FFmpeg 5.0+ (for video transcoding)

## Common Commands

### Backend (from `/backend` directory)

```bash
# Run in development mode
mvn spring-boot:run

# Build JAR
mvn clean package

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=VideoProcessingServiceTest

# Clean build artifacts
mvn clean
```

### Frontend (from `/frontend` directory)

```bash
# Install dependencies
npm install

# Run development server (port 5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Type check
vue-tsc -b
```

### Database Setup

```bash
# Create database
mysql -u root -p
CREATE DATABASE security_monitor;

# Flyway migrations run automatically on backend startup
```

### Redis

```bash
# Start Redis (macOS)
brew services start redis

# Check Redis connection
redis-cli ping  # Should return PONG
```

## Architecture

### Backend Architecture

**Layered Structure**: Controller → Service → Repository → Entity

- **Controllers** (`controller/`): REST endpoints with JWT authentication
- **Services** (`service/`): Business logic including video processing and AI detection
- **Repositories** (`repository/`): Spring Data JPA interfaces
- **Entities** (`entity/`): JPA entities mapped to MySQL tables
- **Config** (`config/`): Spring configuration including Security, WebSocket, Redis, Async
- **Utils** (`util/`): FFmpeg wrapper and JWT utilities

**Key Services**:
- `VideoProcessingService`: Orchestrates async video transcoding via FFmpeg
- `FrameAnalysisService`: Simulates AI detection (5% probability per second, confidence 0.6-1.0)
- `AlertPushService`: Pushes alerts via WebSocket to connected clients
- `CacheService`: Redis operations for performance optimization

**Video Processing Flow**:
1. Upload → `storage/uploads/{userId}/{uuid}_{filename}.mp4`
2. Async transcoding → FFmpeg converts to HLS format
3. Output → `storage/transcoded/{videoId}/playlist.m3u8` + segments
4. Frame analysis → Simulated AI detection runs
5. Alert generation → Based on confidence thresholds
6. WebSocket push → Real-time notifications

### Frontend Architecture

**State Management**: Pinia stores in `store/modules/`
- `auth.ts`: User authentication state and JWT token
- `video.ts`: Video list and current video
- `alert.ts`: Alert history and real-time alerts
- `config.ts`: Danger behaviors and thresholds

**Routing** (`router/index.ts`):
- `/login`: Authentication
- `/dashboard`: Main monitoring view (video player + alerts + charts)
- `/admin/behaviors`: Danger behavior management (admin only)
- `/admin/thresholds`: Alert threshold configuration (admin only)
- `/admin/videos`: Video upload and management (admin only)

**Key Components**:
- `VideoPlayer.vue`: Video.js HLS player with pause-on-alert functionality
- `AlertPanel.vue`: Real-time alert display with WebSocket integration
- `AlertChart.vue`: ECharts visualization of alert statistics

### API Structure

Base URL: `http://localhost:8080/api`

- `/auth/*`: Login/logout
- `/videos/*`: Upload, list, get, delete videos
- `/alerts/*`: Alert history, acknowledgment
- `/admin/danger-behaviors/*`: CRUD for danger types
- `/admin/thresholds/*`: CRUD for alert thresholds
- `/dashboard/stats`: Aggregated statistics
- WebSocket: `ws://localhost:8080/api/ws/alerts?token={jwt}`

### Database Schema

6 tables managed by Flyway migrations (`src/main/resources/db/migration/`):
- `users`: User accounts (default: admin/admin123)
- `videos`: Video metadata and transcoding status
- `danger_behaviors`: Configurable danger types (Fighting, Falling, Intrusion, etc.)
- `alert_thresholds`: Detection sensitivity per behavior
- `alerts`: Generated security alerts
- `alert_statistics`: Hourly aggregated metrics

### Redis Caching Strategy

- `video:{videoId}`: Video metadata (1h TTL)
- `video:list:{userId}`: User's video list (5min TTL)
- `config:danger_behaviors`: Active behaviors (1h TTL)
- `config:thresholds:{behaviorId}`: Thresholds (1h TTL)
- `alert:rate:{behaviorId}:{window}`: Rate limiting counters
- `ws:sessions:{userId}`: Active WebSocket sessions

## Configuration

### Backend (`backend/src/main/resources/application.yml`)

- Server: Port 8080, context path `/api`
- Database: `jdbc:mysql://localhost:3306/security_monitor`
- Redis: localhost:6379
- JWT: 24-hour expiration
- File upload: Max 500MB
- Storage paths: `./storage/{uploads,transcoded,thumbnails}`
- FFmpeg: 10-second HLS segments, medium preset

### Frontend (`frontend/vite.config.ts`)

- Dev server: Port 5173
- API proxy: `/api` → `http://localhost:8080`
- Path alias: `@` → `src/`

## Important Implementation Details

### JWT Authentication

- JWT tokens stored in localStorage
- Axios interceptor adds `Authorization: Bearer {token}` header
- `JwtAuthenticationFilter` validates tokens on backend
- Token contains: userId, username, role, expiration

### Video Transcoding

- FFmpeg command: `ffmpeg -i input.mp4 -codec: copy -start_number 0 -hls_time 10 -hls_list_size 0 -f hls output.m3u8`
- Runs asynchronously via `@Async` with thread pool
- Status progression: UPLOADING → TRANSCODING → READY → (FAILED on error)
- Generates thumbnail: `ffmpeg -i input.mp4 -ss 00:00:01 -vframes 1 thumbnail.jpg`

### Simulated AI Detection

- Detection rate: 5% probability per second of video
- Confidence: Random 0.6-1.0
- Behavior selection: Random from active danger behaviors
- Alert creation: Only if confidence >= threshold
- Rate limiting: Respects max alerts per time window

### WebSocket Real-time Alerts

- Connection: `ws://localhost:8080/api/ws/alerts?token={jwt}`
- Frontend auto-connects on dashboard mount
- Video player pauses for 3 seconds on alert
- Alert overlay displays danger type and confidence

### Static Resource Access

- `WebConfig` maps `/storage/**` to `file:./storage/`
- HLS playlists and segments served as static files
- Security config permits `/storage/**` without authentication

## Troubleshooting

### Backend won't start
- Check Java version: `java -version` (must be 17+)
- Verify MySQL running: `mysql -u root -p`
- Verify Redis running: `redis-cli ping`
- Check FFmpeg installed: `ffmpeg -version`

### Video transcoding fails
- Ensure FFmpeg in PATH
- Check storage directory permissions
- Review logs for FFmpeg errors
- Verify video format is supported (MP4 recommended)

### Redis serialization errors
- `RedisConfig` must register `JavaTimeModule` for LocalDateTime support
- Use `Jackson2JsonRedisSerializer` with custom ObjectMapper

### Frontend can't connect to backend
- Verify backend running on port 8080
- Check Vite proxy configuration
- Ensure CORS enabled in `CorsConfig`

### WebSocket connection fails
- JWT token must be valid and not expired
- Check token format: `?token={jwt}` (no "Bearer " prefix)
- Verify WebSocket endpoint: `/api/ws/alerts`

## Default Credentials

- Username: `admin`
- Password: `admin123`
- Role: `ADMIN`

## Testing

### Test video upload
```bash
curl -X POST http://localhost:8080/api/videos/upload \
  -H "Authorization: Bearer {token}" \
  -F "file=@test-video.mp4"
```

### Test WebSocket
```javascript
const ws = new WebSocket('ws://localhost:8080/api/ws/alerts?token=' + token);
ws.onmessage = (event) => {
  console.log('Alert:', JSON.parse(event.data));
};
```
