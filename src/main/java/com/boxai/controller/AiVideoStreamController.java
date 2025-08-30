package com.boxai.controller;

import com.boxai.service.AiVideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * AI视频流控制器
 * 提供Server-Sent Events实时推送AI视频生成进度
 */
@RestController
@RequestMapping("/api/ai-video/stream")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI视频流", description = "AI视频生成实时进度流")
public class AiVideoStreamController {
    
    private final AiVideoService aiVideoService;
    private final ObjectMapper objectMapper;
    
    /**
     * 获取AI视频生成进度流
     */
    @GetMapping(value = "/progress/{streamId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取生成进度流", description = "通过Server-Sent Events实时获取AI视频生成进度")
    public Flux<ServerSentEvent<String>> getProgressStream(
            @Parameter(description = "流ID") @PathVariable String streamId) {
        
        log.info("开始推送AI视频生成进度流: streamId={}", streamId);
        
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> {
                    try {
                        // 获取当前会话状态
                        var session = aiVideoService.getSessionByStreamId(streamId);
                        if (session == null) {
                            return ServerSentEvent.<String>builder()
                                    .id(String.valueOf(sequence))
                                    .event("error")
                                    .data("{\"error\":\"会话不存在\"}")
                                    .build();
                        }
                        
                        // 构建进度数据
                        Map<String, Object> progressData = Map.of(
                                "streamId", streamId,
                                "status", session.getStatus(),
                                "progress", session.getProgress() != null ? session.getProgress() : 0,
                                "message", getStatusMessage(session.getStatus(), session.getProgress()),
                                "timestamp", System.currentTimeMillis(),
                                "streamUrl", session.getStreamUrl() != null ? session.getStreamUrl() : "",
                                "hlsUrl", session.getHlsUrl() != null ? session.getHlsUrl() : "",
                                "webrtcUrl", session.getWebrtcUrl() != null ? session.getWebrtcUrl() : ""
                        );
                        
                        String jsonData = objectMapper.writeValueAsString(progressData);
                        
                        return ServerSentEvent.<String>builder()
                                .id(String.valueOf(sequence))
                                .event("progress")
                                .data(jsonData)
                                .build();
                        
                    } catch (Exception e) {
                        log.error("构建进度数据失败: streamId={}", streamId, e);
                        return ServerSentEvent.<String>builder()
                                .id(String.valueOf(sequence))
                                .event("error")
                                .data("{\"error\":\"构建进度数据失败\"}")
                                .build();
                    }
                })
                .takeUntil(sse -> {
                    // 当状态为完成、失败或停止时结束流
                    try {
                        if (sse.data() != null && sse.data().contains("\"status\"")) {
                            return sse.data().contains("\"COMPLETED\"") || 
                                   sse.data().contains("\"FAILED\"") || 
                                   sse.data().contains("\"STOPPED\"");
                        }
                    } catch (Exception e) {
                        log.error("检查结束条件失败: streamId={}", streamId, e);
                    }
                    return false;
                })
                .doOnSubscribe(subscription -> log.info("客户端订阅进度流: streamId={}", streamId))
                .doOnCancel(() -> log.info("客户端取消进度流订阅: streamId={}", streamId))
                .doOnComplete(() -> log.info("进度流推送完成: streamId={}", streamId))
                .doOnError(error -> log.error("进度流推送出错: streamId={}", streamId, error));
    }
    
    /**
     * 获取所有活跃会话的状态流
     */
    @GetMapping(value = "/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取所有会话状态流", description = "实时推送所有活跃AI视频生成会话的状态")
    public Flux<ServerSentEvent<String>> getAllSessionsStatus() {
        
        log.info("开始推送所有会话状态流");
        
        return Flux.interval(Duration.ofSeconds(2))
                .map(sequence -> {
                    try {
                        // 获取所有活跃会话
                        var activeSessions = aiVideoService.getActiveSessions(50);
                        
                        Map<String, Object> statusData = Map.of(
                                "timestamp", System.currentTimeMillis(),
                                "activeSessions", activeSessions.size(),
                                "sessions", activeSessions.stream()
                                        .map(session -> Map.of(
                                                "streamId", session.getStreamId(),
                                                "roomId", session.getRoomId(),
                                                "status", session.getStatus(),
                                                "progress", session.getProgress() != null ? session.getProgress() : 0,
                                                "startTime", session.getStartTime().toString()
                                        ))
                                        .toList()
                        );
                        
                        String jsonData = objectMapper.writeValueAsString(statusData);
                        
                        return ServerSentEvent.<String>builder()
                                .id(String.valueOf(sequence))
                                .event("status")
                                .data(jsonData)
                                .build();
                        
                    } catch (Exception e) {
                        log.error("构建状态数据失败", e);
                        return ServerSentEvent.<String>builder()
                                .id(String.valueOf(sequence))
                                .event("error")
                                .data("{\"error\":\"构建状态数据失败\"}")
                                .build();
                    }
                })
                .doOnSubscribe(subscription -> log.info("客户端订阅状态流"))
                .doOnCancel(() -> log.info("客户端取消状态流订阅"))
                .doOnError(error -> log.error("状态流推送出错", error));
    }
    
    /**
     * 获取状态消息
     */
    private String getStatusMessage(String status, Integer progress) {
        if (status == null) return "未知状态";
        
        return switch (status.toUpperCase()) {
            case "PENDING" -> "任务排队中...";
            case "GENERATING" -> {
                if (progress != null) {
                    if (progress < 20) yield "初始化AI模型...";
                    else if (progress < 40) yield "分析提示词...";
                    else if (progress < 60) yield "生成关键帧...";
                    else if (progress < 80) yield "插值中间帧...";
                    else if (progress < 100) yield "后处理优化...";
                    else yield "准备流媒体...";
                } else {
                    yield "生成中...";
                }
            }
            case "STREAMING" -> "正在流式传输...";
            case "COMPLETED" -> "生成完成";
            case "FAILED" -> "生成失败";
            case "STOPPED" -> "已停止";
            default -> status;
        };
    }
}
