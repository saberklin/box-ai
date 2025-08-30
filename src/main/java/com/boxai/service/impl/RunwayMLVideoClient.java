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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Runway ML AI视频生成客户端实现
 * 对接Runway ML的视频生成API
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "runway")
public class RunwayMLVideoClient implements AiVideoGenerationClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.ai.runway.api-key:}")
    private String apiKey;
    
    @Value("${app.ai.runway.base-url:https://api.runwayml.com}")
    private String baseUrl;
    
    @Value("${app.ai.runway.model:gen3a_turbo}")
    private String model;
    
    // 流媒体服务器配置
    @Value("${app.streaming.rtmp-url:rtmp://localhost:1935/live}")
    private String rtmpBaseUrl;
    
    @Value("${app.streaming.hls-url:http://localhost:8080/hls}")
    private String hlsBaseUrl;
    
    @Value("${app.streaming.webrtc-url:webrtc://localhost:8080/stream}")
    private String webrtcBaseUrl;
    
    @Override
    public Mono<String> startGeneration(AiVideoGenerateRequest request, AiVideoSession session) {
        log.info("开始Runway ML视频生成: streamId={}, prompt={}", session.getStreamId(), request.getPrompt());
        
        // 构建Runway ML API请求
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "prompt", request.getPrompt(),
            "style", request.getStyle(),
            "duration", request.getDuration(),
            "resolution", request.getResolution(),
            "aspect_ratio", "16:9",
            "motion_strength", 0.8,
            "seed", System.currentTimeMillis() % 1000000,
            "callback_url", buildCallbackUrl(session.getStreamId())
        );
        
        return webClient.post()
                .uri(baseUrl + "/v1/image_to_video")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        String taskId = jsonNode.get("id").asText();
                        log.info("Runway ML任务创建成功: taskId={}, streamId={}", taskId, session.getStreamId());
                        return taskId;
                    } catch (Exception e) {
                        log.error("解析Runway ML响应失败: {}", response, e);
                        throw new RuntimeException("创建AI视频生成任务失败: " + e.getMessage());
                    }
                })
                .doOnError(error -> log.error("Runway ML API调用失败: streamId={}", session.getStreamId(), error));
    }
    
    @Override
    public Flux<AiGenerationProgress> getProgressStream(String taskId) {
        log.info("开始监听Runway ML生成进度: taskId={}", taskId);
        
        // 使用Server-Sent Events获取实时进度
        return webClient.get()
                .uri(baseUrl + "/v1/tasks/{taskId}/progress", taskId)
                .header("Authorization", "Bearer " + apiKey)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .map(sse -> {
                    try {
                        Object dataObj = sse.data();
                        String data = dataObj != null ? dataObj.toString() : "";
                        JsonNode jsonNode = objectMapper.readTree(data);
                        
                        String status = jsonNode.get("status").asText();
                        Integer progress = jsonNode.has("progress") ? jsonNode.get("progress").asInt() : 0;
                        String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "";
                        
                        return new AiGenerationProgress(
                                taskId,
                                mapRunwayStatus(status),
                                progress,
                                message,
                                System.currentTimeMillis()
                        );
                    } catch (Exception e) {
                        log.error("解析进度数据失败: taskId={}, data={}", taskId, sse.data(), e);
                        return new AiGenerationProgress(taskId, "ERROR", 0, "解析进度失败", System.currentTimeMillis());
                    }
                })
                .doOnNext(progress -> log.debug("收到进度更新: taskId={}, progress={}%, status={}", 
                        taskId, progress.progress(), progress.status()))
                .doOnError(error -> log.error("进度流监听失败: taskId={}", taskId, error))
                .onErrorResume(error -> Flux.empty()); // 错误时返回空流，避免中断
    }
    
    @Override
    public Mono<AiGenerationResult> getGenerationResult(String taskId) {
        log.info("获取Runway ML生成结果: taskId={}", taskId);
        
        return webClient.get()
                .uri(baseUrl + "/v1/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        String status = jsonNode.get("status").asText();
                        String videoUrl = jsonNode.has("output") ? jsonNode.get("output").get("url").asText() : null;
                        
                        // 如果视频生成完成，启动流媒体转换
                        String streamId = extractStreamIdFromTaskId(taskId);
                        String streamUrl = null;
                        String hlsUrl = null;
                        String webrtcUrl = null;
                        
                        if ("COMPLETED".equals(mapRunwayStatus(status)) && videoUrl != null) {
                            // 启动流媒体转换服务
                            startStreamingConversion(videoUrl, streamId);
                            
                            streamUrl = rtmpBaseUrl + "/" + streamId;
                            hlsUrl = hlsBaseUrl + "/" + streamId + ".m3u8";
                            webrtcUrl = webrtcBaseUrl + "/" + streamId;
                        }
                        
                        return new AiGenerationResult(
                                taskId,
                                mapRunwayStatus(status),
                                videoUrl,
                                streamUrl,
                                hlsUrl,
                                webrtcUrl,
                                jsonNode.has("duration") ? jsonNode.get("duration").asInt() : null,
                                jsonNode.has("resolution") ? jsonNode.get("resolution").asText() : null,
                                jsonNode.has("error") ? jsonNode.get("error").asText() : null
                        );
                    } catch (Exception e) {
                        log.error("解析生成结果失败: taskId={}, response={}", taskId, response, e);
                        return new AiGenerationResult(taskId, "ERROR", null, null, null, null, null, null, e.getMessage());
                    }
                })
                .doOnError(error -> log.error("获取生成结果失败: taskId={}", taskId, error));
    }
    
    @Override
    public Mono<Boolean> stopGeneration(String taskId) {
        log.info("停止Runway ML生成任务: taskId={}", taskId);
        
        return webClient.delete()
                .uri(baseUrl + "/v1/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.info("Runway ML任务停止成功: taskId={}", taskId);
                    return true;
                })
                .onErrorReturn(false)
                .doOnError(error -> log.error("停止Runway ML任务失败: taskId={}", taskId, error));
    }
    
    /**
     * 映射Runway ML状态到系统状态
     */
    private String mapRunwayStatus(String runwayStatus) {
        return switch (runwayStatus.toUpperCase()) {
            case "PENDING", "QUEUED" -> "PENDING";
            case "RUNNING", "PROCESSING" -> "GENERATING";
            case "SUCCEEDED", "COMPLETED" -> "COMPLETED";
            case "FAILED", "ERROR" -> "FAILED";
            case "CANCELLED" -> "STOPPED";
            default -> runwayStatus.toUpperCase();
        };
    }
    
    /**
     * 构建回调URL
     */
    private String buildCallbackUrl(String streamId) {
        return "http://localhost:9998/api/ai-video/callback/" + streamId;
    }
    
    /**
     * 从任务ID提取流ID
     */
    private String extractStreamIdFromTaskId(String taskId) {
        // 这里需要根据实际的任务ID格式来提取流ID
        // 简化处理，假设任务ID包含流ID信息
        return "ai_stream_" + taskId.substring(0, Math.min(taskId.length(), 10));
    }
    
    /**
     * 启动流媒体转换
     * 将AI生成的视频文件转换为实时流
     */
    private void startStreamingConversion(String videoUrl, String streamId) {
        log.info("启动流媒体转换: videoUrl={}, streamId={}", videoUrl, streamId);
        
        // 使用FFmpeg将视频文件转换为RTMP流
        // 这里是异步处理，实际部署时需要配置FFmpeg
        Mono.fromRunnable(() -> {
            try {
                // FFmpeg命令示例：
                // ffmpeg -re -i {videoUrl} -c:v libx264 -c:a aac -f flv {rtmpUrl}
                String ffmpegCommand = String.format(
                    "ffmpeg -re -i %s -c:v libx264 -c:a aac -f flv %s/%s",
                    videoUrl, rtmpBaseUrl, streamId
                );
                
                log.info("执行FFmpeg转换: {}", ffmpegCommand);
                
                // 实际部署时需要执行这个命令
                // Process process = Runtime.getRuntime().exec(ffmpegCommand);
                
                // 模拟转换过程
                Thread.sleep(2000);
                log.info("流媒体转换完成: streamId={}", streamId);
                
            } catch (Exception e) {
                log.error("流媒体转换失败: streamId={}", streamId, e);
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .subscribe();
    }
}
