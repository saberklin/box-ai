# 交接文档 

1.包厢项目位置  D:\codeDemo\box-ai

2.桌面端项目 D:\codeDemo\box-ai\fx-box

3.nft项目D:\codeDemo\Nft

## 📋 目录
1. [系统架构总览](#系统架构总览)
2. [代码架构分析](#代码架构分析)
3. [完整交互流程](#完整交互流程)
4. [硬件控制详解](#硬件控制详解)
5. [消息传递机制](#消息传递机制)
6. [集成示例代码](#集成示例代码)
7. [部署与运维](#部署与运维)

---

## 🏗️ 系统架构总览

### 整体架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端层        │    │   服务端层      │    │   客户端层      │
│                 │    │                 │    │                 │
│ • 微信小程序    │───▶│ Spring Boot API │───▶│ JavaFX桌面端    │
│ • Web前端       │    │ (端口:9998)     │    │ (fx-box)        │
│ • 移动端App     │    │                 │    │                 │
│                 │    │ • PostgreSQL    │    │ • Redis订阅器   │
│                 │    │ • Redis队列     │    │ • 多屏管理器    │
│                 │    │ • 微信API       │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                               ┌─────────────────┐
                                               │   硬件层        │
                                               │                 │
                                               │ • DMX灯光设备   │
                                               │ • 音视频播放器  │
                                               │ • 多屏幕显示器  │
                                               └─────────────────┘
```

### 核心技术栈
- **服务端**: Spring Boot 3.x + Java 21 + PostgreSQL + Redis + MyBatis-Plus
- **客户端**: JavaFX + Redis Pub/Sub + TCP DMX + Multi-Screen
- **前端**: 微信小程序 + Web前端 + JWT认证

---

## 🔍 代码架构分析

### 服务端架构 (Spring Boot)

#### 核心模块结构
```
src/main/java/com/boxai/
├── controller/                 # 控制器层
│   ├── AuthController.java        # 用户认证
│   ├── RoomController.java        # 房间管理
│   ├── PlaybackController.java    # 播放控制 🔥
│   ├── LightingController.java    # 灯光控制 🔥
│   ├── TrackController.java       # 音乐曲目
│   ├── UserProfileController.java # 用户画像
│   └── SearchController.java      # 搜索功能
├── service/                    # 服务层
│   ├── DeviceControlService.java  # 设备控制服务 🔥
│   ├── WechatAuthService.java     # 微信认证
│   └── impl/                      # 服务实现
├── domain/                     # 数据模型
│   ├── entity/                    # 实体类
│   ├── dto/                       # 数据传输对象
│   │   ├── device/                # 设备控制命令 🔥
│   │   └── request/               # 请求参数
│   └── mapper/                    # 数据访问层
├── auth/                       # JWT认证模块
└── common/                     # 公共组件
```

#### 关键服务分析

**DeviceControlService - 设备控制核心**
```java
@Service
public class DeviceControlServiceImpl implements DeviceControlService {
    private static final String CHANNEL = "device:control";      // 播放控制频道
    private static final String LIGHT_CHANNEL = "device:light";  // 灯光控制频道
    
    // 发布播放控制命令
    public void publish(DeviceControlCommand command) {
        String payload = objectMapper.writeValueAsString(command);
        stringRedisTemplate.convertAndSend(CHANNEL, payload);
    }
    
    // 发布灯光控制命令
    public void publishLighting(LightingControlCommand command) {
        String payload = objectMapper.writeValueAsString(command);
        stringRedisTemplate.convertAndSend(LIGHT_CHANNEL, payload);
    }
}
```

### 客户端架构 (JavaFX)

#### 核心组件结构
```
fx-box/src/main/java/app/
├── MainApp.java                    # 主应用程序 🔥
├── DeviceControlSubscriber.java    # Redis消息订阅器 🔥
├── MultiScreenManager.java         # 多屏显示管理器
└── MappingDialog.java              # 屏幕映射配置
```

#### 关键组件分析

**MainApp - 主控制中心**
```java
public class MainApp extends Application {
    // 媒体播放器
    private MediaPlayer videoPlayer;
    private MediaPlayer audioPlayer;
    
    // DMX灯光控制
    private Socket dmxSocket;
    private byte[] dmxData = new byte[512];
    
    // Redis订阅器
    private DeviceControlSubscriber subscriber;
    
    @Override
    public void start(Stage stage) {
        // 启动Redis订阅
        subscriber = new DeviceControlSubscriber(this, "127.0.0.1", 6379, "device:control");
        subscriber.start();
    }
}
```

**DeviceControlSubscriber - 消息处理中心**
```java
public class DeviceControlSubscriber {
    // 订阅两个频道：播放控制 + 灯光控制
    jedis.subscribe(new JedisPubSub() {
        @Override 
        public void onMessage(String ch, String msg) {
            handleMessage(msg);  // 统一消息处理
        }
    }, "device:control", "device:light");
    
    private void handleMessage(String msg) {
        JsonNode root = mapper.readTree(msg);
        String action = text(root, "action");
        
        switch (action == null ? "" : action.toUpperCase()) {
            case "PLAY" -> Platform.runLater(app::togglePlay);
            case "PAUSE" -> Platform.runLater(() -> app.videoPlayer().pause());
            case "STOP" -> Platform.runLater(() -> app.videoPlayer().stop());
            case "NEXT" -> Platform.runLater(app::playNextFromList);
            case "" -> { // 灯光命令处理
                Integer brightness = root.get("brightness").asInt();
                String color = root.get("color").asText();
                String rhythm = root.get("rhythm").asText();
                Platform.runLater(() -> app.applyLight(brightness, color, rhythm));
            }
        }
    }
}
```

---

## 🔄 完整交互流程

### 流程1: 播放控制流程

```
微信小程序 → Spring Boot API → Redis消息队列 → JavaFX客户端 → 媒体播放器

1. 小程序发起播放请求
   POST /api/playback/control
   {roomId:1, action:"PLAY"}

2. 服务端验证JWT并发布Redis消息
   Redis频道: device:control
   消息内容: {"roomId":1,"action":"PLAY","timestamp":...}

3. JavaFX客户端接收消息
   DeviceControlSubscriber.handleMessage()
   Platform.runLater(app::togglePlay)

4. 硬件响应
   mediaPlayer.play() 开始播放媒体文件
```

### 流程2: 灯光控制流程

```
微信小程序 → Spring Boot API → PostgreSQL → Redis消息队列 → JavaFX客户端 → DMX灯光设备

1. 小程序发起灯光调节
   POST /api/lighting
   {roomId:1, brightness:80, color:"#FF6B6B", rhythm:"SOFT"}

2. 服务端保存设置并发布消息
   - 保存到PostgreSQL数据库
   - Redis频道: device:light
   - 消息: {"roomId":1,"brightness":80,"color":"#FF6B6B","rhythm":"SOFT"}

3. JavaFX客户端处理灯光命令
   - 转换为DMX数据: dmxData[0]=brightness*2.55, dmxData[1,2,3]=RGB值
   - TCP发送: Socket.write(dmxData)

4. DMX硬件响应
   灯光设备接收DMX512数据并改变灯光效果
```

### 流程3: 用户认证流程

```
微信小程序 → 微信API → Spring Boot API → PostgreSQL → Redis缓存

1. 小程序获取登录凭证
   wx.login() 获取 jsCode

2. 服务端调用微信API
   GET /sns/jscode2session (appid + secret + jsCode)
   返回: {openid, session_key, unionid}

3. 用户信息处理
   - 查询/创建用户记录 (PostgreSQL)
   - 生成JWT令牌
   - 缓存session_key (Redis, 7天)

4. 返回认证信息
   {token, userId, openId}
```

### 流程4: 房间管理流程

```
微信小程序 → Spring Boot API → PostgreSQL → 微信API → 二维码服务

1. 房间绑定
   POST /api/rooms/bind {roomCode:"R001", userId:123}
   - 查询/创建房间
   - 添加房间成员

2. 生成小程序码
   GET /api/rooms/1/qr
   - 调用微信小程序码生成API
   - Base64编码处理
   - 返回: {qrCodeBase64, roomId, version}
```

---

## 🎛️ 硬件控制详解

### DMX灯光控制协议

#### TCP连接建立
```java
// JavaFX客户端连接DMX设备
private void toggleDmx() {
    dmxSocket = new Socket();
    dmxSocket.connect(new InetSocketAddress("127.0.0.1", 6454), 2000);
    // 默认连接 Art-Net标准端口 6454
}
```

#### DMX数据帧格式
```java
// DMX 512通道数据映射
private void sendDmxFrame() {
    OutputStream os = dmxSocket.getOutputStream();
    int universe = uniField.getValue();  // DMX宇宙编号
    
    // 发送宇宙编号 (2字节)
    byte hi = (byte)((universe >> 8) & 0xFF);
    byte lo = (byte)(universe & 0xFF);
    os.write(new byte[]{hi, lo});
    
    // 发送512通道数据
    os.write(dmxData, 0, 512);
    os.flush();
}
```

#### 灯光参数映射
```java
void applyLight(Integer brightness, String colorHex, String rhythm) {
    // 通道映射规则
    if (brightness != null) 
        dmxData[0] = (byte)(brightness * 2.55);  // 通道1: 总亮度 (0-100 → 0-255)
    
    // 解析十六进制颜色
    if (colorHex != null && colorHex.startsWith("#")) {
        int r = Integer.parseInt(colorHex.substring(1, 3), 16);
        int g = Integer.parseInt(colorHex.substring(3, 5), 16);
        int b = Integer.parseInt(colorHex.substring(5, 7), 16);
        dmxData[1] = (byte) r;  // 通道2: 红色
        dmxData[2] = (byte) g;  // 通道3: 绿色
        dmxData[3] = (byte) b;  // 通道4: 蓝色
    }
    
    // 节奏模式映射
    if (rhythm != null) {
        int value = switch (rhythm.toUpperCase()) {
            case "SOFT" -> 32;     // 轻柔模式
            case "NORMAL" -> 96;   // 普通模式
            case "STRONG" -> 160;  // 强烈模式
            case "AUTO" -> 224;    // 自动模式
            default -> 0;
        };
        dmxData[4] = (byte) value;  // 通道5: 节奏模式
    }
    
    sendDmxFrame();  // 立即发送到硬件
}
```

### 多媒体播放控制

#### 视频播放器控制
```java
public void togglePlay() {
    if (videoPlayer == null) return;
    switch (videoPlayer.getStatus()) {
        case PLAYING -> videoPlayer.pause();
        case PAUSED, READY, STOPPED -> videoPlayer.play();
    }
}

void playNextFromList() {
    int idx = videoList.getSelectionModel().getSelectedIndex();
    if (idx + 1 < videoList.getItems().size()) {
        videoList.getSelectionModel().select(idx + 1);
        playSelected();
    }
}
```

#### 多屏显示管理
```java
// MultiScreenManager - 多屏幕输出管理
public void showOnScreens(List<MappingItem> mappings, MediaPlayer player) {
    for (MappingItem item : mappings) {
        Screen screen = Screen.getScreens().get(item.screenIndex);
        
        // 创建新窗口显示在指定屏幕
        Stage stage = new Stage();
        MediaView view = new MediaView(player);
        
        // 设置窗口位置和大小
        Rectangle2D bounds = screen.getBounds();
        stage.setX(bounds.getMinX() + item.x * bounds.getWidth());
        stage.setY(bounds.getMinY() + item.y * bounds.getHeight());
        stage.setWidth(item.w * bounds.getWidth());
        stage.setHeight(item.h * bounds.getHeight());
        
        stage.show();
    }
}
```

---

## 📡 消息传递机制

### Redis Pub/Sub 频道设计

#### 频道定义
```java
// 服务端发布频道
private static final String CHANNEL = "device:control";      // 播放控制
private static final String LIGHT_CHANNEL = "device:light";  // 灯光控制
```

#### 消息格式标准

**播放控制命令**
```json
{
  "roomId": 1,
  "action": "PLAY",        // PLAY, PAUSE, STOP, NEXT, PREVIOUS
  "trackId": 456,          // 可选：指定曲目ID
  "userId": 123,           // 操作用户ID
  "timestamp": 1718000000000
}
```

**灯光控制命令**
```json
{
  "roomId": 1,
  "brightness": 80,        // 0-100
  "color": "#FF6B6B",      // 十六进制颜色
  "rhythm": "SOFT",        // SOFT, NORMAL, STRONG, AUTO
  "timestamp": 1718000000000
}
```

#### 客户端订阅处理
```java
// 同时订阅两个频道
jedis.subscribe(new JedisPubSub() {
    @Override 
    public void onMessage(String channel, String message) {
        // 根据消息内容判断类型
        JsonNode root = mapper.readTree(message);
        String action = root.get("action");
        
        if (action != null) {
            // 播放控制命令
            handlePlaybackControl(action, root);
        } else {
            // 灯光控制命令 (无action字段)
            handleLightingControl(root);
        }
    }
}, "device:control", "device:light");
```

---

## 💻 集成示例代码

### 小程序端集成

#### 播放控制
```javascript
// 小程序播放控制API
const playbackAPI = {
  // 播放/暂停
  togglePlay: (roomId) => {
    return wx.request({
      url: `${config.apiBase}/api/playback/control`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${wx.getStorageSync('token')}`
      },
      data: {
        roomId: roomId,
        action: 'PLAY'
      }
    });
  },
  
  // 下一首
  playNext: (roomId) => {
    return wx.request({
      url: `${config.apiBase}/api/playback/control`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${wx.getStorageSync('token')}`
      },
      data: {
        roomId: roomId,
        action: 'NEXT'
      }
    });
  },
  
  // 添加到播放队列
  addToQueue: (roomId, trackId) => {
    return wx.request({
      url: `${config.apiBase}/api/playback/queue`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${wx.getStorageSync('token')}`
      },
      data: {
        roomId: roomId,
        trackId: trackId,
        userId: wx.getStorageSync('userId')
      }
    });
  }
};
```

#### 灯光控制
```javascript
// 小程序灯光控制API
const lightingAPI = {
  // 设置灯光
  setLighting: (roomId, brightness, color, rhythm) => {
    return wx.request({
      url: `${config.apiBase}/api/lighting`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${wx.getStorageSync('token')}`
      },
      data: {
        roomId: roomId,
        brightness: brightness,
        color: color,
        rhythm: rhythm
      }
    });
  },
  
  // 预设模式
  setPreset: (roomId, preset) => {
    const presets = {
      'romantic': { brightness: 30, color: '#FF69B4', rhythm: 'SOFT' },
      'party': { brightness: 100, color: '#FF0000', rhythm: 'STRONG' },
      'relax': { brightness: 50, color: '#87CEEB', rhythm: 'NORMAL' }
    };
    
    const setting = presets[preset];
    return lightingAPI.setLighting(roomId, setting.brightness, setting.color, setting.rhythm);
  }
};
```

### Web前端集成

#### React播放控制组件
```jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

const PlaybackControl = ({ roomId }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [queue, setQueue] = useState([]);
  
  // 播放控制
  const handlePlayToggle = async () => {
    try {
      const action = isPlaying ? 'PAUSE' : 'PLAY';
      await axios.post('/api/playback/control', {
        roomId,
        action
      });
      setIsPlaying(!isPlaying);
    } catch (error) {
      console.error('播放控制失败:', error);
    }
  };
  
  // 获取播放队列
  const fetchQueue = async () => {
    try {
      const response = await axios.get(`/api/playback/queue/${roomId}`);
      setQueue(response.data.data);
    } catch (error) {
      console.error('获取队列失败:', error);
    }
  };
  
  useEffect(() => {
    fetchQueue();
  }, [roomId]);
  
  return (
    <div className="playback-control">
      <button onClick={handlePlayToggle}>
        {isPlaying ? '暂停' : '播放'}
      </button>
      <button onClick={() => handleControl('NEXT')}>下一首</button>
      <button onClick={() => handleControl('STOP')}>停止</button>
      
      <div className="queue">
        <h3>播放队列</h3>
        {queue.map(item => (
          <div key={item.id} className="queue-item">
            {item.trackTitle} - {item.artist}
          </div>
        ))}
      </div>
    </div>
  );
};
```

#### React灯光控制组件
```jsx
const LightingControl = ({ roomId }) => {
  const [brightness, setBrightness] = useState(80);
  const [color, setColor] = useState('#FF6B6B');
  const [rhythm, setRhythm] = useState('NORMAL');
  
  const handleLightingChange = async () => {
    try {
      await axios.post('/api/lighting', {
        roomId,
        brightness,
        color,
        rhythm
      });
    } catch (error) {
      console.error('灯光控制失败:', error);
    }
  };
  
  return (
    <div className="lighting-control">
      <div className="brightness-control">
        <label>亮度: {brightness}%</label>
        <input
          type="range"
          min="0"
          max="100"
          value={brightness}
          onChange={(e) => setBrightness(Number(e.target.value))}
          onMouseUp={handleLightingChange}
        />
      </div>
      
      <div className="color-control">
        <label>颜色:</label>
        <input
          type="color"
          value={color}
          onChange={(e) => setColor(e.target.value)}
          onBlur={handleLightingChange}
        />
      </div>
      
      <div className="rhythm-control">
        <label>节奏:</label>
        <select
          value={rhythm}
          onChange={(e) => {
            setRhythm(e.target.value);
            handleLightingChange();
          }}
        >
          <option value="SOFT">轻柔</option>
          <option value="NORMAL">普通</option>
          <option value="STRONG">强烈</option>
          <option value="AUTO">自动</option>
        </select>
      </div>
      
      <div className="preset-buttons">
        <button onClick={() => setPreset('romantic')}>浪漫模式</button>
        <button onClick={() => setPreset('party')}>派对模式</button>
        <button onClick={() => setPreset('relax')}>放松模式</button>
      </div>
    </div>
  );
};
```

---

## 🚀 部署与运维

### 1. 环境配置

#### 服务端配置 (application.yml)
```yaml
server:
  port: 9998

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai-box
    username: postgres
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

app:
  wechat:
    appid: your_wechat_appid
    secret: your_wechat_secret
  jwt:
    secret: your_jwt_secret_key_32_chars_min
```

#### 客户端配置
```java
// MainApp.java 中的配置
// Redis连接配置
subscriber = new DeviceControlSubscriber(
    this, 
    "127.0.0.1",  // Redis服务器地址
    6379,         // Redis端口
    "device:control"
);

// DMX设备连接配置
ipField.setText("127.0.0.1");    // DMX设备IP
portField.setValue(6454);        // Art-Net标准端口
uniField.setValue(0);            // DMX宇宙编号
```

### 2. 启动脚本

#### 服务端启动 (start-server.sh)
```bash
#!/bin/bash
# 检查环境
java -version || { echo "Java 21+ required"; exit 1; }
pg_isready -h localhost -p 5432 || { echo "PostgreSQL not ready"; exit 1; }
redis-cli ping || { echo "Redis not ready"; exit 1; }

# 启动应用
echo "Starting Box-AI Server..."
java -jar target/box-ai-*.jar \
  --spring.profiles.active=prod \
  --server.port=9998

echo "Server started at http://localhost:9998"
echo "Swagger UI: http://localhost:9998/swagger-ui/index.html"
```

#### 客户端启动 (start-client.sh)
```bash
#!/bin/bash
cd fx-box

# 检查环境
mvn -version || { echo "Maven required"; exit 1; }
java --list-modules | grep javafx || { echo "JavaFX modules not found"; exit 1; }

# 启动JavaFX应用
echo "Starting JavaFX Client..."
mvn javafx:run

echo "JavaFX Client started"
```

### 3. 监控与故障排查

#### 服务监控
```bash
# 检查服务状态
curl -f http://localhost:9998/actuator/health

# 监控Redis消息
redis-cli monitor | grep "device:"

# 查看应用日志
tail -f logs/box-ai.log | grep -E "(ERROR|WARN|device:)"
```

#### 常见问题解决

**Redis连接失败**
```bash
# 检查Redis服务
systemctl status redis
redis-cli ping

# 启动Redis
sudo systemctl start redis
```

**DMX设备连接失败**
```bash
# 检查网络连通性
ping dmx_device_ip
telnet dmx_device_ip 6454

# 检查防火墙
sudo ufw allow 6454/tcp
```

**微信API调用失败**
```bash
# 检查网络连接
curl "https://api.weixin.qq.com/sns/jscode2session?appid=test&secret=test&js_code=test&grant_type=authorization_code"

# 验证配置
grep -E "appid|secret" src/main/resources/application.yml
```

### 4. 性能优化

#### 服务端优化
```yaml
# application.yml 性能配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  data:
    redis:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8

# JVM参数
java -Xms512m -Xmx2g -XX:+UseG1GC -jar box-ai.jar
```

#### 客户端优化
```java
// JavaFX性能优化
System.setProperty("prism.vsync", "false");
System.setProperty("prism.lcdtext", "false");
System.setProperty("javafx.animation.fullspeed", "true");
```

---

## 📞 API接口汇总

### 认证接口
- `POST /api/auth/wechat/login` - 微信静默登录
- `GET /api/auth/me` - 获取当前用户信息

### 房间管理
- `POST /api/rooms/bind` - 绑定房间
- `GET /api/rooms/{id}/qr` - 获取房间小程序码
- `POST /api/rooms/{id}/reset` - 重置房间状态

### 播放控制
- `POST /api/playback/control` - 播放控制 (播放/暂停/停止/下一首)
- `POST /api/playback/queue` - 添加到播放队列
- `GET /api/playback/queue/{roomId}` - 获取播放队列

### 灯光控制
- `POST /api/lighting` - 设置灯光效果
- `GET /api/lighting/{roomId}` - 获取当前灯光设置

### 音乐管理
- `GET /api/tracks/search` - 搜索音乐
- `POST /api/tracks/like` - 收藏音乐
- `GET /api/tracks/popular` - 获取热门音乐

### 用户画像
- `GET /api/user-profile/analysis` - 获取用户偏好分析
- `POST /api/user-profile/behavior` - 记录用户行为

---

## 🎉 总结

**Box-AI KTV系统实现了完整的端到端交互链路：**

1. **前端交互**: 微信小程序/Web前端发起操作请求
2. **服务端处理**: Spring Boot接收请求，业务处理，发布Redis消息
3. **消息传递**: Redis Pub/Sub实现实时消息推送
4. **客户端响应**: JavaFX接收消息，控制硬件设备
5. **硬件执行**: DMX灯光设备、多媒体播放器等执行具体操作

**核心特点：**
- ✅ 实时响应：Redis Pub/Sub确保毫秒级消息传递
- ✅ 硬件集成：TCP DMX协议直接控制专业灯光设备
- ✅ 多屏支持：JavaFX MultiScreenManager管理多显示器输出
- ✅ 微信生态：完整的微信小程序认证和二维码生成
- ✅ 数据持久化：PostgreSQL存储业务数据，Redis缓存热点数据
- ✅ 监控运维：完善的日志记录和故障排查机制

现在你可以根据这份文档快速理解整个系统的架构和交互流程，并进行相应的开发和集成工作！

---

## 🏭 生产环境部署方案

### 1. 软件部署位置详解

#### 云端服务器部署 (阿里云/腾讯云等)

**部署内容：Spring Boot后端服务**
```
部署位置: 云服务器 (如: api.boxai.com)
部署软件:
├── box-ai-backend.jar          # Spring Boot应用
├── PostgreSQL数据库            # 用户数据、房间信息、播放记录等
├── Redis集群                   # 消息队列和缓存
├── Nginx负载均衡器             # 反向代理和负载均衡
└── 监控系统 (Prometheus/Grafana)

服务器配置:
- 操作系统: Ubuntu 22.04 LTS 或 CentOS 8
- 运行环境: Docker + Docker Compose
- 网络: 公网IP + 域名解析
- 端口: 80(HTTP), 443(HTTPS), 6379(Redis)
```

**云端部署目录结构：**
```
/opt/boxai/
├── docker-compose.prod.yml     # Docker编排文件
├── nginx.conf                  # Nginx配置
├── ssl/                        # SSL证书
│   ├── cert.pem
│   └── key.pem
├── logs/                       # 日志目录
├── backups/                    # 备份目录
└── app/
    └── box-ai-backend.jar      # Spring Boot应用
```

#### 包间主机部署 (每个包间一台)

**部署内容：JavaFX桌面客户端**
```
部署位置: 包间主机 (如: 192.168.20.10, 192.168.20.11...)
部署软件:
├── fx-box.jar                  # JavaFX桌面应用
├── Java 21运行环境             # JDK/JRE
├── Redis客户端库               # 用于订阅云端消息
├── DMX控制驱动                 # 灯光设备驱动
└── 多媒体解码器                # 音视频播放支持

硬件环境:
- 操作系统: Windows 11 或 Ubuntu 22.04
- 硬件: i5处理器 + 16GB内存 + 512GB SSD
- 网络: 千兆以太网连接到门店局域网
- 接口: HDMI多屏输出 + USB设备接口
```

**包间主机部署目录结构：**
```
Windows系统:
C:\Program Files\BoxAI\
├── fx-box.jar                           # JavaFX客户端
├── config\
│   └── application-prod.properties      # 生产环境配置
├── logs\                               # 客户端日志
├── media\                              # 本地媒体文件
└── scripts\
    └── start.bat                       # 启动脚本

Linux系统:
/opt/boxai/
├── fx-box.jar                          # JavaFX客户端
├── config/
│   └── application-prod.properties     # 生产环境配置
├── logs/                               # 客户端日志
├── media/                              # 本地媒体文件
└── scripts/
    └── start.sh                        # 启动脚本
```

### 2. 配置文件部署

#### 云端服务配置 (部署在云服务器)

**application-prod.yml** (Spring Boot配置)
```yaml
# 部署在: /opt/boxai/app/application-prod.yml
server:
  port: 9998

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_box
    username: boxai
    password: ${DB_PASSWORD}
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}

app:
  wechat:
    appid: ${WECHAT_APPID}
    secret: ${WECHAT_SECRET}
  jwt:
    secret: ${JWT_SECRET}
```

**docker-compose.prod.yml** (Docker编排配置)
```yaml
version: '3.8'
services:
  api-1:
    image: boxai/api:latest
    ports:
      - "9998:9998"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres-master
      - REDIS_HOST=redis-cluster
    restart: always
    
  api-2:
    image: boxai/api:latest
    ports:
      - "9999:9998"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres-master
      - REDIS_HOST=redis-cluster
    restart: always

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - api-1
      - api-2
    restart: always

  postgres-master:
    image: postgres:15
    environment:
      POSTGRES_DB: ai_box
      POSTGRES_USER: boxai
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: always

  redis-cluster:
    image: redis:7-alpine
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    restart: always

volumes:
  postgres_data:
  redis_data:
```

#### 包间主机配置 (部署在每台包间主机)

**application-prod.properties** (JavaFX客户端配置)
```properties
# 部署位置: 
# Windows: C:\Program Files\BoxAI\config\application-prod.properties
# Linux: /opt/boxai/config/application-prod.properties

# 房间标识 (每台主机不同)
room.id=${ROOM_ID}              # 如: R001, R002, R003...
room.name=${ROOM_NAME}          # 如: VIP包间A, 豪华包间B...

# 云端服务连接 (所有主机相同)
api.base.url=https://api.boxai.com
redis.host=redis.boxai.com      # 云端Redis地址
redis.port=6379
redis.password=${REDIS_PASSWORD}
redis.ssl.enabled=true

# 本地设备连接 (每台主机可能不同)
dmx.host=192.168.20.100         # 本包间DMX设备IP
dmx.port=6454
dmx.universe=0

# 显示配置
display.primary.width=1920
display.primary.height=1080
display.secondary.width=1920
display.secondary.height=1080

# 性能优化
javafx.animation.fullspeed=true
prism.vsync=false
prism.lcdtext=false
```

**具体配置示例：**

**包间A配置文件：**
```properties
# 房间标识 (包间A专属)
room.id=R001
room.name=VIP包间A

# 云端服务连接 (所有包间相同)
api.base.url=https://api.boxai.com
redis.host=redis.boxai.com
redis.port=6379
redis.password=your_redis_password_here
redis.ssl.enabled=true

# 本地设备连接 (包间A的设备IP)
dmx.host=192.168.20.101
dmx.port=6454
dmx.universe=0

# 性能优化
javafx.animation.fullspeed=true
prism.vsync=false
prism.lcdtext=false
```

**包间B配置文件：**
```properties
# 房间标识 (包间B专属)
room.id=R002
room.name=豪华包间B

# 云端服务连接 (所有包间相同)
api.base.url=https://api.boxai.com
redis.host=redis.boxai.com
redis.port=6379
redis.password=your_redis_password_here
redis.ssl.enabled=true

# 本地设备连接 (包间B的设备IP)
dmx.host=192.168.20.102
dmx.port=6454
dmx.universe=0

# 性能优化
javafx.animation.fullspeed=true
prism.vsync=false
prism.lcdtext=false
```

### 3. 启动脚本配置

#### Windows启动脚本
```batch
@echo off
REM start.bat - Windows启动脚本
REM 部署位置: C:\Program Files\BoxAI\scripts\start.bat

cd /d "C:\Program Files\BoxAI"

echo 启动Box-AI包间客户端...
echo 房间配置文件: config\application-prod.properties

java -Xms512m -Xmx2g -XX:+UseG1GC ^
     -Dspring.config.location=config/application-prod.properties ^
     -Djavafx.animation.fullspeed=true ^
     -Dprism.vsync=false ^
     -Dprism.lcdtext=false ^
     -jar fx-box.jar

if %ERRORLEVEL% NEQ 0 (
    echo 启动失败，错误代码: %ERRORLEVEL%
    pause
)
```

#### Linux启动脚本
```bash
#!/bin/bash
# start.sh - Linux启动脚本
# 部署位置: /opt/boxai/scripts/start.sh

cd /opt/boxai

echo "启动Box-AI包间客户端..."
echo "房间配置文件: config/application-prod.properties"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ Java未安装，请先安装Java 21"
    exit 1
fi

# 检查配置文件
if [ ! -f "config/application-prod.properties" ]; then
    echo "❌ 配置文件不存在: config/application-prod.properties"
    exit 1
fi

# 启动应用
java -Xms512m -Xmx2g -XX:+UseG1GC \
     -Dspring.config.location=config/application-prod.properties \
     -Djavafx.animation.fullspeed=true \
     -Dprism.vsync=false \
     -Dprism.lcdtext=false \
     -jar fx-box.jar

echo "✅ 应用已退出"
```

### 4. 批量部署脚本

#### 批量配置部署脚本
```bash
#!/bin/bash
# deploy-room-configs.sh - 批量部署包间配置

# 房间配置列表
declare -A ROOMS=(
    ["192.168.20.10"]="R001,VIP包间A,192.168.20.101"
    ["192.168.20.11"]="R002,豪华包间B,192.168.20.102"
    ["192.168.20.12"]="R003,标准包间C,192.168.20.103"
    ["192.168.20.13"]="R004,商务包间D,192.168.20.104"
    ["192.168.20.14"]="R005,家庭包间E,192.168.20.105"
)

# 云端服务配置
API_BASE_URL="https://api.boxai.com"
REDIS_HOST="redis.boxai.com"
REDIS_PASSWORD="your_redis_password_here"

echo "🚀 开始批量部署包间配置..."

for room_ip in "${!ROOMS[@]}"; do
    IFS=',' read -r room_id room_name dmx_host <<< "${ROOMS[$room_ip]}"
    
    echo "📍 配置包间: $room_name ($room_ip)"
    
    # 远程创建配置文件
    ssh root@$room_ip << EOF
        # 创建目录
        mkdir -p /opt/boxai/config
        mkdir -p /opt/boxai/logs
        mkdir -p /opt/boxai/scripts
        
        # 创建配置文件
        cat > /opt/boxai/config/application-prod.properties << EOL
# 房间标识
room.id=$room_id
room.name=$room_name

# 云端服务连接
api.base.url=$API_BASE_URL
redis.host=$REDIS_HOST
redis.port=6379
redis.password=$REDIS_PASSWORD
redis.ssl.enabled=true

# 本地设备连接
dmx.host=$dmx_host
dmx.port=6454
dmx.universe=0

# 显示配置
display.primary.width=1920
display.primary.height=1080
display.secondary.width=1920
display.secondary.height=1080

# 性能优化
javafx.animation.fullspeed=true
prism.vsync=false
prism.lcdtext=false
EOL

        # 创建启动脚本
        cat > /opt/boxai/scripts/start.sh << 'EOL'
#!/bin/bash
cd /opt/boxai

echo "启动Box-AI包间客户端..."
echo "房间配置: \$(grep 'room.name=' config/application-prod.properties | cut -d'=' -f2)"

java -Xms512m -Xmx2g -XX:+UseG1GC \\
     -Dspring.config.location=config/application-prod.properties \\
     -jar fx-box.jar
EOL

        # 设置执行权限
        chmod +x /opt/boxai/scripts/start.sh
        
        echo "✅ 配置完成: $room_name"
EOF
    
    if [ $? -eq 0 ]; then
        echo "✅ 包间 $room_name 配置成功"
    else
        echo "❌ 包间 $room_name 配置失败"
    fi
done

echo "🎉 批量部署完成！"
echo ""
echo "📋 部署总结:"
echo "  配置文件: /opt/boxai/config/application-prod.properties"
echo "  启动脚本: /opt/boxai/scripts/start.sh"
echo "  日志目录: /opt/boxai/logs/"
echo ""
echo "🚀 启动命令: ssh root@<包间IP> '/opt/boxai/scripts/start.sh'"
```

### 5. 配置项说明

| 配置项 | 说明 | 每个包间是否不同 | 示例值 |
|--------|------|------------------|--------|
| `room.id` | 房间唯一标识 | ✅ 不同 | R001, R002, R003... |
| `room.name` | 房间显示名称 | ✅ 不同 | VIP包间A, 豪华包间B... |
| `api.base.url` | 云端API地址 | ❌ 相同 | https://api.boxai.com |
| `redis.host` | Redis服务器地址 | ❌ 相同 | redis.boxai.com |
| `redis.password` | Redis密码 | ❌ 相同 | your_redis_password |
| `dmx.host` | 本包间DMX设备IP | ✅ 不同 | 192.168.20.101, 102... |
| `dmx.port` | DMX设备端口 | ❌ 相同 | 6454 |
| `dmx.universe` | DMX宇宙编号 | ✅ 可能不同 | 0, 1, 2... |

### 6. 部署验证

#### 验证配置文件
```bash
# 检查配置文件是否存在
ls -la /opt/boxai/config/application-prod.properties

# 查看配置内容
cat /opt/boxai/config/application-prod.properties

# 验证房间ID配置
grep "room.id=" /opt/boxai/config/application-prod.properties
```

#### 验证应用启动
```bash
# 启动应用
/opt/boxai/scripts/start.sh

# 查看启动日志
tail -f /opt/boxai/logs/application.log

# 应该看到类似输出：
# 2024-12-XX XX:XX:XX INFO  - 房间ID: R001
# 2024-12-XX XX:XX:XX INFO  - 房间名称: VIP包间A  
# 2024-12-XX XX:XX:XX INFO  - Redis连接成功: redis.boxai.com:6379
# 2024-12-XX XX:XX:XX INFO  - DMX设备连接: 192.168.20.101:6454
```

#### 健康检查
```bash
# 检查应用健康状态
curl http://localhost:8080/health

# 预期返回：
# {
#   "status": "UP",
#   "redis": "CONNECTED",
#   "dmx": "CONNECTED",
#   "room": {
#     "id": "R001",
#     "name": "VIP包间A"
#   }
# }
```

### 7. 部署架构总结

```
┌─────────────────────────────────────────────────────────────┐
│                      云端部署                               │
│  服务器: api.boxai.com (公网)                               │
│  软件: Spring Boot + PostgreSQL + Redis + Nginx            │
│  配置: /opt/boxai/app/application-prod.yml                  │
│        /opt/boxai/docker-compose.prod.yml                  │
└─────────────────────────────────────────────────────────────┘
                              │ (互联网连接)
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    门店网络环境                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  包间主机A  │  │  包间主机B  │  │  包间主机N  │        │
│  │192.168.20.10│  │192.168.20.11│  │192.168.20.N │        │
│  │             │  │             │  │             │        │
│  │fx-box.jar   │  │fx-box.jar   │  │fx-box.jar   │        │
│  │ROOM_ID=R001 │  │ROOM_ID=R002 │  │ROOM_ID=R00N │        │
│  │DMX=.20.101  │  │DMX=.20.102  │  │DMX=.20.10N  │        │
│  │             │  │             │  │             │        │
│  │config/      │  │config/      │  │config/      │        │
│  │├─app-prod.  │  │├─app-prod.  │  │├─app-prod.  │        │
│  │└─properties │  │└─properties │  │└─properties │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

**关键要点：**
- ✅ **配置文件位置**: 每台包间主机的 `config/application-prod.properties`
- ✅ **配置差异化**: 每个包间有独立的房间ID和设备IP配置
- ✅ **统一管理**: 所有包间都连接到同一个云端服务
- ✅ **批量部署**: 使用脚本可以快速部署多个包间
- ✅ **易于维护**: 标准化的目录结构和配置格式

---

**文档版本**: v2.0  
**更新时间**: 2024年12月  
**技术支持**: Box-AI开发团队
