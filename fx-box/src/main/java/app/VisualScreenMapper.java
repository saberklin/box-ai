package app;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * 可视化屏幕映射器
 * 提供与原MappingDialog相同的可视化多屏幕映射功能
 */
public class VisualScreenMapper {
    
    private Stage mappingStage;
    private Pane screenContainer;
    private List<ScreenPanel> screenPanels = new ArrayList<>();
    private Rectangle videoRegion;
    private MediaPlayer currentPlayer;
    private MultiScreenManager multiScreenManager;
    
    // 映射结果回调
    public interface MappingCallback {
        void onMappingConfirmed(List<MultiScreenManager.MappingItem> mappings);
        void onMappingCancelled();
    }
    
    // 屏幕面板
    private static class ScreenPanel extends StackPane {
        public final int screenIndex;
        public final Screen screen;
        public final Rectangle screenRect;
        public final Label screenLabel;
        
        public ScreenPanel(int index, Screen screen, double scale) {
            this.screenIndex = index;
            this.screen = screen;
            
            double width = screen.getBounds().getWidth() * scale;
            double height = screen.getBounds().getHeight() * scale;
            
            // 屏幕矩形
            screenRect = new Rectangle(width, height);
            screenRect.setFill(Color.LIGHTGRAY);
            screenRect.setStroke(Color.BLACK);
            screenRect.setStrokeWidth(2);
            
            // 屏幕标签
            screenLabel = new Label("屏幕 " + (index + 1) + "\n" + 
                                  (int)screen.getBounds().getWidth() + "x" + 
                                  (int)screen.getBounds().getHeight());
            screenLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 12));
            screenLabel.setStyle("-fx-text-fill: black; -fx-background-color: rgba(255,255,255,0.8); " +
                                "-fx-padding: 5; -fx-background-radius: 3;");
            
            getChildren().addAll(screenRect, screenLabel);
            setPrefSize(width, height);
            setMaxSize(width, height);
            setMinSize(width, height);
        }
    }
    
    public VisualScreenMapper(MultiScreenManager multiScreenManager) {
        this.multiScreenManager = multiScreenManager;
    }
    
    public void showMappingDialog(MediaPlayer player, MappingCallback callback) {
        this.currentPlayer = player;
        
        if (player == null || player.getMedia() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("无法进行屏幕映射");
            alert.setContentText("请先导入并加载视频文件");
            FontManager.applyChineseFontToDialog(alert);
            alert.showAndWait();
            return;
        }
        
        createMappingStage(callback);
    }
    
    private void createMappingStage(MappingCallback callback) {
        mappingStage = new Stage();
        mappingStage.setTitle("可视化屏幕映射");
        mappingStage.initModality(Modality.APPLICATION_MODAL);
        
        // 应用中文字体
        FontManager.applyChineseFontToStage(mappingStage);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f0f0f0;");
        
        // 标题
        Label titleLabel = new Label("屏幕映射配置 - 拖动红色框调整映射区域");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #333;");
        
        // 屏幕显示区域
        screenContainer = new Pane();
        screenContainer.setPrefSize(800, 500);
        screenContainer.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1;");
        
        // 创建屏幕面板
        createScreenPanels();
        
        // 创建视频映射区域
        createVideoRegion();
        
        // 控制按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button confirmBtn = new Button("确认映射");
        Button cancelBtn = new Button("取消");
        Button resetBtn = new Button("重置");
        
        confirmBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        resetBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        
        confirmBtn.setOnAction(e -> {
            List<MultiScreenManager.MappingItem> mappings = calculateMappings();
            if (!mappings.isEmpty()) {
                // 应用映射
                multiScreenManager.showOnScreens(mappings, currentPlayer);
                callback.onMappingConfirmed(mappings);
            }
            mappingStage.close();
        });
        
        cancelBtn.setOnAction(e -> {
            callback.onMappingCancelled();
            mappingStage.close();
        });
        
        resetBtn.setOnAction(e -> resetVideoRegion());
        
        buttonBox.getChildren().addAll(confirmBtn, resetBtn, cancelBtn);
        
        // 说明文字
        Label instructionLabel = new Label(
            "操作说明:\n" +
            "• 红色框代表视频映射区域\n" +
            "• 拖动红色框调整映射位置\n" +
            "• 拖动红色框角落调整映射大小\n" +
            "• 可以跨多个屏幕进行映射"
        );
        instructionLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        
        root.getChildren().addAll(
            titleLabel,
            screenContainer,
            instructionLabel,
            buttonBox
        );
        
        Scene scene = new Scene(root, 850, 650);
        mappingStage.setScene(scene);
        mappingStage.setResizable(false);
        mappingStage.show();
    }
    
    private void createScreenPanels() {
        screenPanels.clear();
        screenContainer.getChildren().clear();
        
        List<Screen> screens = Screen.getScreens();
        if (screens.isEmpty()) return;
        
        // 计算缩放比例
        double maxWidth = 750;
        double maxHeight = 400;
        double scale = calculateScale(screens, maxWidth, maxHeight);
        
        // 找到所有屏幕的边界
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        
        for (Screen screen : screens) {
            minX = Math.min(minX, screen.getBounds().getMinX());
            minY = Math.min(minY, screen.getBounds().getMinY());
        }
        
        // 创建屏幕面板
        for (int i = 0; i < screens.size(); i++) {
            Screen screen = screens.get(i);
            ScreenPanel panel = new ScreenPanel(i, screen, scale);
            
            // 计算面板位置
            double x = (screen.getBounds().getMinX() - minX) * scale + 25;
            double y = (screen.getBounds().getMinY() - minY) * scale + 25;
            
            panel.setLayoutX(x);
            panel.setLayoutY(y);
            
            screenPanels.add(panel);
            screenContainer.getChildren().add(panel);
        }
    }
    
    private void createVideoRegion() {
        // 创建可拖拽的视频映射区域
        videoRegion = new Rectangle(200, 150);
        videoRegion.setFill(Color.TRANSPARENT);
        videoRegion.setStroke(Color.RED);
        videoRegion.setStrokeWidth(3);
        
        // 初始位置在第一个屏幕中央
        if (!screenPanels.isEmpty()) {
            ScreenPanel firstPanel = screenPanels.get(0);
            videoRegion.setLayoutX(firstPanel.getLayoutX() + firstPanel.getPrefWidth()/2 - 100);
            videoRegion.setLayoutY(firstPanel.getLayoutY() + firstPanel.getPrefHeight()/2 - 75);
        }
        
        // 添加拖拽功能
        addDragSupport(videoRegion);
        
        // 添加调整大小功能
        addResizeSupport(videoRegion);
        
        screenContainer.getChildren().add(videoRegion);
    }
    
    private void addDragSupport(Rectangle region) {
        final Delta dragDelta = new Delta();
        
        region.setOnMousePressed(e -> {
            if (isInResizeZone(region, e)) return;
            
            dragDelta.x = region.getLayoutX() - e.getSceneX();
            dragDelta.y = region.getLayoutY() - e.getSceneY();
            region.setCursor(Cursor.MOVE);
        });
        
        region.setOnMouseDragged(e -> {
            if (isInResizeZone(region, e)) return;
            
            double newX = e.getSceneX() + dragDelta.x;
            double newY = e.getSceneY() + dragDelta.y;
            
            // 限制在容器范围内
            newX = Math.max(0, Math.min(newX, screenContainer.getPrefWidth() - region.getWidth()));
            newY = Math.max(0, Math.min(newY, screenContainer.getPrefHeight() - region.getHeight()));
            
            region.setLayoutX(newX);
            region.setLayoutY(newY);
        });
        
        region.setOnMouseReleased(e -> {
            region.setCursor(Cursor.DEFAULT);
        });
    }
    
    private void addResizeSupport(Rectangle region) {
        region.setOnMouseMoved(e -> {
            if (isInResizeZone(region, e)) {
                region.setCursor(Cursor.SE_RESIZE);
            } else {
                region.setCursor(Cursor.DEFAULT);
            }
        });
        
        final Delta resizeDelta = new Delta();
        
        region.setOnMousePressed(e -> {
            if (isInResizeZone(region, e)) {
                resizeDelta.x = region.getWidth() - e.getX();
                resizeDelta.y = region.getHeight() - e.getY();
                region.setCursor(Cursor.SE_RESIZE);
            }
        });
        
        region.setOnMouseDragged(e -> {
            if (isInResizeZone(region, e)) {
                double newWidth = e.getX() + resizeDelta.x;
                double newHeight = e.getY() + resizeDelta.y;
                
                // 最小尺寸限制
                newWidth = Math.max(50, newWidth);
                newHeight = Math.max(50, newHeight);
                
                // 不超出容器边界
                double maxWidth = screenContainer.getPrefWidth() - region.getLayoutX();
                double maxHeight = screenContainer.getPrefHeight() - region.getLayoutY();
                
                newWidth = Math.min(newWidth, maxWidth);
                newHeight = Math.min(newHeight, maxHeight);
                
                region.setWidth(newWidth);
                region.setHeight(newHeight);
            }
        });
    }
    
    private boolean isInResizeZone(Rectangle region, MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        double width = region.getWidth();
        double height = region.getHeight();
        
        // 右下角15x15像素区域为调整大小区域
        return (x >= width - 15 && y >= height - 15);
    }
    
    private double calculateScale(List<Screen> screens, double maxWidth, double maxHeight) {
        double totalWidth = 0;
        double totalHeight = 0;
        
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        
        for (Screen screen : screens) {
            minX = Math.min(minX, screen.getBounds().getMinX());
            maxX = Math.max(maxX, screen.getBounds().getMaxX());
            minY = Math.min(minY, screen.getBounds().getMinY());
            maxY = Math.max(maxY, screen.getBounds().getMaxY());
        }
        
        totalWidth = maxX - minX;
        totalHeight = maxY - minY;
        
        double scaleX = maxWidth / totalWidth;
        double scaleY = maxHeight / totalHeight;
        
        return Math.min(scaleX, scaleY) * 0.8; // 留一些边距
    }
    
    private void resetVideoRegion() {
        if (!screenPanels.isEmpty() && videoRegion != null) {
            ScreenPanel firstPanel = screenPanels.get(0);
            videoRegion.setLayoutX(firstPanel.getLayoutX() + firstPanel.getPrefWidth()/2 - 100);
            videoRegion.setLayoutY(firstPanel.getLayoutY() + firstPanel.getPrefHeight()/2 - 75);
            videoRegion.setWidth(200);
            videoRegion.setHeight(150);
        }
    }
    
    private List<MultiScreenManager.MappingItem> calculateMappings() {
        List<MultiScreenManager.MappingItem> mappings = new ArrayList<>();
        
        if (videoRegion == null || screenPanels.isEmpty()) {
            return mappings;
        }
        
        Bounds videoBounds = videoRegion.getBoundsInParent();
        
        // 检查视频区域与哪些屏幕重叠
        for (ScreenPanel panel : screenPanels) {
            Bounds screenBounds = panel.getBoundsInParent();
            
            // 检查是否有重叠
            if (videoBounds.intersects(screenBounds)) {
                // 计算重叠区域
                double intersectX = Math.max(videoBounds.getMinX(), screenBounds.getMinX());
                double intersectY = Math.max(videoBounds.getMinY(), screenBounds.getMinY());
                double intersectMaxX = Math.min(videoBounds.getMaxX(), screenBounds.getMaxX());
                double intersectMaxY = Math.min(videoBounds.getMaxY(), screenBounds.getMaxY());
                
                if (intersectMaxX > intersectX && intersectMaxY > intersectY) {
                    // 计算相对于屏幕的坐标 (0-1)
                    double relX = (intersectX - screenBounds.getMinX()) / screenBounds.getWidth();
                    double relY = (intersectY - screenBounds.getMinY()) / screenBounds.getHeight();
                    double relW = (intersectMaxX - intersectX) / screenBounds.getWidth();
                    double relH = (intersectMaxY - intersectY) / screenBounds.getHeight();
                    
                    mappings.add(new MultiScreenManager.MappingItem(panel.screenIndex, relX, relY, relW, relH));
                }
            }
        }
        
        return mappings;
    }
    
    // 辅助类用于拖拽
    private static class Delta {
        double x, y;
    }
}
