package com.boxai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.boxai.domain.dto.AiVideoStreamResponse;
import com.boxai.domain.dto.device.AiVideoStreamCommand;
import com.boxai.domain.dto.request.AiVideoGenerateRequest;
import com.boxai.domain.entity.AiVideoSession;
import com.boxai.domain.mapper.AiVideoSessionMapper;
import com.boxai.service.AiVideoService;
import com.boxai.service.DeviceControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI视频生成服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiVideoServiceImpl implements AiVideoService {
    
    private final AiVideoSessionMapper aiVideoSessionMapper;
    private final DeviceControlService deviceControlService;
    
    // AI视频生成服务的基础URL（实际部署时需要配置）
    private static final String AI_VIDEO_SERVICE_URL = "http://localhost:8888";
    private static final String STREAM_SERVER_URL = "rtmp://localhost:1935/live";
    private static final String HLS_SERVER_URL = "http://localhost:8080/hls";
    private static final String WEBRTC_SERVER_URL = "webrtc://localhost:8080/stream";
    
    @Override
    @Transactional
    public AiVideoStreamResponse startVideoGeneration(AiVideoGenerateRequest request) {
        // 生成唯一的流ID
        String streamId = "ai_stream_" + UUID.randomUUID().toString().replace("-", "");
        
        // 创建视频生成会话
        AiVideoSession session = new AiVideoSession();
        BeanUtils.copyProperties(request, session);
        session.setStreamId(streamId);
        session.setStatus("PENDING");
        session.setProgress(0);
        session.setStartTime(LocalDateTime.now());
        
        // 生成流媒体URL
        session.setStreamUrl(STREAM_SERVER_URL + "/" + streamId);
        session.setHlsUrl(HLS_SERVER_URL + "/" + streamId + ".m3u8");
        session.setWebrtcUrl(WEBRTC_SERVER_URL + "/" + streamId);
        session.setBitrate(calculateBitrate(request.getResolution(), request.getFrameRate()));
        
        aiVideoSessionMapper.insert(session);
        
        // 异步启动AI视频生成
        CompletableFuture.runAsync(() -> {
            try {
                startAiVideoGeneration(session);
            } catch (Exception e) {
                log.error("AI视频生成失败: streamId={}", streamId, e);
                updateSessionStatus(streamId, "FAILED", 0, e.getMessage());
            }
        });
        
        // 返回响应
        AiVideoStreamResponse response = new AiVideoStreamResponse();
        BeanUtils.copyProperties(session, response);
        response.setEstimatedTimeRemaining(request.getDuration());
        response.setEstimatedEndTime(LocalDateTime.now().plusSeconds(request.getDuration()));
        
        log.info("开始AI视频生成: streamId={}, roomId={}, type={}", streamId, request.getRoomId(), request.getVideoType());
        
        return response;
    }
    
    @Override
    public AiVideoStreamResponse getGenerationStatus(String streamId) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            throw new RuntimeException("视频流不存在: " + streamId);
        }
        
        AiVideoStreamResponse response = new AiVideoStreamResponse();
        BeanUtils.copyProperties(session, response);
        
        // 计算预计剩余时间
        if ("GENERATING".equals(session.getStatus()) && session.getProgress() > 0) {
            int remainingProgress = 100 - session.getProgress();
            long elapsedTime = java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds();
            int estimatedRemaining = (int) (elapsedTime * remainingProgress / session.getProgress());
            response.setEstimatedTimeRemaining(estimatedRemaining);
        }
        
        return response;
    }
    
    @Override
    @Transactional
    public void stopVideoGeneration(String streamId) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            throw new RuntimeException("视频流不存在: " + streamId);
        }
        
        // 停止AI生成进程
        try {
            stopAiVideoGeneration(streamId);
        } catch (Exception e) {
            log.error("停止AI视频生成失败: streamId={}", streamId, e);
        }
        
        // 更新会话状态
        session.setStatus("STOPPED");
        session.setEndTime(LocalDateTime.now());
        aiVideoSessionMapper.updateById(session);
        
        // 通知桌面端停止播放
        AiVideoStreamCommand command = new AiVideoStreamCommand();
        command.setRoomId(session.getRoomId());
        command.setStreamId(streamId);
        command.setAction("STOP_STREAM");
        command.setTimestamp(System.currentTimeMillis());
        
        deviceControlService.publishAiVideo(command);
        
        log.info("停止AI视频生成: streamId={}", streamId);
    }
    
    @Override
    public List<AiVideoStreamResponse> getActiveStreams(Long roomId) {
        QueryWrapper<AiVideoSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId)
                   .in("status", "GENERATING", "STREAMING")
                   .orderByDesc("start_time");
        
        List<AiVideoSession> sessions = aiVideoSessionMapper.selectList(queryWrapper);
        
        return sessions.stream()
                .map(session -> {
                    AiVideoStreamResponse response = new AiVideoStreamResponse();
                    BeanUtils.copyProperties(session, response);
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void pushStreamToDesktop(String streamId, Long roomId) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            throw new RuntimeException("视频流不存在: " + streamId);
        }
        
        // 发送流推送命令到桌面端
        AiVideoStreamCommand command = new AiVideoStreamCommand();
        command.setRoomId(roomId);
        command.setStreamId(streamId);
        command.setAction("START_STREAM");
        command.setStreamUrl(session.getStreamUrl());
        command.setHlsUrl(session.getHlsUrl());
        command.setWebrtcUrl(session.getWebrtcUrl());
        command.setResolution(session.getResolution());
        command.setFrameRate(session.getFrameRate());
        command.setTimestamp(System.currentTimeMillis());
        
        deviceControlService.publishAiVideo(command);
        
        log.info("推送AI视频流到桌面端: streamId={}, roomId={}", streamId, roomId);
    }
    
    @Override
    @Transactional
    public void updateGenerationProgress(String streamId, Integer progress, String status) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session == null) {
            log.warn("更新进度时视频流不存在: streamId={}", streamId);
            return;
        }
        
        session.setProgress(progress);
        session.setStatus(status);
        
        if ("STREAMING".equals(status)) {
            // 视频开始流式传输，推送到桌面端
            pushStreamToDesktop(streamId, session.getRoomId());
        } else if ("COMPLETED".equals(status)) {
            session.setEndTime(LocalDateTime.now());
        }
        
        aiVideoSessionMapper.updateById(session);
        
        log.debug("更新AI视频生成进度: streamId={}, progress={}%, status={}", streamId, progress, status);
    }
    
    @Override
    public List<AiVideoSession> getGenerationHistory(Long roomId, Integer limit) {
        QueryWrapper<AiVideoSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId)
                   .orderByDesc("start_time")
                   .last("LIMIT " + (limit != null ? limit : 50));
        
        return aiVideoSessionMapper.selectList(queryWrapper);
    }
    
    /**
     * 启动AI视频生成（模拟实现）
     */
    private void startAiVideoGeneration(AiVideoSession session) {
        String streamId = session.getStreamId();
        
        try {
            // 更新状态为生成中
            updateSessionStatus(streamId, "GENERATING", 0, null);
            
            // 模拟AI视频生成过程
            for (int progress = 0; progress <= 100; progress += 10) {
                Thread.sleep(2000); // 模拟生成时间
                
                String status = progress < 100 ? "GENERATING" : "STREAMING";
                updateSessionStatus(streamId, status, progress, null);
                
                // 当进度达到50%时开始流式传输
                if (progress == 50) {
                    // 这里应该调用实际的AI视频生成服务
                    // 例如：调用 Stable Video Diffusion, RunwayML, 或其他AI视频生成API
                    log.info("AI视频生成达到50%，开始流式传输: streamId={}", streamId);
                }
            }
            
            // 生成完成，持续流式传输
            log.info("AI视频生成完成，开始持续流式传输: streamId={}", streamId);
            
            // 根据设定的时长持续流式传输
            Thread.sleep(session.getDuration() * 1000L);
            
            updateSessionStatus(streamId, "COMPLETED", 100, null);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateSessionStatus(streamId, "STOPPED", session.getProgress(), "用户停止");
        } catch (Exception e) {
            log.error("AI视频生成过程中出错: streamId={}", streamId, e);
            updateSessionStatus(streamId, "FAILED", session.getProgress(), e.getMessage());
        }
    }
    
    /**
     * 停止AI视频生成（模拟实现）
     */
    private void stopAiVideoGeneration(String streamId) {
        // 这里应该调用实际的AI视频生成服务停止接口
        log.info("停止AI视频生成进程: streamId={}", streamId);
    }
    
    /**
     * 更新会话状态
     */
    private void updateSessionStatus(String streamId, String status, Integer progress, String errorMessage) {
        AiVideoSession session = getSessionByStreamId(streamId);
        if (session != null) {
            session.setStatus(status);
            session.setProgress(progress);
            if (errorMessage != null) {
                session.setErrorMessage(errorMessage);
            }
            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status)) {
                session.setEndTime(LocalDateTime.now());
            }
            aiVideoSessionMapper.updateById(session);
        }
    }
    
    /**
     * 根据流ID获取会话
     */
    private AiVideoSession getSessionByStreamId(String streamId) {
        QueryWrapper<AiVideoSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("stream_id", streamId);
        return aiVideoSessionMapper.selectOne(queryWrapper);
    }
    
    /**
     * 计算比特率
     */
    private Integer calculateBitrate(String resolution, Integer frameRate) {
        // 根据分辨率和帧率计算合适的比特率
        if ("1920x1080".equals(resolution)) {
            return frameRate * 200; // 1080p: ~6000kbps for 30fps
        } else if ("1280x720".equals(resolution)) {
            return frameRate * 120; // 720p: ~3600kbps for 30fps
        } else {
            return frameRate * 80; // 默认: ~2400kbps for 30fps
        }
    }
}
