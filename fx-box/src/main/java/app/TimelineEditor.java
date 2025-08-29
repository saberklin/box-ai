package app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间轴编辑器 - 可视化编辑声光电同步项目
 */
public class TimelineEditor extends VBox {
    
    // 时间轴配置
    private static final double TIMELINE_HEIGHT = 400;
    private static final double TRACK_HEIGHT = 60;
    private static final double RULER_HEIGHT = 30;
    private static final double PIXELS_PER_SECOND = 50; // 每秒对应的像素数
    
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
        public String fileName;
        public String filePath;
        public TrackType trackType;
        public double startTime;    // 在时间轴上的开始时间
        public double duration;     // 片段时长
        public double offsetTime;   // 文件内的偏移时间
        
        public MediaClip(String fileName, String filePath, TrackType trackType, double startTime, double duration) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.trackType = trackType;
            this.startTime = startTime;
            this.duration = duration;
            this.offsetTime = 0;
        }
        
        public double getEndTime() {
            return startTime + duration;
        }
    }
    
    // 组件
    private Canvas timelineCanvas;
    private ScrollPane scrollPane;
    private VBox tracksContainer;
    private Slider zoomSlider;
    private Label timeLabel;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    
    // 数据
    private List<MediaClip> mediaClips = new ArrayList<>();
    private double totalDuration = 300; // 默认5分钟
    private double currentTime = 0;
    private double zoomFactor = 1.0;
    private boolean isPlaying = false;
    
    // 拖拽状态
    private MediaClip draggingClip = null;
    private double dragStartX = 0;
    private double dragStartTime = 0;
    
    // 监听器
    private TimelineListener listener;
    
    public interface TimelineListener {
        default void onTimeChanged(double time) {}
        default void onClipAdded(MediaClip clip) {}
        default void onClipMoved(MediaClip clip) {}
        default void onClipRemoved(MediaClip clip) {}
        default void onPlayStateChanged(boolean playing) {}
    }
    
    public TimelineEditor() {
        initComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initComponents() {
        // 时间轴画布
        timelineCanvas = new Canvas(1000, TIMELINE_HEIGHT);
        
        // 滚动面板
        scrollPane = new ScrollPane();
        scrollPane.setContent(timelineCanvas);
        scrollPane.setPrefHeight(TIMELINE_HEIGHT + 20);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // 控制按钮
        playButton = new Button("播放");
        pauseButton = new Button("暂停");
        stopButton = new Button("停止");
        
        // 缩放控制
        zoomSlider = new Slider(0.1, 5.0, 1.0);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        
        // 时间显示
        timeLabel = new Label("00:00 / 05:00");
        
        // 轨道容器
        tracksContainer = new VBox(5);
        tracksContainer.setPadding(new Insets(10));
    }
    
    private void setupLayout() {
        // 控制栏
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        controls.getChildren().addAll(
            playButton, pauseButton, stopButton,
            new Separator(),
            new Label("缩放:"), zoomSlider,
            new Separator(),
            timeLabel
        );
        
        // 轨道标签
        VBox trackLabels = new VBox();
        trackLabels.setPrefWidth(100);
        for (TrackType type : TrackType.values()) {
            Label label = new Label(type.name);
            label.setPrefHeight(TRACK_HEIGHT);
            label.setStyle("-fx-background-color: " + toHexString(type.color) + "; -fx-alignment: center;");
            trackLabels.getChildren().add(label);
        }
        
        // 时间轴区域
        HBox timelineArea = new HBox();
        timelineArea.getChildren().addAll(trackLabels, scrollPane);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        // 主布局
        getChildren().addAll(
            controls,
            new Separator(),
            timelineArea,
            new Separator(),
            new Label("提示: 拖拽媒体文件到时间轴上添加片段，拖拽片段调整位置")
        );
    }
    
    private void setupEventHandlers() {
        // 播放控制
        playButton.setOnAction(e -> {
            isPlaying = true;
            if (listener != null) listener.onPlayStateChanged(true);
            updatePlayButtons();
        });
        
        pauseButton.setOnAction(e -> {
            isPlaying = false;
            if (listener != null) listener.onPlayStateChanged(false);
            updatePlayButtons();
        });
        
        stopButton.setOnAction(e -> {
            isPlaying = false;
            currentTime = 0;
            if (listener != null) {
                listener.onPlayStateChanged(false);
                listener.onTimeChanged(currentTime);
            }
            updatePlayButtons();
            redrawTimeline();
        });
        
        // 缩放控制
        zoomSlider.valueProperty().addListener((obs, old, newVal) -> {
            zoomFactor = newVal.doubleValue();
            updateCanvasSize();
            redrawTimeline();
        });
        
        // 时间轴点击
        timelineCanvas.setOnMouseClicked(e -> {
            double clickTime = (e.getX() / zoomFactor) / PIXELS_PER_SECOND;
            setCurrentTime(Math.max(0, Math.min(clickTime, totalDuration)));
        });
        
        // 拖拽支持
        setupDragAndDrop();
        
        // 初始绘制
        Platform.runLater(this::redrawTimeline);
    }
    
    private void setupDragAndDrop() {
        // 接受文件拖拽
        timelineCanvas.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        
        timelineCanvas.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    TrackType trackType = getTrackTypeFromFile(file);
                    if (trackType != null) {
                        double dropTime = (e.getX() / zoomFactor) / PIXELS_PER_SECOND;
                        double duration = estimateFileDuration(file);
                        
                        MediaClip clip = new MediaClip(
                            file.getName(),
                            file.getAbsolutePath(),
                            trackType,
                            dropTime,
                            duration
                        );
                        
                        addMediaClip(clip);
                        success = true;
                    }
                }
            }
            
            e.setDropCompleted(success);
            e.consume();
        });
        
        // 片段拖拽
        timelineCanvas.setOnMousePressed(e -> {
            MediaClip clip = getClipAtPosition(e.getX(), e.getY());
            if (clip != null) {
                draggingClip = clip;
                dragStartX = e.getX();
                dragStartTime = clip.startTime;
                timelineCanvas.setCursor(Cursor.MOVE);
            }
        });
        
        timelineCanvas.setOnMouseDragged(e -> {
            if (draggingClip != null) {
                double deltaX = e.getX() - dragStartX;
                double deltaTime = (deltaX / zoomFactor) / PIXELS_PER_SECOND;
                double newStartTime = Math.max(0, dragStartTime + deltaTime);
                
                draggingClip.startTime = newStartTime;
                redrawTimeline();
            }
        });
        
        timelineCanvas.setOnMouseReleased(e -> {
            if (draggingClip != null) {
                if (listener != null) {
                    listener.onClipMoved(draggingClip);
                }
                draggingClip = null;
                timelineCanvas.setCursor(Cursor.DEFAULT);
            }
        });
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
    
    private double estimateFileDuration(File file) {
        // 简单估算，实际应该读取文件元数据
        return 60.0; // 默认60秒
    }
    
    private MediaClip getClipAtPosition(double x, double y) {
        int trackIndex = (int) ((y - RULER_HEIGHT) / TRACK_HEIGHT);
        if (trackIndex < 0 || trackIndex >= TrackType.values().length) {
            return null;
        }
        
        TrackType trackType = TrackType.values()[trackIndex];
        double time = (x / zoomFactor) / PIXELS_PER_SECOND;
        
        for (MediaClip clip : mediaClips) {
            if (clip.trackType == trackType && 
                time >= clip.startTime && 
                time <= clip.getEndTime()) {
                return clip;
            }
        }
        return null;
    }
    
    private void updateCanvasSize() {
        double width = Math.max(1000, totalDuration * PIXELS_PER_SECOND * zoomFactor);
        timelineCanvas.setWidth(width);
    }
    
    private void redrawTimeline() {
        GraphicsContext gc = timelineCanvas.getGraphicsContext2D();
        double width = timelineCanvas.getWidth();
        double height = timelineCanvas.getHeight();
        
        // 清空画布
        gc.clearRect(0, 0, width, height);
        
        // 绘制背景
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // 绘制时间标尺
        drawTimeRuler(gc, width);
        
        // 绘制轨道
        drawTracks(gc, width);
        
        // 绘制媒体片段
        drawMediaClips(gc);
        
        // 绘制播放头
        drawPlayhead(gc, height);
    }
    
    private void drawTimeRuler(GraphicsContext gc, double width) {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, width, RULER_HEIGHT);
        
        gc.setStroke(Color.BLACK);
        gc.setFont(Font.font(10));
        
        double secondWidth = PIXELS_PER_SECOND * zoomFactor;
        int interval = Math.max(1, (int)(30 / secondWidth)); // 至少30像素间隔
        
        for (int second = 0; second <= totalDuration; second += interval) {
            double x = second * secondWidth;
            if (x > width) break;
            
            gc.strokeLine(x, 0, x, RULER_HEIGHT);
            
            int minutes = second / 60;
            int seconds = second % 60;
            String timeText = String.format("%02d:%02d", minutes, seconds);
            gc.fillText(timeText, x + 2, RULER_HEIGHT - 5);
        }
    }
    
    private void drawTracks(GraphicsContext gc, double width) {
        for (int i = 0; i < TrackType.values().length; i++) {
            TrackType type = TrackType.values()[i];
            double y = RULER_HEIGHT + i * TRACK_HEIGHT;
            
            // 轨道背景
            gc.setFill(type.color.deriveColor(0, 1, 1, 0.3));
            gc.fillRect(0, y, width, TRACK_HEIGHT);
            
            // 轨道边框
            gc.setStroke(Color.GRAY);
            gc.strokeRect(0, y, width, TRACK_HEIGHT);
        }
    }
    
    private void drawMediaClips(GraphicsContext gc) {
        for (MediaClip clip : mediaClips) {
            int trackIndex = clip.trackType.ordinal();
            double y = RULER_HEIGHT + trackIndex * TRACK_HEIGHT + 5;
            double x = clip.startTime * PIXELS_PER_SECOND * zoomFactor;
            double w = clip.duration * PIXELS_PER_SECOND * zoomFactor;
            double h = TRACK_HEIGHT - 10;
            
            // 片段背景
            gc.setFill(clip.trackType.color);
            gc.fillRoundRect(x, y, w, h, 5, 5);
            
            // 片段边框
            gc.setStroke(Color.DARKGRAY);
            gc.strokeRoundRect(x, y, w, h, 5, 5);
            
            // 片段文本
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(12));
            String text = clip.fileName;
            if (w > 100) { // 只有足够宽度才显示文本
                gc.fillText(text, x + 5, y + h/2 + 4);
            }
        }
    }
    
    private void drawPlayhead(GraphicsContext gc, double height) {
        double x = currentTime * PIXELS_PER_SECOND * zoomFactor;
        
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeLine(x, 0, x, height);
        
        // 播放头顶部三角形
        gc.setFill(Color.RED);
        double[] xPoints = {x-5, x+5, x};
        double[] yPoints = {0, 0, 10};
        gc.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void updatePlayButtons() {
        playButton.setDisable(isPlaying);
        pauseButton.setDisable(!isPlaying);
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    // 公共方法
    public void addMediaClip(MediaClip clip) {
        mediaClips.add(clip);
        
        // 更新总时长
        double maxEndTime = mediaClips.stream()
            .mapToDouble(MediaClip::getEndTime)
            .max()
            .orElse(totalDuration);
        
        if (maxEndTime > totalDuration) {
            totalDuration = maxEndTime + 30; // 额外30秒缓冲
            updateCanvasSize();
        }
        
        redrawTimeline();
        
        if (listener != null) {
            listener.onClipAdded(clip);
        }
    }
    
    public void removeMediaClip(MediaClip clip) {
        mediaClips.remove(clip);
        redrawTimeline();
        
        if (listener != null) {
            listener.onClipRemoved(clip);
        }
    }
    
    public void setCurrentTime(double time) {
        currentTime = Math.max(0, Math.min(time, totalDuration));
        
        // 更新时间显示
        int currentMinutes = (int) (currentTime / 60);
        int currentSeconds = (int) (currentTime % 60);
        int totalMinutes = (int) (totalDuration / 60);
        int totalSecondsRem = (int) (totalDuration % 60);
        
        timeLabel.setText(String.format("%02d:%02d / %02d:%02d", 
            currentMinutes, currentSeconds, totalMinutes, totalSecondsRem));
        
        redrawTimeline();
        
        if (listener != null) {
            listener.onTimeChanged(currentTime);
        }
    }
    
    public void setTimelineListener(TimelineListener listener) {
        this.listener = listener;
    }
    
    public List<MediaClip> getMediaClips() {
        return new ArrayList<>(mediaClips);
    }
    
    public double getCurrentTime() {
        return currentTime;
    }
    
    public double getTotalDuration() {
        return totalDuration;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        updatePlayButtons();
        if (listener != null) {
            listener.onPlayStateChanged(playing);
        }
    }
}
