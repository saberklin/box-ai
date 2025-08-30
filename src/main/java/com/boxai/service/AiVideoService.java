package com.boxai.service;

import com.boxai.domain.dto.AiVideoStreamResponse;
import com.boxai.domain.dto.request.AiVideoGenerateRequest;
import com.boxai.domain.entity.AiVideoSession;

import java.util.List;

/**
 * AI视频生成服务接口
 */
public interface AiVideoService {
    
    /**
     * 开始AI视频生成
     * 
     * @param request 生成请求
     * @return 视频流响应
     */
    AiVideoStreamResponse startVideoGeneration(AiVideoGenerateRequest request);
    
    /**
     * 获取视频生成状态
     * 
     * @param streamId 流ID
     * @return 视频流响应
     */
    AiVideoStreamResponse getGenerationStatus(String streamId);
    
    /**
     * 停止视频生成
     * 
     * @param streamId 流ID
     */
    void stopVideoGeneration(String streamId);
    
    /**
     * 获取包间的活跃视频流
     * 
     * @param roomId 包间ID
     * @return 活跃的视频流列表
     */
    List<AiVideoStreamResponse> getActiveStreams(Long roomId);
    
    /**
     * 推送视频流到桌面端
     * 
     * @param streamId 流ID
     * @param roomId 包间ID
     */
    void pushStreamToDesktop(String streamId, Long roomId);
    
    /**
     * 更新视频生成进度
     * 
     * @param streamId 流ID
     * @param progress 进度百分比
     * @param status 状态
     */
    void updateGenerationProgress(String streamId, Integer progress, String status);
    
    /**
     * 获取视频生成历史
     * 
     * @param roomId 包间ID
     * @param limit 限制数量
     * @return 历史记录
     */
    List<AiVideoSession> getGenerationHistory(Long roomId, Integer limit);
}
