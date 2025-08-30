package com.boxai.service;

import com.boxai.domain.dto.request.AiVideoGenerateRequest;
import com.boxai.domain.entity.AiVideoSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI视频生成客户端接口
 * 用于对接外部AI视频生成服务（如Runway ML、Stable Video Diffusion等）
 */
public interface AiVideoGenerationClient {
    
    /**
     * 开始AI视频生成任务
     * 
     * @param request 生成请求
     * @param session 会话信息
     * @return 生成任务ID
     */
    Mono<String> startGeneration(AiVideoGenerateRequest request, AiVideoSession session);
    
    /**
     * 获取生成进度流（Server-Sent Events）
     * 
     * @param taskId 任务ID
     * @return 进度更新流
     */
    Flux<AiGenerationProgress> getProgressStream(String taskId);
    
    /**
     * 获取生成的视频流URL
     * 
     * @param taskId 任务ID
     * @return 视频流URL
     */
    Mono<AiGenerationResult> getGenerationResult(String taskId);
    
    /**
     * 停止生成任务
     * 
     * @param taskId 任务ID
     * @return 停止结果
     */
    Mono<Boolean> stopGeneration(String taskId);
    
    /**
     * AI生成进度信息
     */
    record AiGenerationProgress(
            String taskId,
            String status,
            Integer progress,
            String message,
            Long timestamp
    ) {}
    
    /**
     * AI生成结果
     */
    record AiGenerationResult(
            String taskId,
            String status,
            String videoUrl,
            String streamUrl,
            String hlsUrl,
            String webrtcUrl,
            Integer duration,
            String resolution,
            String errorMessage
    ) {}
}
