package app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.Closeable;

/**
 * 订阅后端发布的设备控制命令（Redis channel: device:control）
 */
public class DeviceControlSubscriber implements Closeable {
  private final String redisHost;
  private final int redisPort;
  private final String channel;
  private final ObjectMapper mapper = new ObjectMapper();
  private Thread worker;
  private volatile boolean running = false;

  // 播放器引用（由 MainApp 注入）
  private final MainApp app;

  public DeviceControlSubscriber(MainApp app, String redisHost, int redisPort, String channel) {
    this.app = app;
    this.redisHost = redisHost;
    this.redisPort = redisPort;
    this.channel = channel;
  }

  public void start() {
    if (running) return;
    running = true;
    worker = new Thread(() -> {
      try (Jedis jedis = new Jedis(redisHost, redisPort)) {
        jedis.subscribe(new JedisPubSub() {
          @Override public void onMessage(String ch, String msg) {
            // 同时处理 device:control 与 device:light
            handleMessage(msg);
          }
        }, channel, "device:light");
      } catch (Exception e) {
        // 日志仅控制台
        System.err.println("Redis 订阅异常: " + e.getMessage());
      }
    }, "device-control-subscriber");
    worker.setDaemon(true);
    worker.start();
  }

  private void handleMessage(String msg) {
    try {
      JsonNode root = mapper.readTree(msg);
      String action = text(root, "action");
      Long roomId = longOrNull(root, "roomId");
      Long trackId = longOrNull(root, "trackId");

      // 简单演示：仅控制本机播放器
      switch (action == null ? "" : action.toUpperCase()) {
        case "PLAY" -> Platform.runLater(app::togglePlay);
        case "PAUSE" -> Platform.runLater(() -> {
          var p = app.videoPlayer(); if (p != null) p.pause();
        });
        case "STOP" -> Platform.runLater(() -> {
          var p = app.videoPlayer(); if (p != null) p.stop();
        });
        case "NEXT" -> Platform.runLater(app::playNextFromList);
        case "" -> { // 灯光命令无 action，转发为 DMX
          Integer brightness = root.hasNonNull("brightness") ? root.get("brightness").asInt() : null;
          String color = text(root, "color");
          String rhythm = text(root, "rhythm");
          Platform.runLater(() -> app.applyLight(brightness, color, rhythm));
        }
        default -> {}
      }
    } catch (Exception e) {
      System.err.println("解析控制命令失败: " + e.getMessage());
    }
  }

  private static String text(JsonNode n, String f) { return n.hasNonNull(f) ? n.get(f).asText() : null; }
  private static Long longOrNull(JsonNode n, String f) { return n.hasNonNull(f) ? n.get(f).asLong() : null; }

  @Override public void close() {
    running = false;
    if (worker != null) worker.interrupt();
  }
}


