package app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 简化的时间轴编辑器 - 完全基于JavaFX控件，不使用Canvas
 * 解决所有渲染纹理问题
 */
public class SimpleTimelineEditor extends VBox {
    
    // 时间轴配置
    private static final double TRACK_HEIGHT = 60;
    private static final double PIXELS_PER_SECOND = 20;
    
    // 轨道类型
    public enum TrackType {
        VIDEO("视频轨道", "-fx-background-color: lightblue;"),
        AUDIO("音频轨道", "-fx-background-color: lightgreen;"), 
        LIGHT("灯光轨道", "-fx-background-color: lightyellow;");
        
        public final String name;
        public final String style;
        
        TrackType(String name, String style) {
            this.name = name;
            this.style = style;
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
        public Button clipButton;
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
    
    // 组件
    private HBox toolbar;
    private ScrollPane scrollPane;
    private VBox tracksContainer;
    private Slider timeSlider;
    private Label timeLabel;
    private Slider zoomSlider;
    
    // 轨道面板
    private Map<TrackType, Pane> trackPanes = new HashMap<>();
    private Rectangle playheadLine;
    
    // 数据
    private List<MediaClip> clips = new CopyOnWriteArrayList<>();
    private Set<MediaClip> selectedClips = new HashSet<>();
    private MediaClip clipboard = null;
    
    // 状态
    private double totalDuration = 300; // 5分钟
    private double currentTime = 0;
    private double zoomFactor = 1.0;
    private boolean isPlaying = false;
    
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
    
    public SimpleTimelineEditor() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        updateTimelineView();
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
        
        Button copyBtn = new Button("📋 复制");
        Button pasteBtn = new Button("📄 粘贴");
        Button deleteBtn = new Button("🗑 删除");
        
        copyBtn.setOnAction(e -> copySelectedClips());
        pasteBtn.setOnAction(e -> pasteClips());
        deleteBtn.setOnAction(e -> deleteSelectedClips());
        
        timeLabel = new Label("00:00 / 05:00");
        timeLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold;");
        
        // 时间滑块
        timeSlider = new Slider(0, totalDuration, 0);
        timeSlider.setPrefWidth(200);
        timeSlider.valueProperty().addListener((obs, old, val) -> {
            setCurrentTime(val.doubleValue());
        });
        
        zoomSlider = new Slider(0.5, 3.0, 1.0);
        zoomSlider.setPrefWidth(100);
        zoomSlider.valueProperty().addListener((obs, old, val) -> {
            zoomFactor = val.doubleValue();
            updateTimelineView();
        });
        
        Label zoomLabel = new Label("缩放:");
        zoomLabel.setStyle("-fx-text-fill: white;");
        
        // 应用样式到按钮
        for (javafx.scene.Node node : List.of(playBtn, pauseBtn, stopBtn, copyBtn, pasteBtn, deleteBtn)) {
            if (node instanceof Button btn) {
                btn.setStyle("-fx-background-color: linear-gradient(to bottom, #4a90e2, #357abd); " +
                           "-fx-text-fill: white; -fx-border-radius: 4; -fx-background-radius: 4; " +
                           "-fx-padding: 4 8 4 8; -fx-font-size: 11px;");
            }
        }
        
        toolbar.getChildren().addAll(
            playBtn, pauseBtn, stopBtn,
            new Separator(),
            copyBtn, pasteBtn, deleteBtn,
            new Separator(),
            new Label("时间:") {{ setStyle("-fx-text-fill: white;"); }},
            timeSlider, timeLabel,
            new Separator(),
            zoomLabel, zoomSlider
        );
        
        // 轨道容器
        tracksContainer = new VBox();
        tracksContainer.setStyle("-fx-background-color: #1a1a1a;");
        
        // 为每个轨道类型创建轨道面板
        for (TrackType trackType : TrackType.values()) {
            VBox trackBox = new VBox();
            
            // 轨道标签
            Label trackLabel = new Label(trackType.name);
            trackLabel.setPrefHeight(30);
            trackLabel.setAlignment(Pos.CENTER_LEFT);
            trackLabel.setPadding(new Insets(5, 10, 5, 10));
            trackLabel.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #404040; -fx-border-width: 0 0 1 0;");
            
            // 轨道面板
            Pane trackPane = new Pane();
            trackPane.setPrefHeight(TRACK_HEIGHT);
            trackPane.setStyle(trackType.style + " -fx-border-color: #333; -fx-border-width: 0 0 1 0;");
            trackPanes.put(trackType, trackPane);
            
            // 拖拽支持
            trackPane.setOnDragOver(e -> {
                if (e.getDragboard().hasFiles()) {
                    e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                }
                e.consume();
            });
            
            trackPane.setOnDragDropped(e -> {
                if (e.getDragboard().hasFiles()) {
                    double dropTime = e.getX() / (PIXELS_PER_SECOND * zoomFactor);
                    
                    for (File file : e.getDragboard().getFiles()) {
                        TrackType fileTrackType = getTrackTypeFromFile(file);
                        if (fileTrackType == trackType) {
                            MediaClip clip = new MediaClip(
                                UUID.randomUUID().toString(),
                                file.getName(),
                                file.getAbsolutePath(),
                                trackType,
                                dropTime,
                                60.0 // 默认60秒
                            );
                            addClip(clip);
                        }
                    }
                }
                e.setDropCompleted(true);
                e.consume();
            });
            
            trackBox.getChildren().addAll(trackLabel, trackPane);
            tracksContainer.getChildren().add(trackBox);
        }
        
        // 播放头线
        playheadLine = new Rectangle(2, tracksContainer.getPrefHeight());
        playheadLine.setFill(Color.RED);
        playheadLine.setMouseTransparent(false);
        playheadLine.setOnMouseDragged(e -> {
            double newTime = e.getX() / (PIXELS_PER_SECOND * zoomFactor);
            setCurrentTime(Math.max(0, Math.min(newTime, totalDuration)));
        });
        
        // 设置轨道容器的高度
        tracksContainer.setPrefHeight(TrackType.values().length * (TRACK_HEIGHT + 30)); // 轨道高度 + 标签高度
        
        // 滚动面板
        scrollPane = new ScrollPane();
        scrollPane.setContent(tracksContainer);
        scrollPane.setPrefHeight(300);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-border-color: #404040;");
    }
    
    private void setupLayout() {
        getChildren().addAll(
            toolbar,
            scrollPane,
            new Label("提示: 拖拽媒体文件到对应轨道，单击片段选择，双击编辑，空格键播放/暂停") {{
                setStyle("-fx-text-fill: #888; -fx-padding: 5;");
            }}
        );
        
        setStyle("-fx-background-color: #1a1a1a;");
    }
    
    private void setupEventHandlers() {
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
            }
        });
        
        setFocusTraversable(true);
    }
    
    private Button createClipButton(MediaClip clip) {
        Button clipBtn = new Button(clip.fileName);
        clipBtn.setPrefHeight(TRACK_HEIGHT - 10);
        clipBtn.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-color: #666; -fx-border-width: 1; " +
                        "-fx-font-size: 10px; -fx-text-fill: black; -fx-background-radius: 3; -fx-border-radius: 3;");
        
        // 单击选择
        clipBtn.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (!e.isControlDown()) {
                    selectedClips.clear();
                }
                
                if (e.isControlDown() && selectedClips.contains(clip)) {
                    selectedClips.remove(clip);
                } else {
                    selectedClips.add(clip);
                }
                
                updateClipSelection();
                
                if (listener != null) {
                    listener.onClipSelected(new ArrayList<>(selectedClips));
                }
                
                // 双击编辑
                if (e.getClickCount() == 2) {
                    // 触发编辑事件
                    if (listener != null) {
                        listener.onClipSelected(List.of(clip));
                    }
                }
            } else if (e.getButton() == MouseButton.SECONDARY) {
                // 右键菜单
                showContextMenu(e, clip);
            }
            e.consume();
        });
        
        return clipBtn;
    }
    
    private void showContextMenu(javafx.scene.input.MouseEvent e, MediaClip clip) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem copyItem = new MenuItem("复制");
        copyItem.setOnAction(ev -> {
            selectedClips.clear();
            selectedClips.add(clip);
            copySelectedClips();
        });
        
        MenuItem pasteItem = new MenuItem("粘贴");
        pasteItem.setOnAction(ev -> pasteClips());
        pasteItem.setDisable(clipboard == null);
        
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(ev -> {
            selectedClips.clear();
            selectedClips.add(clip);
            deleteSelectedClips();
        });
        
        contextMenu.getItems().addAll(copyItem, pasteItem, new SeparatorMenuItem(), deleteItem);
        contextMenu.show(clip.clipButton, e.getScreenX(), e.getScreenY());
    }
    
    private void updateTimelineView() {
        Platform.runLater(() -> {
            // 更新所有片段的位置和大小
            for (MediaClip clip : clips) {
                updateClipPosition(clip);
            }
            
            // 更新播放头位置
            updatePlayheadPosition();
            
            // 更新时间显示
            updateTimeDisplay();
        });
    }
    
    private void updateClipPosition(MediaClip clip) {
        if (clip.clipButton != null) {
            double x = clip.startTime * PIXELS_PER_SECOND * zoomFactor;
            double width = clip.duration * PIXELS_PER_SECOND * zoomFactor;
            
            clip.clipButton.setLayoutX(x);
            clip.clipButton.setPrefWidth(Math.max(width, 50)); // 最小宽度50
        }
    }
    
    private void updatePlayheadPosition() {
        double x = currentTime * PIXELS_PER_SECOND * zoomFactor;
        playheadLine.setLayoutX(x);
    }
    
    private void updateClipSelection() {
        for (MediaClip clip : clips) {
            if (clip.clipButton != null) {
                if (selectedClips.contains(clip)) {
                    clip.clipButton.setStyle(clip.clipButton.getStyle() + " -fx-border-color: yellow; -fx-border-width: 3;");
                } else {
                    clip.clipButton.setStyle(clip.clipButton.getStyle().replaceAll("-fx-border-color: yellow; -fx-border-width: 3;", ""));
                }
            }
        }
    }
    
    private void updateTimeDisplay() {
        String current = formatTime(currentTime);
        String total = formatTime(totalDuration);
        timeLabel.setText(current + " / " + total);
        
        timeSlider.setValue(currentTime);
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
    
    private String formatTime(double seconds) {
        int m = (int) (seconds / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d", m, s);
    }
    
    // 片段操作方法
    private void copySelectedClips() {
        if (!selectedClips.isEmpty()) {
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
        }
    }
    
    private void deleteSelectedClips() {
        for (MediaClip clip : selectedClips) {
            removeClip(clip);
        }
        selectedClips.clear();
        updateClipSelection();
    }
    
    // 公共API
    public void addClip(MediaClip clip) {
        clips.add(clip);
        
        // 创建片段按钮
        clip.clipButton = createClipButton(clip);
        
        // 添加到对应轨道
        Pane trackPane = trackPanes.get(clip.trackType);
        if (trackPane != null) {
            trackPane.getChildren().add(clip.clipButton);
            updateClipPosition(clip);
        }
        
        if (listener != null) {
            listener.onClipAdded(clip);
        }
    }
    
    public void removeClip(MediaClip clip) {
        clips.remove(clip);
        
        // 从轨道面板移除按钮
        if (clip.clipButton != null) {
            Pane trackPane = trackPanes.get(clip.trackType);
            if (trackPane != null) {
                trackPane.getChildren().remove(clip.clipButton);
            }
        }
        
        if (listener != null) {
            listener.onClipRemoved(clip);
        }
    }
    
    public void clearAllClips() {
        // 清空所有轨道
        for (Pane pane : trackPanes.values()) {
            pane.getChildren().clear();
        }
        
        clips.clear();
        selectedClips.clear();
    }
    
    public void setCurrentTime(double time) {
        currentTime = Math.max(0, Math.min(time, totalDuration));
        updatePlayheadPosition();
        updateTimeDisplay();
        
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
