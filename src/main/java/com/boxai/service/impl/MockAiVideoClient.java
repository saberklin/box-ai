package com.boxai.service.impl;

import com.boxai.domain.dto.request.AiVideoGenerateRequest;
import com.boxai.domain.entity.AiVideoSession;
import com.boxai.service.AiVideoGenerationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟AI视频生成客户端
 * 用于开发和测试环境，模拟真实的AI视频生成流程
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiVideoClient implements AiVideoGenerationClient {
    
    @Value("${app.streaming.rtmp-url:rtmp://localhost:1935/live}")
    private String rtmpBaseUrl;
    
    @Value("${app.streaming.hls-url:http://localhost:8080/hls}")
    private String hlsBaseUrl;
    
    @Value("${app.streaming.webrtc-url:webrtc://localhost:8080/stream}")
    private String webrtcBaseUrl;
    
    // 模拟任务状态存储
    private final ConcurrentHashMap<String, MockTaskState> taskStates = new ConcurrentHashMap<>();
    
    @Override
    public Mono<String> startGeneration(AiVideoGenerateRequest request, AiVideoSession session) {
        String taskId = "mock_task_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("开始模拟AI视频生成: taskId={}, streamId={}, prompt={}", 
                taskId, session.getStreamId(), request.getPrompt());
        
        // 创建模拟任务状态
        MockTaskState taskState = new MockTaskState(
                taskId,
                session.getStreamId(),
                request.getPrompt(),
                request.getDuration(),
                "PENDING"
        );
        taskStates.put(taskId, taskState);
        
        // 异步开始模拟生成过程
        startMockGeneration(taskState);
        
        return Mono.just(taskId);
    }
    
    @Override
    public Flux<AiGenerationProgress> getProgressStream(String taskId) {
        log.info("开始模拟进度流: taskId={}", taskId);
        
        MockTaskState taskState = taskStates.get(taskId);
        if (taskState == null) {
            return Flux.error(new RuntimeException("任务不存在: " + taskId));
        }
        
        // 模拟进度更新流
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> {
                    MockTaskState state = taskStates.get(taskId);
                    if (state == null) {
                        return new AiGenerationProgress(taskId, "ERROR", 0, "任务不存在", System.currentTimeMillis());
                    }
                    
                    return new AiGenerationProgress(
                            taskId,
                            state.status,
                            state.progress.get(),
                            generateProgressMessage(state.status, state.progress.get()),
                            System.currentTimeMillis()
                    );
                })
                .takeUntil(progress -> "COMPLETED".equals(progress.status()) || 
                                     "FAILED".equals(progress.status()) || 
                                     "STOPPED".equals(progress.status()))
                .doOnNext(progress -> log.debug("模拟进度更新: taskId={}, progress={}%, status={}", 
                        taskId, progress.progress(), progress.status()))
                .doOnComplete(() -> log.info("模拟进度流完成: taskId={}", taskId));
    }
    
    @Override
    public Mono<AiGenerationResult> getGenerationResult(String taskId) {
        log.info("获取模拟生成结果: taskId={}", taskId);
        
        MockTaskState taskState = taskStates.get(taskId);
        if (taskState == null) {
            return Mono.just(new AiGenerationResult(taskId, "ERROR", null, null, null, null, null, null, "任务不存在"));
        }
        
        String streamId = taskState.streamId;
        String videoUrl = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4"; // 示例视频
        String streamUrl = rtmpBaseUrl + "/" + streamId;
        String hlsUrl = hlsBaseUrl + "/" + streamId + ".m3u8";
        String webrtcUrl = webrtcBaseUrl + "/" + streamId;
        
        return Mono.just(new AiGenerationResult(
                taskId,
                taskState.status,
                videoUrl,
                streamUrl,
                hlsUrl,
                webrtcUrl,
                taskState.duration,
                "1920x1080",
                null
        ));
    }
    
    @Override
    public Mono<Boolean> stopGeneration(String taskId) {
        log.info("停止模拟生成任务: taskId={}", taskId);
        
        MockTaskState taskState = taskStates.get(taskId);
        if (taskState != null) {
            taskState.status = "STOPPED";
            log.info("模拟任务已停止: taskId={}", taskId);
        }
        
        return Mono.just(true);
    }
    
    /**
     * 开始模拟生成过程
     */
    private void startMockGeneration(MockTaskState taskState) {
        Mono.fromRunnable(() -> {
            try {
                String taskId = taskState.taskId;
                log.info("开始模拟生成过程: taskId={}", taskId);
                
                // 模拟生成阶段
                taskState.status = "GENERATING";
                
                // 模拟进度更新
                for (int progress = 0; progress <= 100; progress += 5) {
                    if ("STOPPED".equals(taskState.status)) {
                        log.info("模拟生成被停止: taskId={}", taskId);
                        return;
                    }
                    
                    taskState.progress.set(progress);
                    
                    // 模拟不同阶段
                    if (progress == 20) {
                        log.info("模拟: 开始分析提示词 - taskId={}", taskId);
                    } else if (progress == 40) {
                        log.info("模拟: 生成关键帧 - taskId={}", taskId);
                    } else if (progress == 60) {
                        log.info("模拟: 插值生成中间帧 - taskId={}", taskId);
                    } else if (progress == 80) {
                        log.info("模拟: 后处理和优化 - taskId={}", taskId);
                    }
                    
                    Thread.sleep(500); // 模拟处理时间
                }
                
                // 模拟完成
                taskState.status = "COMPLETED";
                taskState.progress.set(100);
                log.info("模拟生成完成: taskId={}", taskId);
                
                // 模拟启动流媒体服务
                startMockStreaming(taskState);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                taskState.status = "STOPPED";
                log.info("模拟生成被中断: taskId={}", taskState.taskId);
            } catch (Exception e) {
                taskState.status = "FAILED";
                log.error("模拟生成失败: taskId={}", taskState.taskId, e);
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .subscribe();
    }
    
    /**
     * 模拟启动流媒体服务
     */
    private void startMockStreaming(MockTaskState taskState) {
        log.info("模拟启动流媒体服务: streamId={}", taskState.streamId);
        
        // 这里可以启动一个实际的测试视频流
        // 例如使用FFmpeg播放测试视频到RTMP服务器
        Mono.fromRunnable(() -> {
            try {
                // 模拟流媒体启动时间
                Thread.sleep(2000);
                log.info("模拟流媒体服务启动完成: streamId={}", taskState.streamId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .subscribe();
    }
    
    /**
     * 生成进度消息
     */
    private String generateProgressMessage(String status, int progress) {
        return switch (status) {
            case "PENDING" -> "任务排队中...";
            case "GENERATING" -> switch (progress / 20) {
                case 0 -> "初始化AI模型...";
                case 1 -> "分析提示词...";
                case 2 -> "生成关键帧...";
                case 3 -> "插值中间帧...";
                case 4 -> "后处理优化...";
                default -> "生成中... " + progress + "%";
            };
            case "COMPLETED" -> "生成完成，启动流媒体...";
            case "FAILED" -> "生成失败";
            case "STOPPED" -> "已停止";
            default -> status;
        };
    }
    
    /**
     * 模拟任务状态
     */
    private static class MockTaskState {
        final String taskId;
        final String streamId;
        final String prompt;
        final Integer duration;
        volatile String status;
        final AtomicInteger progress = new AtomicInteger(0);
        
        MockTaskState(String taskId, String streamId, String prompt, Integer duration, String status) {
            this.taskId = taskId;
            this.streamId = streamId;
            this.prompt = prompt;
            this.duration = duration;
            this.status = status;
        }
    }
}
