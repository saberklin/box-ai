package app;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理多屏幕输出：根据归一化裁剪矩形为每个屏幕创建一个 Stage+MediaView，并与主 MediaPlayer 同步。
 */
public class MultiScreenManager {
  public static class MappingItem {
    public int screenIndex;   // 目标屏索引
    public double x;          // 源视频归一化裁剪 X [0,1]
    public double y;          // 源视频归一化裁剪 Y [0,1]
    public double w;          // 归一化宽 [0,1]
    public double h;          // 归一化高 [0,1]
    public MappingItem(int screenIndex, double x, double y, double w, double h) {
      this.screenIndex = screenIndex; this.x = x; this.y = y; this.w = w; this.h = h;
    }
  }

  private Stage fullScreenStage;
  private MediaView fullScreenView;

  public void stopAll() {
    Platform.runLater(() -> {
      if (fullScreenStage != null) {
        try { 
          fullScreenStage.close(); 
        } catch (Exception ignored) {}
        fullScreenStage = null;
        fullScreenView = null;
      }
    });
  }

  public void showOnScreens(List<MappingItem> mappings, MediaPlayer player) {
    stopAll();
    if (player == null || player.getMedia() == null) return;
    double vw = player.getMedia().getWidth();
    double vh = player.getMedia().getHeight();
    if (vw <= 0 || vh <= 0) {
      // 等准备好后重试
      player.setOnReady(() -> showOnScreens(mappings, player));
      return;
    }
    
    Platform.runLater(() -> {
      List<Screen> screens = Screen.getScreens();
      if (mappings.isEmpty() || screens.isEmpty()) return;
      
      // 使用第一个映射项对应的屏幕作为全屏显示
      MappingItem firstMapping = mappings.get(0);
      if (firstMapping.screenIndex < 0 || firstMapping.screenIndex >= screens.size()) return;
      
      Screen targetScreen = screens.get(firstMapping.screenIndex);
      Rectangle2D screenBounds = targetScreen.getBounds();

      // 创建单个全屏窗口
      fullScreenStage = new Stage();
      fullScreenView = new MediaView(player);
      fullScreenView.setPreserveRatio(true);
      
      // 设置视频裁剪区域（使用第一个映射项的参数）
      Rectangle2D viewport = new Rectangle2D(
          Math.round(firstMapping.x * vw),
          Math.round(firstMapping.y * vh),
          Math.round(firstMapping.w * vw),
          Math.round(firstMapping.h * vh)
      );
      fullScreenView.setViewport(viewport);

      BorderPane pane = new BorderPane(fullScreenView);
      pane.setStyle("-fx-background-color: black;");
      
      Scene scene = new Scene(pane, screenBounds.getWidth(), screenBounds.getHeight());
      
      // 设置窗口属性
      fullScreenStage.setX(screenBounds.getMinX());
      fullScreenStage.setY(screenBounds.getMinY());
      fullScreenStage.setWidth(screenBounds.getWidth());
      fullScreenStage.setHeight(screenBounds.getHeight());
      fullScreenStage.setFullScreen(true);
      fullScreenStage.setAlwaysOnTop(false);
      fullScreenStage.setTitle("视频输出");
      
      // 添加关闭事件处理
      fullScreenStage.setOnCloseRequest(e -> {
        fullScreenStage = null;
        fullScreenView = null;
      });
      
      // 添加ESC键退出全屏
      scene.setOnKeyPressed(e -> {
        if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
          stopAll();
        }
      });
      
      fullScreenStage.setScene(scene);
      
      // 让 MediaView 尺寸适配屏幕并保持比例
      fullScreenView.fitWidthProperty().bind(scene.widthProperty());
      fullScreenView.fitHeightProperty().bind(scene.heightProperty());
      
      fullScreenStage.show();
    });
  }

  // 强制应用中文字体，解决字体回退问题
  private void forceChineseFont(Scene scene) {
    String fontStyle = "-fx-font-family: 'Microsoft YaHei', '微软雅黑', 'SimSun', '宋体', 'SimHei', '黑体', 'Arial Unicode MS', sans-serif;";
    
    if (scene.getRoot() instanceof javafx.scene.Parent root) {
      root.setStyle(fontStyle);
      applyFontToAllNodes(root, fontStyle);
    }
  }

  private void applyFontToAllNodes(javafx.scene.Node node, String fontStyle) {
    try {
      // 检查样式属性是否被绑定，如果被绑定则跳过
      if (!node.styleProperty().isBound()) {
        if (node instanceof javafx.scene.control.Labeled labeled) {
          labeled.setStyle(fontStyle);
        } else if (node instanceof javafx.scene.control.TextInputControl textInput) {
          textInput.setStyle(fontStyle);
        } else {
          node.setStyle(fontStyle);
        }
      }
    } catch (Exception ignored) {
      // 忽略设置失败的情况
    }
    
    if (node instanceof javafx.scene.Parent parent) {
      for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
        applyFontToAllNodes(child, fontStyle);
      }
    }
  }
}


