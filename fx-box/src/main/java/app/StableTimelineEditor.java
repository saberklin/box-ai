package app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 稳定的纯JavaFX时间轴编辑器
 * 解决WebView渲染问题，提供可靠的时间轴编辑功能
 */
public class StableTimelineEditor extends VBox {
    
    // 时间轴配置
    private static final double TIMELINE_HEIGHT = 300;
    private static final double TRACK_HEIGHT = 80;
    private static final double RULER_HEIGHT = 30;
    private static final double PIXELS_PER_SECOND = 50;
    private static final double PLAYHEAD_WIDTH = 2;
    
    // 轨道类型
    public enum TrackType {
        VIDEO("视频轨道", Color.LIGHTBLUE),
        AUDIO("音频轨道", Color.LIGHTGREEN), 
        LIGHT("灯光轨道", Color.LIGHTYELLOW);
        
        public final String name;
        public final Color color;
        
        TrackType(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }
    
    // 媒体片段
    public static class MediaClip {
        public String id;
        public String fileName;
        public String filePath;
        public TrackType trackType;
        public double startTime;
        public double duration;
        public boolean selected = false;
        
        public MediaClip(String id, String fileName, String filePath, TrackType trackType, double startTime, double duration) {
            this.id = id;
            this.fileName = fileName;
            this.filePath = filePath;
            this.trackType = trackType;
            this.startTime = startTime;
            this.duration = duration;
        }
        
        public double getEndTime() {
            return startTime + duration;
        }
    }
    
    // 关键帧
    public static class Keyframe {
        public String id;
        public double time;
        public TrackType track;
        public Map<String, Object> properties;
        
        public Keyframe(String id, double time, TrackType track) {
            this.id = id;
            this.time = time;
            this.track = track;
            this.properties = new HashMap<>();
        }
    }
    
    // 组件
    private Canvas timelineCanvas;
    private ScrollPane scrollPane;
    private HBox toolbar;
    private Label timeLabel;
    private Slider zoomSlider;
    private VBox trackLabels;
    
    // 数据
    private List<MediaClip> clips = new CopyOnWriteArrayList<>();
    private List<Keyframe> keyframes = new CopyOnWriteArrayList<>();
    private Set<MediaClip> selectedClips = new HashSet<>();
    private MediaClip clipboard = null;
    
    // 状态
    private double totalDuration = 300; // 5分钟
    private double currentTime = 0;
    private double zoomFactor = 1.0;
    private boolean isPlaying = false;
    
    // 交互状态
    private boolean isDraggingPlayhead = false;
    private boolean isDraggingClip = false;
    private boolean isResizingClip = false;
    private MediaClip draggedClip = null;
    private double dragStartX = 0;
    private double dragStartTime = 0;
    private String resizeHandle = null; // "left" or "right"
    
    // 监听器
    private TimelineListener listener;
    
    public interface TimelineListener {
        default void onTimeChanged(double time) {}
        default void onClipAdded(MediaClip clip) {}
        default void onClipMoved(MediaClip clip) {}
        default void onClipRemoved(MediaClip clip) {}
        default void onClipSelected(List<MediaClip> clips) {}
        default void onPlayStateChanged(boolean playing) {}
    }
    
    public StableTimelineEditor() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        updateCanvas();
        redraw();
    }
    
    private void initComponents() {
        // 工具栏
        toolbar = new HBox(10);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #404040; -fx-border-width: 0 0 1 0;");
        
        Button playBtn = new Button("▶ 播放");
        Button pauseBtn = new Button("⏸ 暂停");
        Button stopBtn = new Button("⏹ 停止");
        
        playBtn.setOnAction(e -> togglePlay());
        pauseBtn.setOnAction(e -> pause());
        stopBtn.setOnAction(e -> stop());
        
        Button cutBtn = new Button("✂ 裁剪");
        Button copyBtn = new Button("📋 复制");
        Button pasteBtn = new Button("📄 粘贴");
        Button deleteBtn = new Button("🗑 删除");
        Button splitBtn = new Button("⚡ 分割");
        
        cutBtn.setOnAction(e -> cutSelectedClips());
        copyBtn.setOnAction(e -> copySelectedClips());
        pasteBtn.setOnAction(e -> pasteClips());
        deleteBtn.setOnAction(e -> deleteSelectedClips());
        splitBtn.setOnAction(e -> splitClipAtPlayhead());
        
        timeLabel = new Label("00:00:00 / 05:00:00");
        timeLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold;");
        
        zoomSlider = new Slider(0.1, 5.0, 1.0);
        zoomSlider.setPrefWidth(100);
        zoomSlider.valueProperty().addListener((obs, old, val) -> {
            zoomFactor = val.doubleValue();
            updateCanvas();
            redraw();
        });
        
        Label zoomLabel = new Label("缩放:");
        zoomLabel.setStyle("-fx-text-fill: white;");
        
        // 应用样式到按钮
        for (javafx.scene.Node node : List.of(playBtn, pauseBtn, stopBtn, cutBtn, copyBtn, pasteBtn, deleteBtn, splitBtn)) {
            if (node instanceof Button btn) {
                btn.setStyle("-fx-background-color: linear-gradient(to bottom, #4a90e2, #357abd); " +
                           "-fx-text-fill: white; -fx-border-radius: 4; -fx-background-radius: 4; " +
                           "-fx-padding: 4 8 4 8; -fx-font-size: 11px;");
            }
        }
        
        toolbar.getChildren().addAll(
            playBtn, pauseBtn, stopBtn,
            new Separator(),
            cutBtn, copyBtn, pasteBtn, deleteBtn,
            new Separator(),
            splitBtn,
            new Separator(),
            timeLabel,
            new Separator(),
            zoomLabel, zoomSlider
        );
        
        // 时间轴画布
        timelineCanvas = new Canvas();
        timelineCanvas.setStyle("-fx-background-color: #1a1a1a;");
        
        // 滚动面板
        scrollPane = new ScrollPane(timelineCanvas);
        scrollPane.setPrefHeight(TIMELINE_HEIGHT);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-border-color: #404040;");
        
        // 轨道标签
        trackLabels = new VBox();
        trackLabels.setPrefWidth(120);
        trackLabels.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #404040; -fx-border-width: 0 1 0 0;");
        
        // 标题
        Label headerLabel = new Label("轨道");
        headerLabel.setPrefHeight(RULER_HEIGHT);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #404040; -fx-border-width: 0 0 1 0;");
        trackLabels.getChildren().add(headerLabel);
        
        for (TrackType type : TrackType.values()) {
            Label label = new Label(type.name);
            label.setPrefHeight(TRACK_HEIGHT);
            label.setAlignment(Pos.CENTER);
            label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");
            trackLabels.getChildren().add(label);
        }
    }
    
    private void setupLayout() {
        // 主时间轴区域
        HBox timelineArea = new HBox();
        timelineArea.getChildren().addAll(trackLabels, scrollPane);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        getChildren().addAll(
            toolbar,
            timelineArea,
            new Label("提示: 拖拽媒体文件到时间轴，双击片段选择，右键菜单操作，空格键播放/暂停")
        );
        
        setStyle("-fx-background-color: #1a1a1a;");
    }
    
    private void setupEventHandlers() {
        // 画布鼠标事件
        timelineCanvas.setOnMousePressed(this::onMousePressed);
        timelineCanvas.setOnMouseDragged(this::onMouseDragged);
        timelineCanvas.setOnMouseReleased(this::onMouseReleased);
        timelineCanvas.setOnMouseClicked(this::onMouseClicked);
        
        // 键盘事件
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                e.consume();
                togglePlay();
            } else if (e.getCode() == KeyCode.DELETE) {
                e.consume();
                deleteSelectedClips();
            } else if (e.getCode() == KeyCode.C && e.isControlDown()) {
                e.consume();
                copySelectedClips();
            } else if (e.getCode() == KeyCode.V && e.isControlDown()) {
                e.consume();
                pasteClips();
            } else if (e.getCode() == KeyCode.X && e.isControlDown()) {
                e.consume();
                cutSelectedClips();
            } else if (e.getCode() == KeyCode.S && e.isControlDown()) {
                e.consume();
                splitClipAtPlayhead();
            }
        });
        
        setFocusTraversable(true);
        
        // 拖拽支持
        timelineCanvas.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            e.consume();
        });
        
        timelineCanvas.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                double dropTime = (e.getX() / zoomFactor) / PIXELS_PER_SECOND;
                
                for (File file : e.getDragboard().getFiles()) {
                    TrackType trackType = getTrackTypeFromFile(file);
                    if (trackType != null) {
                        MediaClip clip = new MediaClip(
                            UUID.randomUUID().toString(),
                            file.getName(),
                            file.getAbsolutePath(),
                            trackType,
                            dropTime,
                            60.0 // 默认60秒
                        );
                        addClip(clip);
                        dropTime += 0.1; // 避免重叠
                    }
                }
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }
    
    private void onMousePressed(MouseEvent e) {
        requestFocus();
        
        double x = e.getX();
        double y = e.getY();
        
        // 检查是否点击播放头
        double playheadX = currentTime * PIXELS_PER_SECOND * zoomFactor;
        if (Math.abs(x - playheadX) <= 5 && y <= RULER_HEIGHT) {
            isDraggingPlayhead = true;
            timelineCanvas.setCursor(Cursor.H_RESIZE);
            return;
        }
        
        // 检查是否点击片段
        MediaClip clickedClip = getClipAt(x, y);
        String handle = getResizeHandle(clickedClip, x, y);
        
        if (handle != null) {
            // 开始调整大小
            isResizingClip = true;
            resizeHandle = handle;
            draggedClip = clickedClip;
            timelineCanvas.setCursor(Cursor.H_RESIZE);
        } else if (clickedClip != null) {
            // 选择和拖拽片段
            if (!e.isControlDown()) {
                selectedClips.clear();
            }
            
            if (e.isControlDown() && selectedClips.contains(clickedClip)) {
                selectedClips.remove(clickedClip);
            } else {
                selectedClips.add(clickedClip);
            }
            
            if (e.getClickCount() == 1 && e.getButton() == MouseButton.PRIMARY) {
                isDraggingClip = true;
                draggedClip = clickedClip;
                dragStartX = x;
                dragStartTime = clickedClip.startTime;
                timelineCanvas.setCursor(Cursor.MOVE);
            }
            
            updateClipSelection();
            redraw();
            
            if (listener != null) {
                listener.onClipSelected(new ArrayList<>(selectedClips));
            }
        } else {
            // 清空选择
            selectedClips.clear();
            updateClipSelection();
            redraw();
        }
    }
    
    private void onMouseDragged(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        
        if (isDraggingPlayhead) {
            double newTime = Math.max(0, Math.min(x / (PIXELS_PER_SECOND * zoomFactor), totalDuration));
            setCurrentTime(newTime);
        } else if (isResizingClip && draggedClip != null) {
            double newTime = x / (PIXELS_PER_SECOND * zoomFactor);
            
            if ("left".equals(resizeHandle)) {
                double maxStart = draggedClip.startTime + draggedClip.duration - 0.1;
                draggedClip.startTime = Math.max(0, Math.min(newTime, maxStart));
                draggedClip.duration = dragStartTime + draggedClip.duration - draggedClip.startTime;
            } else if ("right".equals(resizeHandle)) {
                draggedClip.duration = Math.max(0.1, newTime - draggedClip.startTime);
            }
            
            redraw();
        } else if (isDraggingClip && draggedClip != null) {
            double deltaTime = (x - dragStartX) / (PIXELS_PER_SECOND * zoomFactor);
            double newStartTime = Math.max(0, dragStartTime + deltaTime);
            
            // 移动所有选中的片段
            double offset = newStartTime - draggedClip.startTime;
            for (MediaClip clip : selectedClips) {
                clip.startTime = Math.max(0, clip.startTime + offset);
            }
            
            redraw();
        }
    }
    
    private void onMouseReleased(MouseEvent e) {
        if (isDraggingClip && listener != null) {
            for (MediaClip clip : selectedClips) {
                listener.onClipMoved(clip);
            }
        }
        
        isDraggingPlayhead = false;
        isDraggingClip = false;
        isResizingClip = false;
        draggedClip = null;
        resizeHandle = null;
        timelineCanvas.setCursor(Cursor.DEFAULT);
    }
    
    private void onMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            double x = e.getX();
            double y = e.getY();
            MediaClip clip = getClipAt(x, y);
            
            if (clip != null && listener != null) {
                // 双击片段触发编辑
                selectedClips.clear();
                selectedClips.add(clip);
                updateClipSelection();
                redraw();
                listener.onClipSelected(List.of(clip));
            }
        } else if (e.getButton() == MouseButton.SECONDARY) {
            // 右键菜单
            showContextMenu(e);
        }
    }
    
    private void showContextMenu(MouseEvent e) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem cutItem = new MenuItem("裁剪");
        cutItem.setOnAction(ev -> cutSelectedClips());
        
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setOnAction(ev -> copySelectedClips());
        
        MenuItem pasteItem = new MenuItem("粘贴");
        pasteItem.setOnAction(ev -> pasteClips());
        pasteItem.setDisable(clipboard == null);
        
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(ev -> deleteSelectedClips());
        
        MenuItem splitItem = new MenuItem("分割");
        splitItem.setOnAction(ev -> splitClipAtPlayhead());
        
        contextMenu.getItems().addAll(cutItem, copyItem, pasteItem, new SeparatorMenuItem(), splitItem, deleteItem);
        
        boolean hasSelection = !selectedClips.isEmpty();
        cutItem.setDisable(!hasSelection);
        copyItem.setDisable(!hasSelection);
        deleteItem.setDisable(!hasSelection);
        splitItem.setDisable(!hasSelection);
        
        contextMenu.show(timelineCanvas, e.getScreenX(), e.getScreenY());
    }
    
    // 片段操作方法
    private void cutSelectedClips() {
        copySelectedClips();
        deleteSelectedClips();
    }
    
    private void copySelectedClips() {
        if (!selectedClips.isEmpty()) {
            // 简化：只复制第一个选中的片段
            clipboard = selectedClips.iterator().next();
        }
    }
    
    private void pasteClips() {
        if (clipboard != null) {
            MediaClip newClip = new MediaClip(
                UUID.randomUUID().toString(),
                clipboard.fileName,
                clipboard.filePath,
                clipboard.trackType,
                currentTime,
                clipboard.duration
            );
            
            addClip(newClip);
            selectedClips.clear();
            selectedClips.add(newClip);
            updateClipSelection();
            redraw();
        }
    }
    
    private void deleteSelectedClips() {
        for (MediaClip clip : selectedClips) {
            clips.remove(clip);
            if (listener != null) {
                listener.onClipRemoved(clip);
            }
        }
        selectedClips.clear();
        updateClipSelection();
        redraw();
    }
    
    private void splitClipAtPlayhead() {
        List<MediaClip> toSplit = selectedClips.stream()
            .filter(clip -> currentTime > clip.startTime && currentTime < clip.getEndTime())
            .toList();
        
        for (MediaClip clip : toSplit) {
            double splitTime = currentTime - clip.startTime;
            
            // 创建新片段
            MediaClip newClip = new MediaClip(
                UUID.randomUUID().toString(),
                clip.fileName,
                clip.filePath,
                clip.trackType,
                currentTime,
                clip.duration - splitTime
            );
            
            // 调整原片段
            clip.duration = splitTime;
            
            addClip(newClip);
        }
        
        redraw();
    }
    
    // 辅助方法
    private MediaClip getClipAt(double x, double y) {
        if (y < RULER_HEIGHT) return null;
        
        int trackIndex = (int) ((y - RULER_HEIGHT) / TRACK_HEIGHT);
        if (trackIndex < 0 || trackIndex >= TrackType.values().length) return null;
        
        TrackType trackType = TrackType.values()[trackIndex];
        double time = (x / zoomFactor) / PIXELS_PER_SECOND;
        
        return clips.stream()
            .filter(clip -> clip.trackType == trackType)
            .filter(clip -> time >= clip.startTime && time <= clip.getEndTime())
            .findFirst()
            .orElse(null);
    }
    
    private String getResizeHandle(MediaClip clip, double x, double y) {
        if (clip == null) return null;
        
        int trackIndex = clip.trackType.ordinal();
        double clipY = RULER_HEIGHT + trackIndex * TRACK_HEIGHT;
        double clipX = clip.startTime * PIXELS_PER_SECOND * zoomFactor;
        double clipWidth = clip.duration * PIXELS_PER_SECOND * zoomFactor;
        
        if (y >= clipY && y <= clipY + TRACK_HEIGHT) {
            if (x >= clipX && x <= clipX + 8) {
                return "left";
            } else if (x >= clipX + clipWidth - 8 && x <= clipX + clipWidth) {
                return "right";
            }
        }
        return null;
    }
    
    private TrackType getTrackTypeFromFile(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".mkv")) {
            return TrackType.VIDEO;
        } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".flac")) {
            return TrackType.AUDIO;
        }
        return null;
    }
    
    private void updateCanvas() {
        double width = Math.max(800, totalDuration * PIXELS_PER_SECOND * zoomFactor);
        timelineCanvas.setWidth(width);
        timelineCanvas.setHeight(TIMELINE_HEIGHT - RULER_HEIGHT);
    }
    
    private void redraw() {
        Platform.runLater(() -> {
            GraphicsContext gc = timelineCanvas.getGraphicsContext2D();
            double width = timelineCanvas.getWidth();
            double height = timelineCanvas.getHeight();
            
            // 清空画布
            gc.clearRect(0, 0, width, height);
            
            // 绘制背景
            gc.setFill(Color.web("#1a1a1a"));
            gc.fillRect(0, 0, width, height);
            
            // 绘制轨道背景
            drawTracks(gc, width, height);
            
            // 绘制时间标尺
            drawRuler(gc, width);
            
            // 绘制片段
            drawClips(gc);
            
            // 绘制播放头
            drawPlayhead(gc, height);
            
            // 更新时间显示
            updateTimeDisplay();
        });
    }
    
    private void drawTracks(GraphicsContext gc, double width, double height) {
        for (int i = 0; i < TrackType.values().length; i++) {
            TrackType type = TrackType.values()[i];
            double y = i * TRACK_HEIGHT;
            
            // 轨道背景
            gc.setFill(type.color.deriveColor(0, 1, 1, 0.1));
            gc.fillRect(0, y, width, TRACK_HEIGHT);
            
            // 轨道边框
            gc.setStroke(Color.web("#333"));
            gc.setLineWidth(1);
            gc.strokeRect(0, y, width, TRACK_HEIGHT);
        }
    }
    
    private void drawRuler(GraphicsContext gc, double width) {
        // 标尺背景
        gc.setFill(Color.web("#2d2d2d"));
        gc.fillRect(0, -RULER_HEIGHT, width, RULER_HEIGHT);
        
        gc.setStroke(Color.web("#666"));
        gc.setLineWidth(1);
        gc.setFont(Font.font("Consolas", 10));
        
        double secondWidth = PIXELS_PER_SECOND * zoomFactor;
        int interval = Math.max(1, (int)(30 / secondWidth));
        
        for (int second = 0; second <= totalDuration; second += interval) {
            double x = second * secondWidth;
            if (x > width) break;
            
            gc.strokeLine(x, -RULER_HEIGHT, x, -RULER_HEIGHT + 20);
            
            int minutes = second / 60;
            int seconds = second % 60;
            String timeText = String.format("%02d:%02d", minutes, seconds);
            
            gc.setFill(Color.web("#ccc"));
            gc.fillText(timeText, x + 2, -RULER_HEIGHT + 15);
        }
    }
    
    private void drawClips(GraphicsContext gc) {
        for (MediaClip clip : clips) {
            int trackIndex = clip.trackType.ordinal();
            double y = trackIndex * TRACK_HEIGHT + 5;
            double x = clip.startTime * PIXELS_PER_SECOND * zoomFactor;
            double width = clip.duration * PIXELS_PER_SECOND * zoomFactor;
            double height = TRACK_HEIGHT - 10;
            
            // 片段背景
            Color baseColor = clip.trackType.color;
            if (selectedClips.contains(clip)) {
                gc.setFill(baseColor.brighter());
            } else {
                gc.setFill(baseColor);
            }
            gc.fillRoundRect(x, y, width, height, 5, 5);
            
            // 边框
            gc.setStroke(selectedClips.contains(clip) ? Color.YELLOW : Color.GRAY);
            gc.setLineWidth(selectedClips.contains(clip) ? 2 : 1);
            gc.strokeRoundRect(x, y, width, height, 5, 5);
            
            // 文本
            if (width > 50) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 11));
                gc.fillText(clip.fileName, x + 5, y + 20);
                
                gc.setFont(Font.font("Microsoft YaHei", 9));
                gc.fillText(formatDuration(clip.duration), x + 5, y + height - 5);
            }
            
            // 调整手柄
            if (selectedClips.contains(clip) && width > 20) {
                gc.setFill(Color.web("#ffffff", 0.3));
                gc.fillRect(x, y, 8, height);
                gc.fillRect(x + width - 8, y, 8, height);
            }
        }
    }
    
    private void drawPlayhead(GraphicsContext gc, double height) {
        double x = currentTime * PIXELS_PER_SECOND * zoomFactor;
        
        // 播放头线
        gc.setStroke(Color.RED);
        gc.setLineWidth(PLAYHEAD_WIDTH);
        gc.strokeLine(x, -RULER_HEIGHT, x, height);
        
        // 播放头顶部
        gc.setFill(Color.RED);
        double[] xPoints = {x-6, x+6, x};
        double[] yPoints = {-RULER_HEIGHT, -RULER_HEIGHT, -RULER_HEIGHT+12};
        gc.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void updateTimeDisplay() {
        String current = formatTime(currentTime);
        String total = formatTime(totalDuration);
        timeLabel.setText(current + " / " + total);
    }
    
    private void updateClipSelection() {
        for (MediaClip clip : clips) {
            clip.selected = selectedClips.contains(clip);
        }
    }
    
    private String formatTime(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    
    private String formatDuration(double seconds) {
        int m = (int) (seconds / 60);
        int s = (int) (seconds % 60);
        return String.format("%d:%02d", m, s);
    }
    
    // 公共API
    public void addClip(MediaClip clip) {
        clips.add(clip);
        redraw();
        if (listener != null) {
            listener.onClipAdded(clip);
        }
    }
    
    public void removeClip(MediaClip clip) {
        clips.remove(clip);
        selectedClips.remove(clip);
        redraw();
        if (listener != null) {
            listener.onClipRemoved(clip);
        }
    }
    
    public void clearAllClips() {
        clips.clear();
        selectedClips.clear();
        redraw();
    }
    
    public void setCurrentTime(double time) {
        currentTime = Math.max(0, Math.min(time, totalDuration));
        redraw();
        if (listener != null) {
            listener.onTimeChanged(currentTime);
        }
    }
    
    public double getCurrentTime() {
        return currentTime;
    }
    
    public void setPlaying(boolean playing) {
        isPlaying = playing;
        if (listener != null) {
            listener.onPlayStateChanged(playing);
        }
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void setListener(TimelineListener listener) {
        this.listener = listener;
    }
    
    public List<MediaClip> getClips() {
        return new ArrayList<>(clips);
    }
    
    public List<MediaClip> getSelectedClips() {
        return new ArrayList<>(selectedClips);
    }
    
    // 播放控制
    private void togglePlay() {
        setPlaying(!isPlaying);
    }
    
    private void pause() {
        setPlaying(false);
    }
    
    private void stop() {
        setPlaying(false);
        setCurrentTime(0);
    }
}
