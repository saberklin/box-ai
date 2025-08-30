package app;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * 内嵌屏幕映射器 - 直接集成在时间轴页面中
 * 参考MappingDialog的实现，提供可拖拽的视频映射框
 */
public class InlineScreenMapper extends VBox {
    
    private Canvas mappingCanvas;
    private MediaPlayer currentPlayer;
    private MediaView previewView;
    private MultiScreenManager multiScreenManager;
    
    // 屏幕和映射相关
    private List<ScreenRect> screenRects = new ArrayList<>();
    private Rectangle2D videoRect = new Rectangle2D(50, 50, 400, 300); // 视频显示区域
    private Image videoSnapshot;
    
    // 拖拽相关
    private double dragStartX, dragStartY;
    private boolean isDraggingVideo = false;
    private boolean isDraggingResize = false;
    private ResizeHandle currentResizeHandle = ResizeHandle.NONE;
    
    // 调整手柄枚举
    private enum ResizeHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    
    // 控制组件
    private Button applyMappingBtn;
    private Button resetMappingBtn;
    private CheckBox enableMappingChk;
    private Label mappingStatusLabel;
    
    // 屏幕矩形类
    private static class ScreenRect {
        public Rectangle2D bounds;
        public int screenIndex;
        public String label;
        
        public ScreenRect(Rectangle2D bounds, int screenIndex, String label) {
            this.bounds = bounds;
            this.screenIndex = screenIndex;
            this.label = label;
        }
    }
    
    public InlineScreenMapper(MultiScreenManager multiScreenManager) {
        this.multiScreenManager = multiScreenManager;
        initComponents();
        setupLayout();
        setupEventHandlers();
        updateScreenLayout();
    }
    
    private void initComponents() {
        // 画布
        mappingCanvas = new Canvas(600, 350);
        mappingCanvas.setStyle("-fx-border-color: #666; -fx-border-width: 1;");
        
        // 控制按钮
        enableMappingChk = new CheckBox("启用屏幕映射");
        enableMappingChk.setSelected(false);
        enableMappingChk.setOnAction(e -> toggleMappingEnabled());
        
        applyMappingBtn = new Button("应用映射");
        applyMappingBtn.setOnAction(e -> applyMapping());
        applyMappingBtn.setDisable(true);
        
        resetMappingBtn = new Button("重置");
        resetMappingBtn.setOnAction(e -> resetMapping());
        resetMappingBtn.setDisable(true);
        
        mappingStatusLabel = new Label("屏幕映射已禁用");
        mappingStatusLabel.setStyle("-fx-text-fill: #666;");
        
        // 应用样式
        String buttonStyle = "-fx-background-color: linear-gradient(to bottom, #4a90e2, #357abd); " +
                           "-fx-text-fill: white; -fx-border-radius: 4; -fx-background-radius: 4; " +
                           "-fx-padding: 4 8 4 8; -fx-font-size: 11px;";
        applyMappingBtn.setStyle(buttonStyle);
        resetMappingBtn.setStyle(buttonStyle);
    }
    
    private void setupLayout() {
        // 标题
        Label titleLabel = new Label("屏幕映射配置");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #333;");
        
        // 控制面板
        HBox controlPanel = new HBox(10);
        controlPanel.getChildren().addAll(
            enableMappingChk,
            applyMappingBtn,
            resetMappingBtn,
            mappingStatusLabel
        );
        controlPanel.setStyle("-fx-padding: 5; -fx-background-color: #f8f8f8; -fx-border-color: #ddd; -fx-border-width: 1;");
        
        // 说明文字
        Label instructionLabel = new Label(
            "操作说明: 勾选\"启用屏幕映射\"后，拖动红色框调整映射区域，拖动角落调整大小"
        );
        instructionLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        
        getChildren().addAll(
            titleLabel,
            controlPanel,
            mappingCanvas,
            instructionLabel
        );
        
        setSpacing(5);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #fafafa; -fx-border-color: #ccc; -fx-border-width: 1;");
    }
    
    private void setupEventHandlers() {
        mappingCanvas.setOnMousePressed(this::onMousePressed);
        mappingCanvas.setOnMouseDragged(this::onMouseDragged);
        mappingCanvas.setOnMouseReleased(this::onMouseReleased);
        mappingCanvas.setOnMouseMoved(this::onMouseMoved);
    }
    
    private void onMousePressed(MouseEvent e) {
        if (!enableMappingChk.isSelected()) return;
        
        double x = e.getX();
        double y = e.getY();
        
        // 检查是否点击在调整手柄上
        currentResizeHandle = getResizeHandle(x, y);
        
        if (currentResizeHandle != ResizeHandle.NONE) {
            isDraggingResize = true;
            dragStartX = x;
            dragStartY = y;
        } else if (videoRect.contains(x, y)) {
            // 点击在视频矩形内，开始拖动
            isDraggingVideo = true;
            dragStartX = x - videoRect.getMinX();
            dragStartY = y - videoRect.getMinY();
            mappingCanvas.setCursor(Cursor.MOVE);
        }
    }
    
    private void onMouseDragged(MouseEvent e) {
        if (!enableMappingChk.isSelected()) return;
        
        double x = e.getX();
        double y = e.getY();
        
        if (isDraggingVideo) {
            // 拖动视频矩形 - 支持跨屏幕拖动
            double newX = x - dragStartX;
            double newY = y - dragStartY;
            
            // 允许拖动到画布边界外（支持多屏幕）
            newX = Math.max(-videoRect.getWidth() * 0.8, Math.min(newX, mappingCanvas.getWidth() - videoRect.getWidth() * 0.2));
            newY = Math.max(-videoRect.getHeight() * 0.8, Math.min(newY, mappingCanvas.getHeight() - videoRect.getHeight() * 0.2));
            
            videoRect = new Rectangle2D(newX, newY, videoRect.getWidth(), videoRect.getHeight());
            redrawCanvas();
            
        } else if (isDraggingResize && currentResizeHandle != ResizeHandle.NONE) {
            // 根据不同的调整手柄调整大小
            double deltaX = x - dragStartX;
            double deltaY = y - dragStartY;
            
            double newX = videoRect.getMinX();
            double newY = videoRect.getMinY();
            double newWidth = videoRect.getWidth();
            double newHeight = videoRect.getHeight();
            
            switch (currentResizeHandle) {
                case TOP_LEFT:
                    newX += deltaX;
                    newY += deltaY;
                    newWidth -= deltaX;
                    newHeight -= deltaY;
                    break;
                case TOP_RIGHT:
                    newY += deltaY;
                    newWidth += deltaX;
                    newHeight -= deltaY;
                    break;
                case BOTTOM_LEFT:
                    newX += deltaX;
                    newWidth -= deltaX;
                    newHeight += deltaY;
                    break;
                case BOTTOM_RIGHT:
                    newWidth += deltaX;
                    newHeight += deltaY;
                    break;
            }
            
            // 确保最小尺寸
            newWidth = Math.max(50, newWidth);
            newHeight = Math.max(50, newHeight);
            
            videoRect = new Rectangle2D(newX, newY, newWidth, newHeight);
            dragStartX = x;
            dragStartY = y;
            redrawCanvas();
        }
    }
    
    private void onMouseReleased(MouseEvent e) {
        isDraggingVideo = false;
        isDraggingResize = false;
        currentResizeHandle = ResizeHandle.NONE;
        mappingCanvas.setCursor(Cursor.DEFAULT);
    }
    
    private void onMouseMoved(MouseEvent e) {
        if (!enableMappingChk.isSelected()) {
            mappingCanvas.setCursor(Cursor.DEFAULT);
            return;
        }
        
        double x = e.getX();
        double y = e.getY();
        
        // 检查是否在调整手柄上
        ResizeHandle handle = getResizeHandle(x, y);
        
        if (handle != ResizeHandle.NONE) {
            switch (handle) {
                case TOP_LEFT:
                case BOTTOM_RIGHT:
                    mappingCanvas.setCursor(Cursor.NW_RESIZE);
                    break;
                case TOP_RIGHT:
                case BOTTOM_LEFT:
                    mappingCanvas.setCursor(Cursor.NE_RESIZE);
                    break;
            }
        } else if (videoRect.contains(x, y)) {
            mappingCanvas.setCursor(Cursor.MOVE);
        } else {
            mappingCanvas.setCursor(Cursor.DEFAULT);
        }
    }
    
    private ResizeHandle getResizeHandle(double x, double y) {
        if (!enableMappingChk.isSelected()) return ResizeHandle.NONE;
        
        double handleSize = 12;
        double tolerance = handleSize / 2;
        
        // 检查四个角的调整手柄
        // 左上角
        if (Math.abs(x - videoRect.getMinX()) <= tolerance && Math.abs(y - videoRect.getMinY()) <= tolerance) {
            return ResizeHandle.TOP_LEFT;
        }
        // 右上角
        if (Math.abs(x - videoRect.getMaxX()) <= tolerance && Math.abs(y - videoRect.getMinY()) <= tolerance) {
            return ResizeHandle.TOP_RIGHT;
        }
        // 左下角
        if (Math.abs(x - videoRect.getMinX()) <= tolerance && Math.abs(y - videoRect.getMaxY()) <= tolerance) {
            return ResizeHandle.BOTTOM_LEFT;
        }
        // 右下角
        if (Math.abs(x - videoRect.getMaxX()) <= tolerance && Math.abs(y - videoRect.getMaxY()) <= tolerance) {
            return ResizeHandle.BOTTOM_RIGHT;
        }
        
        return ResizeHandle.NONE;
    }
    
    private void toggleMappingEnabled() {
        boolean enabled = enableMappingChk.isSelected();
        applyMappingBtn.setDisable(!enabled);
        resetMappingBtn.setDisable(!enabled);
        
        if (enabled) {
            mappingStatusLabel.setText("屏幕映射已启用 - 拖动红色框调整映射");
            mappingStatusLabel.setStyle("-fx-text-fill: green;");
            updateVideoSnapshot();
        } else {
            mappingStatusLabel.setText("屏幕映射已禁用");
            mappingStatusLabel.setStyle("-fx-text-fill: #666;");
            // 停止当前映射
            if (multiScreenManager != null) {
                multiScreenManager.stopAll();
            }
        }
        
        redrawCanvas();
    }
    
    private void updateScreenLayout() {
        screenRects.clear();
        
        List<Screen> screens = Screen.getScreens();
        if (screens.isEmpty()) return;
        
        // 计算所有屏幕的边界
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (Screen screen : screens) {
            Rectangle2D bounds = screen.getBounds();
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
        }
        
        double totalWidth = maxX - minX;
        double totalHeight = maxY - minY;
        
        // 计算缩放比例
        double canvasWidth = mappingCanvas.getWidth() - 20; // 留边距
        double canvasHeight = mappingCanvas.getHeight() - 20;
        double scaleX = canvasWidth / totalWidth;
        double scaleY = canvasHeight / totalHeight;
        double scale = Math.min(scaleX, scaleY) * 0.8;
        
        // 创建屏幕矩形
        for (int i = 0; i < screens.size(); i++) {
            Screen screen = screens.get(i);
            Rectangle2D bounds = screen.getBounds();
            
            double x = (bounds.getMinX() - minX) * scale + 10;
            double y = (bounds.getMinY() - minY) * scale + 10;
            double w = bounds.getWidth() * scale;
            double h = bounds.getHeight() * scale;
            
            Rectangle2D scaledBounds = new Rectangle2D(x, y, w, h);
            String label = "屏幕" + (i + 1) + "\n" + (int)bounds.getWidth() + "x" + (int)bounds.getHeight();
            
            screenRects.add(new ScreenRect(scaledBounds, i, label));
        }
        
        redrawCanvas();
    }
    
    private void updateVideoSnapshot() {
        if (currentPlayer != null && currentPlayer.getStatus() == MediaPlayer.Status.READY) {
            try {
                // 创建视频快照
                if (previewView != null) {
                    videoSnapshot = previewView.snapshot(null, null);
                }
            } catch (Exception e) {
                System.err.println("创建视频快照失败: " + e.getMessage());
            }
        }
    }
    
    private void redrawCanvas() {
        Platform.runLater(() -> {
            GraphicsContext gc = mappingCanvas.getGraphicsContext2D();
            
            // 清空画布
            gc.clearRect(0, 0, mappingCanvas.getWidth(), mappingCanvas.getHeight());
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, mappingCanvas.getWidth(), mappingCanvas.getHeight());
            
            // 先绘制视频映射区域（在屏幕下层）
            if (enableMappingChk.isSelected()) {
                // 视频快照背景
                if (videoSnapshot != null) {
                    gc.drawImage(videoSnapshot,
                               videoRect.getMinX(), videoRect.getMinY(),
                               videoRect.getWidth(), videoRect.getHeight());
                } else {
                    gc.setFill(Color.LIGHTBLUE);
                    gc.fillRect(videoRect.getMinX(), videoRect.getMinY(),
                               videoRect.getWidth(), videoRect.getHeight());
                }
            }
            
            // 绘制屏幕（透明背景，在视频上层）
            for (ScreenRect screenRect : screenRects) {
                // 透明屏幕背景，方便看到下面的视频
                gc.setFill(Color.rgb(200, 200, 200, 0.3)); // 30% 透明度
                gc.fillRect(screenRect.bounds.getMinX(), screenRect.bounds.getMinY(),
                           screenRect.bounds.getWidth(), screenRect.bounds.getHeight());
                
                // 屏幕边框
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(2);
                gc.strokeRect(screenRect.bounds.getMinX(), screenRect.bounds.getMinY(),
                             screenRect.bounds.getWidth(), screenRect.bounds.getHeight());
                
                // 屏幕标签背景
                gc.setFill(Color.rgb(255, 255, 255, 0.8)); // 半透明白色背景
                gc.fillRect(screenRect.bounds.getMinX() + 2, screenRect.bounds.getMinY() + 2, 80, 35);
                
                // 屏幕标签
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 10));
                gc.fillText(screenRect.label,
                           screenRect.bounds.getMinX() + 5,
                           screenRect.bounds.getMinY() + 20);
            }
            
            // 绘制红色映射框（最上层）
            if (enableMappingChk.isSelected()) {
                // 红色边框（加粗，更明显）
                gc.setStroke(Color.RED);
                gc.setLineWidth(4);
                gc.strokeRect(videoRect.getMinX(), videoRect.getMinY(),
                             videoRect.getWidth(), videoRect.getHeight());
                
                // 四个角的调整手柄
                gc.setFill(Color.RED);
                double handleSize = 12;
                // 左上角
                gc.fillRect(videoRect.getMinX() - handleSize/2, videoRect.getMinY() - handleSize/2, handleSize, handleSize);
                // 右上角
                gc.fillRect(videoRect.getMaxX() - handleSize/2, videoRect.getMinY() - handleSize/2, handleSize, handleSize);
                // 左下角
                gc.fillRect(videoRect.getMinX() - handleSize/2, videoRect.getMaxY() - handleSize/2, handleSize, handleSize);
                // 右下角（主要调整手柄）
                gc.fillRect(videoRect.getMaxX() - handleSize/2, videoRect.getMaxY() - handleSize/2, handleSize, handleSize);
                
                // 视频标签（带半透明背景）
                String label = "视频映射区域 (可拖动调整)";
                gc.setFill(Color.rgb(255, 0, 0, 0.8)); // 半透明红色背景
                gc.fillRect(videoRect.getMinX(), videoRect.getMinY() - 25, label.length() * 8, 20);
                
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 12));
                gc.fillText(label, videoRect.getMinX() + 5, videoRect.getMinY() - 10);
                
                // 显示当前映射的屏幕数量
                List<MultiScreenManager.MappingItem> currentMappings = calculateMappings();
                if (!currentMappings.isEmpty()) {
                    String mappingInfo = "映射到 " + currentMappings.size() + " 个屏幕";
                    gc.setFill(Color.rgb(0, 128, 0, 0.8)); // 半透明绿色背景
                    gc.fillRect(videoRect.getMinX(), videoRect.getMaxY() + 5, mappingInfo.length() * 8, 20);
                    
                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 10));
                    gc.fillText(mappingInfo, videoRect.getMinX() + 5, videoRect.getMaxY() + 18);
                }
            }
        });
    }
    
    private void resetMapping() {
        // 重置视频矩形到默认位置
        videoRect = new Rectangle2D(50, 50, 400, 300);
        redrawCanvas();
    }
    
    private void applyMapping() {
        if (!enableMappingChk.isSelected() || currentPlayer == null) {
            return;
        }
        
        // 计算映射
        List<MultiScreenManager.MappingItem> mappings = calculateMappings();
        
        if (!mappings.isEmpty()) {
            // 应用映射
            multiScreenManager.showOnScreens(mappings, currentPlayer);
            mappingStatusLabel.setText("映射已应用到 " + mappings.size() + " 个屏幕");
            mappingStatusLabel.setStyle("-fx-text-fill: blue;");
        } else {
            mappingStatusLabel.setText("未检测到有效的映射区域");
            mappingStatusLabel.setStyle("-fx-text-fill: orange;");
        }
    }
    
    private List<MultiScreenManager.MappingItem> calculateMappings() {
        List<MultiScreenManager.MappingItem> mappings = new ArrayList<>();
        
        // 检查视频矩形与哪些屏幕重叠
        for (ScreenRect screenRect : screenRects) {
            Rectangle2D intersection = getIntersection(videoRect, screenRect.bounds);
            
            if (intersection != null && intersection.getWidth() > 0 && intersection.getHeight() > 0) {
                // 计算交集区域在屏幕上的位置 (0-1)
                double screenX = (intersection.getMinX() - screenRect.bounds.getMinX()) / screenRect.bounds.getWidth();
                double screenY = (intersection.getMinY() - screenRect.bounds.getMinY()) / screenRect.bounds.getHeight();
                double screenW = intersection.getWidth() / screenRect.bounds.getWidth();
                double screenH = intersection.getHeight() / screenRect.bounds.getHeight();
                
                // 计算交集区域在视频中的位置 (0-1) - 这是关键修正
                double videoX = (intersection.getMinX() - videoRect.getMinX()) / videoRect.getWidth();
                double videoY = (intersection.getMinY() - videoRect.getMinY()) / videoRect.getHeight();
                double videoW = intersection.getWidth() / videoRect.getWidth();
                double videoH = intersection.getHeight() / videoRect.getHeight();
                
                // 创建映射项：传递视频坐标给MultiScreenManager
                // MultiScreenManager会使用这些坐标来设置视频的viewport（裁剪区域）
                mappings.add(new MultiScreenManager.MappingItem(
                    screenRect.screenIndex, 
                    videoX, videoY, videoW, videoH  // 传递视频坐标，这样就能正确裁剪视频
                ));
            }
        }
        
        return mappings;
    }
    
    private Rectangle2D getIntersection(Rectangle2D rect1, Rectangle2D rect2) {
        double x1 = Math.max(rect1.getMinX(), rect2.getMinX());
        double y1 = Math.max(rect1.getMinY(), rect2.getMinY());
        double x2 = Math.min(rect1.getMaxX(), rect2.getMaxX());
        double y2 = Math.min(rect1.getMaxY(), rect2.getMaxY());
        
        if (x2 > x1 && y2 > y1) {
            return new Rectangle2D(x1, y1, x2 - x1, y2 - y1);
        }
        return null;
    }
    
    // 公共方法
    public void setCurrentPlayer(MediaPlayer player, MediaView previewView) {
        this.currentPlayer = player;
        this.previewView = previewView;
        updateVideoSnapshot();
        
        if (enableMappingChk.isSelected()) {
            redrawCanvas();
        }
    }
    
    public void refreshScreens() {
        updateScreenLayout();
    }
    
    public void stopMapping() {
        enableMappingChk.setSelected(false);
        toggleMappingEnabled();
    }
}
