package app;

import javafx.concurrent.Worker;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 基于WebView的专业时间轴编辑器
 * 使用HTML5 Canvas实现高性能的时间轴编辑功能
 */
public class WebTimelineEditor extends VBox {
    
    private WebView webView;
    private WebEngine webEngine;
    private JSObject timelineJS;
    
    // 事件监听器
    private Consumer<Double> onTimeChanged;
    private Consumer<Boolean> onPlayStateChanged;
    private Consumer<ClipData> onClipAdded;
    private Consumer<ClipData> onClipMoved;
    private Consumer<ClipData> onClipRemoved;
    private Consumer<KeyframeData> onKeyframeAdded;
    private Consumer<ClipData> onClipDoubleClicked;
    
    // 数据类
    public static class ClipData {
        public String id;
        public String name;
        public String trackType; // video, audio, light
        public double startTime;
        public double duration;
        public String filePath;
        
        public ClipData(String id, String name, String trackType, double startTime, double duration, String filePath) {
            this.id = id;
            this.name = name;
            this.trackType = trackType;
            this.startTime = startTime;
            this.duration = duration;
            this.filePath = filePath;
        }
    }
    
    public static class KeyframeData {
        public String id;
        public double time;
        public int track;
        public int brightness;
        public String color;
        public String rhythm;
        
        public KeyframeData(String id, double time, int track, int brightness, String color, String rhythm) {
            this.id = id;
            this.time = time;
            this.track = track;
            this.brightness = brightness;
            this.color = color;
            this.rhythm = rhythm;
        }
    }
    
    // Java与JavaScript的桥接类
    public class JavaScriptBridge {
        
        public void call(String event, String dataJson) {
            try {
                switch (event) {
                    case "timeChanged":
                        if (onTimeChanged != null) {
                            // 解析JSON获取时间
                            double time = parseTimeFromJson(dataJson);
                            javafx.application.Platform.runLater(() -> onTimeChanged.accept(time));
                        }
                        break;
                        
                    case "playStateChanged":
                        if (onPlayStateChanged != null) {
                            boolean playing = parsePlayingFromJson(dataJson);
                            javafx.application.Platform.runLater(() -> onPlayStateChanged.accept(playing));
                        }
                        break;
                        
                    case "clipAdded":
                        if (onClipAdded != null) {
                            ClipData clip = parseClipFromJson(dataJson);
                            javafx.application.Platform.runLater(() -> onClipAdded.accept(clip));
                        }
                        break;
                        
                    case "clipMoved":
                        if (onClipMoved != null) {
                            ClipData clip = parseClipFromJson(dataJson);
                            javafx.application.Platform.runLater(() -> onClipMoved.accept(clip));
                        }
                        break;
                        
                    case "clipRemoved":
                        if (onClipRemoved != null) {
                            ClipData clip = parseClipFromJson(dataJson);
                            javafx.application.Platform.runLater(() -> onClipRemoved.accept(clip));
                        }
                        break;
                        
                    case "keyframeAdded":
                        if (onKeyframeAdded != null) {
                            KeyframeData keyframe = parseKeyframeFromJson(dataJson);
                            javafx.application.Platform.runLater(() -> onKeyframeAdded.accept(keyframe));
                        }
                        break;
                        
                    case "clipDoubleClicked":
                        if (onClipDoubleClicked != null) {
                            ClipData clip = parseClipFromJson(dataJson);
                            javafx.application.Platform.runLater(() -> onClipDoubleClicked.accept(clip));
                        }
                        break;
                }
            } catch (Exception e) {
                System.err.println("JavaScript桥接错误: " + e.getMessage());
            }
        }
        
        private double parseTimeFromJson(String json) {
            // 简单的JSON解析，实际项目中应使用JSON库
            if (json.contains("\"time\":")) {
                String timeStr = json.substring(json.indexOf("\"time\":") + 7);
                timeStr = timeStr.substring(0, timeStr.indexOf("}")).trim();
                return Double.parseDouble(timeStr);
            }
            return 0;
        }
        
        private boolean parsePlayingFromJson(String json) {
            return json.contains("\"playing\":true");
        }
        
        private ClipData parseClipFromJson(String json) {
            // 简化的JSON解析，实际应使用Jackson或Gson
            return new ClipData("temp", "temp", "video", 0, 60, "");
        }
        
        private KeyframeData parseKeyframeFromJson(String json) {
            return new KeyframeData("temp", 0, 2, 100, "#ffffff", "normal");
        }
    }
    
    public WebTimelineEditor() {
        initWebView();
        loadTimelineHTML();
    }
    
    private void initWebView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        
        // 启用JavaScript
        webEngine.setJavaScriptEnabled(true);
        
        // 设置用户代理，确保现代浏览器特性可用
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // 监听页面加载完成
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                setupJavaScriptBridge();
            }
        });
        
        getChildren().add(webView);
    }
    
    private void loadTimelineHTML() {
        try {
            // 加载专业时间轴HTML文件
            URL htmlUrl = getClass().getResource("/professional-timeline.html");
            if (htmlUrl != null) {
                webEngine.load(htmlUrl.toExternalForm());
            } else {
                System.err.println("无法找到professional-timeline.html文件");
            }
        } catch (Exception e) {
            System.err.println("加载时间轴HTML失败: " + e.getMessage());
        }
    }
    
    private void setupJavaScriptBridge() {
        try {
            // 获取JavaScript的window对象
            JSObject window = (JSObject) webEngine.executeScript("window");
            
            // 注入Java桥接对象
            window.setMember("javaConnector", new JavaScriptBridge());
            
            // 获取时间轴JavaScript对象的引用
            timelineJS = (JSObject) webEngine.executeScript("window.timeline");
            
            System.out.println("JavaScript桥接设置完成");
            
        } catch (Exception e) {
            System.err.println("设置JavaScript桥接失败: " + e.getMessage());
        }
    }
    
    // 公共API方法
    
    /**
     * 设置当前播放时间
     */
    public void setCurrentTime(double time) {
        if (timelineJS != null) {
            try {
                timelineJS.call("setTime", time);
            } catch (Exception e) {
                System.err.println("设置时间失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 设置播放状态
     */
    public void setPlaying(boolean playing) {
        if (timelineJS != null) {
            try {
                timelineJS.call("setPlaying", playing);
            } catch (Exception e) {
                System.err.println("设置播放状态失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 添加媒体片段
     */
    public void addClip(ClipData clip) {
        if (timelineJS != null) {
            try {
                // 构建JavaScript对象
                String jsClip = String.format(
                    "{id:'%s', name:'%s', trackType:'%s', startTime:%f, duration:%f, filePath:'%s'}",
                    clip.id, clip.name, clip.trackType, clip.startTime, clip.duration, clip.filePath
                );
                webEngine.executeScript("timeline.addClipFromJava(" + jsClip + ")");
            } catch (Exception e) {
                System.err.println("添加片段失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 清空所有片段
     */
    public void clearClips() {
        if (timelineJS != null) {
            try {
                webEngine.executeScript("timeline.clearAllClips()");
            } catch (Exception e) {
                System.err.println("清空片段失败: " + e.getMessage());
            }
        }
    }
    
    // 事件监听器设置方法
    
    public void setOnTimeChanged(Consumer<Double> listener) {
        this.onTimeChanged = listener;
    }
    
    public void setOnPlayStateChanged(Consumer<Boolean> listener) {
        this.onPlayStateChanged = listener;
    }
    
    public void setOnClipAdded(Consumer<ClipData> listener) {
        this.onClipAdded = listener;
    }
    
    public void setOnClipMoved(Consumer<ClipData> listener) {
        this.onClipMoved = listener;
    }
    
    public void setOnClipRemoved(Consumer<ClipData> listener) {
        this.onClipRemoved = listener;
    }
    
    public void setOnKeyframeAdded(Consumer<KeyframeData> listener) {
        this.onKeyframeAdded = listener;
    }
    
    public void setOnClipDoubleClicked(Consumer<ClipData> listener) {
        this.onClipDoubleClicked = listener;
    }
    
    /**
     * 获取WebView组件（用于调试）
     */
    public WebView getWebView() {
        return webView;
    }
    
    /**
     * 执行自定义JavaScript代码
     */
    public Object executeScript(String script) {
        try {
            return webEngine.executeScript(script);
        } catch (Exception e) {
            System.err.println("执行JavaScript失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 刷新时间轴
     */
    public void refresh() {
        if (timelineJS != null) {
            try {
                webEngine.executeScript("timeline.render()");
            } catch (Exception e) {
                System.err.println("刷新时间轴失败: " + e.getMessage());
            }
        }
    }
}
