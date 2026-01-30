# 视频播放问题修复说明

## 问题描述

视频播放时出现错误：
```
VIDEOJS: ERROR: (CODE:2 MEDIA_ERR_NETWORK)
HLS playlist request error at URL: http://localhost:8080/api/files/videos/39/playlist.m3u8
```

## 问题原因

1. 后端安全修复后，所有 `/files/**` 路径都需要JWT认证
2. video.js在请求HLS文件（.m3u8和.ts片段）时，默认不会添加Authorization头
3. 导致后端返回401未授权错误，视频无法加载

## 修复方案

### 1. 修改VideoPlayer.vue组件

**文件**: `frontend/src/components/VideoPlayer.vue`

**修改内容**:
- 导入useAuthStore获取JWT token
- 配置video.js的全局xhr beforeRequest钩子
- 在每个HLS请求前自动添加Authorization头

**关键代码**:
```typescript
// 全局配置video.js的xhr请求，添加JWT认证
const token = authStore.token

if (token && (videojs as any).Vhs) {
  (videojs as any).Vhs.xhr.beforeRequest = function(options: any) {
    options.headers = options.headers || {}
    options.headers['Authorization'] = `Bearer ${token}`
    return options
  }
}
```

## 测试步骤

### 1. 重启前端服务

```bash
cd frontend
npm run dev
```

### 2. 测试视频播放

1. 登录系统
2. 进入Dashboard页面
3. 选择一个已转码完成的视频
4. 检查视频是否能正常播放

### 3. 检查浏览器控制台

打开浏览器开发者工具（F12），检查：
- Network标签：查看HLS请求是否携带Authorization头
- Console标签：确认没有CORS或认证错误

## 预期结果

- ✅ 视频能正常加载和播放
- ✅ HLS请求携带JWT token
- ✅ 没有401认证错误
- ✅ 没有CORS错误

## 可能的问题和解决方案

### 问题1: 仍然出现401错误

**原因**: JWT token可能已过期或无效

**解决方案**:
1. 退出登录
2. 重新登录获取新token
3. 再次尝试播放视频

### 问题2: CORS错误

**原因**: 后端CORS配置可能不正确

**解决方案**:
检查 `backend/src/main/resources/application.yml`:
```yaml
cors:
  allowed-origins: http://localhost:5173,http://localhost:3000
```

确保包含前端的URL。

### 问题3: video.js版本兼容性

**原因**: 不同版本的video.js配置方式可能不同

**解决方案**:
检查 `package.json` 中的video.js版本，确保使用7.x或8.x版本。

## 后续优化建议

### 1. 添加错误处理

在VideoPlayer.vue中添加错误监听：
```typescript
player.on('error', () => {
  const error = player.error()
  console.error('Video playback error:', error)
  // 显示友好的错误提示
})
```

### 2. 添加加载状态

显示视频加载进度，提升用户体验。

### 3. 处理token过期

当token过期时，自动刷新token或提示用户重新登录。

## 相关文件

- `frontend/src/components/VideoPlayer.vue` - 视频播放器组件
- `backend/src/main/java/com/security/monitor/controller/FileController.java` - 文件访问控制器
- `backend/src/main/java/com/security/monitor/config/SecurityConfig.java` - 安全配置

## 注意事项

⚠️ **重要**:
- 确保后端服务使用Java 17运行
- 确保已配置JWT_SECRET环境变量
- 确保数据库和Redis服务正常运行
