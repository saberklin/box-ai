# AI视频生成服务测试指南

## 概述

本文档介绍如何测试和使用Box-AI系统中新集成的AI视频生成服务。该服务支持多种AI提供商，包括模拟服务（用于开发测试）、Runway ML和Stability AI。

## 服务架构

### AI客户端架构
```
AiVideoService (业务逻辑)
    ↓
AiVideoGenerationClient (接口)
    ↓
具体实现:
- MockAiVideoClient (模拟服务，默认)
- RunwayMLVideoClient (Runway ML)
- StableDiffusionVideoClient (Stability AI)
```

### 流式处理流程
1. **生成请求** → AI服务启动视频生成任务
2. **进度监听** → 实时获取生成进度 (Server-Sent Events)
3. **结果获取** → 获取生成的视频流URL
4. **流媒体推送** → 推送到桌面端显示

## 配置说明

### 1. AI服务提供商配置
在 `application-ai.yml` 中配置：

```yaml
app:
  ai:
    # 选择AI服务提供商: mock, runway, stability
    provider: mock  # 默认使用模拟服务
    
    # Runway ML配置
    runway:
      api-key: ${RUNWAY_API_KEY:your-runway-api-key}
      base-url: https://api.runwayml.com
      model: gen3a_turbo
      
    # Stability AI配置  
    stability:
      api-key: ${STABILITY_API_KEY:your-stability-api-key}
      base-url: https://api.stability.ai
```

### 2. 流媒体服务配置
```yaml
app:
  streaming:
    rtmp-url: rtmp://localhost:1935/live
    hls-url: http://localhost:8080/hls
    webrtc-url: webrtc://localhost:8080/stream
```

## API接口测试

### 1. 启动AI视频生成

**POST** `/api/ai-video/generate`

```json
{
  "roomId": 1001,
  "videoType": "MUSIC_VISUALIZATION",
  "prompt": "科幻未来城市，霓虹灯闪烁，配合音乐节拍",
  "style": "CYBERPUNK",
  "resolution": "1920x1080",
  "frameRate": 30,
  "duration": 60,
  "audioSync": true,
  "currentTrackId": 12345,
  "priority": "HIGH",
  "realtime": true
}
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "streamId": "ai_stream_20240830_001",
    "status": "PENDING",
    "progress": 0,
    "streamUrl": "rtmp://localhost:1935/live/ai_stream_20240830_001",
    "hlsUrl": "http://localhost:8080/hls/ai_stream_20240830_001.m3u8",
    "webrtcUrl": "webrtc://localhost:8080/stream/ai_stream_20240830_001",
    "estimatedTimeRemaining": 60
  }
}
```

### 2. 获取生成状态

**GET** `/api/ai-video/status/{streamId}`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "streamId": "ai_stream_20240830_001",
    "status": "GENERATING",
    "progress": 45,
    "streamUrl": "rtmp://localhost:1935/live/ai_stream_20240830_001",
    "hlsUrl": "http://localhost:8080/hls/ai_stream_20240830_001.m3u8",
    "webrtcUrl": "webrtc://localhost:8080/stream/ai_stream_20240830_001"
  }
}
```

### 3. 实时进度流 (Server-Sent Events)

**GET** `/api/ai-video/stream/progress/{streamId}`

```
Content-Type: text/event-stream

event: progress
data: {"streamId":"ai_stream_20240830_001","status":"GENERATING","progress":20,"message":"分析提示词...","timestamp":1693478400000}

event: progress
data: {"streamId":"ai_stream_20240830_001","status":"GENERATING","progress":50,"message":"生成关键帧...","timestamp":1693478410000}

event: progress
data: {"streamId":"ai_stream_20240830_001","status":"COMPLETED","progress":100,"message":"生成完成","timestamp":1693478460000}
```

### 4. 停止生成

**POST** `/api/ai-video/stop/{streamId}`

```json
{
  "code": 0,
  "message": "success"
}
```

### 5. 预设接口

#### 音乐可视化预设
**POST** `/api/ai-video/presets/music-visualization`
- `roomId`: 包间ID
- `trackId`: 当前播放歌曲ID (可选)
- `style`: 可视化风格 (CYBERPUNK, NATURE, ABSTRACT, RETRO, MODERN)

#### 环境场景预设
**POST** `/api/ai-video/presets/ambient-scene`
- `roomId`: 包间ID
- `sceneType`: 场景类型 (NATURE, URBAN, SPACE, UNDERWATER)
- `duration`: 视频时长（秒）

## 桌面端集成测试

### 1. Redis消息测试
桌面端会监听 `device:ai-video` 频道的消息：

```json
{
  "action": "START_STREAM",
  "streamId": "ai_stream_20240830_001",
  "streamUrl": "rtmp://localhost:1935/live/ai_stream_20240830_001",
  "hlsUrl": "http://localhost:8080/hls/ai_stream_20240830_001.m3u8",
  "webrtcUrl": "webrtc://localhost:8080/stream/ai_stream_20240830_001",
  "roomId": 1001
}
```

### 2. 桌面端功能
- **接收流命令**: 监听Redis消息
- **创建播放器**: 使用JavaFX MediaPlayer
- **全屏显示**: 在主屏幕全屏显示AI视频
- **停止播放**: 接收STOP_STREAM命令时停止

## 模拟服务测试

默认使用MockAiVideoClient，模拟完整的AI视频生成流程：

### 特性
- **模拟进度更新**: 每500ms更新一次进度
- **状态转换**: PENDING → GENERATING → COMPLETED
- **进度消息**: 根据进度显示不同阶段的消息
- **流媒体模拟**: 生成模拟的流URL

### 测试步骤
1. 启动应用程序
2. 调用生成接口
3. 观察控制台日志中的进度更新
4. 使用SSE接口实时监听进度
5. 检查桌面端是否收到Redis消息

## 生产环境部署

### 1. 配置真实AI服务
```bash
# 设置环境变量
export RUNWAY_API_KEY="your-runway-api-key"
export STABILITY_API_KEY="your-stability-api-key"

# 修改配置
app.ai.provider=runway  # 或 stability
```

### 2. 流媒体服务器
需要部署：
- **RTMP服务器**: 如Nginx-RTMP
- **HLS服务器**: 用于HTTP Live Streaming
- **WebRTC服务器**: 用于低延迟流传输

### 3. FFmpeg配置
确保系统安装FFmpeg用于视频格式转换：
```bash
# Windows
choco install ffmpeg

# Linux
apt-get install ffmpeg

# macOS
brew install ffmpeg
```

## 故障排除

### 常见问题

1. **编译错误**: 确保使用Java 21和正确的Maven版本
2. **Redis连接失败**: 检查Redis服务是否启动
3. **AI服务调用失败**: 检查API密钥和网络连接
4. **流媒体播放失败**: 检查流媒体服务器配置

### 日志级别
在 `application-ai.yml` 中设置调试日志：
```yaml
logging:
  level:
    com.boxai.service.impl.MockAiVideoClient: DEBUG
    com.boxai.service.impl.AiVideoServiceImpl: DEBUG
```

## 总结

AI视频生成服务已完全集成到Box-AI系统中，支持：

✅ **多AI提供商支持** (Mock, Runway ML, Stability AI)  
✅ **流式进度监听** (Server-Sent Events)  
✅ **实时流媒体推送** (RTMP/HLS/WebRTC)  
✅ **桌面端集成** (JavaFX + Redis)  
✅ **预设接口** (音乐可视化、环境场景)  
✅ **完整的错误处理和重试机制**  
✅ **生产环境部署支持**  

现在可以开始测试和使用AI视频生成功能了！
