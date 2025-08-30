package com.boxai.controller;

import com.boxai.common.web.ApiResponse;
import com.boxai.service.AiVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI视频生成回调控制器
 * 处理来自外部AI服务的回调通知
 */
@RestController
@RequestMapping("/api/ai-video/callback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI视频回调", description = "处理AI视频生成服务的回调通知")
public class AiVideoCallbackController {
    
    private final AiVideoService aiVideoService;
    
    /**
     * Runway ML回调接口
     */
    @PostMapping("/runway/{streamId}")
    @Operation(summary = "Runway ML回调", description = "接收Runway ML的生成状态回调")
    public ApiResponse<Void> runwayCallback(
            @Parameter(description = "流ID") @PathVariable String streamId,
            @Parameter(description = "回调数据") @RequestBody Map<String, Object> callbackData) {
        
        log.info("收到Runway ML回调: streamId={}, data={}", streamId, callbackData);
        
        try {
            // 解析回调数据
            String status = (String) callbackData.get("status");
            Integer progress = callbackData.containsKey("progress") ? 
                    ((Number) callbackData.get("progress")).intValue() : null;
            String message = (String) callbackData.get("message");
            String videoUrl = (String) callbackData.get("video_url");
            
            // 处理回调
            handleAiCallback(streamId, "runway", status, progress, message, videoUrl);
            
            return ApiResponse.success();
            
        } catch (Exception e) {
            log.error("处理Runway ML回调失败: streamId={}", streamId, e);
            return ApiResponse.error(500, "处理回调失败: " + e.getMessage());
        }
    }
    
    /**
     * Stability AI回调接口
     */
    @PostMapping("/stability/{streamId}")
    @Operation(summary = "Stability AI回调", description = "接收Stability AI的生成状态回调")
    public ApiResponse<Void> stabilityCallback(
            @Parameter(description = "流ID") @PathVariable String streamId,
            @Parameter(description = "回调数据") @RequestBody Map<String, Object> callbackData) {
        
        log.info("收到Stability AI回调: streamId={}, data={}", streamId, callbackData);
        
        try {
            // 解析回调数据
            String status = (String) callbackData.get("status");
            String videoUrl = (String) callbackData.get("video");
            String errorMessage = (String) callbackData.get("error");
            
            // 处理回调
            handleAiCallback(streamId, "stability", status, null, errorMessage, videoUrl);
            
            return ApiResponse.success();
            
        } catch (Exception e) {
            log.error("处理Stability AI回调失败: streamId={}", streamId, e);
            return ApiResponse.error(500, "处理回调失败: " + e.getMessage());
        }
    }
    
    /**
     * 通用AI回调处理
     */
    @PostMapping("/{streamId}")
    @Operation(summary = "通用AI回调", description = "接收任意AI服务的回调通知")
    public ApiResponse<Void> genericCallback(
            @Parameter(description = "流ID") @PathVariable String streamId,
            @Parameter(description = "回调数据") @RequestBody Map<String, Object> callbackData) {
        
        log.info("收到通用AI回调: streamId={}, data={}", streamId, callbackData);
        
        try {
            String provider = (String) callbackData.getOrDefault("provider", "unknown");
            String status = (String) callbackData.get("status");
            Integer progress = callbackData.containsKey("progress") ? 
                    ((Number) callbackData.get("progress")).intValue() : null;
            String message = (String) callbackData.get("message");
            String videoUrl = (String) callbackData.get("video_url");
            
            handleAiCallback(streamId, provider, status, progress, message, videoUrl);
            
            return ApiResponse.success();
            
        } catch (Exception e) {
            log.error("处理通用AI回调失败: streamId={}", streamId, e);
            return ApiResponse.error(500, "处理回调失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理AI回调的通用逻辑
     */
    private void handleAiCallback(String streamId, String provider, String status, 
                                 Integer progress, String message, String videoUrl) {
        
        log.info("处理AI回调: streamId={}, provider={}, status={}, progress={}", 
                streamId, provider, status, progress);
        
        // 根据不同的状态进行处理
        switch (status != null ? status.toLowerCase() : "") {
            case "completed", "success", "complete-success" -> {
                if (videoUrl != null) {
                    log.info("AI视频生成完成: streamId={}, videoUrl={}", streamId, videoUrl);
                    // 这里可以触发流媒体转换和推送
                    // aiVideoService.handleVideoCompleted(streamId, videoUrl);
                }
            }
            case "failed", "error", "complete-error" -> {
                log.warn("AI视频生成失败: streamId={}, message={}", streamId, message);
                // aiVideoService.handleVideoFailed(streamId, message);
            }
            case "processing", "in-progress", "generating" -> {
                log.debug("AI视频生成中: streamId={}, progress={}%", streamId, progress);
                // aiVideoService.handleVideoProgress(streamId, progress, message);
            }
            default -> {
                log.debug("AI视频状态更新: streamId={}, status={}", streamId, status);
            }
        }
    }
}
