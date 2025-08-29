package app;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 声光电同步控制器
 * 统一管理视频、音频、灯光的同步播放
 */
public class SyncController {
    
    // 同步项目
    public static class SyncProject {
        public String name;
        public String videoFile;
        public String audioFile;
        public List<LightCue> lightCues;
        public double totalDuration; // 总时长（秒）
        
        public SyncProject(String name) {
            this.name = name;
            this.lightCues = new ArrayList<>();
        }
    }
    
    // 灯光提示点
    public static class LightCue {
        public double timeSeconds;    // 触发时间（秒）
        public Integer brightness;    // 亮度 0-100
        public String color;         // 颜色 #RRGGBB
        public String rhythm;        // 节奏 SOFT/NORMAL/STRONG/AUTO
        public double duration;      // 持续时间（秒），0表示瞬间切换
        
        public LightCue(double timeSeconds) {
            this.timeSeconds = timeSeconds;
        }
    }
    
    // 播放状态
    public enum PlayState {
        STOPPED, PLAYING, PAUSED
    }
    
    private SyncProject currentProject;
    private PlayState playState = PlayState.STOPPED;
    private double currentTimeSeconds = 0;
    private AnimationTimer syncTimer;
    
    // 媒体播放器引用
    private MediaPlayer videoPlayer;
    private MediaPlayer audioPlayer;
    private CustomAudioPlayer customAudioPlayer;
    private boolean useCustomAudioPlayer = false;
    
    // 灯光控制回调
    private LightController lightController;
    
    // 同步监听器
    private final List<SyncListener> listeners = new CopyOnWriteArrayList<>();
    
    public interface SyncListener {
        default void onTimeUpdate(double currentSeconds, double totalSeconds) {}
        default void onStateChanged(PlayState newState) {}
        default void onProjectChanged(SyncProject project) {}
    }
    
    public interface LightController {
        void applyLight(Integer brightness, String color, String rhythm);
    }
    
    public SyncController() {
        setupSyncTimer();
    }
    
    /**
     * 设置当前同步项目
     */
    public void setProject(SyncProject project) {
        stop(); // 停止当前播放
        this.currentProject = project;
        this.currentTimeSeconds = 0;
        
        // 加载项目媒体文件
        loadProjectMedia(project);
        
        // 通知监听器
        listeners.forEach(l -> l.onProjectChanged(project));
    }
    
    /**
     * 设置媒体播放器
     */
    public void setVideoPlayer(MediaPlayer player) {
        this.videoPlayer = player;
    }
    
    public void setAudioPlayer(MediaPlayer player) {
        this.audioPlayer = player;
        this.useCustomAudioPlayer = false;
    }
    
    public void setCustomAudioPlayer(CustomAudioPlayer player) {
        this.customAudioPlayer = player;
        this.useCustomAudioPlayer = true;
    }
    
    /**
     * 设置灯光控制器
     */
    public void setLightController(LightController controller) {
        this.lightController = controller;
    }
    
    /**
     * 添加同步监听器
     */
    public void addListener(SyncListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(SyncListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 开始同步播放
     */
    public void play() {
        if (currentProject == null) return;
        
        if (playState == PlayState.PAUSED) {
            // 从暂停位置继续
            resumeAllMedia();
        } else {
            // 从头开始播放
            seekTo(0);
            startAllMedia();
        }
        
        playState = PlayState.PLAYING;
        syncTimer.start();
        
        listeners.forEach(l -> l.onStateChanged(playState));
    }
    
    /**
     * 暂停同步播放
     */
    public void pause() {
        if (playState != PlayState.PLAYING) return;
        
        pauseAllMedia();
        playState = PlayState.PAUSED;
        syncTimer.stop();
        
        listeners.forEach(l -> l.onStateChanged(playState));
    }
    
    /**
     * 停止同步播放
     */
    public void stop() {
        stopAllMedia();
        playState = PlayState.STOPPED;
        currentTimeSeconds = 0;
        syncTimer.stop();
        
        listeners.forEach(l -> l.onStateChanged(playState));
    }
    
    /**
     * 跳转到指定时间
     */
    public void seekTo(double seconds) {
        currentTimeSeconds = Math.max(0, Math.min(seconds, getTotalDuration()));
        
        // 同步所有媒体到指定时间
        if (videoPlayer != null) {
            videoPlayer.seek(Duration.seconds(currentTimeSeconds));
        }
        
        if (useCustomAudioPlayer && customAudioPlayer != null) {
            customAudioPlayer.setCurrentTime(currentTimeSeconds);
        } else if (audioPlayer != null) {
            audioPlayer.seek(Duration.seconds(currentTimeSeconds));
        }
        
        // 应用当前时间点的灯光状态
        applyLightAtTime(currentTimeSeconds);
        
        listeners.forEach(l -> l.onTimeUpdate(currentTimeSeconds, getTotalDuration()));
    }
    
    /**
     * 获取总时长
     */
    public double getTotalDuration() {
        if (currentProject != null) {
            return currentProject.totalDuration;
        }
        return 0;
    }
    
    /**
     * 获取当前播放时间
     */
    public double getCurrentTime() {
        return currentTimeSeconds;
    }
    
    /**
     * 获取播放状态
     */
    public PlayState getPlayState() {
        return playState;
    }
    
    /**
     * 获取当前项目
     */
    public SyncProject getCurrentProject() {
        return currentProject;
    }
    
    // === 私有方法 ===
    
    private void setupSyncTimer() {
        syncTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                // 计算时间增量
                double deltaSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                
                // 更新当前时间
                currentTimeSeconds += deltaSeconds;
                
                // 检查是否播放结束
                if (currentTimeSeconds >= getTotalDuration()) {
                    Platform.runLater(() -> stop());
                    return;
                }
                
                // 处理灯光提示点
                processLightCues();
                
                // 通知时间更新
                Platform.runLater(() -> 
                    listeners.forEach(l -> l.onTimeUpdate(currentTimeSeconds, getTotalDuration()))
                );
            }
        };
    }
    
    private void startAllMedia() {
        if (videoPlayer != null) {
            videoPlayer.play();
        }
        
        if (useCustomAudioPlayer && customAudioPlayer != null) {
            customAudioPlayer.play();
        } else if (audioPlayer != null) {
            audioPlayer.play();
        }
    }
    
    private void pauseAllMedia() {
        if (videoPlayer != null) {
            videoPlayer.pause();
        }
        
        if (useCustomAudioPlayer && customAudioPlayer != null) {
            customAudioPlayer.pause();
        } else if (audioPlayer != null) {
            audioPlayer.pause();
        }
    }
    
    private void resumeAllMedia() {
        if (videoPlayer != null) {
            videoPlayer.play();
        }
        
        if (useCustomAudioPlayer && customAudioPlayer != null) {
            customAudioPlayer.play();
        } else if (audioPlayer != null) {
            audioPlayer.play();
        }
    }
    
    private void stopAllMedia() {
        if (videoPlayer != null) {
            videoPlayer.stop();
        }
        
        if (useCustomAudioPlayer && customAudioPlayer != null) {
            customAudioPlayer.stop();
        } else if (audioPlayer != null) {
            audioPlayer.stop();
        }
    }
    
    private void processLightCues() {
        if (currentProject == null || lightController == null) return;
        
        // 查找当前时间点需要触发的灯光提示
        for (LightCue cue : currentProject.lightCues) {
            // 检查是否在触发时间窗口内（允许50ms误差）
            if (Math.abs(currentTimeSeconds - cue.timeSeconds) <= 0.05) {
                Platform.runLater(() -> 
                    lightController.applyLight(cue.brightness, cue.color, cue.rhythm)
                );
            }
        }
    }
    
    private void applyLightAtTime(double timeSeconds) {
        if (currentProject == null || lightController == null) return;
        
        // 找到指定时间点最近的灯光状态
        LightCue lastCue = null;
        for (LightCue cue : currentProject.lightCues) {
            if (cue.timeSeconds <= timeSeconds) {
                lastCue = cue;
            } else {
                break;
            }
        }
        
        if (lastCue != null) {
            lightController.applyLight(lastCue.brightness, lastCue.color, lastCue.rhythm);
        }
    }
    
    /**
     * 加载项目媒体文件
     */
    private void loadProjectMedia(SyncProject project) {
        if (project == null) return;
        
        try {
            // 加载视频文件
            if (project.videoFile != null && !project.videoFile.isEmpty()) {
                java.io.File videoFile = new java.io.File(project.videoFile);
                if (videoFile.exists()) {
                    javafx.scene.media.Media videoMedia = new javafx.scene.media.Media(videoFile.toURI().toString());
                    if (videoPlayer != null) {
                        videoPlayer.dispose();
                    }
                    videoPlayer = new MediaPlayer(videoMedia);
                    
                    // 设置视频播放器事件
                    videoPlayer.setOnReady(() -> {
                        // 如果项目没有设置总时长，使用视频时长
                        if (project.totalDuration <= 0 && videoPlayer.getTotalDuration() != null) {
                            project.totalDuration = videoPlayer.getTotalDuration().toSeconds();
                        }
                    });
                }
            }
            
            // 加载音频文件
            if (project.audioFile != null && !project.audioFile.isEmpty()) {
                java.io.File audioFile = new java.io.File(project.audioFile);
                if (audioFile.exists()) {
                    javafx.scene.media.Media audioMedia = new javafx.scene.media.Media(audioFile.toURI().toString());
                    if (audioPlayer != null) {
                        audioPlayer.dispose();
                    }
                    audioPlayer = new MediaPlayer(audioMedia);
                    
                    // 设置音频播放器事件
                    audioPlayer.setOnReady(() -> {
                        // 如果项目没有设置总时长，使用音频时长
                        if (project.totalDuration <= 0 && audioPlayer.getTotalDuration() != null) {
                            project.totalDuration = audioPlayer.getTotalDuration().toSeconds();
                        }
                    });
                }
            }
            
        } catch (Exception e) {
            System.err.println("加载项目媒体文件失败: " + e.getMessage());
        }
    }
}
