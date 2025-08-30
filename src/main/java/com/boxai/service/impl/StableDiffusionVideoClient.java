package com.boxai.service.impl;

import com.boxai.domain.dto.request.AiVideoGenerateRequest;
import com.boxai.domain.entity.AiVideoSession;
import com.boxai.service.AiVideoGenerationClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Stable Video Diffusion AI视频生成客户端实现
 * 对接Stability AI的Stable Video Diffusion API
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "stability", matchIfMissing = false)
public class StableDiffusionVideoClient implements AiVideoGenerationClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.ai.stability.api-key:}")
    private String apiKey;
    
    @Value("${app.ai.stability.base-url:https://api.stability.ai}")
    private String baseUrl;
    
    @Value("${app.streaming.rtmp-url:rtmp://localhost:1935/live}")
    private String rtmpBaseUrl;
    
    @Value("${app.streaming.hls-url:http://localhost:8080/hls}")
    private String hlsBaseUrl;
    
    @Override
    public Mono<String> startGeneration(AiVideoGenerateRequest request, AiVideoSession session) {
        log.info("开始Stable Video Diffusion生成: streamId={}, prompt={}", session.getStreamId(), request.getPrompt());
        
        // 构建Stability AI请求
        Map<String, Object> requestBody = Map.of(
            "text_prompts", Map.of("text", request.getPrompt(), "weight", 1.0),
            "cfg_scale", 7.0,
            "motion_bucket_id", 127,
            "noise_aug_strength", 0.02,
            "seed", System.currentTimeMillis() % 1000000,
            "steps", 25,
            "video_length", Math.min(request.getDuration(), 25) // SVD限制25帧
        );
        
        return webClient.post()
                .uri(baseUrl + "/v2beta/image-to-video")
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        String taskId = jsonNode.get("id").asText();
                        log.info("Stability AI任务创建成功: taskId={}, streamId={}", taskId, session.getStreamId());
                        return taskId;
                    } catch (Exception e) {
                        log.error("解析Stability AI响应失败: {}", response, e);
                        throw new RuntimeException("创建AI视频生成任务失败: " + e.getMessage());
                    }
                })
                .doOnError(error -> log.error("Stability AI调用失败: streamId={}", session.getStreamId(), error));
    }
    
    @Override
    public Flux<AiGenerationProgress> getProgressStream(String taskId) {
        log.info("开始轮询Stability AI生成进度: taskId={}", taskId);
        
        // Stability AI不支持SSE，使用轮询方式
        return Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> getTaskStatus(taskId))
                .takeUntil(progress -> "COMPLETED".equals(progress.status()) || 
                                     "FAILED".equals(progress.status()) || 
                                     "STOPPED".equals(progress.status()))
                .doOnNext(progress -> log.debug("轮询进度更新: taskId={}, progress={}%, status={}", 
                        taskId, progress.progress(), progress.status()))
                .doOnError(error -> log.error("进度轮询失败: taskId={}", taskId, error));
    }
    
    private Mono<AiGenerationProgress> getTaskStatus(String taskId) {
        return webClient.get()
                .uri(baseUrl + "/v2beta/image-to-video/result/{taskId}", taskId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        String status = jsonNode.get("status").asText();
                        
                        // Stability AI的进度估算
                        Integer progress = switch (status.toLowerCase()) {
                            case "in-progress" -> 50;
                            case "complete-success" -> 100;
                            case "complete-error" -> 0;
                            default -> 10;
                        };
                        
                        return new AiGenerationProgress(
                                taskId,
                                mapStabilityStatus(status),
                                progress,
                                status,
                                System.currentTimeMillis()
                        );
                    } catch (Exception e) {
                        log.error("解析任务状态失败: taskId={}, response={}", taskId, response, e);
                        return new AiGenerationProgress(taskId, "ERROR", 0, "解析状态失败", System.currentTimeMillis());
                    }
                })
                .onErrorReturn(new AiGenerationProgress(taskId, "ERROR", 0, "获取状态失败", System.currentTimeMillis()));
    }
    
    @Override
    public Mono<AiGenerationResult> getGenerationResult(String taskId) {
        log.info("获取Stability AI生成结果: taskId={}", taskId);
        
        return webClient.get()
                .uri(baseUrl + "/v2beta/image-to-video/result/{taskId}", taskId)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "video/*")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        String status = jsonNode.get("status").asText();
                        String videoUrl = null;
                        
                        if ("complete-success".equals(status.toLowerCase()) && jsonNode.has("video")) {
                            videoUrl = jsonNode.get("video").asText();
                        }
                        
                        String streamId = extractStreamIdFromTaskId(taskId);
                        String streamUrl = null;
                        String hlsUrl = null;
                        String webrtcUrl = null;
                        
                        if (videoUrl != null) {
                            // 启动流媒体转换
                            startStreamingConversion(videoUrl, streamId);
                            
                            streamUrl = rtmpBaseUrl + "/" + streamId;
                            hlsUrl = hlsBaseUrl + "/" + streamId + ".m3u8";
                            webrtcUrl = "webrtc://localhost:8080/stream/" + streamId;
                        }
                        
                        return new AiGenerationResult(
                                taskId,
                                mapStabilityStatus(status),
                                videoUrl,
                                streamUrl,
                                hlsUrl,
                                webrtcUrl,
                                25, // SVD固定25帧
                                "1024x576", // SVD默认分辨率
                                jsonNode.has("error") ? jsonNode.get("error").asText() : null
                        );
                    } catch (Exception e) {
                        log.error("解析生成结果失败: taskId={}, response={}", taskId, response, e);
                        return new AiGenerationResult(taskId, "ERROR", null, null, null, null, null, null, e.getMessage());
                    }
                });
    }
    
    @Override
    public Mono<Boolean> stopGeneration(String taskId) {
        log.info("Stability AI不支持取消任务，标记为停止: taskId={}", taskId);
        // Stability AI API不支持取消任务，这里只是标记
        return Mono.just(true);
    }
    
    /**
     * 映射Stability AI状态
     */
    private String mapStabilityStatus(String stabilityStatus) {
        return switch (stabilityStatus.toLowerCase()) {
            case "in-progress" -> "GENERATING";
            case "complete-success" -> "COMPLETED";
            case "complete-error" -> "FAILED";
            default -> "PENDING";
        };
    }
    
    private String extractStreamIdFromTaskId(String taskId) {
        return "ai_stream_" + taskId.substring(0, Math.min(taskId.length(), 10));
    }
    
    private void startStreamingConversion(String videoUrl, String streamId) {
        log.info("启动Stability AI视频流转换: videoUrl={}, streamId={}", videoUrl, streamId);
        
        Mono.fromRunnable(() -> {
            try {
                // 模拟FFmpeg转换过程
                Thread.sleep(1000);
                log.info("Stability AI流媒体转换完成: streamId={}", streamId);
            } catch (Exception e) {
                log.error("流媒体转换失败: streamId={}", streamId, e);
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .subscribe();
    }
}
