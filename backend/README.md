# Security Monitor Backend

Intelligent Security Monitoring System - Spring Boot Backend

## Features

- **JWT Authentication** - Secure user authentication with JWT tokens
- **Video Upload & Processing** - Upload videos and transcode to HLS format using FFmpeg
- **Simulated AI Detection** - Mock frame-by-frame analysis with configurable danger behaviors
- **Real-time Alerts** - WebSocket-based real-time alert push to connected clients
- **Admin Configuration** - Manage danger behaviors and alert thresholds
- **Dashboard Statistics** - Aggregated statistics for visualization
- **Redis Caching** - High-performance caching for frequently accessed data
- **MySQL Persistence** - Reliable data storage with Flyway migrations

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+
- FFmpeg 5.0+ (for video transcoding)

## Installation

### 1. Install FFmpeg

**macOS:**
```bash
brew install ffmpeg
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install ffmpeg
```

**Windows:**
Download from https://ffmpeg.org/download.html

### 2. Setup MySQL Database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE security_monitor;
```

### 3. Setup Redis

**macOS:**
```bash
brew install redis
brew services start redis
```

**Ubuntu/Debian:**
```bash
sudo apt install redis-server
sudo systemctl start redis
```

### 4. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/security_monitor
    username: root
    password: your_password

  redis:
    host: localhost
    port: 6379
```

### 5. Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

The backend will start on `http://localhost:8080/api`

## Default Credentials

- **Username:** admin
- **Password:** admin123

## API Endpoints

### Authentication

- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout

### Videos

- `POST /api/videos/upload` - Upload video file
- `GET /api/videos` - Get user's videos
- `GET /api/videos/{id}` - Get video details
- `DELETE /api/videos/{id}` - Delete video

### Alerts

- `GET /api/alerts` - Get all alerts (last 100)
- `GET /api/alerts/video/{videoId}` - Get alerts for specific video
- `GET /api/alerts/unacknowledged` - Get unacknowledged alerts
- `GET /api/alerts/recent?hours=24` - Get recent alerts
- `PUT /api/alerts/{id}/acknowledge` - Acknowledge alert
- `DELETE /api/alerts/{id}` - Delete alert

### Admin - Danger Behaviors

- `GET /api/admin/danger-behaviors` - Get all behaviors
- `GET /api/admin/danger-behaviors/active` - Get active behaviors
- `GET /api/admin/danger-behaviors/{id}` - Get behavior by ID
- `POST /api/admin/danger-behaviors` - Create behavior
- `PUT /api/admin/danger-behaviors/{id}` - Update behavior
- `DELETE /api/admin/danger-behaviors/{id}` - Delete behavior

### Admin - Thresholds

- `GET /api/admin/thresholds` - Get all thresholds
- `GET /api/admin/thresholds/{id}` - Get threshold by ID
- `GET /api/admin/thresholds/behavior/{behaviorId}` - Get threshold by behavior
- `POST /api/admin/thresholds` - Create threshold
- `PUT /api/admin/thresholds/{id}` - Update threshold
- `DELETE /api/admin/thresholds/{id}` - Delete threshold

### Dashboard

- `GET /api/dashboard/stats?hours=24` - Get dashboard statistics

### WebSocket

- `ws://localhost:8080/api/ws/alerts?token={jwt_token}` - Real-time alerts

## Project Structure

```
backend/
├── src/main/java/com/security/monitor/
│   ├── config/              # Configuration classes
│   │   ├── AsyncConfig.java
│   │   ├── CorsConfig.java
│   │   ├── JacksonConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebSocketConfig.java
│   ├── controller/          # REST controllers
│   │   ├── AlertController.java
│   │   ├── AuthController.java
│   │   ├── DangerBehaviorController.java
│   │   ├── DashboardController.java
│   │   ├── ThresholdController.java
│   │   └── VideoController.java
│   ├── dto/                 # Data Transfer Objects
│   ├── entity/              # JPA entities
│   ├── repository/          # Spring Data repositories
│   ├── service/             # Business logic
│   │   ├── AlertPushService.java
│   │   ├── AuthService.java
│   │   ├── CacheService.java
│   │   ├── FrameAnalysisService.java
│   │   └── VideoProcessingService.java
│   ├── util/                # Utility classes
│   │   ├── FFmpegUtil.java
│   │   └── JwtUtil.java
│   ├── websocket/           # WebSocket handlers
│   └── SecurityMonitorApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/        # Flyway migrations
└── storage/                 # File storage
    ├── uploads/             # Original videos
    ├── transcoded/          # HLS output
    └── thumbnails/          # Video thumbnails
```

## Video Processing Flow

1. User uploads video via `/api/videos/upload`
2. Video saved to `storage/uploads/{userId}/{videoId}.mp4`
3. Video entity created with status `UPLOADING`
4. Async transcoding task started
5. FFmpeg transcodes video to HLS format
6. Output saved to `storage/transcoded/{videoId}/`
7. Thumbnail generated
8. Video status updated to `READY`
9. Frame analysis task started
10. Simulated AI detection runs (5% detection rate per second)
11. Alerts generated based on thresholds
12. Alerts pushed via WebSocket to connected clients

## Simulated AI Detection

The system simulates AI-based danger detection with the following logic:

- **Detection Rate:** 5% probability per second of video
- **Confidence Range:** 0.6 to 1.0 (random)
- **Behaviors:** Randomly selects from active danger behaviors
- **Threshold Check:** Only creates alert if confidence >= threshold
- **Rate Limiting:** Respects max alerts per time window

## Database Schema

### Tables

- **users** - User accounts
- **videos** - Uploaded videos and metadata
- **danger_behaviors** - Configurable danger types
- **alert_thresholds** - Detection thresholds per behavior
- **alerts** - Generated alerts
- **alert_statistics** - Hourly aggregated statistics

### Default Danger Behaviors

1. **Fighting** (Severity 4, Critical)
2. **Falling** (Severity 3, High)
3. **Intrusion** (Severity 3, High)
4. **Loitering** (Severity 2, Medium)
5. **Running** (Severity 2, Medium)

## Redis Caching

- `video:{videoId}` - Video metadata (1h TTL)
- `video:list:{userId}` - User's video list (5min TTL)
- `config:danger_behaviors` - Active behaviors (1h TTL)
- `config:thresholds:{behaviorId}` - Thresholds (1h TTL)
- `alert:rate:{behaviorId}:{window}` - Rate limiting counters
- `ws:sessions:{userId}` - Active WebSocket sessions

## Testing

### Test Video Upload

```bash
curl -X POST http://localhost:8080/api/videos/upload \
  -H "Authorization: Bearer {token}" \
  -F "file=@test-video.mp4"
```

### Test WebSocket Connection

```javascript
const ws = new WebSocket('ws://localhost:8080/api/ws/alerts?token=' + token);
ws.onmessage = (event) => {
  console.log('Alert received:', JSON.parse(event.data));
};
```

## Troubleshooting

### FFmpeg not found

Ensure FFmpeg is installed and in PATH:
```bash
ffmpeg -version
```

### MySQL connection failed

Check MySQL is running:
```bash
mysql -u root -p
```

### Redis connection failed

Check Redis is running:
```bash
redis-cli ping
```

Should return `PONG`

## License

MIT License
