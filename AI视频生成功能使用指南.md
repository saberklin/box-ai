# 🎬 AI实时视频生成功能使用指南

## 📋 功能概述

Box-AI系统现已完整实现**AI生成实时视频推送到桌面端并输出到大屏**的功能，支持：

- 🎨 **AI视频生成**：基于文本提示词生成实时视频内容
- 📡 **实时流推送**：将AI生成的视频实时推送到包间桌面端
- 🖥️ **大屏输出**：自动在包间大屏上全屏显示AI视频
- 🎵 **音乐同步**：可与当前播放的音乐进行节拍同步
- 🎯 **多种预设**：提供音乐可视化、环境场景等快捷模式

## 🏗️ 系统架构

```
微信小程序/Web前端 → Spring Boot后端 → AI视频生成服务 → 流媒体服务器 → JavaFX桌面端 → 大屏显示
                                    ↓                    ↓
                                 PostgreSQL          Redis消息队列
```

## 🚀 核心API接口

### 1. 开始AI视频生成

```http
POST /api/ai-video/generate
Content-Type: application/json

{
  "roomId": 1001,
  "videoType": "MUSIC_VISUALIZATION",
  "prompt": "科幻未来城市，霓虹灯闪烁，配合音乐节拍",
  "style": "CYBERPUNK",
  "resolution": "1920x1080",
  "frameRate": 30,
  "duration": 180,
  "audioSync": true,
  "currentTrackId": 12345,
  "realtime": true
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "streamId": "ai_stream_1693123456789",
    "streamUrl": "rtmp://localhost:1935/live/ai_stream_1693123456789",
    "hlsUrl": "http://localhost:8080/hls/ai_stream_1693123456789.m3u8",
    "webrtcUrl": "webrtc://localhost:8080/stream/ai_stream_1693123456789",
    "status": "PENDING",
    "progress": 0,
    "resolution": "1920x1080",
    "frameRate": 30,
    "estimatedTimeRemaining": 180
  }
}
```

### 2. 查询生成状态

```http
GET /api/ai-video/status/{streamId}
```

### 3. 停止视频生成

```http
POST /api/ai-video/stop/{streamId}
```

### 4. 快捷预设接口

#### 音乐可视化预设
```http
POST /api/ai-video/presets/music-visualization?roomId=1001&trackId=12345&style=CYBERPUNK
```

#### 环境场景预设
```http
POST /api/ai-video/presets/ambient-scene?roomId=1001&sceneType=NATURE&duration=300
```

## 🎯 视频类型和风格

### 视频类型 (videoType)
- **MUSIC_VISUALIZATION**: 音乐可视化 - 根据音乐节拍生成动态视觉效果
- **AMBIENT_SCENE**: 环境场景 - 生成背景氛围场景
- **DANCE_EFFECT**: 舞蹈效果 - 动感舞蹈视觉效果  
- **CUSTOM_THEME**: 自定义主题 - 根据用户提示词自由生成

### 视频风格 (style)
- **CYBERPUNK**: 赛博朋克 - 未来科幻城市，霓虹灯效果
- **NATURE**: 自然风光 - 森林、湖泊、山川等自然景观
- **ABSTRACT**: 抽象艺术 - 流动色彩和几何形状
- **RETRO**: 复古怀旧 - 80年代美学风格
- **MODERN**: 现代简约 - 简洁的几何元素和色彩

## 🔄 完整工作流程

### 1. 用户发起AI视频生成
```
小程序用户选择 "AI视频背景" → 选择风格和提示词 → 发送生成请求
```

### 2. 后端处理流程
```java
// 1. 接收请求并创建会话
AiVideoSession session = new AiVideoSession();
session.setStreamId("ai_stream_" + UUID.randomUUID());
session.setStatus("PENDING");

// 2. 异步启动AI生成
CompletableFuture.runAsync(() -> {
    startAiVideoGeneration(session);
});

// 3. 生成过程中更新进度
updateGenerationProgress(streamId, 50, "GENERATING");

// 4. 开始流式传输时推送到桌面端
pushStreamToDesktop(streamId, roomId);
```

### 3. 桌面端接收和显示
```java
// 1. Redis订阅消息
jedis.subscribe(..., "device:ai-video");

// 2. 处理流开始命令
case "START_STREAM" -> {
    Platform.runLater(() -> 
        app.startAiVideoStream(streamId, streamUrl, hlsUrl, webrtcUrl)
    );
}

// 3. 创建全屏大屏显示
Stage aiVideoStage = new Stage();
aiVideoStage.setFullScreen(true);
MediaView aiVideoView = new MediaView(aiVideoPlayer);
```

## 📊 数据库设计

**t_ai_video_session 表结构**：
```sql
CREATE TABLE t_ai_video_session (
  id bigint PRIMARY KEY,
  room_id bigint REFERENCES t_room(id),
  stream_id varchar(64) UNIQUE NOT NULL,
  video_type varchar(32) NOT NULL,
  prompt text,
  style varchar(32),
  resolution varchar(16) DEFAULT '1920x1080',
  status varchar(16) DEFAULT 'PENDING',
  progress int DEFAULT 0,
  stream_url varchar(512),
  hls_url varchar(512),
  webrtc_url varchar(512),
  start_time timestamptz DEFAULT now(),
  -- 其他字段...
);
```

## 🎮 使用示例

### 示例1: 音乐同步可视化

```bash
# 1. 开始播放音乐
curl -X POST "http://localhost:9998/api/playback/control" \
  -H "Content-Type: application/json" \
  -d '{"roomId":1001,"action":"PLAY","trackId":12345}'

# 2. 启动AI音乐可视化
curl -X POST "http://localhost:9998/api/ai-video/presets/music-visualization" \
  -H "Content-Type: application/json" \
  -d 'roomId=1001&trackId=12345&style=CYBERPUNK'

# 3. 查询生成状态
curl "http://localhost:9998/api/ai-video/status/ai_stream_1693123456789"
```

### 示例2: 自定义场景生成

```bash
# 生成自定义AI视频
curl -X POST "http://localhost:9998/api/ai-video/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 1001,
    "videoType": "CUSTOM_THEME", 
    "prompt": "梦幻的水下世界，五彩斑斓的珊瑚礁，游动的热带鱼群",
    "style": "NATURE",
    "duration": 300,
    "resolution": "1920x1080"
  }'
```

### 示例3: 环境氛围背景

```bash
# 生成自然风光背景
curl -X POST "http://localhost:9998/api/ai-video/presets/ambient-scene" \
  -d 'roomId=1001&sceneType=NATURE&duration=600'
```

## 🔧 技术实现细节

### AI视频生成流程
1. **接收请求** → 创建生成会话记录
2. **调用AI服务** → 连接AI视频生成API（如Runway ML、Stable Video Diffusion）
3. **实时生成** → AI模型根据提示词实时生成视频帧
4. **流媒体推送** → 将生成的视频编码为RTMP/HLS流
5. **桌面端接收** → JavaFX客户端接收并播放视频流
6. **大屏输出** → 自动全屏显示在包间大屏上

### 流媒体技术栈
- **RTMP服务器**: 用于实时流传输
- **HLS服务器**: 提供HTTP Live Streaming
- **WebRTC**: 支持低延迟实时通信
- **JavaFX MediaPlayer**: 桌面端播放器

### 消息通信机制
```java
// Redis频道: device:ai-video
{
  "roomId": 1001,
  "streamId": "ai_stream_12345",
  "action": "START_STREAM",
  "streamUrl": "rtmp://localhost:1935/live/ai_stream_12345",
  "hlsUrl": "http://localhost:8080/hls/ai_stream_12345.m3u8",
  "resolution": "1920x1080",
  "frameRate": 30
}
```

## 🎨 AI提示词最佳实践

### 音乐可视化提示词
- "根据音乐节拍生成动态几何图形，霓虹色彩，赛博朋克风格"
- "流动的粒子效果，随音乐律动变化，抽象艺术风格"
- "音频波形可视化，彩色频谱分析，现代电子风格"

### 环境场景提示词
- "宁静的森林深处，阳光透过树叶洒下，鸟儿轻声歌唱"
- "未来城市夜景，高楼大厦，霓虹灯闪烁，飞行器穿梭"
- "海底珊瑚礁，五彩斑斓，热带鱼群游弋，阳光从海面洒下"

### 抽象艺术提示词
- "流动的液体金属，反射光线，形成抽象的几何图案"
- "彩色烟雾缓慢扩散，形成梦幻般的色彩渐变"
- "分形几何动画，无限递归的图案，数学美学"

## 📈 性能优化

### 生成优化
- **分辨率选择**: 根据包间屏幕选择合适分辨率
- **帧率控制**: 平衡流畅度和性能
- **缓存策略**: 常用场景预生成缓存
- **队列管理**: 多请求时的优先级队列

### 流媒体优化
- **自适应码率**: 根据网络状况调整
- **缓冲策略**: 预加载减少卡顿
- **多协议支持**: RTMP/HLS/WebRTC自动选择

## 🔍 故障排查

### 常见问题

1. **AI视频流无法播放**
   - 检查流媒体服务器是否启动
   - 验证Redis消息是否正常发送
   - 确认JavaFX MediaPlayer支持的格式

2. **生成进度卡住**
   - 检查AI视频生成服务状态
   - 查看后端日志中的错误信息
   - 验证数据库连接和会话记录

3. **大屏显示异常**
   - 检查多屏配置
   - 验证全屏模式权限
   - 确认显示器分辨率设置

### 日志监控
```bash
# 后端日志
tail -f logs/box-ai.log | grep "AI视频"

# 桌面端日志  
# JavaFX控制台输出中查看AI视频相关信息
```

## 🚀 部署指南

### 1. 后端部署
确保以下服务已启动：
- PostgreSQL数据库（包含t_ai_video_session表）
- Redis服务器
- 流媒体服务器（RTMP/HLS）

### 2. 桌面端部署
- 确保JavaFX运行环境
- 配置Redis连接信息
- 测试多屏显示功能

### 3. AI服务集成
- 配置AI视频生成服务API
- 设置认证密钥和配额
- 测试生成和流推送

---

通过这套完整的AI实时视频生成系统，用户可以在KTV包间中享受到**个性化的AI生成视觉背景**，大大提升娱乐体验！系统支持音乐同步、多种风格选择，并能实时推送到大屏显示，真正实现了**智能化的视觉娱乐体验**。
