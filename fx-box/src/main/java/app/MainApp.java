package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import atlantafx.base.theme.PrimerDark;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class MainApp extends Application {
  private MediaPlayer videoPlayer;
  private MediaView previewView;
  private ListView<File> videoList;
  private Label status;
  private Slider posSlider;
  private Label timeLabel;
  private Slider volSlider;
  private boolean holdSeek = false;
  private TextField streamField;
  private final MultiScreenManager multiScreen = new MultiScreenManager();
  // Redis 订阅器
  private DeviceControlSubscriber subscriber;

  // 音频
  private MediaPlayer audioPlayer;
  private CustomAudioPlayer customAudioPlayer;
  private ListView<File> audioList;
  private Slider audioVol;
  private ComboBox<AudioDeviceManager.AudioDevice> audioOutputDevice;
  private ComboBox<AudioDeviceManager.AudioDevice> audioInputDevice;
  private boolean useCustomPlayer = false;
  
  // 同步控制器
  private SyncController syncController;
  private ListView<SyncController.SyncProject> projectList;
  private Button createProjectBtn;
  private Button playProjectBtn;
  private Button pauseProjectBtn;
  private Button stopProjectBtn;
  private Slider projectTimeSlider;
  private Label projectTimeLabel;
  
  // 时间轴编辑器
  private StableTimelineEditor stableTimelineEditor;
  private MediaView syncPreviewView;
  


  // 灯光 TCP DMX
  private TextField ipField;
  private Spinner<Integer> portField;
  private Spinner<Integer> uniField;
  private Button dmxConnectBtn;
  private Socket dmxSocket;
  private byte[] dmxData = new byte[512];

  @Override public void start(Stage stage) {
    stage.setTitle("FX Box");

    // 顶部工具栏
    Button btnOpen = new Button("打开", new FontIcon("fas-folder-open"));
    Button btnPlay = new Button("播放/暂停", new FontIcon("fas-play"));
    Button btnMap = new Button("屏幕映射", new FontIcon("fas-desktop"));
    Button btnStopExt = new Button("停止外部", new FontIcon("fas-ban"));
    streamField = new TextField(); streamField.setPromptText("http/rtsp/udp 地址..."); streamField.setPrefWidth(260);
    Button btnOpenStream = new Button("打开网络流", new FontIcon("fas-plug"));
    ToolBar tool = new ToolBar(btnOpen, btnPlay, new Separator(), new Label("网络流:"), streamField, btnOpenStream, new Separator(), btnMap, btnStopExt);

    // 视频页
    previewView = new MediaView();
    previewView.setPreserveRatio(false);
    previewView.setFitWidth(800);
    previewView.setFitHeight(450);

    videoList = new ListView<>();
    Button addToList = new Button("添加");
    Button removeSel = new Button("移除");
    VBox listBox = new VBox(6, videoList, new HBox(6, addToList, removeSel));
    listBox.setPadding(new Insets(8));
    listBox.setPrefWidth(280);

    SplitPane sp = new SplitPane();
    sp.setDividerPositions(0.3);
    sp.getItems().addAll(listBox, previewView);

    // TabPane
    TabPane tabs = new TabPane();
    Tab videoTab = new Tab("视频", sp); videoTab.setClosable(false);

    // 音频页
    audioList = new ListView<>();
    audioVol = new Slider(0, 100, 60);
    Button addAudio = new Button("添加音频");
    Button removeAudio = new Button("移除选中");
    Button playAudio = new Button("播放/暂停");
    Button stopAudio = new Button("停止");
    Button refreshAudioDevices = new Button("刷新设备");
    Button testOutputDevice = new Button("测试输出");
    Button testInputDevice = new Button("测试输入");
    Button switchPlayer = new Button("切换播放器");
    
    // 音频设备选择
    audioOutputDevice = new ComboBox<>();
    audioOutputDevice.setPromptText("选择输出设备");
    audioOutputDevice.setPrefWidth(250);
    audioInputDevice = new ComboBox<>();
    audioInputDevice.setPromptText("选择输入设备");
    audioInputDevice.setPrefWidth(250);
    
    // 设备选择区域
    GridPane deviceGrid = new GridPane();
    deviceGrid.setHgap(10);
    deviceGrid.setVgap(8);
    deviceGrid.addRow(0, new Label("输出设备:"), audioOutputDevice, testOutputDevice, refreshAudioDevices);
    deviceGrid.addRow(1, new Label("输入设备:"), audioInputDevice, testInputDevice, switchPlayer);
    
    HBox audioBtnsTop = new HBox(8, addAudio, removeAudio);
    HBox audioBtnsBottom = new HBox(8, playAudio, stopAudio, new Label("音量"), audioVol);
    VBox audioPane = new VBox(10, deviceGrid, new Separator(), audioBtnsTop, audioList, audioBtnsBottom);
    audioPane.setPadding(new Insets(10));
    Tab audioTab = new Tab("音频", audioPane); audioTab.setClosable(false);

    // 灯光页（TCP DMX）
    ipField = new TextField("127.0.0.1");
    portField = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(5568, 65535, 6454));
    uniField = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65535, 0));
    dmxConnectBtn = new Button("连接");
    Button blackoutBtn = new Button("全黑");
    GridPane dmxGrid = new GridPane();
    dmxGrid.setHgap(8); dmxGrid.setVgap(6); dmxGrid.setPadding(new Insets(10));
    dmxGrid.addRow(0, new Label("IP"), ipField, new Label("端口"), portField, new Label("Universe"), uniField, dmxConnectBtn, blackoutBtn);

    GridPane sliders = new GridPane(); sliders.setHgap(6); sliders.setVgap(6);
    for (int i = 0; i < 16; i++) {
      int ch = i;
      Slider s = new Slider(0, 255, 0);
      s.setShowTickMarks(true); s.setShowTickLabels(true); s.setMajorTickUnit(128);
      s.valueProperty().addListener((o, ov, nv) -> {
        dmxData[ch] = (byte) nv.intValue();
        sendDmxFrame();
      });
      sliders.addRow(i, new Label(String.format("Ch%03d", ch+1)), s);
    }
    VBox lightPane = new VBox(10, dmxGrid, new Separator(), new ScrollPane(sliders));
    lightPane.setPadding(new Insets(10));
    Tab lightTab = new Tab("灯光", lightPane); lightTab.setClosable(false);
    
    // === 同步项目 Tab ===
    VBox syncPane = createSyncPane();
    Tab syncTab = new Tab("同步项目", syncPane); syncTab.setClosable(false);
    
    tabs.getTabs().addAll(videoTab, audioTab, lightTab, syncTab);

    status = new Label("就绪");
    posSlider = new Slider(0, 1000, 0);
    posSlider.setPrefWidth(400);
    timeLabel = new Label("00:00 / 00:00");
    volSlider = new Slider(0, 100, 60);
    Label volText = new Label("音量");
    HBox bottom = new HBox(12, status, new Separator(), new Label("进度"), posSlider, timeLabel, new Separator(), volText, volSlider);
    bottom.getStyleClass().add("status-bar");
    bottom.setPadding(new Insets(6, 10, 6, 10));

    // 菜单栏
    MenuBar menuBar = new MenuBar();
    Menu mFile = new Menu("文件");
    MenuItem miOpen = new MenuItem("打开本地视频	Ctrl+O");
    MenuItem miOpenStream = new MenuItem("打开网络流	Ctrl+L");
    MenuItem miExit = new MenuItem("退出");
    mFile.getItems().addAll(miOpen, miOpenStream, new SeparatorMenuItem(), miExit);
    Menu mPlayback = new Menu("播放");
    MenuItem miPlayPause = new MenuItem("播放/暂停	Space");
    mPlayback.getItems().addAll(miPlayPause);
    Menu mView = new Menu("视图");
    MenuItem miMap = new MenuItem("屏幕映射	Ctrl+M");
    MenuItem miStopExt = new MenuItem("停止外部输出");
    mView.getItems().addAll(miMap, miStopExt);
    menuBar.getMenus().addAll(mFile, mPlayback, mView);

    BorderPane root = new BorderPane();
    root.setTop(new VBox(menuBar, tool));
    root.setCenter(tabs);
    root.setBottom(bottom);

    // 交互
    addToList.setOnAction(e -> chooseFiles());
    removeSel.setOnAction(e -> videoList.getItems().removeAll(videoList.getSelectionModel().getSelectedItems()));
    videoList.setOnMouseClicked(e -> { if (e.getClickCount()==2) playSelected(); });
    btnOpen.setOnAction(e -> chooseFiles());
    btnPlay.setOnAction(e -> togglePlay());
    btnMap.setOnAction(e -> showMappingDemo());
    btnStopExt.setOnAction(e -> multiScreen.stopAll());
    btnOpenStream.setOnAction(e -> openStream());
    // 菜单/快捷键
    miOpen.setOnAction(e -> chooseFiles());
    miOpenStream.setOnAction(e -> openStream());
    miExit.setOnAction(e -> stage.close());
    miPlayPause.setOnAction(e -> togglePlay());
    miMap.setOnAction(e -> showMappingDemo());
    miStopExt.setOnAction(e -> multiScreen.stopAll());
    // 音频交互
    addAudio.setOnAction(e -> chooseAudios());
    removeAudio.setOnAction(e -> audioList.getItems().removeAll(audioList.getSelectionModel().getSelectedItems()));
    audioList.setOnMouseClicked(e -> { if (e.getClickCount()==2) playSelectedAudio(); });
    playAudio.setOnAction(e -> togglePlayAudio());
    stopAudio.setOnAction(e -> stopAudioPlayback());
    audioVol.valueProperty().addListener((o, ov, nv) -> {
      if (useCustomPlayer && customAudioPlayer != null) {
        customAudioPlayer.setVolume(nv.doubleValue()/100.0);
      } else if (audioPlayer != null) {
        audioPlayer.setVolume(nv.doubleValue()/100.0);
      }
    });
    
    // 音频设备事件
    refreshAudioDevices.setOnAction(e -> refreshAudioDeviceList());
    testOutputDevice.setOnAction(e -> testAudioOutputDevice());
    testInputDevice.setOnAction(e -> testAudioInputDevice());
    switchPlayer.setOnAction(e -> switchAudioPlayer());
    audioOutputDevice.setOnAction(e -> setAudioOutputDevice());
    audioInputDevice.setOnAction(e -> setAudioInputDevice());
    // 灯光交互
    dmxConnectBtn.setOnAction(e -> toggleDmx());
    blackoutBtn.setOnAction(e -> { for (int i=0;i<512;i++) dmxData[i]=0; sendDmxFrame(); });

    Scene scene = new Scene(root, 1100, 700);
    // 应用 Atlantafx 主题
    Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
    // 不再加载任何自定义 CSS
    // 快捷键
    scene.getAccelerators().put(
        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.O, javafx.scene.input.KeyCombination.CONTROL_DOWN),
        this::chooseFiles);
    scene.getAccelerators().put(
        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.L, javafx.scene.input.KeyCombination.CONTROL_DOWN),
        this::openStream);
    scene.getAccelerators().put(
        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.M, javafx.scene.input.KeyCombination.CONTROL_DOWN),
        this::showMappingDemo);
    scene.getAccelerators().put(
        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.SPACE),
        this::togglePlay);
    stage.setScene(scene);
    stage.show();

    // 使用统一字体管理器
    Platform.runLater(() -> {
      FontManager.applyChineseFontToStage(stage);
      FontManager.enableAutoFontApplication();
    });

    // 初始化同步控制器
    initSyncController();
    
    // 初始化音频设备列表
    refreshAudioDeviceList();

    // 启动 Redis 订阅（默认本机 6379，可按需改为配置文件）
    subscriber = new DeviceControlSubscriber(this, "127.0.0.1", 6379, "device:control");
    subscriber.start();

    stage.setOnCloseRequest(e -> {
      try { 
        if (subscriber != null) subscriber.close(); 
        multiScreen.stopAll(); // 确保关闭所有外部窗口
      } catch (Exception ignored) {}
    });
  }

  private void chooseFiles() {
    FileChooser fc = new FileChooser();
    fc.setTitle("选择视频文件");
    fc.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("视频", "*.mp4", "*.mov", "*.mkv", "*.avi"),
        new FileChooser.ExtensionFilter("所有文件", "*.*")
    );
    List<File> files = fc.showOpenMultipleDialog(previewView.getScene().getWindow());
    if (files != null) videoList.getItems().addAll(files);
  }

  private void chooseAudios() {
    FileChooser fc = new FileChooser();
    fc.setTitle("选择音频文件");
    fc.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("音频", "*.mp3", "*.aac", "*.wav", "*.m4a"),
        new FileChooser.ExtensionFilter("所有文件", "*.*")
    );
    List<File> files = fc.showOpenMultipleDialog(previewView.getScene().getWindow());
    if (files != null) audioList.getItems().addAll(files);
  }

  private void playSelected() {
    File f = videoList.getSelectionModel().getSelectedItem();
    if (f == null) return;
    if (videoPlayer != null) videoPlayer.dispose();
    videoPlayer = new MediaPlayer(new Media(f.toURI().toString()));
    previewView.setMediaPlayer(videoPlayer);
    videoPlayer.setOnReady(() -> {
      status.setText("已加载: "+f.getName());
      setupPlayerBindings();
    });
    videoPlayer.setOnError(() -> status.setText("错误: "+videoPlayer.getError()));
    videoPlayer.setOnEndOfMedia(() -> status.setText("播放结束"));
    videoPlayer.play();
  }

  private void openStream() {
    String url = streamField.getText(); if (url == null || url.isBlank()) return;
    try {
      if (videoPlayer != null) videoPlayer.dispose();
      videoPlayer = new MediaPlayer(new Media(url));
      previewView.setMediaPlayer(videoPlayer);
      videoPlayer.setOnReady(() -> { status.setText("打开流成功"); setupPlayerBindings(); });
      videoPlayer.setOnError(() -> status.setText("流错误: "+videoPlayer.getError()));
      videoPlayer.play();
    } catch (Exception ex) {
      status.setText("打开流失败: "+ex.getMessage());
    }
  }

  public void togglePlay() {
    if (videoPlayer == null) return;
    switch (videoPlayer.getStatus()) {
      case PLAYING -> videoPlayer.pause();
      case PAUSED, READY, STOPPED -> videoPlayer.play();
    }
  }

  // 供订阅器调用的访问器与操作
  MediaPlayer videoPlayer() { return videoPlayer; }
  void playNextFromList() {
    int idx = videoList.getSelectionModel().getSelectedIndex();
    if (idx < 0 && !videoList.getItems().isEmpty()) { videoList.getSelectionModel().select(0); playSelected(); return; }
    if (idx + 1 < videoList.getItems().size()) { videoList.getSelectionModel().select(idx + 1); playSelected(); }
  }

  // 根据亮度/颜色/节奏应用灯光，并通过 TCP 发送到 DMX 设备
  void applyLight(Integer brightness, String colorHex, String rhythm) {
    // 简单映射：
    // - 亮度 -> 通道1
    // - 颜色 -> 通道2(R)/3(G)/4(B)
    // - 节奏 -> 通道5 预设值
    if (brightness != null) dmxData[0] = (byte) Math.max(0, Math.min(255, (int) Math.round(brightness * 2.55)));

    if (colorHex != null && colorHex.startsWith("#") && (colorHex.length() == 7 || colorHex.length() == 9)) {
      try {
        int r = Integer.parseInt(colorHex.substring(1, 3), 16);
        int g = Integer.parseInt(colorHex.substring(3, 5), 16);
        int b = Integer.parseInt(colorHex.substring(5, 7), 16);
        dmxData[1] = (byte) r; dmxData[2] = (byte) g; dmxData[3] = (byte) b;
      } catch (Exception ignored) {}
    }

    if (rhythm != null) {
      int v = switch (rhythm.toUpperCase()) {
        case "SOFT" -> 32; case "NORMAL" -> 96; case "STRONG" -> 160; case "AUTO" -> 224; default -> 0;
      };
      dmxData[4] = (byte) v;
    }

    sendDmxFrame();
  }

  private void playSelectedAudio() {
    File f = audioList.getSelectionModel().getSelectedItem();
    if (f == null) return;
    
    if (useCustomPlayer) {
      // 使用自定义播放器
      AudioDeviceManager.AudioDevice selectedDevice = audioOutputDevice.getSelectionModel().getSelectedItem();
      if (selectedDevice == null) {
        status.setText("请先选择输出设备");
        return;
      }
      
      if (customAudioPlayer != null) {
        customAudioPlayer.dispose();
      }
      
      customAudioPlayer = new CustomAudioPlayer(selectedDevice);
      if (customAudioPlayer.loadAudio(f)) {
        status.setText("音频已加载到设备: " + selectedDevice.name + " - " + f.getName());
        customAudioPlayer.setVolume(audioVol.getValue() / 100.0);
        customAudioPlayer.play();
      } else {
        status.setText("无法在指定设备播放音频: " + f.getName());
      }
    } else {
      // 使用JavaFX MediaPlayer
    if (audioPlayer != null) audioPlayer.dispose();
    audioPlayer = new MediaPlayer(new Media(f.toURI().toString()));
      audioPlayer.setOnReady(() -> {
        status.setText("音频: " + f.getName());
      });
      audioPlayer.setOnError(() -> {
        status.setText("音频错误: " + audioPlayer.getError());
      });
      audioPlayer.setVolume(audioVol.getValue() / 100.0);
    audioPlayer.play();
    }
  }

  private void togglePlayAudio() {
    if (useCustomPlayer && customAudioPlayer != null) {
      if (customAudioPlayer.isPlaying()) {
        customAudioPlayer.pause();

      } else {
        customAudioPlayer.play();

      }
    } else {
    if (audioPlayer == null) { playSelectedAudio(); return; }
    switch (audioPlayer.getStatus()) {
        case PLAYING -> {
          audioPlayer.pause();
  
        }
        case PAUSED, READY, STOPPED -> {
          audioPlayer.play();
  
        }
      }
    }
  }

  private void setupPlayerBindings() {
    if (videoPlayer == null) return;
    // 绑定音量
    volSlider.valueProperty().addListener((obs, o, n) -> {
      if (videoPlayer != null) videoPlayer.setVolume(n.doubleValue() / 100.0);
    });
    volSlider.setValue(videoPlayer.getVolume() * 100.0);

    // 更新进度与时间文本
    videoPlayer.currentTimeProperty().addListener((obs, o, n) -> updateTime());
    videoPlayer.totalDurationProperty().addListener((obs, o, n) -> updateTime());

    // 拖动进度
    posSlider.setOnMousePressed(e -> holdSeek = true);
    posSlider.setOnMouseReleased(e -> {
      if (videoPlayer.getTotalDuration() != null && !videoPlayer.getTotalDuration().isUnknown()) {
        double frac = posSlider.getValue() / posSlider.getMax();
        videoPlayer.seek(videoPlayer.getTotalDuration().multiply(frac));
      }
      holdSeek = false;
    });
  }

  private void updateTime() {
    if (videoPlayer == null) return;
    var cur = videoPlayer.getCurrentTime();
    var tot = videoPlayer.getTotalDuration();
    if (tot == null || tot.isUnknown()) {
      timeLabel.setText("--:-- / --:--");
      return;
    }
    int c = (int) Math.floor(cur.toSeconds());
    int t = (int) Math.floor(tot.toSeconds());
    timeLabel.setText(formatTime(c) + " / " + formatTime(t));
    if (!holdSeek) posSlider.setValue(t == 0 ? 0 : (c * posSlider.getMax() / t));
  }

  private static String formatTime(int sec) {
    int m = sec / 60; int s = sec % 60;
    return String.format("%02d:%02d", m, s);
  }

  // 映射演示：将预览以左半/右半分块到两个屏（如存在）
  private void showMappingDemo() {
    if (videoPlayer == null) return;
    // 打开映射对话框，获取每个屏幕的归一化矩形
    List<MappingDialog.Item> initial = buildSmartDefaultMappings();
    MappingDialog dlg = new MappingDialog((Stage) previewView.getScene().getWindow(), initial, videoPlayer);
    dlg.showAndWait();
    List<MappingDialog.Item> res = dlg.result();
    // 转换为 MultiScreenManager 映射并输出
    MultiScreenManager msm = new MultiScreenManager();
    java.util.List<MultiScreenManager.MappingItem> out = new java.util.ArrayList<>();
    for (MappingDialog.Item it : res) out.add(new MultiScreenManager.MappingItem(it.screenIndex, it.x, it.y, it.w, it.h));
    msm.showOnScreens(out, videoPlayer);
  }

  private List<MappingDialog.Item> buildSmartDefaultMappings() {
    List<MappingDialog.Item> list = new java.util.ArrayList<>();
    List<Screen> screens = Screen.getScreens();
    int n = screens.size(); if (n == 0) return list;
    // 简单策略：
    // 1 屏：填满
    // 2 屏：左右平分
    // 3 屏：左半（屏0），右上（屏1）/右下（屏2）
    // 4+ 屏：2x2 网格填前四块
    if (n == 1) {
      list.add(new MappingDialog.Item(0, 0,0,1,1));
    } else if (n == 2) {
      list.add(new MappingDialog.Item(0, 0,0,0.5,1));
      list.add(new MappingDialog.Item(1, 0.5,0,0.5,1));
    } else if (n == 3) {
      list.add(new MappingDialog.Item(0, 0,0,0.5,1));
      list.add(new MappingDialog.Item(1, 0.5,0,0.5,0.5));
      list.add(new MappingDialog.Item(2, 0.5,0.5,0.5,0.5));
    } else {
      list.add(new MappingDialog.Item(0, 0,0,0.5,0.5));
      list.add(new MappingDialog.Item(1, 0.5,0,0.5,0.5));
      list.add(new MappingDialog.Item(2, 0,0.5,0.5,0.5));
      list.add(new MappingDialog.Item(3, 0.5,0.5,0.5,0.5));
    }
    return list;
  }

  // --- TCP DMX ---
  private void toggleDmx() {
    if (dmxSocket != null && dmxSocket.isConnected() && !dmxSocket.isClosed()) {
      try { dmxSocket.close(); } catch (IOException ignored) {}
      dmxSocket = null;
      dmxConnectBtn.setText("连接");
      status.setText("DMX 已断开");
      return;
    }
    try {
      dmxSocket = new Socket();
      dmxSocket.connect(new InetSocketAddress(ipField.getText().trim(), portField.getValue()), 2000);
      dmxConnectBtn.setText("断开");
      status.setText("DMX 已连接");
      sendDmxFrame();
    } catch (Exception ex) {
      status.setText("DMX 连接失败: "+ex.getMessage());
      dmxSocket = null;
    }
  }

  private void sendDmxFrame() {
    if (dmxSocket == null || !dmxSocket.isConnected() || dmxSocket.isClosed()) return;
    try {
      OutputStream os = dmxSocket.getOutputStream();
      int uni = uniField.getValue();
      byte hi = (byte)((uni >> 8) & 0xFF);
      byte lo = (byte)(uni & 0xFF);
      os.write(new byte[]{hi, lo});
      os.write(dmxData, 0, 512);
      os.flush();
    } catch (IOException ex) {
      status.setText("DMX 发送失败: "+ex.getMessage());
    }
  }

  public static void main(String[] args) { launch(args); }

  // === 同步控制相关方法 ===
  
  private VBox createSyncPane() {
    // 创建稳定的时间轴编辑器
    stableTimelineEditor = new StableTimelineEditor();
    
    // 创建视频预览
    syncPreviewView = new MediaView();
    syncPreviewView.setFitWidth(320);
    syncPreviewView.setFitHeight(240);
    syncPreviewView.setPreserveRatio(true);
    
    VBox previewBox = new VBox(5);
    previewBox.getChildren().addAll(
      new Label("视频预览:"),
      syncPreviewView
    );
    previewBox.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-padding: 5;");
    
    // 项目列表（缩小）
    projectList = new ListView<>();
    projectList.setPrefHeight(120);
    projectList.setCellFactory(lv -> new ListCell<SyncController.SyncProject>() {
      @Override
      protected void updateItem(SyncController.SyncProject item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item.name);
      }
    });
    
    // 项目控制按钮
    createProjectBtn = new Button("新建项目");
    Button deleteProjectBtn = new Button("删除项目");
    deleteProjectBtn.setOnAction(e -> deleteSelectedProject());
    
    VBox projectControls = new VBox(5);
    projectControls.getChildren().addAll(
      new Label("项目管理:"),
      projectList,
      new HBox(5, createProjectBtn, deleteProjectBtn)
    );
    projectControls.setPrefWidth(200);
    
    createProjectBtn.setOnAction(e -> createNewProject());
    
    // 监听项目选择
    projectList.getSelectionModel().selectedItemProperty().addListener((obs, old, project) -> {
      if (project != null) {
        loadProjectToTimeline(project);
      }
    });
    
    // 设置稳定时间轴监听器
    stableTimelineEditor.setListener(new StableTimelineEditor.TimelineListener() {
      @Override
      public void onTimeChanged(double time) {
        if (syncController != null) {
          syncController.seekTo(time);
        }
      }
      
      @Override
      public void onClipAdded(StableTimelineEditor.MediaClip clip) {
        // 当添加媒体片段时，自动加载到播放器
        loadStableClipToPlayer(clip);
        status.setText("已添加片段: " + clip.fileName);
      }
      
      @Override
      public void onClipMoved(StableTimelineEditor.MediaClip clip) {
        status.setText("片段已移动: " + clip.fileName + " 到 " + formatTime((int)clip.startTime));
      }
      
      @Override
      public void onClipRemoved(StableTimelineEditor.MediaClip clip) {
        status.setText("已删除片段: " + clip.fileName);
      }
      
      @Override
      public void onClipSelected(List<StableTimelineEditor.MediaClip> clips) {
        if (!clips.isEmpty()) {
          StableTimelineEditor.MediaClip clip = clips.get(0);
          status.setText("已选择片段: " + clip.fileName);
          // 双击效果：加载到预览窗口
          loadStableClipToPlayer(clip);
        }
      }
      
      @Override
      public void onPlayStateChanged(boolean playing) {
        if (syncController != null) {
          if (playing) {
            syncController.play();
          } else {
            syncController.pause();
          }
        }
      }
    });
    
    // 顶部区域：项目管理 + 预览
    HBox topArea = new HBox(10);
    topArea.getChildren().addAll(projectControls, previewBox);
    
    // 主布局
    VBox syncPane = new VBox(10);
    syncPane.getChildren().addAll(
      topArea,
      new Separator(),
      new Label("稳定时间轴编辑器 - 拖拽媒体文件到轨道上 (支持快捷键: 空格键播放/暂停, Delete删除, 双击选择):"),
      stableTimelineEditor
    );
    syncPane.setPadding(new Insets(10));
    
    return syncPane;
  }
  
  private void initSyncController() {
    syncController = new SyncController();
    
    // 设置灯光控制器
    syncController.setLightController(this::applyLight);
    
    // 添加同步监听器
    syncController.addListener(new SyncController.SyncListener() {
      @Override
      public void onTimeUpdate(double currentSeconds, double totalSeconds) {
        Platform.runLater(() -> {
          // 更新稳定时间轴编辑器的播放头位置
          if (stableTimelineEditor != null) {
            stableTimelineEditor.setCurrentTime(currentSeconds);
          }
        });
      }
      
      @Override
      public void onStateChanged(SyncController.PlayState newState) {
        Platform.runLater(() -> {
          // 同步稳定时间轴编辑器的播放状态
          if (stableTimelineEditor != null) {
            stableTimelineEditor.setPlaying(newState == SyncController.PlayState.PLAYING);
          }
        });
      }
      
      @Override
      public void onProjectChanged(SyncController.SyncProject project) {
        Platform.runLater(() -> {
          if (project != null) {
            // 设置媒体播放器
            if (videoPlayer != null) {
              syncController.setVideoPlayer(videoPlayer);
            }
            if (useCustomPlayer && customAudioPlayer != null) {
              syncController.setCustomAudioPlayer(customAudioPlayer);
            } else if (audioPlayer != null) {
              syncController.setAudioPlayer(audioPlayer);
            }
          }
        });
      }
    });
  }
  
  private void createNewProject() {
    // 创建新的同步项目对话框
    javafx.scene.control.Dialog<SyncController.SyncProject> dialog = new javafx.scene.control.Dialog<>();
    dialog.setTitle("创建同步项目");
    dialog.setHeaderText("设置项目基本信息");
    
    // 应用中文字体
    Platform.runLater(() -> FontManager.applyChineseFontToDialog(dialog));
    
    // 创建表单
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));
    
    TextField nameField = new TextField();
    nameField.setPromptText("项目名称");
    
    TextField videoField = new TextField();
    videoField.setPromptText("视频文件路径");
    Button videoBtn = new Button("选择");
    videoBtn.setOnAction(e -> {
      FileChooser fc = new FileChooser();
      fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("视频文件", "*.mp4", "*.avi", "*.mov", "*.mkv"));
      File file = fc.showOpenDialog(dialog.getOwner());
      if (file != null) videoField.setText(file.getAbsolutePath());
    });
    
    TextField audioField = new TextField();
    audioField.setPromptText("音频文件路径");
    Button audioBtn = new Button("选择");
    audioBtn.setOnAction(e -> {
      FileChooser fc = new FileChooser();
      fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav", "*.m4a", "*.flac"));
      File file = fc.showOpenDialog(dialog.getOwner());
      if (file != null) audioField.setText(file.getAbsolutePath());
    });
    
    Spinner<Double> durationSpinner = new Spinner<>(1.0, 3600.0, 60.0, 1.0);
    durationSpinner.setEditable(true);
    
    grid.add(new Label("项目名称:"), 0, 0);
    grid.add(nameField, 1, 0);
    grid.add(new Label("视频文件:"), 0, 1);
    grid.add(videoField, 1, 1);
    grid.add(videoBtn, 2, 1);
    grid.add(new Label("音频文件:"), 0, 2);
    grid.add(audioField, 1, 2);
    grid.add(audioBtn, 2, 2);
    grid.add(new Label("总时长(秒):"), 0, 3);
    grid.add(durationSpinner, 1, 3);
    
    dialog.getDialogPane().setContent(grid);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    
    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == ButtonType.OK) {
        SyncController.SyncProject project = new SyncController.SyncProject(nameField.getText());
        project.videoFile = videoField.getText();
        project.audioFile = audioField.getText();
        project.totalDuration = durationSpinner.getValue();
        return project;
      }
      return null;
    });
    
    dialog.showAndWait().ifPresent(project -> {
      projectList.getItems().add(project);
      projectList.getSelectionModel().select(project);
    });
  }
  
  private void updateProjectDetails(TextArea detailsArea, SyncController.SyncProject project) {
    StringBuilder details = new StringBuilder();
    details.append("项目名称: ").append(project.name).append("\n");
    details.append("视频文件: ").append(project.videoFile != null ? project.videoFile : "未设置").append("\n");
    details.append("音频文件: ").append(project.audioFile != null ? project.audioFile : "未设置").append("\n");
    details.append("总时长: ").append(formatTime((int)project.totalDuration)).append("\n");
    details.append("灯光提示点: ").append(project.lightCues.size()).append("个\n");
    
    if (!project.lightCues.isEmpty()) {
      details.append("\n灯光提示点详情:\n");
      for (int i = 0; i < project.lightCues.size(); i++) {
        SyncController.LightCue cue = project.lightCues.get(i);
        details.append(String.format("  %d. %s - 亮度:%s 颜色:%s 节奏:%s\n", 
          i + 1, formatTime((int)cue.timeSeconds), 
          cue.brightness != null ? cue.brightness + "%" : "无",
          cue.color != null ? cue.color : "无",
          cue.rhythm != null ? cue.rhythm : "无"));
      }
    }
    
    detailsArea.setText(details.toString());
  }
  
  private void deleteSelectedProject() {
    SyncController.SyncProject selected = projectList.getSelectionModel().getSelectedItem();
    if (selected != null) {
      projectList.getItems().remove(selected);
      // 清空时间轴
      if (stableTimelineEditor != null) {
        stableTimelineEditor.clearAllClips();
      }
    }
  }
  
  private void loadProjectToTimeline(SyncController.SyncProject project) {
    if (stableTimelineEditor == null) return;
    
    // 清空当前时间轴
    stableTimelineEditor.clearAllClips();
    
    // 加载项目的媒体文件到时间轴
    if (project.videoFile != null && !project.videoFile.isEmpty()) {
      File videoFile = new File(project.videoFile);
      if (videoFile.exists()) {
        StableTimelineEditor.MediaClip videoClip = new StableTimelineEditor.MediaClip(
          "video_" + System.currentTimeMillis(),
          videoFile.getName(),
          project.videoFile,
          StableTimelineEditor.TrackType.VIDEO,
          0, // 从0开始
          project.totalDuration > 0 ? project.totalDuration : 60
        );
        stableTimelineEditor.addClip(videoClip);
      }
    }
    
    if (project.audioFile != null && !project.audioFile.isEmpty()) {
      File audioFile = new File(project.audioFile);
      if (audioFile.exists()) {
        StableTimelineEditor.MediaClip audioClip = new StableTimelineEditor.MediaClip(
          "audio_" + System.currentTimeMillis(),
          audioFile.getName(),
          project.audioFile,
          StableTimelineEditor.TrackType.AUDIO,
          0, // 从0开始
          project.totalDuration > 0 ? project.totalDuration : 60
        );
        stableTimelineEditor.addClip(audioClip);
      }
    }
    
    // 设置同步控制器项目
    syncController.setProject(project);
  }
  
  private void loadClipToPlayer(TimelineEditor.MediaClip clip) {
    if (clip.trackType == TimelineEditor.TrackType.VIDEO) {
      try {
        File videoFile = new File(clip.filePath);
        if (videoFile.exists()) {
          // 加载视频到预览窗口
          if (videoPlayer != null) {
            videoPlayer.dispose();
          }
          
          Media media = new Media(videoFile.toURI().toString());
          videoPlayer = new MediaPlayer(media);
          syncPreviewView.setMediaPlayer(videoPlayer);
          
          // 设置到同步控制器
          syncController.setVideoPlayer(videoPlayer);
          
          videoPlayer.setOnReady(() -> {
            status.setText("视频已加载: " + clip.fileName);
          });
          
          videoPlayer.setOnError(() -> {
            status.setText("视频加载错误: " + videoPlayer.getError());
          });
        }
      } catch (Exception e) {
        status.setText("加载视频失败: " + e.getMessage());
      }
    } else if (clip.trackType == TimelineEditor.TrackType.AUDIO) {
      try {
        File audioFile = new File(clip.filePath);
        if (audioFile.exists()) {
          // 加载音频
          if (audioPlayer != null) {
            audioPlayer.dispose();
          }
          
          Media media = new Media(audioFile.toURI().toString());
          audioPlayer = new MediaPlayer(media);
          
          // 设置到同步控制器
          syncController.setAudioPlayer(audioPlayer);
          
          audioPlayer.setOnReady(() -> {
            status.setText("音频已加载: " + clip.fileName);
          });
          
          audioPlayer.setOnError(() -> {
            status.setText("音频加载错误: " + audioPlayer.getError());
          });
        }
      } catch (Exception e) {
        status.setText("加载音频失败: " + e.getMessage());
      }
    }
  }
  
  private void loadWebClipToPlayer(WebTimelineEditor.ClipData clip) {
    if ("video".equals(clip.trackType)) {
      try {
        File videoFile = new File(clip.filePath);
        if (videoFile.exists()) {
          // 加载视频到预览窗口
          if (videoPlayer != null) {
            videoPlayer.dispose();
          }
          
          Media media = new Media(videoFile.toURI().toString());
          videoPlayer = new MediaPlayer(media);
          syncPreviewView.setMediaPlayer(videoPlayer);
          
          // 设置到同步控制器
          syncController.setVideoPlayer(videoPlayer);
          
          videoPlayer.setOnReady(() -> {
            status.setText("视频已加载: " + clip.name);
          });
          
          videoPlayer.setOnError(() -> {
            status.setText("视频加载错误: " + videoPlayer.getError());
          });
        }
      } catch (Exception e) {
        status.setText("加载视频失败: " + e.getMessage());
      }
    } else if ("audio".equals(clip.trackType)) {
      try {
        File audioFile = new File(clip.filePath);
        if (audioFile.exists()) {
          // 加载音频
          if (audioPlayer != null) {
            audioPlayer.dispose();
          }
          
          Media media = new Media(audioFile.toURI().toString());
          audioPlayer = new MediaPlayer(media);
          
          // 设置到同步控制器
          syncController.setAudioPlayer(audioPlayer);
          
          audioPlayer.setOnReady(() -> {
            status.setText("音频已加载: " + clip.name);
          });
          
          audioPlayer.setOnError(() -> {
            status.setText("音频加载错误: " + audioPlayer.getError());
          });
        }
      } catch (Exception e) {
        status.setText("加载音频失败: " + e.getMessage());
      }
    }
  }
  
  private void loadStableClipToPlayer(StableTimelineEditor.MediaClip clip) {
    if (clip.trackType == StableTimelineEditor.TrackType.VIDEO) {
      try {
        File videoFile = new File(clip.filePath);
        if (videoFile.exists()) {
          // 加载视频到预览窗口
          if (videoPlayer != null) {
            videoPlayer.dispose();
          }
          
          Media media = new Media(videoFile.toURI().toString());
          videoPlayer = new MediaPlayer(media);
          syncPreviewView.setMediaPlayer(videoPlayer);
          
          // 设置到同步控制器
          syncController.setVideoPlayer(videoPlayer);
          
          videoPlayer.setOnReady(() -> {
            status.setText("视频已加载: " + clip.fileName);
          });
          
          videoPlayer.setOnError(() -> {
            status.setText("视频加载错误: " + videoPlayer.getError());
          });
        }
      } catch (Exception e) {
        status.setText("加载视频失败: " + e.getMessage());
      }
    } else if (clip.trackType == StableTimelineEditor.TrackType.AUDIO) {
      try {
        File audioFile = new File(clip.filePath);
        if (audioFile.exists()) {
          // 加载音频
          if (audioPlayer != null) {
            audioPlayer.dispose();
          }
          
          Media media = new Media(audioFile.toURI().toString());
          audioPlayer = new MediaPlayer(media);
          
          // 设置到同步控制器
          syncController.setAudioPlayer(audioPlayer);
          
          audioPlayer.setOnReady(() -> {
            status.setText("音频已加载: " + clip.fileName);
          });
          
          audioPlayer.setOnError(() -> {
            status.setText("音频加载错误: " + audioPlayer.getError());
          });
        }
      } catch (Exception e) {
        status.setText("加载音频失败: " + e.getMessage());
      }
    }
  }

  // 音频设备管理方法
  private void refreshAudioDeviceList() {
    Platform.runLater(() -> {
      try {
        // 清空现有列表
        audioOutputDevice.getItems().clear();
        audioInputDevice.getItems().clear();
        
        // 使用AudioDeviceManager获取真实设备列表
        List<AudioDeviceManager.AudioDevice> outputDevices = AudioDeviceManager.getOutputDevices();
        List<AudioDeviceManager.AudioDevice> inputDevices = AudioDeviceManager.getInputDevices();
        
        // 添加到下拉框
        audioOutputDevice.getItems().addAll(outputDevices);
        audioInputDevice.getItems().addAll(inputDevices);
        
        // 设置默认选择（选择默认设备）
        AudioDeviceManager.AudioDevice defaultOutput = AudioDeviceManager.getCurrentDefaultOutputDevice();
        AudioDeviceManager.AudioDevice defaultInput = AudioDeviceManager.getCurrentDefaultInputDevice();
        
        if (defaultOutput != null) {
          audioOutputDevice.getSelectionModel().select(defaultOutput);
        } else if (!outputDevices.isEmpty()) {
          audioOutputDevice.getSelectionModel().select(0);
        }
        
        if (defaultInput != null) {
          audioInputDevice.getSelectionModel().select(defaultInput);
        } else if (!inputDevices.isEmpty()) {
          audioInputDevice.getSelectionModel().select(0);
        }
        
        status.setText("已刷新音频设备列表 - 输出:" + outputDevices.size() + " 输入:" + inputDevices.size());
        
      } catch (Exception e) {
        status.setText("获取音频设备失败: " + e.getMessage());
      }
    });
  }
  
  private void setAudioOutputDevice() {
    AudioDeviceManager.AudioDevice selectedDevice = audioOutputDevice.getSelectionModel().getSelectedItem();
    if (selectedDevice != null) {
      status.setText("已选择输出设备: " + selectedDevice.name + " (播放时将自动切换)");
    }
  }
  
  private void setAudioInputDevice() {
    AudioDeviceManager.AudioDevice selectedDevice = audioInputDevice.getSelectionModel().getSelectedItem();
    if (selectedDevice != null) {
      // 尝试设置为默认设备
      boolean success = AudioDeviceManager.setDefaultInputDevice(selectedDevice);
      if (success) {
        status.setText("已设置输入设备: " + selectedDevice.name);
      } else {
        status.setText("输入设备已选择: " + selectedDevice.name + " (可能需要管理员权限)");
      }
    }
  }
  
  private void testAudioOutputDevice() {
    AudioDeviceManager.AudioDevice selectedDevice = audioOutputDevice.getSelectionModel().getSelectedItem();
    if (selectedDevice != null) {
      status.setText("正在测试输出设备: " + selectedDevice.name + "...");
      
      // 在后台线程中测试设备
      new Thread(() -> {
        boolean success = AudioDeviceManager.testOutputDevice(selectedDevice);
        Platform.runLater(() -> {
          if (success) {
            status.setText("输出设备测试成功: " + selectedDevice.name);
          } else {
            status.setText("输出设备测试失败: " + selectedDevice.name);
          }
        });
      }).start();
    }
  }
  
  private void testAudioInputDevice() {
    AudioDeviceManager.AudioDevice selectedDevice = audioInputDevice.getSelectionModel().getSelectedItem();
    if (selectedDevice != null) {
      status.setText("输入设备信息: " + selectedDevice.name);
      
      // 显示设备详细信息
      String deviceInfo = AudioDeviceManager.getDeviceInfo(selectedDevice);
      System.out.println("音频输入设备信息:\n" + deviceInfo);
    }
  }
  
  // 获取当前选择的音频设备信息
  public AudioDeviceManager.AudioDevice getCurrentAudioOutputDevice() {
    return audioOutputDevice.getSelectionModel().getSelectedItem();
  }
  
  public AudioDeviceManager.AudioDevice getCurrentAudioInputDevice() {
    return audioInputDevice.getSelectionModel().getSelectedItem();
  }
  

  
  private void switchAudioPlayer() {
    // 停止当前播放
    stopAudioPlayback();
    
    // 切换播放器类型
    useCustomPlayer = !useCustomPlayer;
    
    // 更新状态显示
    String playerType = useCustomPlayer ? "自定义播放器 (支持指定设备)" : "JavaFX播放器 (仅默认设备)";
    status.setText("已切换到: " + playerType);
    

  }
  
  private void stopAudioPlayback() {
    if (useCustomPlayer && customAudioPlayer != null) {
      customAudioPlayer.stop();
    } else if (audioPlayer != null) {
      audioPlayer.stop();
    }
  }
}
