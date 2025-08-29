package app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化的映射对话框：左侧显示屏幕列表，右侧画布显示缩略图以及可拖拽的归一化矩形。
 * 提供静态快照/低频实时预览两种模式（默认静态，避免卡顿）。
 */
public class MappingDialog extends Stage {
  public static class Item {
    public int screenIndex; public double x, y, w, h;
    public Item(int screenIndex, double x, double y, double w, double h) {
      this.screenIndex = screenIndex; this.x = x; this.y = y; this.w = w; this.h = h;
    }
  }

  private final List<Item> items = new ArrayList<>();
  private final Canvas canvas = new Canvas(800, 600);
  private final CheckBox chkRealtime = new CheckBox("实时预览（低频）");
  private Image snapshot;
  private final MediaPlayer player;
  private final MediaView previewView;
  private final List<ScreenRect> screenRects = new ArrayList<>();

  private double dragStartX, dragStartY;
  private boolean dragging = false;
  private Rectangle2D videoRect = new Rectangle2D(50, 50, 400, 300); // 视频显示区域
  private boolean isDraggingVideo = false;
  private boolean isResizingVideo = false;
  private boolean isDraggingScreen = false;
  private int draggedScreenIndex = -1;
  private int resizeHandle = -1; // 0=左上, 1=右上, 2=右下, 3=左下

  // 屏幕矩形类
  private static class ScreenRect {
    final int screenIndex;
    Rectangle2D bounds; // 改为可变
    final String label;
    
    ScreenRect(int screenIndex, Rectangle2D bounds, String label) {
      this.screenIndex = screenIndex;
      this.bounds = bounds;
      this.label = label;
    }
  }

  public MappingDialog(Stage owner, List<Item> initial, MediaPlayer sourcePlayer) {
    initOwner(owner); initModality(Modality.APPLICATION_MODAL);
    setTitle("屏幕映射");
    this.player = sourcePlayer;
    this.previewView = new MediaView(player);
    this.previewView.setPreserveRatio(true);

    // 初始化屏幕矩形
    initScreenRects();

    if (initial != null && !initial.isEmpty()) items.addAll(initial);
    else {
      // 默认为每个屏幕创建映射项
      for (ScreenRect sr : screenRects) {
        items.add(new Item(sr.screenIndex, 0, 0, 1, 1));
      }
    }

    Button btnOk = new Button("确定");
    Button btnCancel = new Button("取消");
    Button btnReset = new Button("重置视频区域");
    Label instructions = new Label("拖动蓝色视频区域和灰色屏幕框来调整映射关系");
    instructions.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
    
    VBox leftPanel = new VBox(10);
    leftPanel.setPadding(new Insets(10));
    leftPanel.getChildren().addAll(instructions, chkRealtime);
    
    HBox bottom = new HBox(10, btnReset, btnOk, btnCancel);
    bottom.setPadding(new Insets(15));
    bottom.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
    
    // 设置按钮最小宽度
    btnReset.setMinWidth(100);
    btnOk.setMinWidth(80);
    btnCancel.setMinWidth(80);

    BorderPane root = new BorderPane();
    root.setLeft(leftPanel);
    root.setCenter(canvas);
    root.setBottom(bottom);
    root.setPadding(new Insets(10));
    javafx.scene.Scene sc = new javafx.scene.Scene(root, 1000, 650); // 增加窗口大小
    // 不加载任何自定义 CSS
    setScene(sc);
    
    // 应用中文字体
    Platform.runLater(() -> FontManager.applyChineseFontToStage(this));
    
    // 设置窗口可调整大小
    setResizable(true);
    setMinWidth(800);
    setMinHeight(500);
    


    makeSnapshot();
    draw();

    // 新的交互逻辑：拖动和调整视频区域，拖动屏幕
    canvas.setOnMousePressed(e -> {
      dragStartX = e.getX(); 
      dragStartY = e.getY();
      
      // 检查是否点击在视频区域的调整手柄上
      resizeHandle = getResizeHandle(e.getX(), e.getY());
      if (resizeHandle >= 0) {
        isResizingVideo = true;
      } else if (isPointInRect(e.getX(), e.getY(), videoRect)) {
        // 点击在视频区域内，开始拖动视频
        isDraggingVideo = true;
      } else {
        // 检查是否点击在屏幕矩形内
        for (int i = 0; i < screenRects.size(); i++) {
          ScreenRect sr = screenRects.get(i);
          if (isPointInRect(e.getX(), e.getY(), sr.bounds)) {
            isDraggingScreen = true;
            draggedScreenIndex = i;
            break;
          }
        }
      }
    });
    
    canvas.setOnMouseDragged(e -> {
      if (isResizingVideo) {
        resizeVideoRect(e.getX(), e.getY());
      } else if (isDraggingVideo) {
        moveVideoRect(e.getX() - dragStartX, e.getY() - dragStartY);
        dragStartX = e.getX();
        dragStartY = e.getY();
      } else if (isDraggingScreen && draggedScreenIndex >= 0) {
        moveScreenRect(draggedScreenIndex, e.getX() - dragStartX, e.getY() - dragStartY);
        dragStartX = e.getX();
        dragStartY = e.getY();
      }
      draw();
    });
    
    canvas.setOnMouseReleased(e -> {
      isDraggingVideo = false;
      isResizingVideo = false;
      isDraggingScreen = false;
      resizeHandle = -1;
      draggedScreenIndex = -1;
      updateMappingItems();
    });

    btnReset.setOnAction(e -> {
      // 重置视频区域到画布中央
      videoRect = new Rectangle2D(50, 50, 400, 300);
      updateMappingItems();
      draw();
    });
    btnCancel.setOnAction(e -> close());
    btnOk.setOnAction(e -> close());
    chkRealtime.selectedProperty().addListener((o, ov, nv) -> draw());
  }


  
  // 初始化屏幕矩形布局
  private void initScreenRects() {
    List<Screen> screens = Screen.getScreens();
    screenRects.clear();
    
    // 在画布上布局屏幕矩形，增加边界空间
    double canvasW = canvas.getWidth();
    double canvasH = canvas.getHeight();
    int screenCount = screens.size();
    double margin = 50; // 边界空间
    
    if (screenCount == 1) {
      screenRects.add(new ScreenRect(0, new Rectangle2D(canvasW - 300, 100, 250, 150), "屏幕 1"));
    } else if (screenCount == 2) {
      screenRects.add(new ScreenRect(0, new Rectangle2D(canvasW - 250, 50, 200, 120), "屏幕 1"));
      screenRects.add(new ScreenRect(1, new Rectangle2D(canvasW - 250, 200, 200, 120), "屏幕 2"));
    } else if (screenCount >= 3) {
      // 网格布局
      int cols = (int) Math.ceil(Math.sqrt(screenCount));
      int rows = (int) Math.ceil((double) screenCount / cols);
      double rectW = 150;
      double rectH = 90;
      double startX = canvasW - 300;
      double startY = 50;
      
      for (int i = 0; i < screenCount && i < 6; i++) { // 最多显示6个屏幕
        int row = i / cols;
        int col = i % cols;
        double x = startX + col * (rectW + 10);
        double y = startY + row * (rectH + 10);
        screenRects.add(new ScreenRect(i, new Rectangle2D(x, y, rectW, rectH), "屏幕 " + (i + 1)));
      }
    }
  }
  
  // 检查点是否在矩形内
  private boolean isPointInRect(double x, double y, Rectangle2D rect) {
    return x >= rect.getMinX() && x <= rect.getMaxX() && 
           y >= rect.getMinY() && y <= rect.getMaxY();
  }
  
  // 获取调整手柄索引
  private int getResizeHandle(double x, double y) {
    double handleSize = 8;
    Rectangle2D[] handles = {
      new Rectangle2D(videoRect.getMinX() - handleSize/2, videoRect.getMinY() - handleSize/2, handleSize, handleSize), // 左上
      new Rectangle2D(videoRect.getMaxX() - handleSize/2, videoRect.getMinY() - handleSize/2, handleSize, handleSize), // 右上
      new Rectangle2D(videoRect.getMaxX() - handleSize/2, videoRect.getMaxY() - handleSize/2, handleSize, handleSize), // 右下
      new Rectangle2D(videoRect.getMinX() - handleSize/2, videoRect.getMaxY() - handleSize/2, handleSize, handleSize)  // 左下
    };
    
    for (int i = 0; i < handles.length; i++) {
      if (isPointInRect(x, y, handles[i])) {
        return i;
      }
    }
    return -1;
  }
  
  // 移动视频区域
  private void moveVideoRect(double deltaX, double deltaY) {
    double newX = Math.max(0, Math.min(canvas.getWidth() - videoRect.getWidth(), videoRect.getMinX() + deltaX));
    double newY = Math.max(0, Math.min(canvas.getHeight() - videoRect.getHeight(), videoRect.getMinY() + deltaY));
    videoRect = new Rectangle2D(newX, newY, videoRect.getWidth(), videoRect.getHeight());
  }
  
  // 移动屏幕矩形
  private void moveScreenRect(int screenIndex, double deltaX, double deltaY) {
    if (screenIndex < 0 || screenIndex >= screenRects.size()) return;
    
    ScreenRect sr = screenRects.get(screenIndex);
    Rectangle2D oldBounds = sr.bounds;
    
    // 计算新位置，确保不超出画布边界
    double newX = Math.max(0, Math.min(canvas.getWidth() - oldBounds.getWidth(), oldBounds.getMinX() + deltaX));
    double newY = Math.max(0, Math.min(canvas.getHeight() - oldBounds.getHeight(), oldBounds.getMinY() + deltaY));
    
    // 更新屏幕矩形位置（大小不变）
    sr.bounds = new Rectangle2D(newX, newY, oldBounds.getWidth(), oldBounds.getHeight());
  }
  
  // 调整视频区域大小
  private void resizeVideoRect(double mouseX, double mouseY) {
    double minSize = 50;
    double newX = videoRect.getMinX();
    double newY = videoRect.getMinY();
    double newW = videoRect.getWidth();
    double newH = videoRect.getHeight();
    
    switch (resizeHandle) {
      case 0: // 左上
        newW = Math.max(minSize, videoRect.getMaxX() - mouseX);
        newH = Math.max(minSize, videoRect.getMaxY() - mouseY);
        newX = videoRect.getMaxX() - newW;
        newY = videoRect.getMaxY() - newH;
        break;
      case 1: // 右上
        newW = Math.max(minSize, mouseX - videoRect.getMinX());
        newH = Math.max(minSize, videoRect.getMaxY() - mouseY);
        newY = videoRect.getMaxY() - newH;
        break;
      case 2: // 右下
        newW = Math.max(minSize, mouseX - videoRect.getMinX());
        newH = Math.max(minSize, mouseY - videoRect.getMinY());
        break;
      case 3: // 左下
        newW = Math.max(minSize, videoRect.getMaxX() - mouseX);
        newH = Math.max(minSize, mouseY - videoRect.getMinY());
        newX = videoRect.getMaxX() - newW;
        break;
    }
    
    // 确保不超出画布边界
    newX = Math.max(0, Math.min(canvas.getWidth() - newW, newX));
    newY = Math.max(0, Math.min(canvas.getHeight() - newH, newY));
    newW = Math.min(canvas.getWidth() - newX, newW);
    newH = Math.min(canvas.getHeight() - newY, newH);
    
    videoRect = new Rectangle2D(newX, newY, newW, newH);
  }
  
  // 更新映射项
  private void updateMappingItems() {
    items.clear();
    
    // 检查视频区域与哪些屏幕重叠
    for (ScreenRect sr : screenRects) {
      Rectangle2D intersection = getIntersection(videoRect, sr.bounds);
      if (intersection != null && intersection.getWidth() > 10 && intersection.getHeight() > 10) {
        // 计算相对于视频区域的归一化坐标
        double x = (intersection.getMinX() - videoRect.getMinX()) / videoRect.getWidth();
        double y = (intersection.getMinY() - videoRect.getMinY()) / videoRect.getHeight();
        double w = intersection.getWidth() / videoRect.getWidth();
        double h = intersection.getHeight() / videoRect.getHeight();
        
        items.add(new Item(sr.screenIndex, x, y, w, h));
      }
    }
  }
  
  // 计算两个矩形的交集
  private Rectangle2D getIntersection(Rectangle2D r1, Rectangle2D r2) {
    double x = Math.max(r1.getMinX(), r2.getMinX());
    double y = Math.max(r1.getMinY(), r2.getMinY());
    double maxX = Math.min(r1.getMaxX(), r2.getMaxX());
    double maxY = Math.min(r1.getMaxY(), r2.getMaxY());
    
    if (x < maxX && y < maxY) {
      return new Rectangle2D(x, y, maxX - x, maxY - y);
    }
    return null;
  }

  private Item getOrCreateItem(int screenIndex) {
    for (Item it : items) if (it.screenIndex == screenIndex) return it;
    Item it = new Item(screenIndex, 0, 0, 1, 1); items.add(it); return it;
  }

  private void makeSnapshot() {
    try {
      WritableImage img = new WritableImage(640, 360);
      MediaView mv = new MediaView(player);
      mv.setFitWidth(640); mv.setFitHeight(360); mv.setPreserveRatio(true);
      SnapshotParameters sp = new SnapshotParameters();
      snapshot = mv.snapshot(sp, img);
    } catch (Exception ignored) { snapshot = null; }
  }

  private void draw() {
    GraphicsContext g = canvas.getGraphicsContext2D();
    
    // 清空画布
    g.setFill(javafx.scene.paint.Color.web("#2a2a2a")); 
    g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    
    // 绘制画布边界（增加视觉边界）
    g.setStroke(javafx.scene.paint.Color.web("#555"));
    g.setLineWidth(1);
    g.strokeRect(10, 10, canvas.getWidth() - 20, canvas.getHeight() - 20);
    
    // 绘制所有屏幕矩形（可拖动位置）
    g.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
    g.setLineWidth(2);
    g.setFill(javafx.scene.paint.Color.web("#404040"));
    
    for (ScreenRect sr : screenRects) {
      Rectangle2D bounds = sr.bounds;
      g.fillRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
      g.strokeRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
      
      // 绘制屏幕标签 - 添加背景和边框确保可见性
      double labelX = bounds.getMinX() + 5;
      double labelY = bounds.getMinY() + 15;
      
      // 绘制标签背景（半透明黑色）
      g.setFill(javafx.scene.paint.Color.web("#000000", 0.7));
      g.fillRect(labelX - 2, labelY - 12, sr.label.length() * 8 + 4, 16);
      
      // 绘制标签边框
      g.setStroke(javafx.scene.paint.Color.WHITE);
      g.setLineWidth(1);
      g.strokeRect(labelX - 2, labelY - 12, sr.label.length() * 8 + 4, 16);
      
      // 绘制标签文字（白色）
      g.setFill(javafx.scene.paint.Color.WHITE);
      g.fillText(sr.label, labelX, labelY);
    }
    
    // 绘制视频区域（可拖动的蓝色矩形）
    g.setFill(javafx.scene.paint.Color.web("#4a90e2", 0.3));
    g.fillRect(videoRect.getMinX(), videoRect.getMinY(), videoRect.getWidth(), videoRect.getHeight());
    
    g.setStroke(javafx.scene.paint.Color.web("#4a90e2"));
    g.setLineWidth(2);
    g.strokeRect(videoRect.getMinX(), videoRect.getMinY(), videoRect.getWidth(), videoRect.getHeight());
    
    // 绘制视频缩略图（如果有）
    if (chkRealtime.isSelected()) {
      makeSnapshot();
    }
    if (snapshot != null) {
      g.drawImage(snapshot, videoRect.getMinX(), videoRect.getMinY(), videoRect.getWidth(), videoRect.getHeight());
    }
    
    // 绘制调整手柄
    drawResizeHandles(g);
    
    // 绘制重叠区域的高亮（增加边框宽度）
    g.setStroke(javafx.scene.paint.Color.YELLOW);
    g.setLineWidth(5); // 增加边框宽度，避免拖动时卡住
    for (ScreenRect sr : screenRects) {
      Rectangle2D intersection = getIntersection(videoRect, sr.bounds);
      if (intersection != null && intersection.getWidth() > 10 && intersection.getHeight() > 10) {
        g.strokeRect(intersection.getMinX(), intersection.getMinY(), 
                    intersection.getWidth(), intersection.getHeight());
      }
    }
    
    // 绘制视频区域标签 - 添加背景确保可见性
    double videoLabelX = videoRect.getMinX() + 5;
    double videoLabelY = videoRect.getMinY() + 15;
    String videoLabel = "视频区域";
    
    // 绘制标签背景（半透明黑色）
    g.setFill(javafx.scene.paint.Color.web("#000000", 0.8));
    g.fillRect(videoLabelX - 2, videoLabelY - 12, videoLabel.length() * 12 + 4, 16);
    
    // 绘制标签边框
    g.setStroke(javafx.scene.paint.Color.web("#4a90e2"));
    g.setLineWidth(1);
    g.strokeRect(videoLabelX - 2, videoLabelY - 12, videoLabel.length() * 12 + 4, 16);
    
    // 绘制标签文字（白色）
    g.setFill(javafx.scene.paint.Color.WHITE);
    g.fillText(videoLabel, videoLabelX, videoLabelY);
  }
  
  private void drawResizeHandles(GraphicsContext g) {
    double handleSize = 8;
    g.setFill(javafx.scene.paint.Color.WHITE);
    g.setStroke(javafx.scene.paint.Color.web("#4a90e2"));
    g.setLineWidth(1);
    
    // 四个角的调整手柄
    double[][] handlePos = {
      {videoRect.getMinX(), videoRect.getMinY()}, // 左上
      {videoRect.getMaxX(), videoRect.getMinY()}, // 右上
      {videoRect.getMaxX(), videoRect.getMaxY()}, // 右下
      {videoRect.getMinX(), videoRect.getMaxY()}  // 左下
    };
    
    for (double[] pos : handlePos) {
      double x = pos[0] - handleSize/2;
      double y = pos[1] - handleSize/2;
      g.fillRect(x, y, handleSize, handleSize);
      g.strokeRect(x, y, handleSize, handleSize);
    }
  }

  public List<Item> result() { return items; }
}


